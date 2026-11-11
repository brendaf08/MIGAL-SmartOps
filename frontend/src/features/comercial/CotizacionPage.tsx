import { Button, Skeleton, Stack, Text, Textarea } from '@mantine/core';
import { notifications } from '@mantine/notifications';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { Link, useParams } from 'react-router';
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
import { obtenerCotizacion, registrarAceptacion, registrarRechazo } from './api';
import { PASOS, pasoActual, totales } from './calculos';
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

/** CU-09: detalle de una cotización con su ciclo de vida, líneas, totales, cliente y condiciones. */
export function CotizacionPage() {
  const { id = '' } = useParams();
  const queryClient = useQueryClient();
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
              onClick={() => window.print()}
            >
              Imprimir
            </Button>
            {cotizacion.estado === 'Emitida' && (
              <>
                <Button variant="default" component={Link} to={`/cotizaciones/${id}/editar`}>
                  Modificar
                </Button>
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
                <Button
                  leftSection={<Icon name="check" />}
                  onClick={() => {
                    setClaveAceptacion(crypto.randomUUID());
                    setAceptando(true);
                  }}
                >
                  Registrar aceptación
                </Button>
              </>
            )}
            {cotizacion.estado === 'AceptadaCliente' && (
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
            <Alert title="Esta cotización no reserva existencias">
              El stock se consulta y se reserva al convertirla en venta, cuando el cliente la acepta
              y MIGAL valida su viabilidad.
            </Alert>
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
          label="Motivo del rechazo (opcional)"
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
