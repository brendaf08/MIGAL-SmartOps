import { Button, NumberInput, Text } from '@mantine/core';
import { notifications } from '@mantine/notifications';
import { useMutation, useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router';
import { StatusBadge } from '@/components/domain/StatusBadge/StatusBadge';
import { PageHeader } from '@/components/layout/PageHeader/PageHeader';
import { ActionBar } from '@/components/ui/ActionBar/ActionBar';
import { Card } from '@/components/ui/Card/Card';
import { EmptyState } from '@/components/ui/EmptyState/EmptyState';
import { Icon } from '@/components/ui/Icon/Icon';
import { ConfirmDialog } from '@/components/ui/Modal/ConfirmDialog';
import type { Column } from '@/components/ui/DataTable/DataTable';
import { descartarDocumento, obtenerDocumento, validarDocumento, type LineaOcr } from './api';
import { leerDestino, rutaDestino } from './destino';
import { LineasOcrTabla } from './LineasOcrTabla';

const plural = (n: number, uno: string, varios: string) => `${n} ${n === 1 ? uno : varios}`;

/** P24 (CU-08 y CU-13): el usuario revisa las líneas del OCR, asocia los productos y decide qué usar. */
export function RevisionOcrPage() {
  const navigate = useNavigate();
  const { id: idParam } = useParams();
  const id = Number(idParam);
  const [params] = useSearchParams();
  const destino = leerDestino(params.get('destino'));
  const [lineas, setLineas] = useState<LineaOcr[] | null>(null);
  const [descartando, setDescartando] = useState(false);

  const query = useQuery({
    queryKey: ['documento', id],
    queryFn: () => obtenerDocumento(id),
    enabled: Number.isInteger(id),
  });
  const documento = query.data;
  const filas = lineas ?? documento?.lineas ?? [];

  const usar = useMutation({
    mutationFn: () => validarDocumento(id, filas),
    onSuccess: () => {
      void navigate(destino ? `${rutaDestino(destino)}?documento=${id}` : '/documentos');
    },
    onError: (e) => {
      notifications.show({
        color: 'danger',
        title: 'No se pudieron usar los datos',
        message: e.message,
      });
    },
  });

  const descartar = useMutation({
    mutationFn: () => descartarDocumento(id),
    onSuccess: () => {
      void navigate('/documentos');
    },
    onError: (e) => {
      setDescartando(false);
      notifications.show({ color: 'danger', title: 'No se pudo descartar', message: e.message });
    },
  });

  if (query.isError || (documento === undefined && !query.isLoading)) {
    return (
      <>
        <PageHeader
          title="Revisión del OCR"
          breadcrumbs={[{ label: 'Documentos OCR', href: '/documentos' }, { label: 'Revisión del OCR' }]}
        />
        <EmptyState
          title="No se encontró el documento"
          description="Puede haberse descartado. Volver a la lista para elegir otro."
          action={
            <Button variant="default" onClick={() => void navigate('/documentos')}>
              Volver a documentos
            </Button>
          }
        />
      </>
    );
  }

  if (!documento) {
    return (
      <>
        <PageHeader
          title="Revisión del OCR"
          breadcrumbs={[{ label: 'Documentos OCR', href: '/documentos' }, { label: 'Revisión del OCR' }]}
        />
        <Text c="dimmed">Cargando el documento.</Text>
      </>
    );
  }

  const sinAsociar = filas.filter((l) => !l.sku).length;
  const motivoBloqueo =
    sinAsociar > 0
      ? `Falta asociar ${plural(sinAsociar, 'línea', 'líneas')} a un producto del catálogo.`
      : null;

  const columnas: Column<LineaOcr>[] = [
    {
      key: 'cantidad',
      header: 'Cantidad',
      block: true,
      render: (l) => (
        <NumberInput
          aria-label={`Cantidad de ${l.texto}`}
          min={0}
          miw={80}
          maw={110}
          hideControls
          value={l.cantidad}
          onChange={(v) =>
            setLineas(filas.map((x) => (x.id === l.id ? { ...x, cantidad: Number(v) || 0 } : x)))
          }
        />
      ),
    },
  ];

  return (
    <>
      <PageHeader
        title="Revisión del OCR"
        breadcrumbs={[{ label: 'Documentos OCR', href: '/documentos' }, { label: 'Revisión del OCR' }]}
        description={documento ? `${documento.archivo}, ${documento.tipo}` : undefined}
        status={documento && <StatusBadge entity="documento" value={documento.estado} />}
      />
      <Card
        title="Líneas extraídas"
        description={`${plural(filas.length, 'línea', 'líneas')}. Confirmar el producto de cada una antes de usar los datos.`}
        padded={false}
      >
        <LineasOcrTabla
          caption="Líneas extraídas del documento"
          lineas={filas}
          onSkuChange={(lineaId, sku) =>
            setLineas(filas.map((x) => (x.id === lineaId ? { ...x, sku } : x)))
          }
          columnas={columnas}
        />
      </Card>
      <ActionBar
        aria-label="Resumen y acciones del documento"
        summary={
          <Text size="sm" fw={600}>
            {plural(filas.length, 'línea', 'líneas')}, {sinAsociar} sin asociar
          </Text>
        }
        reason={motivoBloqueo}
      >
        <Button
          variant="default"
          leftSection={<Icon name="trash" />}
          onClick={() => setDescartando(true)}
        >
          Descartar documento
        </Button>
        <Button
          leftSection={<Icon name="check" />}
          disabled={motivoBloqueo !== null || usar.isPending}
          loading={usar.isPending}
          onClick={() => usar.mutate()}
        >
          Usar estos datos
        </Button>
      </ActionBar>
      <ConfirmDialog
        open={descartando}
        title={`Descartar documento ${documento?.archivo ?? ''}`}
        description="El documento y sus líneas extraídas se quitan de la lista."
        consequences={['La acción no se puede deshacer.']}
        confirmLabel="Descartar documento"
        tone="danger"
        loading={descartar.isPending}
        onConfirm={() => descartar.mutate()}
        onCancel={() => setDescartando(false)}
      />
    </>
  );
}
