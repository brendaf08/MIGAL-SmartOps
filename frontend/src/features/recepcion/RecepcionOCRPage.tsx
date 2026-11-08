import { Badge, Button, FileButton, Group, NumberInput, Select, Stack, Text } from '@mantine/core';
import { notifications } from '@mantine/notifications';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useNavigate } from 'react-router';
import { MatchConfidence } from '@/components/domain/MatchConfidence/MatchConfidence';
import { StatusBadge } from '@/components/domain/StatusBadge/StatusBadge';
import { PageHeader } from '@/components/layout/PageHeader/PageHeader';
import { ActionBar } from '@/components/ui/ActionBar/ActionBar';
import { Alert } from '@/components/ui/Alert/Alert';
import { Card } from '@/components/ui/Card/Card';
import { Columns } from '@/components/ui/Columns/Columns';
import { DataTable, type Column } from '@/components/ui/DataTable/DataTable';
import { Dropzone } from '@/components/ui/Dropzone/Dropzone';
import { FileItem } from '@/components/ui/Dropzone/FileItem';
import { Icon } from '@/components/ui/Icon/Icon';
import { ConfirmDialog } from '@/components/ui/Modal/ConfirmDialog';
import { PRODUCTOS } from '@/features/inventario/fixtures';
import { confirmarRecepcion, procesarDocumento } from './api';
import {
  diferenciaLinea,
  igualarCoincidenciasAltas,
  resumirRecepcion,
  type DiferenciaLinea,
} from './resumen';
import type { DocumentoProcesado, LineaRecepcion } from './types';

const OPCIONES = PRODUCTOS.map((p) => ({ value: p.sku, label: `${p.sku}, ${p.nombre}` }));

/** La diferencia se comunica con forma y texto, no solo con color. */
function Diferencia({ valor }: { valor: DiferenciaLinea }) {
  switch (valor.tipo) {
    case 'completa':
      return (
        <Badge color="success" leftSection={<Icon name="status-ok" size={16} />}>
          Completa
        </Badge>
      );
    case 'faltan':
      return (
        <Badge color="warning" leftSection={<Icon name="alert-triangle" size={16} />}>
          Faltan {valor.cantidad}
        </Badge>
      );
    case 'sobran':
      return (
        <Badge color="warning" leftSection={<Icon name="alert-triangle" size={16} />}>
          Sobran {valor.cantidad}
        </Badge>
      );
    case 'sinAsociar':
      return (
        <Badge color="danger" leftSection={<Icon name="status-danger" size={16} />}>
          Sin asociar
        </Badge>
      );
  }
}

const plural = (n: number, uno: string, varios: string) => `${n} ${n === 1 ? uno : varios}`;

