import { Anchor, Button, Skeleton, Text, Textarea } from '@mantine/core';
import { notifications } from '@mantine/notifications';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router';
import { Constancia, type CambioStock } from '@/components/domain/Constancia/Constancia';
import { StatusBadge } from '@/components/domain/StatusBadge/StatusBadge';
import { StockBar } from '@/components/domain/StockBar/StockBar';
import { PageHeader } from '@/components/layout/PageHeader/PageHeader';
import { ActionBar } from '@/components/ui/ActionBar/ActionBar';
import { Alert } from '@/components/ui/Alert/Alert';
import { Card } from '@/components/ui/Card/Card';
import { DataTable, type Column } from '@/components/ui/DataTable/DataTable';
import { Icon } from '@/components/ui/Icon/Icon';
import { ConfirmDialog } from '@/components/ui/Modal/ConfirmDialog';
import { CLIENTES } from '@/features/comercial/clientes/fixtures';
import { PRODUCTOS } from '@/features/inventario/fixtures';
import { formatCurrency, formatDate, formatNumber } from '@/lib/format';
import {
  ESTADOS_ACTIVOS,
  ESTADOS_COMPLETABLES,
  cancelarVenta,
  completarVenta,
  existenciaDe,
  obtenerVenta,
  pendienteDe,
  planAtencion,
  tienePendientes,
  totalesVenta,
  unidadesPendientes,
  type ResultadoCompletar,
} from './api';
import type { LineaVenta } from './tipos';

const unidadDe = (sku: string) => PRODUCTOS.find((p) => p.sku === sku)?.unidad ?? 'unidad';

const columns: Column<LineaVenta>[] = [
  { key: 'sku', header: 'SKU', mono: true },
  { key: 'nombre', header: 'Producto', primary: true },
  {
    key: 'solicitada',
    header: 'Solicitada',
    numeric: true,
    render: (l) => formatNumber(l.solicitada),
  },
  { key: 'atendida', header: 'Atendida', numeric: true, render: (l) => formatNumber(l.atendida) },
  {
    key: 'pendiente',
    header: 'Pendiente',
    numeric: true,
    render: (l) => formatNumber(pendienteDe(l)),
  },
  {
    key: 'subtotal',
    header: 'Subtotal',
    numeric: true,
    render: (l) => formatCurrency(l.solicitada * l.precio),
  },
];

/**
 * CU-10: detalle de la venta, con las modalidades completa, parcial y pendiente. Completar atiende lo posible
 * con el stock libre; cancelar exige confirmación y no está disponible en ventas despachadas.
 */
