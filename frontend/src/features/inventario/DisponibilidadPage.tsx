import { Alert, Button } from '@mantine/core';
import { useDebouncedValue } from '@mantine/hooks';
import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { StatusBadge } from '@/components/domain/StatusBadge/StatusBadge';
import { PageHeader } from '@/components/layout/PageHeader/PageHeader';
import { DataTable, type Column } from '@/components/ui/DataTable/DataTable';
import { EmptyState } from '@/components/ui/EmptyState/EmptyState';
import { Pagination } from '@/components/ui/Pagination/Pagination';
import { SearchInput } from '@/components/ui/SearchInput/SearchInput';
import { Tabs } from '@/components/ui/Tabs/Tabs';
import { formatNumber } from '@/lib/format';
import { consultarDisponibilidad } from './api';
import { resumen } from './calculos';
import classes from './DisponibilidadPage.module.css';
import { ProductoDetalle } from './ProductoDetalle';
import type { ProductoDisponibilidad } from './types';

const PAGE_SIZE = 20;

/** Columna numérica de una ubicación; en teléfono se omite y se consulta en el detalle. */
const cantidad = (
  key: 'tienda' | 'almacen1' | 'almacen2' | 'reservado',
  header: string,
): Column<ProductoDisponibilidad> => ({
  key,
  header,
  numeric: true,
  phone: false,
  render: (p) => formatNumber(p[key]),
});

/** En teléfono solo se ven el producto, la cantidad libre y el nivel. */
const columns: Column<ProductoDisponibilidad>[] = [
  { key: 'sku', header: 'SKU', mono: true },
  {
    key: 'nombre',
    header: 'Producto',
    primary: true,
    render: (p) => (
      <>
        <div>{p.nombre}</div>
        <small className={classes.category}>{p.categoria}</small>
      </>
    ),
  },
  cantidad('tienda', 'Tienda'),
  cantidad('almacen1', 'Almacén 1'),
  cantidad('almacen2', 'Almacén 2'),
  {
    key: 'fisico',
    header: 'Físico',
    numeric: true,
    phone: false,
    render: (p) => formatNumber(resumen(p).fisico),
  },
  cantidad('reservado', 'Reservado'),
  {
    key: 'libre',
    header: 'Libre',
    numeric: true,
    render: (p) => <b>{formatNumber(resumen(p).libre)}</b>,
  },
  {
    key: 'nivel',
    header: 'Nivel',
    render: (p) => <StatusBadge entity="stock" value={resumen(p).nivel} />,
  },
];

/** CU-04: consulta de disponibilidad por ubicación, con búsqueda, filtro de alerta y paginación en el servidor. */
export function DisponibilidadPage() {
  const [tab, setTab] = useState('todos');
  const [q, setQ] = useState('');
  const [page, setPage] = useState(1);
  const [selected, setSelected] = useState<ProductoDisponibilidad | null>(null);
  const [debouncedQ] = useDebouncedValue(q, 300);
  const soloAlerta = tab === 'alerta';

  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ['disponibilidad', { q: debouncedQ, soloAlerta, page }],
    queryFn: () =>
      consultarDisponibilidad({ q: debouncedQ, soloAlerta, page, pageSize: PAGE_SIZE }),
    placeholderData: keepPreviousData,
  });

  /** Cambiar el filtro o la búsqueda vuelve a la primera página. */
  const filter = (nextTab: string, nextQ: string) => {
    setTab(nextTab);
    setQ(nextQ);
    setPage(1);
  };

  return (
    <>
      <PageHeader
        title="Disponibilidad"
        description="Existencias físicas, reservadas y libres por ubicación."
      />
      <div className={classes.toolbar}>
        <Tabs
          aria-label="Filtro de disponibilidad"
          value={tab}
          onChange={(v) => filter(v, q)}
          items={[
            { value: 'todos', label: 'Todos', count: data?.totalGeneral },
            { value: 'alerta', label: 'En alerta', count: data?.enAlerta, icon: 'alert-triangle' },
          ]}
        />
        <SearchInput
          className={classes.search}
          aria-label="Buscar producto"
          placeholder="Nombre o SKU"
          value={q}
          onChange={(v) => filter(tab, v)}
        />
      </div>
      {isError ? (
        <Alert color="danger" title="No se pudo consultar la disponibilidad" role="alert">
          Verificar la conexión e intentar de nuevo.{' '}
          <Button variant="subtle" size="compact-sm" onClick={() => void refetch()}>
            Reintentar
          </Button>
        </Alert>
      ) : (
        <>
          <DataTable
            caption="Disponibilidad por producto"
            columns={columns}
            rows={data?.items ?? []}
            rowKey="sku"
            loading={isLoading}
            onRowClick={setSelected}
            empty={
              <EmptyState
                title="Sin resultados"
                description="Ningún producto coincide con la búsqueda o el filtro."
                action={
                  <Button variant="default" onClick={() => filter('todos', '')}>
                    Limpiar filtros
                  </Button>
                }
              />
            }
          />
          <div className={classes.pager}>
            <Pagination
              page={page}
              total={data?.total ?? 0}
              pageSize={PAGE_SIZE}
              onChange={setPage}
            />
          </div>
        </>
      )}
      <ProductoDetalle producto={selected} onClose={() => setSelected(null)} />
    </>
  );
}
