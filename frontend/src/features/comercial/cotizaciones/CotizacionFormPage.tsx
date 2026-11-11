import { Button, NumberInput, Select, Skeleton, Stack, Text, Textarea } from '@mantine/core';
import { useForm } from '@mantine/form';
import { notifications } from '@mantine/notifications';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { zod4Resolver } from 'mantine-form-zod-resolver';
import { useState } from 'react';
import { useNavigate, useParams } from 'react-router';
import { STATUS } from '@/components/domain/StatusBadge/status';
import { PageHeader } from '@/components/layout/PageHeader/PageHeader';
import { Alert } from '@/components/ui/Alert/Alert';
import { Card } from '@/components/ui/Card/Card';
import { FormPage } from '@/components/ui/FormPage/FormPage';
import { Icon } from '@/components/ui/Icon/Icon';
import { subtotalLineas, totalLineas } from '@/components/ui/LineasEditor/calculos';
import { LineasEditor } from '@/components/ui/LineasEditor/LineasEditor';
import { ConfirmDialog } from '@/components/ui/Modal/ConfirmDialog';
import { opcionesProducto } from '@/features/catalogo/opciones';
import { formatCurrency, formatDate } from '@/lib/format';
import {
  actualizarCotizacion,
  crearCotizacion,
  obtenerCotizacion,
  type DatosCotizacion,
} from '../api';
import { diasEntre, hoyIso, sumarDias, totales } from '../calculos';
import { listarClientes } from '../clientes/api';
import type { Cotizacion } from '../types';
import {
  esquemaCotizacion,
  FORMAS_PAGO,
  SIN_CLIENTE,
  VALIDEZ,
  type ValoresCotizacion,
} from './esquema';
import { usePrecargaDocumento } from '@/features/documentos-ocr/usePrecargaDocumento';
import classes from './CotizacionForm.module.css';

/** Valores iniciales: un formulario vacío para una cotización nueva, o los datos de la cotización a modificar. */
function valoresIniciales(cotizacion?: Cotizacion): ValoresCotizacion {
  if (!cotizacion) {
    return {
      clienteId: '',
      lineas: [],
      descuento: 0,
      validezDias: VALIDEZ.defecto,
      formaPago: FORMAS_PAGO[0],
      observaciones: '',
    };
  }
  const { cliente } = cotizacion;
  return {
    clienteId: cliente ? cliente.id : SIN_CLIENTE,
    lineas: cotizacion.lineas.map((l) => ({
      id: l.id,
      sku: l.sku,
      cantidad: l.cantidad,
      precio: l.precio,
    })),
    descuento: totales(cotizacion).descuento,
    validezDias: diasEntre(cotizacion.emision, cotizacion.vencimiento),
    formaPago: cotizacion.formaPago ?? FORMAS_PAGO[0],
    observaciones: cotizacion.condiciones ?? '',
  };
}

/** Convierte los valores del formulario a los datos de la cotización: el descuento pasa de soles a fracción. */
function datosDesde(valores: ValoresCotizacion): DatosCotizacion {
  const subtotal = subtotalLineas(valores.lineas);
  return {
    clienteId: valores.clienteId === SIN_CLIENTE ? undefined : valores.clienteId,
    lineas: valores.lineas.map(({ sku, cantidad, precio }) => ({ sku, cantidad, precio })),
    descuento: subtotal > 0 ? Math.round((valores.descuento / subtotal) * 1e6) / 1e6 : 0,
    validezDias: valores.validezDias,
    formaPago: valores.formaPago,
    condiciones: valores.observaciones || undefined,
  };
}

interface CotizacionFormProps {
  /** Cotización a modificar; ausente para emitir una nueva. */
  cotizacion?: Cotizacion;
}