/** CU-13 y CU-08: el OCR solo precarga datos; los movimientos de entrada se generan únicamente al confirmar. */
export function RecepcionOCRPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [archivo, setArchivo] = useState<File | null>(null);
  const [doc, setDoc] = useState<DocumentoProcesado | null>(null);
  const [lineas, setLineas] = useState<LineaRecepcion[]>([]);
  const [confirming, setConfirming] = useState(false);

  const procesar = useMutation({
    mutationFn: procesarDocumento,
    onSuccess: (result) => {
      setDoc(result);
      setLineas(result.lineas);
    },
  });
  const confirmar = useMutation({
    mutationFn: () => confirmarRecepcion(doc!, lineas),
    onSuccess: (r) => {
      void queryClient.invalidateQueries({ queryKey: ['recepciones'] });
      notifications.show({
        color: 'success',
        title: 'Recepción confirmada',
        message: `Se registró la recepción ${r.id} y sus entradas en el Kardex.`,
      });
      void navigate('/recepciones');
    },
    onError: (e) => {
      setConfirming(false);
      notifications.show({
        color: 'danger',
        title: 'No se pudo confirmar la recepción',
        message: e.message,
      });
    },
  });

  const cargar = (files: File[]) => {
    const [file] = files;
    if (!file) return;
    setArchivo(file);
    setDoc(null);
    procesar.mutate(file);
  };
  const update = (id: number, patch: Partial<LineaRecepcion>) =>
    setLineas((prev) => prev.map((l) => (l.id === id ? { ...l, ...patch } : l)));

  const resumen = resumirRecepcion(lineas);
  const { sinAsociar: sinProducto, parciales } = resumen;

  const columns: Column<LineaRecepcion>[] = [
    { key: 'texto', header: 'Texto extraído', primary: true, mono: true },
    {
      key: 'sku',
      header: 'Producto del catálogo',
      block: true,
      render: (l) => (
        <Select
          aria-label={`Producto para ${l.texto}`}
          placeholder="Seleccionar producto"
          searchable
          miw={260}
          data={OPCIONES}
          value={l.sku || null}
          onChange={(sku) => update(l.id, { sku: sku ?? '' })}
          error={l.sku ? undefined : 'Asociar un producto.'}
        />
      ),
    },
    {
      key: 'score',
      header: 'Coincidencia',
      block: true,
      render: (l) => (
        <div style={{ minWidth: 180 }}>
          <MatchConfidence score={l.score} compact />
        </div>
      ),
    },
    { key: 'pedido', header: 'Pedido', numeric: true },
    {
      key: 'recibido',
      header: 'Recibido',
      block: true,
      render: (l) => (
        <Group gap="xs" wrap="nowrap">
          <NumberInput
            aria-label={`Cantidad recibida de ${l.texto}`}
            min={0}
            miw={110}
            value={l.recibido}
            onChange={(v) => update(l.id, { recibido: Number(v) || 0 })}
          />
          <Button
            variant="subtle"
            size="compact-sm"
            aria-label={`Igual al pedido, ${l.texto}`}
            disabled={!l.sku || l.recibido === l.pedido}
            onClick={() => update(l.id, { recibido: l.pedido })}
          >
            Igual al pedido
          </Button>
        </Group>
      ),
    },
    {
      key: 'diferencia',
      header: 'Diferencia',
      render: (l) => <Diferencia valor={diferenciaLinea(l)} />,
    },
  ];

  return (
    <>
      <PageHeader
        title="Nueva recepción"
        breadcrumbs={[{ label: 'Recepciones', href: '/recepciones' }, { label: 'Nueva recepción' }]}
        status={doc && <StatusBadge entity="recepcion" value="Pendiente" />}
        description={
          doc
            ? `Orden de compra ${doc.orden}, ${doc.proveedor}, ${doc.almacen}`
            : 'Cargar la guía del proveedor para precargar las líneas.'
        }
        actions={
          !doc && (
            <Button variant="default" onClick={() => void navigate('/recepciones')}>
              Cancelar
            </Button>
          )
        }
      />
      <Stack gap="lg">
        <Alert
          title={
            doc ? 'Datos precargados desde el documento' : 'La carga no modifica el inventario'
          }
        >
          {doc
            ? 'Las entradas al Kardex se generan solo al confirmar, con lo efectivamente recibido.'
            : 'El documento no genera ventas, reservas ni movimientos; se revisa antes de confirmar.'}
        </Alert>
        {!doc ? (
          <Card title="Documento" description="Foto o PDF de la guía del proveedor.">
            <Stack>
              <Dropzone camera disabled={procesar.isPending} onDrop={cargar} />
              {archivo && (
                <FileItem
                  name={archivo.name}
                  size={archivo.size}
                  status={
                    procesar.isPending ? 'processing' : procesar.isError ? 'error' : 'processed'
                  }
                />
              )}
              {procesar.isError && (
                <Alert tone="danger" title="Documento ilegible">
                  Cargar una foto más nítida o un PDF e intentar de nuevo.
                </Alert>
              )}
            </Stack>
          </Card>
        ) : (
          <Columns layout="aside-main">
            <Card title="Documento" description="Foto o PDF de la guía del proveedor.">
              <Stack>
                {archivo && <FileItem name={archivo.name} size={archivo.size} status="processed" />}
                <FileButton onChange={(f) => f && cargar([f])} accept="image/*,application/pdf">
                  {(props) => (
                    <Button {...props} variant="default" leftSection={<Icon name="upload" />}>
                      Otra foto o archivo
                    </Button>
                  )}
                </FileButton>
              </Stack>
            </Card>
            <Card
              title="Líneas de la recepción"
              description={`${plural(lineas.length, 'producto', 'productos')}${sinProducto ? `, ${sinProducto} con validación manual` : ''}`}
              actions={
                <Button
                  variant="default"
                  leftSection={<Icon name="check" />}
                  onClick={() => setLineas((prev) => igualarCoincidenciasAltas(prev))}
                >
                  Igualar coincidencias altas
                </Button>
              }
              padded={false}
            >
              <DataTable caption="Líneas extraídas" columns={columns} rows={lineas} rowKey="id" />
            </Card>
          </Columns>
        )}
      </Stack>
      {doc && (
        <ActionBar
          aria-label="Resumen y confirmación de la recepción"
          summary={
            <Text size="sm" fw={600}>
              {plural(resumen.total, 'línea', 'líneas')}:{' '}
              {plural(resumen.completas, 'completa', 'completas')},{' '}
              {plural(resumen.parciales, 'parcial', 'parciales')},{' '}
              {plural(resumen.sobrantes, 'sobrante', 'sobrantes')},{' '}
              {resumen.sinAsociar} sin asociar
            </Text>
          }
          reason={resumen.motivoBloqueo}
        >
          <Button variant="default" onClick={() => void navigate('/recepciones')}>
            Cancelar
          </Button>
          <Button
            leftSection={<Icon name="check" />}
            disabled={!resumen.puedeConfirmar}
            onClick={() => setConfirming(true)}
          >
            Confirmar recepción
          </Button>
        </ActionBar>
      )}
      <ConfirmDialog
        open={confirming}
        title={`Confirmar recepción ${doc?.orden ?? ''}`}
        description="Se ingresará al inventario la mercadería efectivamente recibida."
        consequences={[
          `Se registrarán ${lineas.length} movimientos de entrada en el Kardex del ${doc?.almacen ?? 'almacén'}.`,
          ...(parciales
            ? ['Las líneas con menos cantidad que el pedido quedan como recepción parcial.']
            : []),
          'La acción no se puede deshacer.',
        ]}
        confirmLabel="Confirmar recepción"
        cancelLabel="Volver"
        loading={confirmar.isPending}
        onConfirm={() => confirmar.mutate()}
        onCancel={() => setConfirming(false)}
      />
    </>
  );
}
