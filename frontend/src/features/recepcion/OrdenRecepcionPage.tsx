import { Badge, Button, NumberInput, Stack, Text, Textarea } from '@mantine/core';
import { notifications } from '@mantine/notifications';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router';
import { Constancia } from '@/components/domain/Constancia/Constancia';
import { StatusBadge } from '@/components/domain/StatusBadge/StatusBadge';
import { PageHeader } from '@/components/layout/PageHeader/PageHeader';
import { ActionBar } from '@/components/ui/ActionBar/ActionBar';
import { Alert } from '@/components/ui/Alert/Alert';
import { Card } from '@/components/ui/Card/Card';
import type { Column } from '@/components/ui/DataTable/DataTable';
import { Dropzone } from '@/components/ui/Dropzone/Dropzone';
import { FileItem } from '@/components/ui/Dropzone/FileItem';
import { Icon } from '@/components/ui/Icon/Icon';
import { ConfirmDialog } from '@/components/ui/Modal/ConfirmDialog';
import { Stepper } from '@/components/ui/Stepper/Stepper';
import { obtenerDocumento, procesarDocumento, type LineaOcr } from '@/features/documentos-ocr/api';
import { LineasOcrTabla } from '@/features/documentos-ocr/LineasOcrTabla';
import { formatNumber } from '@/lib/format';
import {
  ALMACEN,
  confirmarRecepcion,
  listarOrdenesRecepcionables,
  type ResultadoRecepcion,
} from './api';
import { construirLineas } from './lineas';
import { SeleccionOrdenCompra } from './SeleccionOrdenCompra';
import {
  diferenciaLinea,
  igualarCoincidenciasAltas,
  resumirRecepcion,
  type DiferenciaLinea,
} from './resumen';
import type { LineaRecepcion } from './types';

const PASOS = [
  { label: 'Orden de compra', description: 'Pendiente o con recepción parcial' },
  { label: 'Documento', description: 'Guía con OCR, opcional' },
  { label: 'Conteo', description: 'Cantidades recibidas' },
];

const plural = (n: number, uno: string, varios: string) => `${n} ${n === 1 ? uno : varios}`;

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

/**
 * P27 (CU-13): recepción desde una orden de compra en tres pasos, con documento opcional. El OCR solo precarga
 * las cantidades; los movimientos de entrada se generan únicamente al confirmar.
 */
