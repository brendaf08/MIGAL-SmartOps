import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router';
import { StatusBadge } from '@/components/domain/StatusBadge/StatusBadge';
import { PageHeader } from '@/components/layout/PageHeader/PageHeader';
import { type Column } from '@/components/ui/DataTable/DataTable';
import { QueryTable } from '@/components/ui/QueryTable/QueryTable';
import { listarOrdenes } from './api';
import type { OrdenDespacho } from './types';

const columns: Column<OrdenDespacho>[] = [
  { key: 'id', header: 'Orden', mono: true, primary: true },
  { key: 'venta', header: 'Venta', mono: true },
  { key: 'cliente', header: 'Cliente' },
  { key: 'entrega', header: 'Entrega', phone: false },
  {
    key: 'estado',
    header: 'Estado',
    render: (o) => <StatusBadge entity="guia" value={o.estado} />,
  },
];

/** CU-06: órdenes de despacho; cada fila abre su confirmación de salida. */
export function DespachosPage() {
  const navigate = useNavigate();
  const query = useQuery({ queryKey: ['ordenes'], queryFn: listarOrdenes });

  return (
    <>
      <PageHeader title="Despachos" description="Órdenes de despacho pendientes y atendidas." />
      <QueryTable
        query={query}
        errorTitle="No se pudieron cargar las órdenes"
        caption="Órdenes de despacho"
        columns={columns}
        rowKey="id"
        onRowClick={(o) => void navigate(`/despachos/${o.id}`)}
      />
    </>
  );
}
