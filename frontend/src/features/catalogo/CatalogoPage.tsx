import { useDebouncedValue } from '@mantine/hooks';
import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { StatusBadge } from '@/components/domain/StatusBadge/StatusBadge';
import { PageHeader } from '@/components/layout/PageHeader/PageHeader';
import type { Column } from '@/components/ui/DataTable/DataTable';
import { QueryTable } from '@/components/ui/QueryTable/QueryTable';
import { SearchInput } from '@/components/ui/SearchInput/SearchInput';
import { formatNumber } from '@/lib/format';
import type { ProductoDisponibilidad } from '@/features/inventario/types';
import { buscarCatalogo } from './api';

const columns: Column<ProductoDisponibilidad>[] = [
  { key: 'sku', header: 'SKU', mono: true },
  { key: 'nombre', header: 'Producto', primary: true },
  { key: 'categoria', header: 'Categoría', phone: false },
  { key: 'unidad', header: 'Unidad', phone: false },
  { key: 'minimo', header: 'Stock mínimo', numeric: true, render: (p) => formatNumber(p.minimo) },
  {
    key: 'estado',
    header: 'Estado',
    render: () => <StatusBadge entity="registro" value="Activo" />,
  },
];

/** CU-03: catálogo maestro de productos. */
export function CatalogoPage() {
  const [q, setQ] = useState('');
  const [debouncedQ] = useDebouncedValue(q, 300);
  const query = useQuery({
    queryKey: ['catalogo', debouncedQ],
    queryFn: () => buscarCatalogo(debouncedQ),
  });

  return (
    <>
      <PageHeader
        title="Catálogo"
        description="Productos del catálogo maestro con su unidad y stock mínimo."
      />
      <SearchInput
        aria-label="Buscar producto"
        placeholder="Nombre o SKU"
        value={q}
        onChange={setQ}
        mb="md"
        maw={320}
      />
      <QueryTable
        query={query}
        errorTitle="No se pudo cargar el catálogo"
        caption="Catálogo de productos"
        columns={columns}
        rowKey="sku"
      />
    </>
  );
}
