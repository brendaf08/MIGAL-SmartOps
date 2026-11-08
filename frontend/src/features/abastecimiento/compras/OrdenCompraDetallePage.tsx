import { Anchor, Button, Skeleton, Stack, Text } from '@mantine/core';
import { useQuery } from '@tanstack/react-query';
import { Link, useNavigate, useParams } from 'react-router';
import { StatusBadge } from '@/components/domain/StatusBadge/StatusBadge';
import { PageHeader } from '@/components/layout/PageHeader/PageHeader';
import { Alert } from '@/components/ui/Alert/Alert';
import { Card } from '@/components/ui/Card/Card';
import { DataTable, type Column } from '@/components/ui/DataTable/DataTable';
import { Icon } from '@/components/ui/Icon/Icon';
import { formatCurrency, formatDate, formatNumber } from '@/lib/format';
import { useSession } from '@/features/seguridad/session';
import { obtenerOrdenCompra, subtotalLineaOrden, totalOrden } from './api';
import type { LineaOrdenCompra } from '../tipos';

const columns: Column<LineaOrdenCompra>[] = [
  { key: 'sku', header: 'SKU', mono: true },
  { key: 'nombre', header: 'Producto', primary: true },
  { key: 'cantidad', header: 'Pedida', numeric: true, render: (l) => formatNumber(l.cantidad) },
  { key: 'recibido', header: 'Recibida', numeric: true, render: (l) => formatNumber(l.recibido) },
  {
    key: 'pendiente',
    header: 'Pendiente',
    numeric: true,
    render: (l) => formatNumber(l.cantidad - l.recibido),
  },
  { key: 'precio', header: 'Precio', numeric: true, render: (l) => formatCurrency(l.precio) },
  {
    key: 'subtotal',
    header: 'Subtotal',
    numeric: true,
    render: (l) => formatCurrency(subtotalLineaOrden(l)),
  },
];

/** P25 (detalle): orden de compra con sus líneas, avance de recepción y acción de recepción (CU-12). */
export function OrdenCompraDetallePage() {
  const { user } = useSession();
  const { id = '' } = useParams();
  const navigate = useNavigate();
  const { data, isLoading, isError } = useQuery({
    queryKey: ['orden-compra', id],
    queryFn: () => obtenerOrdenCompra(id),
  });

  if (isLoading) return <Skeleton height={160} />;

  if (isError || !data) {
    return (
      <>
        <PageHeader
          title="Orden de compra"
          breadcrumbs={[{ label: 'Órdenes de compra', href: '/compras' }, { label: id }]}
        />
        <Alert tone="danger" title="La orden de compra no existe">
          <Text size="sm">
            Verificar el número de la orden.{' '}
            <Anchor component={Link} to="/compras">
              Volver a las órdenes de compra
            </Anchor>
          </Text>
        </Alert>
      </>
    );
  }

  const { orden, proveedor } = data;
  const total = totalOrden(orden.lineas);
  const completada = orden.estado === 'Completada';

  return (
    <>
      <PageHeader
        title={`Orden de compra ${orden.id}`}
        description={`${proveedor.razonSocial}, emitida el ${formatDate(orden.fecha)}`}
        breadcrumbs={[{ label: 'Órdenes de compra', href: '/compras' }, { label: orden.id }]}
        status={<StatusBadge entity="compra" value={orden.estado} variant="sello" />}
        actions={
          completada || user?.role !== 'Almacenero' ? undefined : (
            <Button
              leftSection={<Icon name="recepcion" />}
              onClick={() => void navigate(`/recepciones/nueva?oc=${orden.id}`)}
            >
              Registrar recepción
            </Button>
          )
        }
      />
      <Stack gap="lg">
        <Card title="Líneas de la orden" padded={false}>
          <DataTable
            caption={`Líneas de la orden ${orden.id}`}
            columns={columns}
            rows={orden.lineas}
            rowKey="sku"
            footer={[{ nombre: 'Total de la orden', subtotal: formatCurrency(total) }]}
          />
        </Card>
        <Card title="Proveedor">
          <Text size="sm">
            {proveedor.razonSocial}. RUC {proveedor.ruc}. Contacto: {proveedor.contacto}.
            Condiciones: {proveedor.condiciones}.
          </Text>
        </Card>
      </Stack>
    </>
  );
}
