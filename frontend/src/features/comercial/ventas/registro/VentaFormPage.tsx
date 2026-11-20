import { Button, Radio, Select, Stack, Text, TextInput } from '@mantine/core';
import { useForm } from '@mantine/form';
import { notifications } from '@mantine/notifications';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { zod4Resolver } from 'mantine-form-zod-resolver';
import { useEffect, useRef, useState } from 'react';
import { Link, useSearchParams } from 'react-router';
import { Constancia } from '@/components/domain/Constancia/Constancia';
import { StatusBadge } from '@/components/domain/StatusBadge/StatusBadge';
import { StockBar } from '@/components/domain/StockBar/StockBar';
import { PageHeader } from '@/components/layout/PageHeader/PageHeader';
import { Alert } from '@/components/ui/Alert/Alert';
import { Card } from '@/components/ui/Card/Card';
import { DataTable, type Column } from '@/components/ui/DataTable/DataTable';
import { FormPage } from '@/components/ui/FormPage/FormPage';
import { Icon } from '@/components/ui/Icon/Icon';
import { LineasEditor } from '@/components/ui/LineasEditor/LineasEditor';
import {
  consolidarLineas,
  subtotalLineas,
  totalLineas,
  type LineaEditable,
} from '@/components/ui/LineasEditor/calculos';
import { ConfirmDialog } from '@/components/ui/Modal/ConfirmDialog';
import { opcionesProducto } from '@/features/catalogo/opciones';
import { PRODUCTOS } from '@/features/inventario/fixtures';
import { hoyIso } from '@/features/comercial/calculos';
import { formatCurrency, formatNumber } from '@/lib/format';
import { ClientePicker, SIN_CLIENTE } from '../../clientes/ClientePicker';
import type { Cotizacion } from '../../types';
import {
  cotizacionUtilizable,
  listarCotizacionesAceptadas,
  registrarVenta,
  stockLibre,
  type EntradaVenta,
  type ResultadoVenta,
} from './api';
import { calcularAtencion, MODALIDADES, type ModalidadAtencion } from './atencion';
import { crearEsquemaVenta } from './esquema';
import { avisarRepetidos } from '@/features/documentos-ocr/avisos';
import { usePrecargaDocumento } from '@/features/documentos-ocr/usePrecargaDocumento';
import classes from './VentaForm.module.css';
import { useCatalogoVigente } from '@/features/catalogo/catalogoVigente';

type Origen = 'manual' | 'cotizacion' | 'documento';

const redondear = (n: number) => Math.round(n * 100) / 100;

/** Cliente de la cotización, identificado por su id técnico. */
const clienteDeCotizacion = (c: Cotizacion | undefined) => c?.cliente?.id ?? null;

/** Líneas de la cotización en el formato del editor, con el descuento global convertido a soles. */
const lineasDeCotizacion = (c: Cotizacion) => {
  const lineas: LineaEditable[] = c.lineas.map((l) => ({
    id: l.id,
    sku: l.sku,
    cantidad: l.cantidad,
    precio: l.precio,
  }));
  return { lineas, descuento: redondear(subtotalLineas(lineas) * c.descuento) };
};

/** Pantalla del registro de venta (P20). Al terminar, una nueva ronda reinicia el formulario. */
const plural = (n: number, uno: string, varios: string) =>
  `${formatNumber(n)} ${n === 1 ? uno : varios}`;

export function VentaFormPage() {
  const [ronda, setRonda] = useState(0);
  return <RegistroVenta key={ronda} onOtra={() => setRonda((r) => r + 1)} />;
}

