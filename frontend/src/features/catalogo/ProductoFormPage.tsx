import { NumberInput, Select, Skeleton, Stack, Switch, Text, TextInput, Textarea } from '@mantine/core';
import { useForm } from '@mantine/form';
import { notifications } from '@mantine/notifications';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { zod4Resolver } from 'mantine-form-zod-resolver';
import { Link, useNavigate, useParams } from 'react-router';
import { Alert } from '@/components/ui/Alert/Alert';
import { Card } from '@/components/ui/Card/Card';
import { FormPage } from '@/components/ui/FormPage/FormPage';
import { formatNumber } from '@/lib/format';
import {
  actualizarProducto,
  listarCategorias,
  obtenerProducto,
  registrarProducto,
  SkuDuplicadoError,
  type DatosProducto,
  type ProductoCatalogo,
} from './api';
import { productoSchema, UNIDADES } from './esquema';

/** Vista previa del efecto del stock mínimo sobre las alertas. */
function efectoMinimo(minimo: number | string) {
  if (minimo === '' || Number.isNaN(Number(minimo)) || Number(minimo) < 0) return null;
  return `Alerta cuando el stock libre sea ${formatNumber(Number(minimo))} o menos.`;
}

interface FormularioProductoProps {
  inicial: ProductoCatalogo | null;
  categorias: string[];
}

/** Formulario montado con los datos ya cargados; se recrea por SKU para no sincronizar valores a mano. */
function FormularioProducto({ inicial, categorias }: FormularioProductoProps) {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const editando = inicial !== null;
  const form = useForm({
    initialValues: {
      sku: inicial?.sku ?? '',
      nombre: inicial?.nombre ?? '',
      descripcion: inicial?.descripcion ?? '',
      categoria: inicial?.categoria ?? '',
      unidad: inicial?.unidad ?? '',
      minimo: inicial?.minimo ?? '',
      precioReferencial: inicial?.precioReferencial ?? 0,
      activo: inicial ? inicial.estado === 'Activo' : true,
    },
    validate: zod4Resolver(productoSchema),
  });

  const mutation = useMutation({
    mutationFn: (valores: typeof form.values) => {
      const datos: DatosProducto = {
        sku: valores.sku.trim().toUpperCase(),
        nombre: valores.nombre.trim(),
        descripcion: valores.descripcion.trim(),
        categoria: valores.categoria,
        unidad: valores.unidad,
        minimo: Number(valores.minimo),
        precioReferencial: Number(valores.precioReferencial),
        estado: valores.activo ? 'Activo' : 'Inactivo',
      };
      return inicial ? actualizarProducto(inicial.sku, datos) : registrarProducto(datos);
    },
    onSuccess: (producto) => {
      void queryClient.invalidateQueries({ queryKey: ['catalogo'] });
      void queryClient.invalidateQueries({ queryKey: ['catalogo-producto'] });
      notifications.show({
        color: 'success',
        title: editando ? 'Cambios guardados' : 'Producto registrado',
        message: `${producto.nombre} (${producto.sku}).`,
      });
      void navigate(`/catalogo/${producto.sku}`);
    },
    onError: (error) => {
      if (error instanceof SkuDuplicadoError) {
        form.setFieldError('sku', error.message);
        return;
      }
      notifications.show({
        color: 'danger',
        title: 'No se pudo guardar el producto',
        message: error.message,
      });
    },
  });

  const cancelar = () => void navigate(inicial ? `/catalogo/${inicial.sku}` : '/catalogo');
  const vistaPrevia = efectoMinimo(form.values.minimo);

  return (
    <FormPage
      title={editando ? 'Modificar producto' : 'Registrar producto'}
      breadcrumbs={[
        { label: 'Catálogo', href: '/catalogo' },
        { label: editando ? 'Modificar producto' : 'Registrar producto' },
      ]}
      submitLabel={editando ? 'Guardar cambios' : 'Registrar producto'}
      submitting={mutation.isPending}
      onSubmit={form.onSubmit((valores) => mutation.mutate(valores))}
      onCancel={cancelar}
    >
      <Card title="Identificación">
        <Stack gap="md">
          <TextInput
            label="SKU"
            placeholder="CEM-049"
            description={editando ? 'El SKU no puede modificarse.' : 'Formato AAA-000.'}
            readOnly={editando}
            required
            {...form.getInputProps('sku')}
          />
          <TextInput label="Nombre oficial" required {...form.getInputProps('nombre')} />
          <Textarea
            label="Descripción"
            description="Opcional. Hasta 300 caracteres."
            autosize
            minRows={2}
            {...form.getInputProps('descripcion')}
          />
        </Stack>
      </Card>
      <Card title="Clasificación y stock">
        <Stack gap="md">
          <Select
            label="Categoría"
            placeholder="Seleccionar categoría"
            required
            data={categorias}
            allowDeselect={false}
            {...form.getInputProps('categoria')}
          />
          <Select
            label="Unidad de medida"
            placeholder="Seleccionar unidad"
            required
            data={[...UNIDADES]}
            allowDeselect={false}
            {...form.getInputProps('unidad')}
          />
          <NumberInput
            label="Precio referencial"
            description="Precio comercial de referencia para cotizaciones y ventas."
            min={0}
            decimalScale={2}
            allowNegative={false}
            clampBehavior="none"
            {...form.getInputProps('precioReferencial')}
          />
          <div>
            <NumberInput
              label="Stock mínimo"
              description="Se usa para las alertas de stock."
              required
              allowDecimal={false}
              allowNegative
              clampBehavior="none"
              {...form.getInputProps('minimo')}
            />
            {vistaPrevia && (
              <Text size="sm" c="dimmed" mt="xs">
                {vistaPrevia}
              </Text>
            )}
          </div>
          <Switch
            label={form.values.activo ? 'Activo' : 'Inactivo'}
            {...form.getInputProps('activo', { type: 'checkbox' })}
          />
        </Stack>
      </Card>
    </FormPage>
  );
}

/** CU-03. Registro de producto nuevo (`/catalogo/nuevo`) o modificación (`/catalogo/:sku/editar`). */
export function ProductoFormPage() {
  const { sku } = useParams();
  const editando = sku !== undefined;
  const categorias = useQuery({ queryKey: ['catalogo-categorias'], queryFn: listarCategorias });
  const producto = useQuery({
    queryKey: ['catalogo-producto', sku],
    queryFn: () => obtenerProducto(sku!),
    enabled: editando,
  });

  if (editando && producto.isPending) return <Skeleton height={320} radius="md" />;
  if (editando && producto.isError) {
    return (
      <Alert tone="danger" title="No se pudo cargar el producto">
        Verificar la conexión e intentar de nuevo.
      </Alert>
    );
  }
  if (editando && !producto.data) {
    return (
      <Alert tone="danger" title="El producto no existe">
        <Link to="/catalogo">Volver al catálogo</Link>
      </Alert>
    );
  }

  return (
    <FormularioProducto
      key={sku ?? 'nuevo'}
      inicial={producto.data ?? null}
      categorias={categorias.data ?? []}
    />
  );
}
