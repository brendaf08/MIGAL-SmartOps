import { Button, FileButton, NumberInput, Select, Stack, Text } from '@mantine/core';
import { notifications } from '@mantine/notifications';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useNavigate } from 'react-router';
import { MatchConfidence } from '@/components/domain/MatchConfidence/MatchConfidence';
import { StatusBadge } from '@/components/domain/StatusBadge/StatusBadge';
import { PageHeader } from '@/components/layout/PageHeader/PageHeader';
import { Alert } from '@/components/ui/Alert/Alert';
import { Card } from '@/components/ui/Card/Card';
import { Columns } from '@/components/ui/Columns/Columns';
import { DataTable, type Column } from '@/components/ui/DataTable/DataTable';
import { Dropzone } from '@/components/ui/Dropzone/Dropzone';
import { FileItem } from '@/components/ui/Dropzone/FileItem';
import { Icon } from '@/components/ui/Icon/Icon';
import { ConfirmDialog } from '@/components/ui/Modal/ConfirmDialog';
import { Stepper } from '@/components/ui/Stepper/Stepper';
import { PRODUCTOS } from '@/features/inventario/fixtures';
import { confirmarRecepcion, procesarDocumento } from './api';
import type { DocumentoProcesado, LineaRecepcion } from './types';

const PASOS = [
  { label: 'Cargar documento', description: 'Foto o PDF de la guía' },
  { label: 'Revisar datos', description: 'Corregir y asociar SKU' },
  { label: 'Confirmar recepción' },
];
const OPCIONES = PRODUCTOS.map((p) => ({ value: p.sku, label: `${p.sku}, ${p.nombre}` }));

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

  const sinProducto = lineas.filter((l) => !l.sku).length;
  const parciales = lineas.filter((l) => l.recibido < l.pedido).length;

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
          <MatchConfidence score={l.score} />
        </div>
      ),
    },
    { key: 'pedido', header: 'Pedido', numeric: true },
    {
      key: 'recibido',
      header: 'Recibido',
      block: true,
      render: (l) => (
        <NumberInput
          aria-label={`Cantidad recibida de ${l.texto}`}
          min={0}
          miw={120}
          value={l.recibido}
          onChange={(v) => update(l.id, { recibido: Number(v) || 0 })}
        />
      ),
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
          <>
            <Button variant="default" onClick={() => void navigate('/recepciones')}>
              Cancelar
            </Button>
            {doc && (
              <Button
                leftSection={<Icon name="check" />}
                disabled={sinProducto > 0}
                onClick={() => setConfirming(true)}
              >
                Confirmar recepción
              </Button>
            )}
          </>
        }
      />
      <Stack gap="lg">
        <Stepper steps={PASOS} current={doc ? 1 : 0} aria-label="Pasos de la recepción" />
        <Alert
          title={
            doc ? 'Datos precargados desde el documento' : 'La carga no modifica el inventario'
          }
        >
          {doc
            ? 'Revisar y corregir las líneas. Los movimientos de entrada al Kardex se generan solo al confirmar la recepción, con las cantidades efectivamente recibidas.'
            : 'El procesamiento del documento no genera ventas, reservas ni movimientos de Kardex; el resultado se revisa antes de confirmar.'}
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
                      Reemplazar archivo
                    </Button>
                  )}
                </FileButton>
              </Stack>
            </Card>
            <Card
              title="Líneas de la recepción"
              description={`${lineas.length} productos${sinProducto ? `, ${sinProducto} requiere${sinProducto > 1 ? 'n' : ''} validación manual` : ''}`}
              padded={false}
            >
              <DataTable caption="Líneas extraídas" columns={columns} rows={lineas} rowKey="id" />
              {sinProducto > 0 && (
                <Text size="sm" c="dimmed" p="md">
                  Asociar un producto a cada línea para habilitar la confirmación.
                </Text>
              )}
            </Card>
          </Columns>
        )}
      </Stack>
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
