import { Button, Radio, Select, Stack, Text } from '@mantine/core';
import { useForm } from '@mantine/form';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { zod4Resolver } from 'mantine-form-zod-resolver';
import { useState } from 'react';
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
  subtotalLineas,
  totalLineas,
  type LineaEditable,
} from '@/components/ui/LineasEditor/calculos';
import { ConfirmDialog } from '@/components/ui/Modal/ConfirmDialog';
import { opcionesProducto } from '@/features/catalogo/opciones';
import { PRODUCTOS } from '@/features/inventario/fixtures';
import { formatCurrency, formatNumber } from '@/lib/format';
import { ClientePicker, SIN_CLIENTE } from '../../clientes/ClientePicker';
import { CLIENTES } from '../../clientes/fixtures';
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
import classes from './VentaForm.module.css';

type Origen = 'manual' | 'cotizacion' | 'documento';

const redondear = (n: number) => Math.round(n * 100) / 100;

/** Cliente registrado con el mismo RUC que la cotización, si existe. */
const clienteDeCotizacion = (c: Cotizacion | undefined) =>
  CLIENTES.find((cl) => cl.documento === c?.cliente?.ruc)?.id ?? null;

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
export function VentaFormPage() {
  const [ronda, setRonda] = useState(0);
  return <RegistroVenta key={ronda} onOtra={() => setRonda((r) => r + 1)} />;
}

function RegistroVenta({ onOtra }: { onOtra: () => void }) {
  const queryClient = useQueryClient();
  const [params] = useSearchParams();
  const cotizacionParam = params.get('cotizacion');
  const inicial = cotizacionParam ? cotizacionUtilizable(cotizacionParam) : undefined;
  const carga = inicial ? lineasDeCotizacion(inicial) : undefined;

  const [origen, setOrigen] = useState<Origen>(inicial ? 'cotizacion' : 'manual');
  const [confirmando, setConfirmando] = useState(false);
  const [resultado, setResultado] = useState<ResultadoVenta | null>(null);
  const cotizaciones = useQuery({
    queryKey: ['cotizaciones-aceptadas'],
    queryFn: listarCotizacionesAceptadas,
  });

  const stock = stockLibre();
  const form = useForm({
    initialValues: {
      cliente: inicial ? clienteDeCotizacion(inicial) : null,
      cotizacionId: inicial?.id ?? null,
      lineas: carga?.lineas ?? [],
      descuento: carga?.descuento ?? 0,
      modalidad: null,
    },
    validate: zod4Resolver(crearEsquemaVenta(stock)),
  });
  const { values } = form;

  const pedido = values.lineas.map((l) => ({ sku: l.sku, cantidad: l.cantidad }));
  const sinCobertura = calcularAtencion(pedido, stock, null).totalPendiente > 0;
  const vista = calcularAtencion(pedido, stock, values.modalidad);
  const subtotal = subtotalLineas(values.lineas);
  const total = totalLineas(values.lineas, values.descuento);

  const motivo = (() => {
    if (values.lineas.length === 0 || values.lineas.some((l) => !l.sku))
      return 'Agregar al menos un producto.';
    if (origen === 'cotizacion' && !cotizacionUtilizable(values.cotizacionId ?? ''))
      return 'La cotización debe estar aceptada y sin convertir en venta.';
    if (values.cliente === null) return 'Elegir el cliente o continuar sin cliente.';
    if (sinCobertura && values.modalidad === null)
      return 'Elegir cómo atender las líneas sin existencias.';
    if (values.descuento < 0 || values.descuento > subtotal)
      return 'El descuento no puede ser negativo ni mayor que el subtotal.';
    return null;
  })();

  const registrar = useMutation({
    mutationFn: (entrada: EntradaVenta) => registrarVenta(entrada),
    onSuccess: async (r) => {
      await queryClient.invalidateQueries({ queryKey: ['ventas'] });
      setConfirmando(false);
      setResultado(r);
    },
  });

  const cambiarOrigen = (valor: string) => {
    setOrigen(valor as Origen);
    form.setFieldValue('cotizacionId', null);
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
      lineas: values.lineas.map((l) => ({
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
        onSubmit={form.onSubmit(() => setConfirmando(true))}
        onCancel={() => window.history.back()}
      >
        <Card title="Cliente">
          <Stack gap="md">
            <ClientePicker
              value={values.cliente}
              onChange={(v) => form.setFieldValue('cliente', v)}
              error={form.errors.cliente as string | undefined}
            />
            <Radio.Group label="Origen de los productos" value={origen} onChange={cambiarOrigen}>
              <Stack gap="xs" mt="xs">
                <Radio value="manual" label="Manual" />
                <Radio value="cotizacion" label="Desde una cotización aceptada" />
                <Radio value="documento" label="Desde un documento" />
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
              />
            )}
            {cotizacionParam && !cotizacionUtilizable(cotizacionParam) && (
              <Alert tone="warning" title="Cotización no disponible">
                La cotización no está aceptada o ya se convirtió en venta.
              </Alert>
            )}
            {origen === 'documento' && (
              <div>
                <Button component={Link} to="/documentos/procesar" variant="default">
                  Procesar un documento
                </Button>
              </div>
            )}
          </Stack>
        </Card>
        <Card title="Productos">
          <LineasEditor
            lineas={values.lineas}
            onChange={(lineas) => form.setFieldValue('lineas', lineas)}
            productos={opcionesProducto()}
            conPrecio
            conDescuento
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
          </Card>
        )}
      </FormPage>
      <ConfirmDialog
        open={confirmando}
        title="Confirmar la venta"
        consequences={[
          `Se registrarán ${formatNumber(vista.lineas.filter((l) => l.atendida > 0).length)} salidas en el Kardex.`,
          ...(vista.totalPendiente > 0
            ? [`Quedan ${formatNumber(vista.totalPendiente)} unidades pendientes.`]
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
