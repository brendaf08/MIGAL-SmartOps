import { Button } from '@mantine/core';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router';
import { StatusBadge } from '@/components/domain/StatusBadge/StatusBadge';
import { PageHeader } from '@/components/layout/PageHeader/PageHeader';
import { type Column } from '@/components/ui/DataTable/DataTable';
import { QueryTable } from '@/components/ui/QueryTable/QueryTable';
import { Icon } from '@/components/ui/Icon/Icon';
import { formatDate } from '@/lib/format';
import { listarRecepciones } from './api';
import type { Recepcion } from './types';

const columns: Column<Recepcion>[] = [
  { key: 'id', header: 'Recepción', mono: true, primary: true },
  { key: 'orden', header: 'Orden de compra', mono: true },
  { key: 'proveedor', header: 'Proveedor' },
  { key: 'almacen', header: 'Almacén', phone: false },
  { key: 'fecha', header: 'Fecha', render: (r) => formatDate(r.fecha) },
  {
    key: 'estado',
    header: 'Estado',
    render: (r) => <StatusBadge entity="recepcion" value={r.estado} />,
  },
];

/** CU-13: historial de recepciones y acceso a una nueva recepción con documento. */
export function RecepcionesPage() {
  const navigate = useNavigate();
  const query = useQuery({ queryKey: ['recepciones'], queryFn: listarRecepciones });

  return (
    <>
      <PageHeader
        title="Recepciones"
        description="Mercadería recibida de proveedores."
        actions={
          <Button
            leftSection={<Icon name="scan" />}
            onClick={() => void navigate('/recepciones/nueva')}
          >
            Nueva recepción
          </Button>
        }
      />
      <QueryTable
        query={query}
        errorTitle="No se pudieron cargar las recepciones"
        caption="Recepciones"
        columns={columns}
        rowKey="id"
      />
    </>
  );
}
