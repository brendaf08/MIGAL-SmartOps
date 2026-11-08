import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { StatusBadge } from '@/components/domain/StatusBadge/StatusBadge';
import { PageHeader } from '@/components/layout/PageHeader/PageHeader';
import type { Column } from '@/components/ui/DataTable/DataTable';
import { QueryTable } from '@/components/ui/QueryTable/QueryTable';
import { Tabs } from '@/components/ui/Tabs/Tabs';
import { formatCurrency, formatDate } from '@/lib/format';
import { listarOrdenesCompra, listarProveedores, type OrdenCompra, type Proveedor } from './api';

const proveedorColumns: Column<Proveedor>[] = [
  { key: 'nombre', header: 'Proveedor', primary: true },
  { key: 'ruc', header: 'RUC', mono: true },
  { key: 'contacto', header: 'Contacto', phone: false },
  {
    key: 'estado',
    header: 'Estado',
    render: (p) => <StatusBadge entity="registro" value={p.estado} />,
  },
];

const ordenColumns: Column<OrdenCompra>[] = [
  { key: 'id', header: 'Orden', mono: true, primary: true },
  { key: 'proveedor', header: 'Proveedor' },
  { key: 'fecha', header: 'Fecha', render: (o) => formatDate(o.fecha) },
  { key: 'total', header: 'Total', numeric: true, render: (o) => formatCurrency(o.total) },
  {
    key: 'estado',
    header: 'Estado',
    render: (o) => <StatusBadge entity="compra" value={o.estado} />,
  },
];

/** CU-11 y CU-12: proveedores y órdenes de compra. */
export function ProveedoresPage() {
  const [tab, setTab] = useState('proveedores');
  const proveedores = useQuery({ queryKey: ['proveedores'], queryFn: listarProveedores });
  const ordenes = useQuery({ queryKey: ['ordenes-compra'], queryFn: listarOrdenesCompra });

  return (
    <>
      <PageHeader
        title="Proveedores y compras"
        description="Proveedores registrados y sus órdenes de compra."
      />
      <Tabs
        aria-label="Vista de abastecimiento"
        value={tab}
        onChange={setTab}
        items={[
          { value: 'proveedores', label: 'Proveedores', count: proveedores.data?.length },
          { value: 'ordenes', label: 'Órdenes de compra', count: ordenes.data?.length },
        ]}
      />
      <div role="tabpanel" style={{ marginTop: 'var(--space-3)' }}>
        {tab === 'proveedores' ? (
          <QueryTable
            query={proveedores}
            errorTitle="No se pudieron cargar los proveedores"
            caption="Proveedores"
            columns={proveedorColumns}
            rowKey="id"
          />
        ) : (
          <QueryTable
            query={ordenes}
            errorTitle="No se pudieron cargar las órdenes"
            caption="Órdenes de compra"
            columns={ordenColumns}
            rowKey="id"
          />
        )}
      </div>
    </>
  );
}
