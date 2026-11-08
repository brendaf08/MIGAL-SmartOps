import { Button, Skeleton, Stack, Text } from '@mantine/core';
import { notifications } from '@mantine/notifications';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useParams } from 'react-router';
import { StatusBadge } from '@/components/domain/StatusBadge/StatusBadge';
import { PageHeader } from '@/components/layout/PageHeader/PageHeader';
import { Alert } from '@/components/ui/Alert/Alert';
import { Card } from '@/components/ui/Card/Card';
import { Columns } from '@/components/ui/Columns/Columns';
import { DataTable, type Column } from '@/components/ui/DataTable/DataTable';
import { Icon } from '@/components/ui/Icon/Icon';
import { Stepper } from '@/components/ui/Stepper/Stepper';
import { formatCurrency, formatDate, formatNumber } from '@/lib/format';
import { obtenerCotizacion, registrarAceptacion } from './api';
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
  const aceptar = useMutation({
    mutationFn: () => registrarAceptacion(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['cotizacion', id] });
      void queryClient.invalidateQueries({ queryKey: ['cotizaciones'] });
      notifications.show({
        color: 'success',
        title: 'Aceptación registrada',
        message: `La cotización ${id} quedó aceptada por el cliente.`,
      });
    },
    onError: (e) =>
      notifications.show({
        color: 'danger',
        title: 'No se pudo registrar la aceptación',
        message: e.message,
      }),
  });

  if (isError) {
    return (
      <>
        <PageHeader
          title="Cotización no encontrada"
          breadcrumbs={[{ label: 'Cotizaciones y ventas', href: '/ventas' }, { label: id }]}
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
        breadcrumbs={[
          { label: 'Cotizaciones y ventas', href: '/ventas' },
          { label: cotizacion.id },
        ]}
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
              <Button
                leftSection={<Icon name="check" />}
                loading={aceptar.isPending}
                onClick={() => aceptar.mutate()}
              >
                Registrar aceptación
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
                  <Text ff="monospace">RUC {cliente.ruc}</Text>
                  <Text size="sm" c="dimmed">
                    {cliente.direccion}
                  </Text>
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
    </>
  );
}
