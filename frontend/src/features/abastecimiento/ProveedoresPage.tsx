import { Button } from '@mantine/core';
import { useDebouncedValue } from '@mantine/hooks';
import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { useNavigate } from 'react-router';
import { StatusBadge } from '@/components/domain/StatusBadge/StatusBadge';
import { PageHeader } from '@/components/layout/PageHeader/PageHeader';
import type { Column } from '@/components/ui/DataTable/DataTable';
import { QueryTable } from '@/components/ui/QueryTable/QueryTable';
import { SearchInput } from '@/components/ui/SearchInput/SearchInput';
import { Tabs } from '@/components/ui/Tabs/Tabs';
import { formatNumber } from '@/lib/format';
import { listarProveedores } from './proveedores/api';
import type { Proveedor } from './tipos';

type Vista = 'todos' | 'activos' | 'inactivos';

const ESTADO_POR_VISTA: Record<Vista, Proveedor['estado'] | undefined> = {
  todos: undefined,
  activos: 'Activo',
  inactivos: 'Inactivo',
};

const columns: Column<Proveedor>[] = [
  { key: 'razonSocial', header: 'Razón social', primary: true },
  { key: 'ruc', header: 'RUC', mono: true },
  { key: 'contacto', header: 'Contacto' },
  {
    key: 'skus',
    header: 'Productos',
    numeric: true,
    phone: false,
    render: (p) => formatNumber(p.skus.length),
  },
  {
    key: 'estado',
    header: 'Estado',
    render: (p) => <StatusBadge entity="registro" value={p.estado} />,
  },
];

/** CU-11: lista de proveedores con búsqueda y filtro por estado. */
export function ProveedoresPage() {
  const navigate = useNavigate();
  const [q, setQ] = useState('');
  const [debouncedQ] = useDebouncedValue(q, 300);
  const [vista, setVista] = useState<Vista>('todos');
  const estado = ESTADO_POR_VISTA[vista];
  const query = useQuery({
    queryKey: ['proveedores', debouncedQ, estado],
    queryFn: () => listarProveedores({ q: debouncedQ, estado }),
  });

  return (
    <>
      <PageHeader
        title="Proveedores"
        description="Proveedores registrados, su contacto y los productos que suministran."
        actions={
          <Button onClick={() => void navigate('/proveedores/nuevo')}>Registrar proveedor</Button>
        }
      />
      <SearchInput
        aria-label="Buscar proveedor"
        placeholder="Razón social o RUC"
        value={q}
        onChange={setQ}
        mb="md"
        maw={360}
      />
      <Tabs
        aria-label="Filtrar por estado"
        value={vista}
        onChange={(v) => setVista(v as Vista)}
        items={[
          { value: 'todos', label: 'Todos' },
          { value: 'activos', label: 'Activos' },
          { value: 'inactivos', label: 'Inactivos' },
        ]}
      />
      <div role="tabpanel" style={{ marginTop: 'var(--space-3)' }}>
        <QueryTable
          query={query}
          errorTitle="No se pudieron cargar los proveedores"
          caption="Proveedores"
          columns={columns}
          rowKey="id"
          onRowClick={(p) => void navigate(`/proveedores/${p.id}`)}
        />
      </div>
    </>
  );
}
