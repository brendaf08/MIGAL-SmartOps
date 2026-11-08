import { Button, Select } from '@mantine/core';
import { useDebouncedValue } from '@mantine/hooks';
import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { Link, useNavigate } from 'react-router';
import { StatusBadge } from '@/components/domain/StatusBadge/StatusBadge';
import { PageHeader } from '@/components/layout/PageHeader/PageHeader';
import { type Column } from '@/components/ui/DataTable/DataTable';
import { Pagination } from '@/components/ui/Pagination/Pagination';
import { QueryTable } from '@/components/ui/QueryTable/QueryTable';
import { SearchInput } from '@/components/ui/SearchInput/SearchInput';
import { formatNumber } from '@/lib/format';
import { buscarCatalogo, listarCategorias, type EstadoProducto, type ProductoCatalogo } from './api';
import classes from './Catalogo.module.css';

const PAGE_SIZE = 20;

/** Opción "Todos" del filtro de estado; el valor vacío se reserva para el `Select` sin selección. */
const TODOS = 'Todos';
const ESTADOS = [TODOS, 'Activo', 'Inactivo'];

const columns: Column<ProductoCatalogo>[] = [
  { key: 'sku', header: 'SKU', mono: true },
  {
    key: 'nombre',
    header: 'Producto',
    primary: true,
    render: (p) => (
      <>
        <div>{p.nombre}</div>
        <small className={classes.categoria}>{p.categoria}</small>
      </>
    ),
  },
  { key: 'unidad', header: 'Unidad', phone: false },
  {
    key: 'minimo',
    header: 'Stock mínimo',
    numeric: true,
    render: (p) => formatNumber(p.minimo),
  },
  {
    key: 'estado',
    header: 'Estado',
    render: (p) => <StatusBadge entity="registro" value={p.estado} />,
  },
];

/** CU-03: catálogo maestro de productos, con búsqueda, filtros, paginación y acceso a la ficha. */
export function CatalogoPage() {
  const navigate = useNavigate();
  const [q, setQ] = useState('');
  const [debouncedQ] = useDebouncedValue(q, 300);
  const [categoria, setCategoria] = useState('');
  const [estado, setEstado] = useState<EstadoProducto | ''>('');
  const [page, setPage] = useState(1);

  const categorias = useQuery({ queryKey: ['catalogo-categorias'], queryFn: listarCategorias });
  const query = useQuery({
    queryKey: ['catalogo', debouncedQ, categoria, estado, page],
    queryFn: () =>
      buscarCatalogo({ q: debouncedQ, categoria, estado, page, pageSize: PAGE_SIZE }),
    placeholderData: keepPreviousData,
  });

  /** Cambiar un filtro vuelve a la primera página de resultados. */
  const conFiltro = <T,>(set: (value: T) => void) => (value: T) => {
    set(value);
    setPage(1);
  };

  return (
    <>
      <PageHeader
        title="Catálogo"
        description="Productos del catálogo maestro con su unidad y stock mínimo."
        actions={
          <Button component={Link} to="/catalogo/nuevo">
            Registrar producto
          </Button>
        }
      />
      <div className={classes.toolbar}>
        <SearchInput
          aria-label="Buscar producto"
          placeholder="Nombre o SKU"
          value={q}
          onChange={conFiltro(setQ)}
          className={classes.buscar}
        />
        <Select
          aria-label="Filtrar por categoría"
          placeholder="Todas las categorías"
          clearable
          data={categorias.data ?? []}
          value={categoria || null}
          onChange={(value) => conFiltro(setCategoria)(value ?? '')}
          className={classes.filtro}
        />
        <Select
          aria-label="Filtrar por estado"
          allowDeselect={false}
          data={ESTADOS}
          value={estado || TODOS}
          onChange={(value) => conFiltro(setEstado)(value === TODOS || !value ? '' : (value as EstadoProducto))}
          className={classes.filtro}
        />
      </div>
      <QueryTable
        query={{
          data: query.data?.items,
          isLoading: query.isLoading,
          isError: query.isError,
        }}
        errorTitle="No se pudo cargar el catálogo"
        caption="Catálogo de productos"
        columns={columns}
        rowKey="sku"
        onRowClick={(p) => void navigate(`/catalogo/${p.sku}`)}
      />
      {query.data && query.data.total > 0 && (
        <div className={classes.paginacion}>
          <Pagination
            page={page}
            total={query.data.total}
            pageSize={PAGE_SIZE}
            onChange={setPage}
          />
        </div>
      )}
    </>
  );
}
