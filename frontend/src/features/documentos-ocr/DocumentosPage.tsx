import { useQuery } from '@tanstack/react-query';
import { StatusBadge } from '@/components/domain/StatusBadge/StatusBadge';
import { PageHeader } from '@/components/layout/PageHeader/PageHeader';
import type { Column } from '@/components/ui/DataTable/DataTable';
import { QueryTable } from '@/components/ui/QueryTable/QueryTable';
import { formatDate } from '@/lib/format';
import { listarDocumentos, type Documento } from './api';

const columns: Column<Documento>[] = [
  { key: 'archivo', header: 'Archivo', primary: true },
  { key: 'tipo', header: 'Tipo', phone: false },
  { key: 'fecha', header: 'Cargado', render: (d) => formatDate(d.fecha) },
  { key: 'usuario', header: 'Cargado por', phone: false },
  {
    key: 'estado',
    header: 'Estado',
    render: (d) => <StatusBadge entity="documento" value={d.estado} />,
  },
];

/** CU-08: documentos procesados por OCR. Procesar un documento no genera ventas, reservas ni movimientos. */
export function DocumentosPage() {
  const query = useQuery({ queryKey: ['documentos'], queryFn: listarDocumentos });

  return (
    <>
      <PageHeader
        title="Documentos OCR"
        description="Notas, pedidos y guías procesados para precargar datos; el usuario siempre los revisa antes de confirmar."
      />
      <QueryTable
        query={query}
        errorTitle="No se pudieron cargar los documentos"
        caption="Documentos procesados"
        columns={columns}
        rowKey="id"
      />
    </>
  );
}
