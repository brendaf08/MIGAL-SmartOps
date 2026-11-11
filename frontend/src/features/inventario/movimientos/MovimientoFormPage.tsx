import {
  Button,
  NumberInput,
  SegmentedControl,
  Select,
  SimpleGrid,
  Stack,
  Text,
  Textarea,
  useMatches,
} from '@mantine/core';
import { useForm } from '@mantine/form';
import { notifications } from '@mantine/notifications';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { zod4Resolver } from 'mantine-form-zod-resolver';
import { useState } from 'react';
import { useNavigate } from 'react-router';
import { Constancia } from '@/components/domain/Constancia/Constancia';
import { StockLocations } from '@/components/domain/StockLocations/StockLocations';
import { PageHeader } from '@/components/layout/PageHeader/PageHeader';
import { Card } from '@/components/ui/Card/Card';
import { FormPage } from '@/components/ui/FormPage/FormPage';
import { ProductoSelect } from '@/components/ui/ProductoSelect/ProductoSelect';
import { ConfirmDialog } from '@/components/ui/Modal/ConfirmDialog';
import { Icon } from '@/components/ui/Icon/Icon';
import { ApiError } from '@/api/cliente';
import { ubicacionesDe } from '../calculos';
import {
  bloqueoDeExistencia,
  movimientoSchema,
  TIPOS_MOVIMIENTO,
  UBICACIONES,
  type TipoMovimiento,
} from './esquema';
import { listarProductosMovibles, registrarMovimiento, type ResultadoMovimiento } from './api';
import { obtenerDisponibilidadProducto } from '../api';
import { useSession } from '@/features/seguridad/session';
import classes from './MovimientoFormPage.module.css';

type ValoresFormulario = {
  tipo: TipoMovimiento;
  sku: string;
  ubicacion: string;
  origen: string;
  destino: string;
  cantidad: number | '';
  motivo: string;
};

const VALORES_INICIALES: ValoresFormulario = {
  tipo: 'entrada',
  sku: '',
  ubicacion: '',
  origen: '',
  destino: '',
  cantidad: '',
  motivo: '',
};

const SEGMENTOS = TIPOS_MOVIMIENTO.map(({ value, label }) => ({ value, label }));

