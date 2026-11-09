import { Button, Skeleton, Stack, Text } from '@mantine/core';
import { useQuery } from '@tanstack/react-query';
import { Link, useParams } from 'react-router';
import { StatusBadge } from '@/components/domain/StatusBadge/StatusBadge';
import { StockBar } from '@/components/domain/StockBar/StockBar';
import { StockLocations } from '@/components/domain/StockLocations/StockLocations';
import { StockSummary } from '@/components/domain/StockSummary/StockSummary';
import { PageHeader } from '@/components/layout/PageHeader/PageHeader';
import { Alert } from '@/components/ui/Alert/Alert';
import { Card } from '@/components/ui/Card/Card';
import { resumen, ubicacionesDe } from '@/features/inventario/calculos';
import { formatNumber } from '@/lib/format';
import { obtenerExistencias, obtenerProducto } from './api';
import classes from './Catalogo.module.css';

/** CU-03, consulta de producto. La ficha no ofrece edición en línea: la modificación ocurre en el formulario. */
export function ProductoFichaPage() {
  const { sku = '' } = useParams();
  const producto = useQuery({
    queryKey: ['catalogo-producto', sku],
    queryFn: () => obtenerProducto(sku),
  });
  const existencias = useQuery({
    queryKey: ['catalogo-existencias', sku],
    queryFn: () => obtenerExistencias(sku),
  });

  if (producto.isPending || existencias.isPending) return <Skeleton height={320} radius="md" />;
  if (producto.isError || existencias.isError) {
    return (
      <Alert tone="danger" title="No se pudo cargar el producto">
        Verificar la conexión e intentar de nuevo.
      </Alert>
    );
  }
  if (!producto.data || !existencias.data) {
    return (
      <Alert tone="danger" title="El producto no existe">
        <Link to="/catalogo">Volver al catálogo</Link>
      </Alert>
    );
  }

  const p = producto.data;
  const e = existencias.data;
  const r = resumen(e);

  return (
    <>
      <PageHeader
        title={p.nombre}
        breadcrumbs={[{ label: 'Catálogo', href: '/catalogo' }, { label: p.nombre }]}
        status={
          <>
            <Text component="span" ff="monospace" c="dimmed">
              {p.sku}
            </Text>{' '}
            <StatusBadge entity="registro" value={p.estado} />
          </>
        }
        actions={
          <Button component={Link} to={`/catalogo/${p.sku}/editar`}>
            Modificar
          </Button>
        }
      />
      <Stack gap="lg">
        <Card title="Datos del producto">
          <dl className={classes.ficha}>
            <dt>Categoría</dt>
            <dd>{p.categoria}</dd>
            <dt>Unidad de medida</dt>
            <dd>{p.unidad}</dd>
            <dt>Stock mínimo</dt>
            <dd>{formatNumber(p.minimo)}</dd>
            <dt>Descripción</dt>
            <dd>{p.descripcion || 'Sin descripción.'}</dd>
          </dl>
        </Card>
        <Card title="Existencias">
          <Stack gap="md">
            <StockSummary
              fisico={r.fisico}
              reservado={e.reservado}
              minimo={e.minimo}
              unidad={e.unidad}
            />
            <StockBar
              libre={Math.max(r.libre, 0)}
              reservado={e.reservado}
              unidad={e.unidad}
              minimo={e.minimo}
            />
            <StockLocations ubicaciones={ubicacionesDe(e)} unidad={e.unidad} />
          </Stack>
        </Card>
      </Stack>
    </>
  );
}
