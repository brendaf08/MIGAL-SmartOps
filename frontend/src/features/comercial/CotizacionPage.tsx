import { Button, Radio, Skeleton, Stack, Text, Textarea } from '@mantine/core';
import { notifications } from '@mantine/notifications';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router';
import { StatusBadge } from '@/components/domain/StatusBadge/StatusBadge';
import { PageHeader } from '@/components/layout/PageHeader/PageHeader';
import { Alert } from '@/components/ui/Alert/Alert';
import { Card } from '@/components/ui/Card/Card';
import { Columns } from '@/components/ui/Columns/Columns';
import { DataTable, type Column } from '@/components/ui/DataTable/DataTable';
import { Icon } from '@/components/ui/Icon/Icon';
import { ConfirmDialog } from '@/components/ui/Modal/ConfirmDialog';
import { Stepper } from '@/components/ui/Stepper/Stepper';
import { formatCurrency, formatDate, formatNumber } from '@/lib/format';
import { API_REAL } from '@/api/cliente';
import { STATUS } from '@/components/domain/StatusBadge/status';
import { tieneRol, type Role } from '@/features/seguridad/roles';
import { useSession } from '@/features/seguridad/session';
import {
  cancelarCotizacion,
  duplicarCotizacion,
  obtenerCotizacion,
  registrarAceptacion,
  registrarRechazo,
  registrarValidacion,
} from './api';
import { PASOS, pasoActual, totales } from './calculos';
import { DesgloseStock } from './DesgloseStock';
import type { LineaCotizacion } from './types';

const columns: Column<LineaCotizacion>[] = [
  { key: 'sku', header: 'SKU', mono: true },
  { key: 'nombre', header: 'Producto', primary: true },
  { key: 'cantidad', header: 'Cantidad', numeric: true, render: (l) => formatNumber(l.cantidad) },
  { key: 'precio', header: 'Precio unit.', numeric: true, render: (l) => formatCurrency(l.precio) },
  {
    key: 'importe',
    header: 'Importe',
    numeric: true,
    render: (l) => formatCurrency(l.cantidad * l.precio),
  },
];

/**
 * Roles que pueden ejecutar cada acción, según x-roles del contrato comercial y de ventas. Una acción que el backend
 * rechazaría para el rol no se muestra.
 */
const ROLES_ACCION = {
  modificar: ['Vendedor'],
  rechazar: ['Vendedor', 'Administrador'],
  cancelar: ['Vendedor', 'Administrador'],
  duplicar: ['Vendedor'],
  aceptar: ['Vendedor'],
  validar: ['Administrador'],
  convertir: ['Vendedor'],
} satisfies Record<string, Role[]>;