export function VentaDetallePage() {
  const { id = '' } = useParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [dialogo, setDialogo] = useState<'completar' | 'cancelar' | null>(null);
  const [motivo, setMotivo] = useState('');
  // Cada apertura de un diálogo inicia una intención nueva; sus reintentos reutilizan la misma clave.
  const [clave, setClave] = useState(() => crypto.randomUUID());
  const abrir = (accion: 'completar' | 'cancelar') => {
    setClave(crypto.randomUUID());
    setMotivo('');
    setDialogo(accion);
  };
  /** La constancia solo existe como resultado de completar la atención en esta pantalla. */
  const [resultado, setResultado] = useState<ResultadoCompletar | null>(null);
  const { data: venta, isError } = useQuery({
    queryKey: ['venta', id],
    queryFn: () => obtenerVenta(id),
  });

  const actualizar = () => {
    void queryClient.invalidateQueries({ queryKey: ['venta', id] });
    void queryClient.invalidateQueries({ queryKey: ['ventas'] });
  };

  const completar = useMutation({
    mutationFn: () => completarVenta(id, clave),
    onSuccess: (r) => {
      setDialogo(null);
      setResultado(r);
      actualizar();
    },
    onError: (e) => {
      setDialogo(null);
      notifications.show({
        color: 'danger',
        title: 'No se pudo completar la venta',
        message: e.message,
      });
    },
  });

  const cancelar = useMutation({
    mutationFn: () => cancelarVenta(id, motivo, clave),
    onSuccess: () => {
      setDialogo(null);
      setResultado(null);
      actualizar();
      notifications.show({ title: 'Venta cancelada', message: `La venta ${id} se canceló.` });
    },
    onError: (e) => {
      setDialogo(null);
      notifications.show({
        color: 'danger',
        title: 'No se pudo cancelar la venta',
        message: e.message,
      });
    },
  });

  if (isError) {
    return (
      <>
        <PageHeader
          title="Venta no encontrada"
          breadcrumbs={[{ label: 'Ventas', href: '/ventas' }, { label: id }]}
        />
        <Alert tone="warning" title="La venta no existe">
          <Anchor component={Link} to="/ventas">
            Volver al listado de ventas
          </Anchor>
        </Alert>
      </>
    );
  }
  if (!venta) return <Skeleton height={240} />;

  const cliente =
    venta.clienteNombre ?? CLIENTES.find((c) => c.id === venta.clienteId)?.nombre ?? 'Sin cliente';
  const totales = totalesVenta(venta);
  const activa = ESTADOS_ACTIVOS.includes(venta.estado);
  const pendientes = tienePendientes(venta);
  const plan = planAtencion(venta);
  const atendible = plan.reduce((suma, a) => suma + a.atendible, 0);
  const completable = pendientes && ESTADOS_COMPLETABLES.includes(venta.estado);
  const sinStock = completable && atendible === 0;
  const atendidas = venta.lineas.reduce((suma, l) => suma + l.atendida, 0);
  const solicitadas = venta.lineas.reduce((suma, l) => suma + l.solicitada, 0);
  const cubreTodo = plan.every((a) => a.atendible === a.pendiente);
  const atendidasAhora = (resultado?.atencion ?? []).reduce((suma, a) => suma + a.atendible, 0);

  /** Existencia libre antes y después de la atención de esta sesión. */
  const cambios: CambioStock[] = (resultado?.atencion ?? [])
    .filter((a) => a.atendible > 0)
    .map((a) => ({
      producto: a.nombre,
      ubicacion: 'Stock libre',
      antes: a.libre,
      despues: a.libre - a.atendible,
      unidad: unidadDe(a.sku),
    }));

  return (
    <>
      <PageHeader
        title={`Venta ${venta.id}`}
        breadcrumbs={[{ label: 'Ventas', href: '/ventas' }, { label: venta.id }]}
        status={<StatusBadge entity="venta" value={venta.estado} variant="sello" />}
        description={`${cliente}, ${formatDate(venta.fecha)}`}
      />
      {resultado && (
        <Constancia
          sello={<StatusBadge entity="venta" value={resultado.venta.estado} variant="sello" />}
          title={
            resultado.venta.estado === 'ListaDespacho'
              ? `Venta ${venta.id} lista para despacho`
              : `Venta ${venta.id} atendida en parte`
          }
          description={`Se atendieron ${formatNumber(atendidasAhora)} unidades con el stock libre.`}
          cambios={cambios}
          nota={
            unidadesPendientes(resultado.venta) > 0
              ? `Quedan pendientes ${formatNumber(unidadesPendientes(resultado.venta))} unidades de la venta.`
              : 'La venta no tiene cantidades pendientes.'
          }
          actions={<Button onClick={() => void navigate('/ventas')}>Ver ventas</Button>}
        />
      )}
      <Card title="Productos" padded={false}>
        <DataTable
          caption="Productos de la venta"
          columns={columns}
          rows={venta.lineas}
          rowKey="sku"
          footer={[
            { nombre: 'Subtotal', subtotal: formatCurrency(totales.subtotal) },
            { nombre: 'Descuento', subtotal: `- ${formatCurrency(totales.descuento)}` },
            { nombre: 'Total', subtotal: formatCurrency(totales.total) },
          ]}
        />
      </Card>
      {pendientes && (
        <Alert tone="warning" title="Hay cantidades pendientes de atención">
          <Text size="sm">
            Atención parcial: se atendieron {formatNumber(atendidas)} de {formatNumber(solicitadas)}{' '}
            unidades; las {formatNumber(unidadesPendientes(venta))} restantes quedan pendientes.
          </Text>
          {plan.map((a) => (
            <StockBar
              key={a.sku}
              compact
              libre={existenciaDe(a.sku).libre}
              reservado={existenciaDe(a.sku).reservado}
              unidad={unidadDe(a.sku)}
              label={`Disponibilidad de ${a.nombre}`}
            />
          ))}
        </Alert>
      )}
      {activa && (
        <ActionBar
          aria-label="Acciones de la venta"
          summary={
            <Text size="sm" fw={600}>
              {pendientes
                ? `${formatNumber(unidadesPendientes(venta))} unidades pendientes`
                : 'Sin cantidades pendientes'}
            </Text>
          }
          reason={sinStock ? 'Sin stock disponible para las unidades pendientes.' : null}
        >
          <Button
            variant="outline"
            color="danger"
            leftSection={<Icon name="status-danger" />}
            onClick={() => abrir('cancelar')}
          >
            Cancelar venta
          </Button>
          {completable && (
            <Button
              leftSection={<Icon name="check" />}
              disabled={sinStock}
              onClick={() => abrir('completar')}
            >
              Completar venta
            </Button>
          )}
        </ActionBar>
      )}
      <ConfirmDialog
        open={dialogo === 'completar'}
        title={`Completar la venta ${venta.id}`}
        description={plan
          .filter((a) => a.atendible > 0)
          .map(
            (a) =>
              `${formatNumber(a.atendible)} de ${formatNumber(a.pendiente)} unidades de ${a.sku}`,
          )
          .join('; ')}
        consequences={[
          'Las unidades atendidas se reservan para esta venta.',
          cubreTodo
            ? 'La venta pasa a lista para despacho.'
            : 'Las unidades sin stock siguen pendientes.',
        ]}
        confirmLabel="Completar venta"
        cancelLabel="Volver"
        loading={completar.isPending}
        onConfirm={() => completar.mutate()}
        onCancel={() => setDialogo(null)}
      />
      <ConfirmDialog
        open={dialogo === 'cancelar'}
        tone="danger"
        title={`Cancelar la venta ${venta.id}`}
        consequences={['Se liberan las reservas de la venta.', 'La acción no se puede deshacer.']}
        confirmLabel="Cancelar venta"
        cancelLabel="Volver"
        loading={cancelar.isPending}
        onConfirm={() => cancelar.mutate()}
        onCancel={() => setDialogo(null)}
      >
        <Textarea
          mt="md"
          label="Motivo de la cancelación"
          description="Obligatorio para registrar la cancelación en el servidor."
          autosize
          minRows={2}
          maxLength={500}
          value={motivo}
          onChange={(e) => setMotivo(e.currentTarget.value)}
        />
      </ConfirmDialog>
    </>
  );
}
