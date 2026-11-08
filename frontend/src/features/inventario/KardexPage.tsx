import { useQuery } from '@tanstack/react-query';
import { MovementBadge } from '@/components/domain/MovementBadge/MovementBadge';
import { PageHeader } from '@/components/layout/PageHeader/PageHeader';
import type { Column } from '@/components/ui/DataTable/DataTable';
import { QueryTable } from '@/components/ui/QueryTable/QueryTable';
import { formatDate } from '@/lib/format';
import { listarMovimientos } from './api';
import type { Movimiento } from './types';

const columns: Column<Movimiento>[] = [
  { key: 'fecha', header: 'Fecha', render: (m) => formatDate(m.fecha) },
  { key: 'sku', header: 'SKU', mono: true, primary: true },
  { key: 'producto', header: 'Producto', phone: false },
  {
    key: 'movimiento',
    header: 'Movimiento',
    render: (m) => <MovementBadge tipo={m.tipo} cantidad={m.cantidad} />,
  },
  { key: 'origen', header: 'Origen' },
  { key: 'ubicacion', header: 'Ubicación', phone: false },
  { key: 'usuario', header: 'Responsable', phone: false },
];

/** CU-05 y CU-07: Kardex con el tipo y el origen de cada movimiento y su responsable (RF-USR-04). */
export function KardexPage() {
  const query = useQuery({ queryKey: ['kardex'], queryFn: listarMovimientos });

  return (
    <>
      <PageHeader
        title="Movimientos y Kardex"
        description="Entradas y salidas de mercadería por producto y ubicación."
      />
      <QueryTable
        query={query}
        errorTitle="No se pudo cargar el Kardex"
        caption="Movimientos de Kardex"
        columns={columns}
        rowKey="id"
      />
    </>
  );
}