/** CU-09. Formulario de emisión y modificación de una cotización, con cliente, productos y condiciones. */
function CotizacionForm({ cotizacion }: CotizacionFormProps) {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const editando = cotizacion !== undefined;
  const soloLectura = cotizacion !== undefined && cotizacion.estado !== 'Emitida';
  const hoy = hoyIso();
  const [pendiente, setPendiente] = useState<ValoresCotizacion | null>(null);
  const clientes = useQuery({ queryKey: ['clientes'], queryFn: listarClientes }).data ?? [];

  const form = useForm<ValoresCotizacion>({
    initialValues: valoresIniciales(cotizacion),
    validate: zod4Resolver(esquemaCotizacion),
  });
  const { values } = form;
  const precarga = usePrecargaDocumento((lineas) => form.setFieldValue('lineas', lineas));

  const total = totalLineas(values.lineas, values.descuento);
  const numeroProductos = values.lineas.length;
  const vencimiento = sumarDias(cotizacion ? cotizacion.emision : hoy, values.validezDias);
  // Un error de la lista de líneas o del descuento se muestra en el editor, que no tiene campos individuales de error.
  const errorLineas =
    (Object.entries(form.errors).find(([k]) => k === 'lineas' || k.startsWith('lineas.'))?.[1] as
      string | undefined) ?? (form.errors.descuento as string | undefined);

  const guardar = useMutation({
    mutationFn: (datos: DatosCotizacion) =>
      cotizacion ? actualizarCotizacion(cotizacion.id, datos) : crearCotizacion(datos),
    onSuccess: (c) => {
      void queryClient.invalidateQueries({ queryKey: ['cotizaciones'] });
      void queryClient.invalidateQueries({ queryKey: ['cotizacion', c.id] });
      notifications.show({
        color: 'success',
        title: editando ? 'Cotización modificada' : 'Cotización emitida',
        message: editando
          ? `La cotización ${c.id} se guardó con los cambios.`
          : `La cotización ${c.id} quedó emitida y vence el ${formatDate(c.vencimiento)}.`,
      });
      void navigate(`/cotizaciones/${c.id}`);
    },
    onError: (e) => {
      setPendiente(null);
      notifications.show({
        color: 'danger',
        title: 'No se pudo guardar la cotización',
        message: e.message,
      });
    },
  });

  const enviar = (valores: ValoresCotizacion) => {
    if (editando) guardar.mutate(datosDesde(valores));
    else setPendiente(valores);
  };

  // Los clientes inactivos se listan deshabilitados para indicar por qué no se pueden elegir.
  const opcionesCliente = [
    { value: SIN_CLIENTE, label: 'Continuar sin cliente' },
    ...clientes.map((c) => ({
      value: c.id,
      label: `${c.nombre}, ${c.documento}${c.estado === 'Inactivo' ? ' (inactivo)' : ''}`,
      disabled: c.estado === 'Inactivo',
    })),
  ];

  const motivo = soloLectura
    ? 'La cotización no se puede modificar en su estado actual.'
    : numeroProductos === 0
      ? 'Agregar al menos un producto para emitir la cotización.'
      : null;

  return (
    <>
      <FormPage
        title={editando ? 'Editar cotización' : 'Nueva cotización'}
        description={
          editando
            ? 'Solo las cotizaciones emitidas se pueden modificar.'
            : 'Emitir una cotización a un cliente. No reserva existencias.'
        }
        breadcrumbs={
          editando
            ? [
                { label: 'Cotizaciones', href: '/cotizaciones' },
                { label: cotizacion.id, href: `/cotizaciones/${cotizacion.id}` },
                { label: 'Editar' },
              ]
            : [{ label: 'Cotizaciones', href: '/cotizaciones' }, { label: 'Nueva cotización' }]
        }
        summary={
          <Text fw={600}>
            {numeroProductos === 1 ? '1 producto' : `${numeroProductos} productos`}, total{' '}
            {formatCurrency(total)}
          </Text>
        }
        reason={motivo}
        submitLabel={editando ? 'Guardar cambios' : 'Emitir cotización'}
        submitting={guardar.isPending}
        canSubmit={!soloLectura && numeroProductos > 0}
        onSubmit={form.onSubmit(enviar)}
        onCancel={() =>
          void navigate(editando ? `/cotizaciones/${cotizacion.id}` : '/cotizaciones')
        }
      >
        {soloLectura && (
          <Alert tone="info" title="Cotización de solo lectura">
            La cotización está en estado {STATUS.cotizacion[cotizacion.estado][1]} y no se puede
            modificar.
          </Alert>
        )}
        <fieldset className={classes.fieldset} disabled={soloLectura}>
          <Stack gap="lg">
            <Card title="Cliente">
              <Select
                label="Cliente"
                placeholder="Buscar por nombre o documento"
                description="Los clientes inactivos no se pueden elegir."
                searchable
                nothingFoundMessage="Ningún cliente coincide con la búsqueda"
                data={opcionesCliente}
                value={values.clienteId || null}
                onChange={(v) => form.setFieldValue('clienteId', v ?? '')}
                error={form.errors.clienteId}
              />
            </Card>

            <Card
              title="Productos"
              padded={false}
              actions={
                <Button
                  variant="default"
                  leftSection={<Icon name="scan" />}
                  onClick={() => void navigate('/documentos/procesar?destino=cotizacion')}
                >
                  Cargar desde un documento
                </Button>
              }
            >
              {precarga.habilitado && (
                <Alert tone="info" title="Productos cargados desde el documento">
                  Revisar las cantidades y completar el precio de cada línea.
                </Alert>
              )}
              <div className={classes.lineas}>
                <LineasEditor
                  lineas={values.lineas}
                  onChange={(lineas) => form.setFieldValue('lineas', lineas)}
                  productos={opcionesProducto()}
                  conPrecio
                  conDescuento
                  descuento={values.descuento}
                  onDescuentoChange={(d) => form.setFieldValue('descuento', d)}
                  error={errorLineas}
                />
              </div>
            </Card>

            <Card title="Condiciones">
              <Stack gap="md">
                <NumberInput
                  label="Validez en días"
                  description={`Vence el ${formatDate(vencimiento)}`}
                  min={VALIDEZ.min}
                  max={VALIDEZ.max}
                  clampBehavior="none"
                  allowDecimal={false}
                  allowNegative={false}
                  value={values.validezDias}
                  onChange={(v) => form.setFieldValue('validezDias', Number(v) || 0)}
                  error={form.errors.validezDias}
                />
                <Select
                  label="Forma de pago"
                  data={[...FORMAS_PAGO]}
                  allowDeselect={false}
                  {...form.getInputProps('formaPago')}
                />
                <Textarea
                  label="Observaciones (opcional)"
                  autosize
                  minRows={2}
                  {...form.getInputProps('observaciones')}
                />
              </Stack>
            </Card>
          </Stack>
        </fieldset>
      </FormPage>

      <ConfirmDialog
        open={pendiente !== null}
        title="Emitir la cotización"
        description={
          pendiente
            ? `Se emitirá con vencimiento el ${formatDate(sumarDias(hoy, pendiente.validezDias))}.`
            : undefined
        }
        consequences={['La emisión no modifica el stock.']}
        confirmLabel="Emitir cotización"
        loading={guardar.isPending}
        onConfirm={() => {
          if (pendiente) guardar.mutate(datosDesde(pendiente));
        }}
        onCancel={() => setPendiente(null)}
      />
    </>
  );
}

/** Pantalla de alta (`nueva`) y de modificación (`:id/editar`) de una cotización. */
export function CotizacionFormPage() {
  const { id } = useParams();
  const query = useQuery({
    queryKey: ['cotizacion', id],
    queryFn: () => obtenerCotizacion(id ?? ''),
    enabled: id !== undefined,
  });

  if (id === undefined) return <CotizacionForm />;
  if (query.isError) {
    return (
      <>
        <PageHeader
          title="Cotización no encontrada"
          breadcrumbs={[{ label: 'Cotizaciones', href: '/cotizaciones' }, { label: id }]}
        />
        <Alert tone="warning" title="La cotización no existe">
          Volver al listado y elegir otra cotización.
        </Alert>
      </>
    );
  }
  if (!query.data) return <Skeleton height={240} />;
  return <CotizacionForm key={query.data.id} cotizacion={query.data} />;
}