/** P13. Registro de un movimiento manual: entrada o salida por ajuste y traslado interno, con constancia al confirmar. */
export function MovimientoFormPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { user } = useSession();
  /** En teléfono las tres opciones se apilan: sus etiquetas no caben en una fila. */
  const orientacion = useMatches<'vertical' | 'horizontal'>({
    base: 'vertical',
    sm: 'horizontal',
  });
  const [confirmando, setConfirmando] = useState(false);
  /** La constancia solo existe como resultado de confirmar el movimiento en esta pantalla. */
  const [resultado, setResultado] = useState<ResultadoMovimiento | null>(null);
  /**
   * Clave de idempotencia de la intención actual: los reintentos del mismo envío la reutilizan, y una nueva
   * intención (tras un registro o tras un rechazo definitivo) genera otra.
   */
  const [claveIdempotencia, setClaveIdempotencia] = useState(() => crypto.randomUUID());
  const form = useForm<ValoresFormulario>({
    initialValues: VALORES_INICIALES,
    validate: zod4Resolver(movimientoSchema),
  });

  const productos = useQuery({ queryKey: ['movimiento-productos'], queryFn: listarProductosMovibles });
  const { sku, ubicacion, origen } = form.values;
  const productoConsulta = useQuery({
    queryKey: ['movimiento-producto', sku],
    queryFn: () => obtenerDisponibilidadProducto(sku),
    enabled: sku !== '',
  });

  const mutation = useMutation({
    mutationFn: (v: ValoresFormulario) =>
      registrarMovimiento({
        tipo: v.tipo,
        sku: v.sku,
        ubicacion: v.ubicacion,
        origen: v.origen,
        destino: v.destino,
        cantidad: Number(v.cantidad),
        motivo: v.motivo.trim(),
        usuario: user?.name ?? '',
        claveIdempotencia,
      }),
    onSuccess: (r) => {
      setConfirmando(false);
      setResultado(r);
      setClaveIdempotencia(crypto.randomUUID());
      void queryClient.invalidateQueries({ queryKey: ['kardex'] });
      void queryClient.invalidateQueries({ queryKey: ['disponibilidad'] });
      void queryClient.invalidateQueries({ queryKey: ['alertas'] });
      void queryClient.invalidateQueries({ queryKey: ['movimiento-producto'] });
    },
    onError: (e) => {
      setConfirmando(false);
      // Un rechazo definitivo (4xx) indica una intención no aceptada: el siguiente envío es una nueva intención.
      if (e instanceof ApiError && e.estado >= 400 && e.estado < 500) setClaveIdempotencia(crypto.randomUUID());
      notifications.show({
        color: 'danger',
        title: 'No se pudo registrar el movimiento',
        message: e.message,
      });
    },
  });

  const { tipo } = form.values;
  const cantidad = Number(form.values.cantidad) || 0;
  const producto = productoConsulta.data ?? null;
  const etiqueta = TIPOS_MOVIMIENTO.find((t) => t.value === tipo)?.label ?? '';
  const movimientos = tipo === 'traslado' ? 2 : 1;
  /** Una salida o un traslado no puede superar la existencia física del almacén de origen. */
  const bloqueo =
    tipo === 'entrada'
      ? null
      : bloqueoDeExistencia(producto, tipo === 'traslado' ? origen : ubicacion, cantidad);

  if (resultado) {
    return (
      <>
        <PageHeader
          title="Registrar movimiento"
          breadcrumbs={[
            { label: 'Movimientos y Kardex', href: '/kardex' },
            { label: 'Constancia' },
          ]}
        />
        <Constancia
          sello={<Text fw={700}>{etiqueta}</Text>}
          title="Movimiento registrado"
          description={
            movimientos === 1
              ? 'Se registró 1 movimiento en el Kardex.'
              : `Se registraron ${movimientos} movimientos en el Kardex.`
          }
          cambios={resultado.cambios}
          nota={
            tipo === 'traslado'
              ? 'El traslado se registró como dos movimientos vinculados: salida en el origen y entrada en el destino.'
              : undefined
          }
          actions={
            <>
              <Button
                variant="default"
                onClick={() => {
                  setResultado(null);
                  form.reset();
                }}
              >
                Registrar otro movimiento
              </Button>
              <Button
                leftSection={<Icon name="kardex" />}
                onClick={() =>
                  void navigate('/kardex', {
                    state: { changedKey: resultado.movimientos[0]?.id },
                  })
                }
              >
                Ver movimientos en el Kardex
              </Button>
            </>
          }
        />
      </>
    );
  }

  return (
    <>
      <FormPage
        title="Registrar movimiento"
        description="Entrada o salida por ajuste, merma o traslado entre almacenes."
        breadcrumbs={[
          { label: 'Movimientos y Kardex', href: '/kardex' },
          { label: 'Nuevo movimiento' },
        ]}
        summary={
          <Text size="sm" fw={600}>
            {movimientos === 1 ? '1 movimiento' : `${movimientos} movimientos vinculados`}
          </Text>
        }
        reason={bloqueo}
        submitLabel="Revisar movimiento"
        canSubmit={!bloqueo}
        onSubmit={form.onSubmit(() => setConfirmando(true))}
        onCancel={() => void navigate('/kardex')}
      >
        <Card title="Tipo de movimiento">
          <SegmentedControl
            aria-label="Tipo de movimiento"
            fullWidth
            orientation={orientacion}
            data={SEGMENTOS}
            value={tipo}
            onChange={(v) => form.setFieldValue('tipo', v as TipoMovimiento)}
          />
        </Card>
        <Card title="Datos del movimiento">
          <Stack gap="md">
            <ProductoSelect
              label="Producto"
              required
              productos={productos.data ?? []}
              value={sku || null}
              onChange={(s) => form.setFieldValue('sku', s ?? '')}
              error={form.errors.sku}
            />
            {producto && (
              <div className={classes.stock}>
                <StockLocations
                  compact
                  ubicaciones={ubicacionesDe(producto)}
                  unidad={producto.unidad}
                  label="Existencias del producto por ubicación"
                />
              </div>
            )}
            <SimpleGrid cols={{ base: 1, sm: 2 }}>
              {tipo === 'traslado' ? (
                <>
                  <Select
                    label="Almacén de origen"
                    required
                    data={[...UBICACIONES]}
                    placeholder="Seleccionar almacén"
                    {...form.getInputProps('origen')}
                  />
                  <Select
                    label="Almacén de destino"
                    required
                    data={[...UBICACIONES]}
                    placeholder="Seleccionar almacén"
                    {...form.getInputProps('destino')}
                  />
                </>
              ) : (
                <Select
                  label="Almacén"
                  required
                  data={[...UBICACIONES]}
                  placeholder="Seleccionar almacén"
                  {...form.getInputProps('ubicacion')}
                />
              )}
              <NumberInput
                label="Cantidad"
                required
                min={1}
                allowDecimal={false}
                allowNegative={false}
                inputMode="numeric"
                {...form.getInputProps('cantidad')}
              />
            </SimpleGrid>
            <Textarea
              label="Motivo"
              required
              autosize
              minRows={3}
              description="Mínimo 10 caracteres."
              {...form.getInputProps('motivo')}
            />
          </Stack>
        </Card>
      </FormPage>
      <ConfirmDialog
        open={confirmando}
        title={`Registrar ${etiqueta.toLowerCase()}`}
        description={producto ? `${producto.nombre}, ${cantidad} ${producto.unidad}.` : undefined}
        consequences={[
          movimientos === 1
            ? 'Se registrará 1 movimiento en el Kardex.'
            : `Se registrarán ${movimientos} movimientos en el Kardex.`,
          'La acción no se puede deshacer.',
        ]}
        confirmLabel="Registrar movimiento"
        loading={mutation.isPending}
        onConfirm={() => mutation.mutate(form.values)}
        onCancel={() => setConfirmando(false)}
      />
    </>
  );
}