/** CU-09: detalle de una cotización con su ciclo de vida, líneas, totales, cliente y condiciones. */
export function CotizacionPage() {
  const { id = '' } = useParams();
  const { user } = useSession();
  const puede = (roles: Role[]) => tieneRol(user?.role, roles);
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const { data: cotizacion, isError } = useQuery({
    queryKey: ['cotizacion', id],
    queryFn: () => obtenerCotizacion(id),
  });
  const [rechazando, setRechazando] = useState(false);
  const [aceptando, setAceptando] = useState(false);
  const [motivoRechazo, setMotivoRechazo] = useState('');
  // Cada apertura del diálogo inicia una intención nueva; sus reintentos reutilizan la misma clave.
  const [claveAceptacion, setClaveAceptacion] = useState(() => crypto.randomUUID());
  const [claveRechazo, setClaveRechazo] = useState(() => crypto.randomUUID());
  const aceptar = useMutation({
    mutationFn: () => registrarAceptacion(id, claveAceptacion),
    onSuccess: () => {
      setAceptando(false);
      void queryClient.invalidateQueries({ queryKey: ['cotizacion', id] });
      void queryClient.invalidateQueries({ queryKey: ['cotizaciones'] });
      notifications.show({
        color: 'success',
        title: 'Aceptación registrada',
        message: `La cotización ${id} quedó aceptada por el cliente.`,
      });
    },
    onError: (e) => {
      setAceptando(false);
      notifications.show({
        color: 'danger',
        title: 'No se pudo registrar la aceptación',
        message: e.message,
      });
    },
  });
  const [cancelando, setCancelando] = useState(false);
  const [motivoCancelacion, setMotivoCancelacion] = useState('');
  const [origenCancelacion, setOrigenCancelacion] = useState<'cliente' | 'migal'>('cliente');
  const [claveCancelacion, setClaveCancelacion] = useState(() => crypto.randomUUID());
  const cancelar = useMutation({
    mutationFn: () =>
      cancelarCotizacion(id, origenCancelacion, motivoCancelacion, claveCancelacion),
    onSuccess: () => {
      setCancelando(false);
      void queryClient.invalidateQueries({ queryKey: ['cotizacion', id] });
      void queryClient.invalidateQueries({ queryKey: ['cotizaciones'] });
      notifications.show({
        color: 'success',
        title: 'Cotización cancelada',
        message: `La cotización ${id} quedó cancelada.`,
      });
    },
    onError: (e) => {
      setCancelando(false);
      notifications.show({
        color: 'danger',
        title: 'No se pudo cancelar la cotización',
        message: e.message,
      });
    },
  });
  const [claveDuplicado, setClaveDuplicado] = useState(() => crypto.randomUUID());
  const duplicar = useMutation({
    mutationFn: () => duplicarCotizacion(id, claveDuplicado),
    onSuccess: (nueva) => {
      setClaveDuplicado(crypto.randomUUID());
      void queryClient.invalidateQueries({ queryKey: ['cotizaciones'] });
      notifications.show({
        color: 'success',
        title: 'Cotización duplicada',
        message: `Se emitió la cotización ${nueva.id} con los mismos productos.`,
      });
      void navigate(`/cotizaciones/${nueva.id}`);
    },
    onError: (e) => {
      notifications.show({
        color: 'danger',
        title: 'No se pudo duplicar la cotización',
        message: e.message,
      });
    },
  });
  const [validando, setValidando] = useState(false);
  const [claveValidacion, setClaveValidacion] = useState(() => crypto.randomUUID());
  const validar = useMutation({
    mutationFn: () => registrarValidacion(id, claveValidacion),
    onSuccess: () => {
      setValidando(false);
      void queryClient.invalidateQueries({ queryKey: ['cotizacion', id] });
      void queryClient.invalidateQueries({ queryKey: ['cotizaciones'] });
      notifications.show({
        color: 'success',
        title: 'Validación registrada',
        message: `La cotización ${id} quedó validada por MIGAL.`,
      });
    },
    onError: (e) => {
      setValidando(false);
      notifications.show({
        color: 'danger',
        title: 'No se pudo validar la cotización',
        message: e.message,
      });
    },
  });
  const rechazar = useMutation({
    mutationFn: () => registrarRechazo(id, motivoRechazo, claveRechazo),
    onSuccess: () => {
      setRechazando(false);
      void queryClient.invalidateQueries({ queryKey: ['cotizacion', id] });
      void queryClient.invalidateQueries({ queryKey: ['cotizaciones'] });
      notifications.show({
        color: 'success',
        title: 'Rechazo registrado',
        message: `La cotización ${id} quedó rechazada por el cliente.`,
      });
    },
    onError: (e) => {
      setRechazando(false);
      notifications.show({
        color: 'danger',
        title: 'No se pudo registrar el rechazo',
        message: e.message,
      });
    },
  });

  if (isError) {
    return (
      <>
        <PageHeader
          title="Cotización no encontrada"
          breadcrumbs={[{ label: 'Cotizaciones', href: '/cotizaciones' }, { label: id }]}
        />
        <Alert tone="warning" title="La cotización no existe">
          Volver al listado y elegir otra cotización.
        </Alert>
      </>
    );
  }
  if (!cotizacion) return <Skeleton height={240} />;

  const t = totales(cotizacion);
  const { cliente } = cotizacion;
  const abierta =
    cotizacion.estado === 'Emitida' ||
    cotizacion.estado === 'AceptadaCliente' ||
    cotizacion.estado === 'ValidadaMigal';
  const sinVenta = !cotizacion.ventaId;
  const cerrada = ['Rechazada', 'Cancelada', 'Vencida'].includes(cotizacion.estado);

  return (
    <>
      <PageHeader
        title={`Cotización ${cotizacion.id}`}
        breadcrumbs={[{ label: 'Cotizaciones', href: '/cotizaciones' }, { label: cotizacion.id }]}
        status={<StatusBadge entity="cotizacion" value={cotizacion.estado} variant="sello" />}
        description={`Emitida el ${formatDate(cotizacion.emision)}, vence el ${formatDate(cotizacion.vencimiento)}`}
        actions={
          <>
            <Button
              variant="default"
              leftSection={<Icon name="print" />}
              component={Link}
              to={`/cotizaciones/${id}/imprimir`}
            >
              Imprimir
            </Button>
            {abierta && sinVenta && puede(ROLES_ACCION.modificar) && (
              <Button variant="default" component={Link} to={`/cotizaciones/${id}/editar`}>
                Modificar
              </Button>
            )}
            {(cotizacion.estado === 'AceptadaCliente' || cotizacion.estado === 'ValidadaMigal') &&
              sinVenta &&
              puede(ROLES_ACCION.cancelar) && (
                <Button
                  variant="default"
                  color="danger"
                  leftSection={<Icon name="x-circle" />}
                  onClick={() => {
                    setClaveCancelacion(crypto.randomUUID());
                    setMotivoCancelacion('');
                    setOrigenCancelacion('cliente');
                    setCancelando(true);
                  }}
                >
                  Cancelar cotización
                </Button>
              )}
            {cerrada && puede(ROLES_ACCION.duplicar) && (
              <Button
                variant="default"
                loading={duplicar.isPending}
                onClick={() => duplicar.mutate()}
              >
                Duplicar cotización
              </Button>
            )}
            {cotizacion.estado === 'Emitida' && (
              <>
                {puede(ROLES_ACCION.rechazar) && (
                  <Button
                    variant="default"
                    color="danger"
                    leftSection={<Icon name="x-circle" />}
                    onClick={() => {
                      setClaveRechazo(crypto.randomUUID());
                      setMotivoRechazo('');
                      setRechazando(true);
                    }}
                  >
                    Registrar rechazo
                  </Button>
                )}
                {puede(ROLES_ACCION.aceptar) && (
                  <Button
                    leftSection={<Icon name="check" />}
                    onClick={() => {
                      setClaveAceptacion(crypto.randomUUID());
                      setAceptando(true);
                    }}
                  >
                    Registrar aceptación
                  </Button>
                )}
              </>
            )}
            {cotizacion.estado === 'AceptadaCliente' && puede(ROLES_ACCION.validar) && (
              <Button
                variant={API_REAL ? 'filled' : 'default'}
                leftSection={<Icon name="check" />}
                onClick={() => {
                  setClaveValidacion(crypto.randomUUID());
                  setValidando(true);
                }}
              >
                Validar viabilidad
              </Button>
            )}
            {(cotizacion.estado === 'ValidadaMigal' ||
              (cotizacion.estado === 'AceptadaCliente' && !API_REAL)) &&
              puede(ROLES_ACCION.convertir) && (
                <Button component={Link} to={`/ventas/nueva?cotizacion=${id}`}>
                  Convertir en venta
                </Button>
              )}
          </>
        }
      />
      <Stack gap="lg">
        <Stepper
          steps={PASOS}
          current={pasoActual(cotizacion.estado)}
          aria-label="Ciclo de la cotización"
        />
        <Columns layout="main-aside">
          <Stack>
            <Card title="Productos cotizados" padded={false}>
              <DataTable
                caption="Líneas de la cotización"
                columns={columns}
                rows={cotizacion.lineas}
                rowKey="id"
                footer={[
                  { precio: 'Subtotal', importe: formatCurrency(t.subtotal) },
                  ...(cotizacion.descuento
                    ? [
                        {
                          precio: `Descuento (${formatNumber(cotizacion.descuento * 100)} %)`,
                          importe: `− ${formatCurrency(t.descuento)}`,
                        },
                      ]
                    : []),
                  { precio: 'Total', importe: formatCurrency(t.total) },
                ]}
              />
            </Card>
            {cotizacion.ventaId && (
              <Alert tone="info" title="Esta cotización ya originó una venta">
                Ya no se modifica ni se cancela aquí. Para cambiarla, cancelar la venta (hasta que
                salga del almacén) y emitir una cotización nueva.{' '}
                <Link to={`/ventas/${cotizacion.ventaId}`}>Ver la venta</Link>
              </Alert>
            )}
            {abierta && <DesgloseStock lineas={cotizacion.lineas} />}
            {cotizacion.rechazo && (
              <Alert
                tone="warning"
                title={
                  cotizacion.estado === 'Cancelada'
                    ? `Cancelada por ${cotizacion.rechazo.origen === 'migal' ? 'MIGAL' : 'el cliente'}`
                    : `Rechazada por ${cotizacion.rechazo.origen === 'migal' ? 'MIGAL' : 'el cliente'}`
                }
              >
                {cotizacion.rechazo.motivo}
              </Alert>
            )}
          </Stack>
          <Stack>
            <Card title="Cliente">
              {cliente ? (
                <Stack gap={4}>
                  <Text fw={600}>{cliente.nombre}</Text>
                  {cliente.ruc && <Text ff="monospace">RUC {cliente.ruc}</Text>}
                  {cliente.direccion && (
                    <Text size="sm" c="dimmed">
                      {cliente.direccion}
                    </Text>
                  )}
                </Stack>
              ) : (
                <Text c="dimmed">Cotización sin cliente identificado.</Text>
              )}
            </Card>
            <Card title="Condiciones">
              <Stack gap={4}>
                <Text>Vigencia hasta {formatDate(cotizacion.vencimiento)}</Text>
                <Text c="dimmed">
                  {cotizacion.condiciones ?? 'Sin condiciones comerciales adicionales.'}
                </Text>
              </Stack>
            </Card>
            {cotizacion.historial && cotizacion.historial.length > 0 && (
              <Card title="Historial de cambios">
                <Stack gap="sm" component="ul" m={0} p={0} style={{ listStyle: 'none' }}>
                  {cotizacion.historial.map((h) => (
                    <li key={`${h.fecha}-${h.estadoNuevo}`}>
                      <Text size="sm" fw={600}>
                        {STATUS.cotizacion[h.estadoAnterior][1]} →{' '}
                        {STATUS.cotizacion[h.estadoNuevo][1]}
                      </Text>
                      <Text size="sm">{h.motivo}</Text>
                      <Text size="xs" c="dimmed">
                        {h.usuario}, {formatDate(h.fecha)}
                      </Text>
                    </li>
                  ))}
                </Stack>
              </Card>
            )}
          </Stack>
        </Columns>
      </Stack>
      <ConfirmDialog
        open={aceptando}
        title={`Registrar la aceptación de la cotización ${cotizacion.id}`}
        description="El cliente acepta la propuesta. La cotización podrá convertirse en venta."
        consequences={[
          'La cotización queda en estado Aceptada por el cliente.',
          'El stock no se modifica hasta registrar la venta.',
        ]}
        confirmLabel="Registrar aceptación"
        loading={aceptar.isPending}
        onConfirm={() => aceptar.mutate()}
        onCancel={() => setAceptando(false)}
      />
      <ConfirmDialog
        open={validando}
        title={`Validar la cotización ${cotizacion.id}`}
        description="MIGAL confirma la viabilidad comercial de la cotización aceptada."
        consequences={[
          'La cotización queda en estado Validada por MIGAL.',
          'Podrá convertirse en venta mientras no venza.',
        ]}
        confirmLabel="Validar"
        loading={validar.isPending}
        onConfirm={() => validar.mutate()}
        onCancel={() => setValidando(false)}
      />
      <ConfirmDialog
        open={cancelando}
        title={`Cancelar la cotización ${cotizacion.id}`}
        description="La cotización ya estaba aceptada y se deja sin efecto. No se puede reactivar: se podrá duplicar."
        consequences={['La cotización queda en estado Cancelada.', 'El stock no se modifica.']}
        tone="danger"
        confirmLabel="Cancelar cotización"
        loading={cancelar.isPending}
        onConfirm={() => cancelar.mutate()}
        onCancel={() => setCancelando(false)}
      >
        <Radio.Group
          mt="md"
          label="¿Quién desiste?"
          value={origenCancelacion}
          onChange={(v) => setOrigenCancelacion(v as 'cliente' | 'migal')}
        >
          <Stack gap={4} mt={4}>
            <Radio value="cliente" label="El cliente" />
            {puede(['Administrador']) && <Radio value="migal" label="MIGAL" />}
          </Stack>
        </Radio.Group>
        <Textarea
          mt="md"
          label="Motivo de la cancelación"
          description="Obligatorio."
          autosize
          minRows={2}
          maxLength={500}
          value={motivoCancelacion}
          onChange={(e) => setMotivoCancelacion(e.currentTarget.value)}
        />
      </ConfirmDialog>
      <ConfirmDialog
        open={rechazando}
        title={`Registrar el rechazo de la cotización ${cotizacion.id}`}
        description="El cliente no acepta la cotización. No se puede volver a aceptar."
        consequences={['La cotización queda en estado Rechazada.']}
        tone="danger"
        confirmLabel="Registrar rechazo"
        loading={rechazar.isPending}
        onConfirm={() => rechazar.mutate()}
        onCancel={() => setRechazando(false)}
      >
        <Textarea
          mt="md"
          label="Motivo del rechazo"
          description="Obligatorio para registrar el rechazo en el servidor."
          autosize
          minRows={2}
          maxLength={500}
          value={motivoRechazo}
          onChange={(e) => setMotivoRechazo(e.currentTarget.value)}
        />
      </ConfirmDialog>
    </>
  );
}