function RegistroVenta({ onOtra }: { onOtra: () => void }) {
  const queryClient = useQueryClient();
  // Copia local del catálogo vigente para los selectores y las búsquedas por SKU (API real).
  useCatalogoVigente();
  const [params] = useSearchParams();
  const cotizacionParam = params.get('cotizacion');
  const inicial = cotizacionParam ? cotizacionUtilizable(cotizacionParam) : undefined;
  const carga = inicial ? lineasDeCotizacion(inicial) : undefined;

  const [origen, setOrigen] = useState<Origen>(cotizacionParam ? 'cotizacion' : 'manual');
  const [confirmando, setConfirmando] = useState(false);
  // Cada apertura de la confirmación inicia una intención nueva; sus reintentos reutilizan la misma clave.
  const [claveRegistro, setClaveRegistro] = useState(() => crypto.randomUUID());
  const [resultado, setResultado] = useState<ResultadoVenta | null>(null);
  // Bajo la clave `cotizaciones`, para que validar, aceptar o convertir una cotización renueve esta lista; siempre se
  // vuelve a consultar al abrir el formulario, porque una cotización recién validada debe aparecer en el acto.
  const cotizaciones = useQuery({
    queryKey: ['cotizaciones', 'convertibles'],
    queryFn: listarCotizacionesAceptadas,
    staleTime: 0,
    refetchOnMount: 'always',
  });

  const stock = stockLibre();
  const form = useForm({
    initialValues: {
      cliente: inicial ? clienteDeCotizacion(inicial) : null,
      cotizacionId: inicial?.id ?? null,
      lineas: carga?.lineas ?? [],
      descuento: carga?.descuento ?? 0,
      fechaEntrega: '',
      modalidad: null,
    },
    validate: zod4Resolver(crearEsquemaVenta(stock)),
  });
  const { values } = form;
  const cotizacionPrecargada = useRef(!cotizacionParam || Boolean(inicial));
  useEffect(() => {
    if (!cotizacionParam || !cotizaciones.isSuccess || cotizacionPrecargada.current) return;
    const cotizacion = cotizaciones.data.find((c) => c.id === cotizacionParam);
    // Con la lista aún sin la cotización (datos previos), se espera a la consulta actualizada antes de rendirse.
    if (!cotizacion) return;
    cotizacionPrecargada.current = true;
    const cargaCotizacion = lineasDeCotizacion(cotizacion);
    form.setValues({
      cliente: clienteDeCotizacion(cotizacion),
      cotizacionId: cotizacion.id,
      lineas: cargaCotizacion.lineas,
      descuento: cargaCotizacion.descuento,
      modalidad: null,
    });
  }, [cotizacionParam, cotizaciones.data, cotizaciones.isSuccess, form]);
  const precarga = usePrecargaDocumento((lineas, repetidos) => {
    form.setFieldValue('lineas', lineas);
    avisarRepetidos(repetidos);
    setOrigen('documento');
  });

  /** Con una cotización elegida, sus productos, precios y descuento son los de la cotización y no se editan. */
  const desdeCotizacion = origen === 'cotizacion' && values.cotizacionId !== null;
  const pedido = values.lineas.map((l) => ({ sku: l.sku, cantidad: l.cantidad }));
  const sinCobertura = calcularAtencion(pedido, stock, null).totalPendiente > 0;
  const vista = calcularAtencion(pedido, stock, values.modalidad);
  const subtotal = subtotalLineas(values.lineas);
  const total = totalLineas(values.lineas, values.descuento);

  const motivo = (() => {
    if (values.lineas.length === 0 || values.lineas.some((l) => !l.sku))
      return 'Agregar al menos un producto.';
    if (
      origen === 'cotizacion' &&
      !(cotizaciones.data ?? []).some((c) => c.id === values.cotizacionId)
    )
      return 'La cotización debe estar aceptada y sin convertir en venta.';
    if (values.cliente === null) return 'Elegir el cliente o continuar sin cliente.';
    if (sinCobertura && values.modalidad === null)
      return 'Elegir cómo atender las líneas sin existencias.';
    if (!values.fechaEntrega) return 'Indicar la fecha de entrega requerida por el cliente.';
    if (values.fechaEntrega < hoyIso())
      return 'La fecha de entrega requerida no puede ser anterior a hoy.';
    if (values.descuento < 0 || values.descuento > subtotal)
      return 'El descuento no puede ser negativo ni mayor que el subtotal.';
    return null;
  })();

  const registrar = useMutation({
    mutationFn: (entrada: EntradaVenta) => registrarVenta(entrada),
    onSuccess: async (r) => {
      await queryClient.invalidateQueries({ queryKey: ['ventas'] });
      await queryClient.invalidateQueries({ queryKey: ['cotizaciones'] });
      setConfirmando(false);
      setResultado(r);
    },
    onError: (e) => {
      setConfirmando(false);
      notifications.show({
        color: 'danger',
        title: 'No se pudo registrar la venta',
        message: e.message,
      });
    },
  });

  /** Cambiar el origen descarta lo cargado (cliente, productos y descuento) para no mezclar datos de otro origen. */
  const cambiarOrigen = (valor: string) => {
    setOrigen(valor as Origen);
    form.setValues({
      cliente: null,
      cotizacionId: null,
      lineas: [],
      descuento: 0,
      fechaEntrega: '',
      modalidad: null,
    });
    form.clearErrors();
  };

  const elegirCotizacion = (id: string | null) => {
    const c = cotizaciones.data?.find((x) => x.id === id);
    if (!c) {
      form.setFieldValue('cotizacionId', id);
      return;
    }
    const cargaCotizacion = lineasDeCotizacion(c);
    form.setValues({
      ...values,
      cotizacionId: c.id,
      cliente: clienteDeCotizacion(c),
      lineas: cargaCotizacion.lineas,
      descuento: cargaCotizacion.descuento,
    });
  };

  const confirmar = () => {
    const entrada: EntradaVenta = {
      clienteId: values.cliente === SIN_CLIENTE ? undefined : (values.cliente ?? undefined),
      cotizacionId: origen === 'cotizacion' ? (values.cotizacionId ?? undefined) : undefined,
      descuento: values.descuento,
      modalidad: values.modalidad as ModalidadAtencion | null,
      fechaEntregaRequerida: values.fechaEntrega,
      claveIdempotencia: claveRegistro,
      lineas: consolidarLineas(values.lineas).lineas.map((l) => ({
        sku: l.sku,
        nombre: PRODUCTOS.find((p) => p.sku === l.sku)?.nombre ?? l.sku,
        cantidad: l.cantidad,
        precio: l.precio,
      })),
    };
    registrar.mutate(entrada);
  };

  if (resultado) {
    const { venta, atencion, cambios } = resultado;
    return (
      <>
        <PageHeader title="Registrar venta" />
        <Constancia
          sello={<StatusBadge entity="venta" value={venta.estado} variant="sello" />}
          title={`Venta ${venta.id} registrada`}
          description={`${venta.lineas.length} productos. Descuento ${formatCurrency(venta.descuento)}.`}
          cambios={cambios}
          nota={
            atencion.totalPendiente > 0
              ? `Quedan ${formatNumber(atencion.totalPendiente)} unidades pendientes de abastecimiento.`
              : undefined
          }
          actions={
            <>
              <Button component={Link} to={`/ventas/${venta.id}`}>
                Ver la venta
              </Button>
              <Button variant="default" onClick={onOtra}>
                Registrar otra venta
              </Button>
            </>
          }
        />
      </>
    );
  }

  const filas = values.lineas
    .filter((l) => l.sku)
    .map((l) => {
      const p = PRODUCTOS.find((x) => x.sku === l.sku);
      const libre = Math.max(0, stock[l.sku] ?? 0);
      return {
        id: l.id,
        nombre: p?.nombre ?? l.sku,
        cantidad: l.cantidad,
        libre,
        unidad: p?.unidad ?? '',
      };
    });

  /** Desglose de lo que falta con el stock libre actual, independiente de la modalidad que se elija. */
  const desglose = calcularAtencion(pedido, stock, 'parcial')
    .lineas.filter((l) => l.pendiente > 0)
    .map((l) => {
      const p = PRODUCTOS.find((x) => x.sku === l.sku);
      return { ...l, nombre: p?.nombre ?? l.sku, unidad: p?.unidad ?? '' };
    });
  const columnasDesglose: Column<(typeof desglose)[number]>[] = [
    { key: 'nombre', header: 'Producto', primary: true },
    {
      key: 'solicitada',
      header: 'Solicitada',
      numeric: true,
      render: (f) => formatNumber(f.solicitada),
    },
    {
      key: 'atendida',
      header: 'Disponible',
      numeric: true,
      render: (f) => formatNumber(f.atendida),
    },
    {
      key: 'pendiente',
      header: 'Falta abastecer',
      numeric: true,
      render: (f) => (
        <strong>
          {formatNumber(f.pendiente)} {f.unidad}
        </strong>
      ),
    },
  ];

  /** Detalle de lo que falta: solo los productos con cantidad pendiente, con su nombre y unidad. */
  const faltantes = vista.lineas
    .filter((l) => l.pendiente > 0)
    .map((l) => {
      const p = PRODUCTOS.find((x) => x.sku === l.sku);
      return { ...l, nombre: p?.nombre ?? l.sku, unidad: p?.unidad ?? '' };
    });

  const columnas: Column<(typeof filas)[number]>[] = [
    { key: 'nombre', header: 'Producto', primary: true },
    {
      key: 'cantidad',
      header: 'Solicitada',
      numeric: true,
      render: (f) => formatNumber(f.cantidad),
    },
    {
      key: 'libre',
      header: 'Stock libre',
      numeric: true,
      render: (f) => (
        <>
          {f.cantidad > f.libre ? (
            <span className={classes.alerta}>
              <Icon name="alert-triangle" size={16} /> Stock libre: {formatNumber(f.libre)}
            </span>
          ) : (
            formatNumber(f.libre)
          )}
          <StockBar
            libre={f.libre}
            reservado={0}
            compact
            unidad={f.unidad}
            label={`Stock de ${f.nombre}`}
          />
        </>
      ),
    },
  ];

  return (
    <>
      <FormPage
        title="Registrar venta"
        description="Venta a un cliente con productos, descuento y modalidad de atención."
        breadcrumbs={[{ label: 'Ventas', href: '/ventas' }, { label: 'Registrar venta' }]}
        summary={
          <Text size="sm">
            {values.lineas.length} productos, subtotal {formatCurrency(subtotal)}, descuento{' '}
            {formatCurrency(values.descuento)}, total {formatCurrency(total)}
          </Text>
        }
        reason={motivo}
        submitLabel="Confirmar venta"
        canSubmit={motivo === null}
        onSubmit={form.onSubmit(() => {
          setClaveRegistro(crypto.randomUUID());
          setConfirmando(true);
        })}
        onCancel={() => window.history.back()}
      >
        <Card title="Cliente">
          <Stack gap="md">
            <ClientePicker
              value={values.cliente}
              onChange={(v) => form.setFieldValue('cliente', v)}
              error={form.errors.cliente as string | undefined}
              bloqueado={origen === 'cotizacion' && values.cotizacionId !== null}
            />
            <Radio.Group
              label="Origen de los productos"
              description={
                cotizacionParam
                  ? 'La venta se registra desde la cotización elegida; el origen no se puede cambiar.'
                  : undefined
              }
              value={origen}
              onChange={cambiarOrigen}
            >
              <Stack gap="xs" mt="xs">
                <Radio value="manual" label="Manual" disabled={Boolean(cotizacionParam)} />
                <Radio value="cotizacion" label="Desde una cotización aceptada" />
                <Radio
                  value="documento"
                  label="Desde un documento"
                  disabled={Boolean(cotizacionParam)}
                />
              </Stack>
            </Radio.Group>
            {origen === 'cotizacion' && (
              <Select
                label="Cotización aceptada"
                placeholder="Elegir una cotización"
                nothingFoundMessage="No hay cotizaciones aceptadas sin convertir"
                data={(cotizaciones.data ?? []).map((c) => ({
                  value: c.id,
                  label: `${c.id}, ${c.cliente?.nombre ?? 'sin cliente'}`,
                }))}
                value={values.cotizacionId}
                onChange={elegirCotizacion}
                disabled={Boolean(cotizacionParam)}
              />
            )}
            {cotizaciones.isSuccess &&
              !cotizaciones.isFetching &&
              cotizacionParam &&
              !(cotizaciones.data ?? []).some((c) => c.id === cotizacionParam) && (
                <Alert tone="warning" title="Cotización no disponible">
                  La cotización no está aceptada o ya se convirtió en venta.
                </Alert>
              )}
            {origen === 'documento' && (
              <div>
                <Button component={Link} to="/documentos/procesar?destino=venta" variant="default">
                  Procesar un documento
                </Button>
              </div>
            )}
          </Stack>
        </Card>
        <Card title="Entrega">
          <TextInput
            label="Fecha de entrega requerida"
            description="Es la fecha en que el cliente necesita los productos; ayuda a planificar el abastecimiento."
            required
            type="date"
            min={hoyIso()}
            {...form.getInputProps('fechaEntrega')}
          />
        </Card>
        <Card title="Productos">
          {precarga.habilitado && (
            <Alert tone="info" title="Productos cargados desde el documento">
              Revisar las cantidades y completar el precio de cada línea.
            </Alert>
          )}
          <LineasEditor
            lineas={values.lineas}
            onChange={(lineas) => form.setFieldValue('lineas', lineas)}
            productos={opcionesProducto()}
            conPrecio
            precargarPrecio
            conDescuento
            soloLectura={desdeCotizacion}
            descuento={values.descuento}
            onDescuentoChange={(d) => form.setFieldValue('descuento', d)}
            error={form.errors.lineas as string | undefined}
          />
          {form.errors.descuento && (
            <Text size="sm" c="danger">
              {form.errors.descuento as string}
            </Text>
          )}
        </Card>
        {filas.length > 0 && (
          <Card title="Disponibilidad" padded={false}>
            <DataTable
              caption="Disponibilidad de las líneas"
              columns={columnas}
              rows={filas}
              rowKey="id"
            />
          </Card>
        )}
        {sinCobertura && (
          <Card title="Atención de las líneas sin existencias">
            <Stack gap="md">
              <DataTable
                caption="Productos sin existencias suficientes"
                columns={columnasDesglose}
                rows={desglose}
                rowKey="sku"
              />
              <Radio.Group
                label="Modalidad de atención"
                description="Las líneas que superan el stock libre no pueden atenderse completas."
                {...form.getInputProps('modalidad')}
              >
                <Stack gap="xs" mt="xs">
                  {(Object.keys(MODALIDADES) as ModalidadAtencion[]).map((m) => (
                    <Radio key={m} value={m} label={MODALIDADES[m]} />
                  ))}
                </Stack>
              </Radio.Group>
            </Stack>
          </Card>
        )}
      </FormPage>
      <ConfirmDialog
        open={confirmando}
        title="Confirmar la venta"
        consequences={[
          `Se reservará el stock de ${plural(vista.lineas.filter((l) => l.atendida > 0).length, 'producto', 'productos')}. La salida del Kardex se registra al confirmar el despacho.`,
          ...(vista.totalPendiente > 0
            ? [
                `Quedan ${formatNumber(vista.totalPendiente)} unidades pendientes en ${plural(faltantes.length, 'producto', 'productos')}:`,
                ...faltantes
                  .slice(0, 8)
                  .map(
                    (f) =>
                      `• ${f.nombre}: faltan ${formatNumber(f.pendiente)} de ${formatNumber(f.solicitada)} ${f.unidad}`,
                  ),
                ...(faltantes.length > 8 ? [`… y ${faltantes.length - 8} productos más.`] : []),
              ]
            : []),
        ]}
        confirmLabel="Confirmar venta"
        cancelLabel="Volver"
        tone="primary"
        loading={registrar.isPending}
        onConfirm={confirmar}
        onCancel={() => setConfirmando(false)}
      />
    </>
  );
}
