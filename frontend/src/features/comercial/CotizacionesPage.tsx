import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router';
import { StatusBadge } from '@/components/domain/StatusBadge/StatusBadge';
import { PageHeader } from '@/components/layout/PageHeader/PageHeader';
import { type Column } from '@/components/ui/DataTable/DataTable';
import { QueryTable } from '@/components/ui/QueryTable/QueryTable';
import { formatCurrency, formatDate } from '@/lib/format';
import { listarCotizaciones } from './api';
import { totales } from './calculos';
import type { Cotizacion } from './types';

const columns: Column<Cotizacion>[] = [
  { key: 'id', header: 'Número', mono: true, primary: true },
  { key: 'cliente', header: 'Cliente', render: (c) => c.cliente?.nombre ?? 'Sin cliente' },
  { key: 'emision', header: 'Emisión', phone: false, render: (c) => formatDate(c.emision) },
  { key: 'vencimiento', header: 'Vence', render: (c) => formatDate(c.vencimiento) },
  { key: 'total', header: 'Total', numeric: true, render: (c) => formatCurrency(totales(c).total) },
  {
    key: 'estado',
    header: 'Estado',
    render: (c) => <StatusBadge entity="cotizacion" value={c.estado} />,
  },
];

/** CU-09: listado de cotizaciones; cada fila abre su detalle. */
export function CotizacionesPage() {
  const navigate = useNavigate();
  const query = useQuery({ queryKey: ['cotizaciones'], queryFn: listarCotizaciones });

  return (
    <>
      <PageHeader
        title="Cotizaciones y ventas"
        description="Cotizaciones emitidas a clientes y su estado."
      />
      <QueryTable
        query={query}
        errorTitle="No se pudieron cargar las cotizaciones"
        caption="Cotizaciones"
        columns={columns}
        rowKey="id"
        onRowClick={(c) => void navigate(`/ventas/${c.id}`)}
      />
    </>
  );
}
