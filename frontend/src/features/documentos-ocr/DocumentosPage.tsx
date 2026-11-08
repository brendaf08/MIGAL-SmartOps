import { Button } from '@mantine/core';
import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { useNavigate } from 'react-router';
import { StatusBadge } from '@/components/domain/StatusBadge/StatusBadge';
import { STATUS } from '@/components/domain/StatusBadge/status';
import { PageHeader } from '@/components/layout/PageHeader/PageHeader';
import { type Column } from '@/components/ui/DataTable/DataTable';
import { Icon } from '@/components/ui/Icon/Icon';
import { QueryTable } from '@/components/ui/QueryTable/QueryTable';
import { Tabs } from '@/components/ui/Tabs/Tabs';
import { formatDate } from '@/lib/format';
import { listarDocumentos, type Documento, type EstadoDocumento } from './api';

const ESTADOS: EstadoDocumento[] = ['Pendiente', 'Procesado_OCR', 'Validado'];

/** CU-08: documentos procesados por OCR. Procesar un documento no genera ventas, reservas ni movimientos. */
export function DocumentosPage() {
  const navigate = useNavigate();
  const [estado, setEstado] = useState<EstadoDocumento>('Pendiente');
  const query = useQuery({ queryKey: ['documentos'], queryFn: listarDocumentos });
  const documentos = query.data ?? [];

  const columns: Column<Documento>[] = [
    { key: 'archivo', header: 'Documento', primary: true },
    { key: 'tipo', header: 'Tipo', phone: false },
    { key: 'fecha', header: 'Fecha', render: (d) => formatDate(d.fecha) },
    {
      key: 'estado',
      header: 'Estado',
      render: (d) => <StatusBadge entity="documento" value={d.estado} />,
    },
    {
      key: 'revisar',
      header: 'Acción',
      render: (d) => (
        <Button
          variant="default"
          size="compact-md"
          aria-label={`Revisar ${d.archivo}`}
          leftSection={<Icon name="eye" size={16} />}
          onClick={() => void navigate(`/documentos/${d.id}/revision`)}
        >
          Revisar
        </Button>
      ),
    },
  ];

  const filtrados = documentos.filter((d) => d.estado === estado);

  return (
    <>
      <PageHeader
        title="Documentos OCR"
        description="Notas, pedidos y guías procesados para precargar datos; el usuario siempre los revisa antes de confirmar."
        actions={
          <Button
            leftSection={<Icon name="scan" />}
            onClick={() => void navigate('/documentos/procesar')}
          >
            Procesar documento
          </Button>
        }
      />
      <Tabs
        aria-label="Filtrar documentos por estado"
        value={estado}
        onChange={(valor) => setEstado(valor as EstadoDocumento)}
        items={ESTADOS.map((e) => ({
          value: e,
          label: STATUS.documento[e][1],
          count: documentos.filter((d) => d.estado === e).length,
        }))}
      />
      <div role="tabpanel" aria-label={STATUS.documento[estado][1]}>
        <QueryTable
          query={{ data: filtrados, isLoading: query.isLoading, isError: query.isError }}
          errorTitle="No se pudieron cargar los documentos"
          caption="Documentos procesados"
          columns={columns}
          rowKey="id"
        />
      </div>
    </>
  );
}