export function OrdenRecepcionPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [params] = useSearchParams();
  const [paso, setPaso] = useState<0 | 1 | 2>(0);
  const [ordenId, setOrdenId] = useState<string | null>(params.get('oc'));
  const [archivo, setArchivo] = useState<File | null>(null);
  const [lineasOcr, setLineasOcr] = useState<LineaOcr[] | null>(null);
  /** Con `documento=ID` la revisión del OCR (P24) llega ya validada: sus líneas se precargan en el paso del documento. */
  const [usarPrecargado, setUsarPrecargado] = useState(params.has('documento'));
  const precargado = useQuery({
    queryKey: ['documento', params.get('documento') ?? ''],
    queryFn: () => obtenerDocumento(params.get('documento') ?? ''),
    enabled: params.has('documento'),
  });
  const ocr = lineasOcr ?? (usarPrecargado ? (precargado.data?.lineas ?? null) : null);
  const [lineas, setLineas] = useState<LineaRecepcion[]>([]);
  const [nota, setNota] = useState('');
  const [confirming, setConfirming] = useState(false);
  /** La constancia solo existe como resultado de confirmar. */
  const [cerrada, setCerrada] = useState<ResultadoRecepcion | null>(null);

  const ordenes = useQuery({ queryKey: ['ordenes-recepcion'], queryFn: listarOrdenesRecepcionables });
  const orden = ordenes.data?.find((o) => o.id === ordenId) ?? null;

  const procesar = useMutation({
    mutationFn: (file: File) => procesarDocumento(file, 'Guía de proveedor'),
    onSuccess: (documento) => setLineasOcr(documento.lineas),
  });

  const confirmar = useMutation({
    mutationFn: () => confirmarRecepcion(orden!.id, lineas, nota),
    onSuccess: (resultado) => {
      void queryClient.invalidateQueries({ queryKey: ['recepciones'] });
      void queryClient.invalidateQueries({ queryKey: ['ordenes-recepcion'] });
      setConfirming(false);
      setCerrada(resultado);
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
    setLineasOcr(null);
    setUsarPrecargado(false);
    procesar.mutate(file);
  };

  const reiniciar = () => {
    setCerrada(null);
    setPaso(0);
    setArchivo(null);
    setLineasOcr(null);
    setUsarPrecargado(false);
    setLineas([]);
    setNota('');
    procesar.reset();
  };

  const actualizarOcr = (id: string, patch: Partial<LineaOcr>) =>
    setLineasOcr((prev) => (prev ?? ocr)?.map((l) => (l.id === id ? { ...l, ...patch } : l)) ?? null);
  const actualizarConteo = (id: number, patch: Partial<LineaRecepcion>) =>
    setLineas((prev) => prev.map((l) => (l.id === id ? { ...l, ...patch } : l)));

  if (cerrada) {
    const { recepcion, cambios } = cerrada;
    return (
      <>
        <PageHeader
          title="Recepción confirmada"
          breadcrumbs={[{ label: 'Recepciones', href: '/recepciones' }, { label: recepcion.id }]}
        />
        <Constancia
          sello={<StatusBadge entity="recepcion" value={recepcion.estado} variant="sello" />}
          title={`Recepción ${recepcion.id} registrada`}
          description={`Orden ${recepcion.orden}, ${recepcion.proveedor}, ${recepcion.almacen}.`}
          cambios={cambios}
          nota={
            cerrada.estadoOrden === 'Parcial'
              ? `Recepción parcial: quedan por recibir ${formatNumber(cerrada.porRecibir)} unidades en ${plural(cerrada.lineasPendientes, 'línea', 'líneas')}.`
              : 'Orden completada. Las entradas quedaron registradas en el Kardex.'
          }
          actions={
            <>
              <Button variant="default" onClick={() => void navigate('/recepciones')}>
                Volver a recepciones
              </Button>
              {cerrada.estadoOrden === 'Parcial' && (
                <Button variant="default" leftSection={<Icon name="scan" />} onClick={reiniciar}>
                  Registrar otra recepción
                </Button>
              )}
              <Button leftSection={<Icon name="kardex" />} onClick={() => void navigate('/kardex')}>
                Ver movimientos en el Kardex
              </Button>
            </>
          }
        />
      </>
    );
  }

  const sinAsociarOcr = ocr?.filter((l) => !l.sku).length ?? 0;
  const motivoPaso1 = procesar.isPending
    ? 'Leyendo la guía con OCR.'
    : !ocr
      ? 'Cargar la guía o registrar la recepción sin documento.'
      : sinAsociarOcr > 0
        ? `Falta asociar ${plural(sinAsociarOcr, 'línea', 'líneas')} a un producto del catálogo.`
        : null;

  const resumen = resumirRecepcion(lineas);
  const motivoPaso2 =
    resumen.motivoBloqueo ??
    (resumen.sobrantes > 0 && !nota.trim() ? 'Registrar una nota para explicar el sobrante.' : null);
  const conEntrada = lineas.filter((l) => l.recibido > 0).length;

  const columnasOcr: Column<LineaOcr>[] = [
    {
      key: 'cantidad',
      header: 'Cantidad leída',
      block: true,
      render: (l) => (
        <NumberInput
          aria-label={`Cantidad leída de ${l.texto}`}
          min={0}
          miw={80}
          maw={110}
          hideControls
          value={l.cantidad}
          onChange={(v) => actualizarOcr(l.id, { cantidad: Number(v) || 0 })}
        />
      ),
    },
  ];

  const columnasConteo: Column<LineaRecepcion>[] = [
    { key: 'pedido', header: 'Pendiente', numeric: true },
    {
      key: 'recibido',
      header: 'Recibido',
      block: true,
      render: (l) => (
        <NumberInput
          aria-label={`Cantidad recibida de ${l.texto}`}
          min={0}
          miw={80}
          maw={110}
          hideControls
          value={l.recibido}
          onChange={(v) => actualizarConteo(l.id, { recibido: Number(v) || 0 })}
        />
      ),
    },
    {
      key: 'diferencia',
      header: 'Diferencia',
      render: (l) => {
        const d = diferenciaLinea(l);
        return (
          <Stack gap={2} align="flex-start">
            <Diferencia valor={d} />
            {(d.tipo === 'faltan' || d.tipo === 'sobran') && (
              <Button
                variant="subtle"
                size="compact-sm"
                aria-label={`Igual al pedido, ${l.texto}`}
                onClick={() => actualizarConteo(l.id, { recibido: l.pedido })}
              >
                Igual al pedido
              </Button>
            )}
          </Stack>
        );
      },
    },
  ];

  return (
    <>
      <PageHeader
        title="Nueva recepción"
        breadcrumbs={[{ label: 'Recepciones', href: '/recepciones' }, { label: 'Nueva recepción' }]}
        status={paso > 0 && <StatusBadge entity="recepcion" value="Pendiente" />}
        description={
          paso === 0 || !orden
            ? 'Elegir la orden de compra que se recibe.'
            : `Orden de compra ${orden.id}, ${orden.proveedor}, ${ALMACEN}`
        }
      />
      <Stack gap="lg">
        <Stepper aria-label="Pasos de la recepción" steps={PASOS} current={paso} />

        {paso === 0 && (
          <SeleccionOrdenCompra
            ordenes={ordenes.data ?? []}
            seleccionada={ordenId}
            onSeleccionar={setOrdenId}
          />
        )}

        {paso === 1 && (
          <>
            <Alert
              title={
                ocr
                  ? 'Datos precargados desde el documento'
                  : 'La carga no modifica el inventario'
              }
            >
              {ocr
                ? 'Las entradas al Kardex se generan solo al confirmar, con lo efectivamente recibido.'
                : 'El documento no genera ventas, reservas ni movimientos; se revisa antes de confirmar.'}
            </Alert>
            <Card title="Documento" description="Foto o PDF de la guía del proveedor. Es opcional.">
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
                    Cargar una foto más nítida o un PDF, o registrar la recepción sin documento.
                  </Alert>
                )}
              </Stack>
            </Card>
            {ocr && (
              <Card
                title="Líneas de la guía"
                description={`${plural(ocr.length, 'línea', 'líneas')}${sinAsociarOcr ? `, ${sinAsociarOcr} sin asociar` : ''}`}
                padded={false}
              >
                <LineasOcrTabla
                  caption="Líneas extraídas"
                  lineas={ocr}
                  onSkuChange={(id, sku) => actualizarOcr(id, { sku })}
                  columnas={columnasOcr}
                />
              </Card>
            )}
          </>
        )}

        {paso === 2 && (
          <>
            <Alert title="Conteo de lo recibido">
              Las entradas al Kardex se generan solo al confirmar, con lo efectivamente recibido.
            </Alert>
            <Card
              title="Conteo"
              description={`${plural(lineas.length, 'línea', 'líneas')}${resumen.sinAsociar ? `, ${resumen.sinAsociar} sin asociar` : ''}`}
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
              <LineasOcrTabla
                caption="Conteo de la recepción"
                lineas={lineas}
                onSkuChange={(id, sku) => actualizarConteo(id, { sku })}
                columnas={columnasConteo}
              />
            </Card>
            {resumen.sobrantes > 0 && (
              <Textarea
                label="Nota del sobrante"
                description="Motivo de las unidades que superan lo pendiente de la orden."
                required
                autosize
                minRows={2}
                value={nota}
                onChange={(e) => setNota(e.currentTarget.value)}
                error={nota.trim() ? undefined : 'Indicar el motivo del sobrante.'}
              />
            )}
          </>
        )}
      </Stack>

      {paso === 0 && (
        <ActionBar
          aria-label="Orden de compra de la recepción"
          summary={
            orden && (
              <Text size="sm" fw={600}>
                Orden {orden.id}, {plural(orden.lineas.length, 'producto', 'productos')}
              </Text>
            )
          }
          reason={orden ? null : 'Elegir una orden de compra para continuar.'}
        >
          <Button variant="default" onClick={() => void navigate('/recepciones')}>
            Cancelar
          </Button>
          <Button
            leftSection={<Icon name="arrow-right" />}
            disabled={!orden}
            onClick={() => setPaso(1)}
          >
            Continuar
          </Button>
        </ActionBar>
      )}

      {paso === 1 && orden && (
        <ActionBar
          aria-label="Documento de la recepción"
          summary={
            ocr && (
              <Text size="sm" fw={600}>
                {plural(ocr.length, 'línea', 'líneas')}, {sinAsociarOcr} sin asociar
              </Text>
            )
          }
          reason={motivoPaso1}
        >
          <Button variant="default" onClick={() => setPaso(0)}>
            Atrás
          </Button>
          <Button
            variant="default"
            onClick={() => {
              setLineasOcr(null);
              setUsarPrecargado(false);
              setLineas(construirLineas(orden, null));
              setPaso(2);
            }}
          >
            Registrar sin documento
          </Button>
          <Button
            leftSection={<Icon name="check" />}
            disabled={!ocr || sinAsociarOcr > 0 || procesar.isPending}
            onClick={() => {
              setLineas(construirLineas(orden, ocr));
              setPaso(2);
            }}
          >
            Usar datos de la guía
          </Button>
        </ActionBar>
      )}

      {paso === 2 && (
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
          reason={motivoPaso2}
        >
          <Button variant="default" onClick={() => setPaso(1)}>
            Atrás
          </Button>
          <Button
            leftSection={<Icon name="check" />}
            disabled={motivoPaso2 !== null}
            onClick={() => setConfirming(true)}
          >
            Confirmar recepción
          </Button>
        </ActionBar>
      )}

      <ConfirmDialog
        open={confirming}
        title={`Confirmar recepción ${orden?.id ?? ''}`}
        description="Se ingresará al inventario la mercadería efectivamente recibida."
        consequences={[
          `Se registrarán ${plural(conEntrada, 'movimiento', 'movimientos')} de entrada en el Kardex del ${ALMACEN}.`,
          ...(resumen.parciales
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
