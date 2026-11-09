import { Anchor, Select, Stack, Text, TextInput } from '@mantine/core';
import { useForm } from '@mantine/form';
import { notifications } from '@mantine/notifications';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { zod4Resolver } from 'mantine-form-zod-resolver';
import { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router';
import { Alert } from '@/components/ui/Alert/Alert';
import { Card } from '@/components/ui/Card/Card';
import { FormPage } from '@/components/ui/FormPage/FormPage';
import { ConfirmDialog } from '@/components/ui/Modal/ConfirmDialog';
import { LineasEditor } from '@/components/ui/LineasEditor/LineasEditor';
import { totalLineas } from '@/components/ui/LineasEditor/calculos';
import { opcionesProducto } from '@/features/catalogo/opciones';
import { PRODUCTOS } from '@/features/inventario/fixtures';
import { formatCurrency, formatNumber } from '@/lib/format';
import {
  cantidadParaMinimo,
  crearOrdenCompra,
  listarProveedoresCompra,
  precioReferencia,
} from './api';
import { esquemaOrden, hoyISO, type ValoresOrden } from './esquema';

/** Línea precargada desde una alerta de stock crítico (`?sku=`): cantidad para superar el mínimo. */
function lineaInicial(sku: string | null) {
  if (!sku || !PRODUCTOS.some((p) => p.sku === sku)) return [];
  return [{ id: 1, sku, cantidad: cantidadParaMinimo(sku), precio: precioReferencia(sku) }];
}

/** P26: formulario de una orden de compra nueva, con proveedor, fecha y productos (CU-12, flujo principal). */
export function OrdenCompraFormPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [params] = useSearchParams();
  const [confirmando, setConfirmando] = useState(false);
  const proveedores = useQuery({
    queryKey: ['proveedores-compra'],
    queryFn: listarProveedoresCompra,
  });

  const form = useForm<ValoresOrden>({
    initialValues: {
      proveedorId: '',
      fecha: hoyISO(),
      lineas: lineaInicial(params.get('sku')),
    },
    validate: zod4Resolver(esquemaOrden),
  });

  const { lineas, proveedorId } = form.values;
  const proveedor = proveedores.data?.find((p) => p.id === proveedorId);
  const inactivos = proveedores.data?.filter((p) => p.estado === 'Inactivo') ?? [];
  const total = totalLineas(lineas);
  const cantidadProductos = lineas.length;
  const motivo = !proveedorId
    ? 'Seleccionar un proveedor para crear la orden.'
    : lineas.length === 0
      ? 'Agregar al menos un producto para crear la orden.'
      : null;

  const mutation = useMutation({
    mutationFn: (v: ValoresOrden) =>
      crearOrdenCompra({
        proveedorId: v.proveedorId,
        fecha: v.fecha,
        lineas: v.lineas.map(({ sku, cantidad, precio }) => ({ sku, cantidad, precio })),
      }),
    onSuccess: (orden) => {
      void queryClient.invalidateQueries({ queryKey: ['ordenes-compra'] });
      notifications.show({
        color: 'success',
        title: 'Orden de compra creada',
        message: `La orden ${orden.id} quedó pendiente de recepción.`,
      });
      void navigate(`/compras/${orden.id}`);
    },
    onError: (e) => {
      setConfirmando(false);
      notifications.show({
        color: 'danger',
        title: 'No se pudo crear la orden de compra',
        message: e.message,
      });
    },
  });

  return (
    <>
      <FormPage
        title="Nueva orden de compra"
        description="Registrar una orden al proveedor. Queda pendiente hasta su recepción."
        breadcrumbs={[{ label: 'Órdenes de compra', href: '/compras' }, { label: 'Nueva' }]}
        submitLabel="Guardar orden de compra"
        summary={`${formatNumber(cantidadProductos)} ${cantidadProductos === 1 ? 'producto' : 'productos'}, total ${formatCurrency(total)}`}
        reason={motivo}
        canSubmit={motivo === null}
        onSubmit={form.onSubmit(() => setConfirmando(true))}
        onCancel={() => void navigate('/compras')}
      >
        <Card title="Proveedor">
          <Stack gap="sm">
            <Select
              label="Proveedor"
              placeholder="Seleccionar proveedor"
              searchable
              required
              nothingFoundMessage="Ningún proveedor coincide con la búsqueda"
              data={(proveedores.data ?? []).map((p) => ({
                value: p.id,
                label: p.estado === 'Inactivo' ? `${p.razonSocial} (Inactivo)` : p.razonSocial,
                disabled: p.estado === 'Inactivo',
              }))}
              {...form.getInputProps('proveedorId')}
              value={proveedorId || null}
            />
            {inactivos.length > 0 && (
              <Alert tone="info" title="Proveedores inactivos">
                <Text size="sm">
                  Los proveedores inactivos no aceptan órdenes nuevas.{' '}
                  <Anchor component={Link} to="/proveedores/nuevo">
                    Registrar proveedor
                  </Anchor>
                </Text>
              </Alert>
            )}
            <TextInput
              label="Fecha de la orden"
              type="date"
              required
              min={hoyISO()}
              {...form.getInputProps('fecha')}
            />
          </Stack>
        </Card>
        <Card
          title="Productos"
          description="Los productos que suministra el proveedor aparecen primero."
        >
          <LineasEditor
            lineas={lineas}
            onChange={(nuevas) => form.setFieldValue('lineas', nuevas)}
            productos={opcionesProducto()}
            conPrecio
            sugeridos={proveedor?.skus}
            error={typeof form.errors.lineas === 'string' ? form.errors.lineas : undefined}
          />
        </Card>
      </FormPage>
      <ConfirmDialog
        open={confirmando}
        title={`Crear la orden de compra para ${proveedor?.razonSocial ?? 'el proveedor'}`}
        consequences={[
          `Se registra en estado Pendiente con ${formatNumber(cantidadProductos)} productos.`,
          `Total de la orden: ${formatCurrency(total)}.`,
        ]}
        confirmLabel="Crear orden de compra"
        loading={mutation.isPending}
        onConfirm={() => mutation.mutate(form.values)}
        onCancel={() => setConfirmando(false)}
      />
    </>
  );
}
