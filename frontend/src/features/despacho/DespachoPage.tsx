import { Button, Skeleton, Text } from '@mantine/core';
import { notifications } from '@mantine/notifications';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useNavigate, useParams } from 'react-router';
import { Constancia, type CambioStock } from '@/components/domain/Constancia/Constancia';
import { StatusBadge } from '@/components/domain/StatusBadge/StatusBadge';
import { PageHeader } from '@/components/layout/PageHeader/PageHeader';
import { ActionBar } from '@/components/ui/ActionBar/ActionBar';
import { Alert } from '@/components/ui/Alert/Alert';
import { Card } from '@/components/ui/Card/Card';
import { DataTable, type Column } from '@/components/ui/DataTable/DataTable';
import { Icon } from '@/components/ui/Icon/Icon';
import { ConfirmDialog } from '@/components/ui/Modal/ConfirmDialog';
import { formatNumber } from '@/lib/format';
import { confirmarSalida, obtenerOrden, reservaCompleta } from './api';
import type { LineaDespacho } from './types';

const columns: Column<LineaDespacho>[] = [
  { key: 'sku', header: 'SKU', mono: true },
  { key: 'nombre', header: 'Producto', primary: true },
  {
    key: 'cantidad',
    header: 'A despachar',
    numeric: true,
    render: (l) => formatNumber(l.cantidad),
  },
  {
    key: 'reservado',
    header: 'Reservado',
    numeric: true,
    render: (l) => formatNumber(l.reservado),
  },
];

/** CU-06: confirmación de la salida física. El foco inicial queda en "Volver" para evitar confirmaciones accidentales. */
export function DespachoPage() {
  const { id = '' } = useParams();
  const queryClient = useQueryClient();
  const [confirming, setConfirming] = useState(false);
  /** La constancia solo existe como resultado de confirmar la salida en esta pantalla. */
  const [cambios, setCambios] = useState<CambioStock[] | null>(null);
  const navigate = useNavigate();
  const { data: orden, isError } = useQuery({
    queryKey: ['orden', id],
    queryFn: () => obtenerOrden(id),
  });

  const confirmar = useMutation({
    mutationFn: () => confirmarSalida(id),
    onSuccess: (r) => {
      setConfirming(false);
      setCambios(r.cambios);
      void queryClient.invalidateQueries({ queryKey: ['orden', id] });
      void queryClient.invalidateQueries({ queryKey: ['ordenes'] });
    },
    onError: (e) => {
      setConfirming(false);
      notifications.show({
        color: 'danger',
        title: 'No se pudo confirmar la salida',
        message: e.message,
      });
    },
  });

  if (isError) {
    return (
      <>
        <PageHeader
          title="Orden no encontrada"
          breadcrumbs={[{ label: 'Despachos', href: '/despachos' }, { label: id }]}
        />
        <Alert tone="warning" title="La orden no existe">
          Volver al listado y elegir otra orden.
        </Alert>
      </>
    );
  }
  if (!orden) return <Skeleton height={240} />;

  const completa = reservaCompleta(orden.lineas);
  const pendiente = orden.estado === 'Emitida';
  const sinReservar = orden.lineas.filter((l) => l.reservado < l.cantidad).length;

  return (
    <>
      <PageHeader
        title={`Orden de despacho ${orden.id}`}
        breadcrumbs={[{ label: 'Despachos', href: '/despachos' }, { label: orden.id }]}
        status={<StatusBadge entity="guia" value={orden.estado} />}
        description={`Venta ${orden.venta}, ${orden.cliente}, Entrega ${orden.entrega.toLowerCase()}`}
      />
      {cambios && (
        <Constancia
          sello={<StatusBadge entity="guia" value={orden.estado} variant="sello" />}
          title={`Salida de la orden ${orden.id} confirmada`}
          description={`Se registraron las salidas de la orden ${orden.id} en el Kardex.`}
          cambios={cambios}
          nota="La reserva de la venta quedó consumida por la salida."
          actions={
            <>
              <Button variant="default" onClick={() => void navigate('/despachos')}>
                Volver a despachos
              </Button>
              <Button leftSection={<Icon name="kardex" />} onClick={() => void navigate('/kardex')}>
                Ver movimientos en el Kardex
              </Button>
            </>
          }
        />
      )}
      {pendiente &&
        (completa ? (
          <Alert tone="success" title="Todas las cantidades están reservadas">
            La venta puede despacharse completa.
          </Alert>
        ) : (
          <Alert tone="warning" title="Hay cantidades sin reservar">
            La entrega completa no es posible hasta contar con la reserva total. Gestionar el
            abastecimiento antes de confirmar la salida.
          </Alert>
        ))}
      <Card title="Productos a despachar" padded={false}>
        <DataTable
          caption="Líneas de la orden"
          columns={columns}
          rows={orden.lineas}
          rowKey="sku"
        />
      </Card>
      {pendiente && (
        <ActionBar
          aria-label="Resumen y confirmación de la salida"
          summary={
            <Text size="sm" fw={600}>
              {orden.lineas.length === 1 ? '1 línea' : `${orden.lineas.length} líneas`}:{' '}
              {sinReservar === 0
                ? 'todas con la cantidad reservada'
                : `${sinReservar} sin la reserva completa`}
            </Text>
          }
          reason={
            completa
              ? null
              : 'Falta reservar cantidades. Gestionar el abastecimiento antes de confirmar la salida.'
          }
        >
          <Button variant="default" onClick={() => void navigate('/despachos')}>
            Cancelar
          </Button>
          <Button
            leftSection={<Icon name="despacho" />}
            disabled={!completa}
            onClick={() => setConfirming(true)}
          >
            Confirmar salida
          </Button>
        </ActionBar>
      )}
      <ConfirmDialog
        open={confirming}
        title="Confirmar salida de la mercadería"
        description={`Despacho completo de la venta ${orden.venta}.`}
        consequences={[
          'Se registrarán los movimientos de salida en el Kardex.',
          'La acción no se puede deshacer.',
        ]}
        confirmLabel="Confirmar salida"
        cancelLabel="Volver"
        loading={confirmar.isPending}
        onConfirm={() => confirmar.mutate()}
        onCancel={() => setConfirming(false)}
      />
    </>
  );
}
