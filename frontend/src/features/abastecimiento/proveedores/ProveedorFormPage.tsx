import { Alert } from '@/components/ui/Alert/Alert';
import { Anchor, Button, Group, MultiSelect, Stack, TextInput } from '@mantine/core';
import { useForm } from '@mantine/form';
import { notifications } from '@mantine/notifications';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { zod4Resolver } from 'mantine-form-zod-resolver';
import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router';
import { StatusBadge } from '@/components/domain/StatusBadge/StatusBadge';
import { Card } from '@/components/ui/Card/Card';
import { EmptyState } from '@/components/ui/EmptyState/EmptyState';
import { FormPage } from '@/components/ui/FormPage/FormPage';
import { ConfirmDialog } from '@/components/ui/Modal/ConfirmDialog';
import { opcionesProducto } from '@/features/catalogo/opciones';
import type { Proveedor } from '../tipos';
import {
  actualizarProveedor,
  cambiarEstadoProveedor,
  obtenerProveedor,
  registrarProveedor,
  RucDuplicadoError,
  type DatosProveedor,
} from './api';
import classes from './ProveedorFormPage.module.css';
import { esquemaProveedor, type ValoresProveedor } from './esquema';

/** Ruta `proveedores/nueva` (sin identificador) y `proveedores/:id` (edición). */
export function ProveedorFormPage() {
  const { id } = useParams();
  if (!id) return <FormularioProveedor />;
  return <EditarProveedor key={id} id={id} />;
}

function EditarProveedor({ id }: { id: string }) {
  const query = useQuery({
    queryKey: ['proveedor', id],
    queryFn: () => obtenerProveedor(id),
    retry: false,
  });

  if (query.isError) {
    return (
      <EmptyState
        title="Proveedor no encontrado"
        description="El proveedor no existe o ya no está disponible."
        action={<Anchor component={Link} to="/proveedores">Volver a la lista de proveedores</Anchor>}
      />
    );
  }
  if (!query.data) return null;
  return <FormularioProveedor proveedor={query.data} />;
}

function valoresIniciales(proveedor?: Proveedor): ValoresProveedor {
  return {
    razonSocial: proveedor?.razonSocial ?? '',
    ruc: proveedor?.ruc ?? '',
    contacto: proveedor?.contacto ?? '',
    telefono: proveedor?.telefono ?? '',
    correo: proveedor?.correo ?? '',
    condiciones: proveedor?.condiciones ?? '',
    skus: proveedor?.skus ?? [],
  };
}

type Dialogo = 'activar' | 'desactivar' | null;

function FormularioProveedor({ proveedor }: { proveedor?: Proveedor }) {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const inactivo = proveedor?.estado === 'Inactivo';
  const [busqueda, setBusqueda] = useState('');
  const [rucExistente, setRucExistente] = useState<string | null>(null);
  const [dialogo, setDialogo] = useState<Dialogo>(null);
  const productos = opcionesProducto();
  const opciones = productos.map((p) => ({ value: p.sku, label: `${p.sku}, ${p.nombre}` }));
  const termino = busqueda.trim().toLowerCase();
  const sinCoincidencia =
    termino !== '' &&
    !productos.some((p) => `${p.sku} ${p.nombre}`.toLowerCase().includes(termino));

  const form = useForm<ValoresProveedor>({
    initialValues: valoresIniciales(proveedor),
    validate: zod4Resolver(esquemaProveedor),
  });

  const refrescar = () => {
    void queryClient.invalidateQueries({ queryKey: ['proveedores'] });
    if (proveedor) void queryClient.invalidateQueries({ queryKey: ['proveedor', proveedor.id] });
  };

  const guardar = useMutation({
    mutationFn: (valores: DatosProveedor) =>
      proveedor ? actualizarProveedor(proveedor.id, valores) : registrarProveedor(valores),
    onSuccess: (guardado) => {
      refrescar();
      notifications.show({
        color: 'success',
        title: proveedor ? 'Proveedor actualizado' : 'Proveedor registrado',
        message: `${guardado.razonSocial} quedó guardado.`,
      });
      void navigate('/proveedores');
    },
    onError: (error) => {
      if (error instanceof RucDuplicadoError) {
        setRucExistente(error.proveedorId);
        form.setFieldError('ruc', 'Este RUC ya está registrado.');
        return;
      }
      notifications.show({
        color: 'danger',
        title: 'No se pudo guardar el proveedor',
        message: error.message,
      });
    },
  });

  const cambiarEstado = useMutation({
    mutationFn: (estado: Proveedor['estado']) => cambiarEstadoProveedor(proveedor!.id, estado),
    onSuccess: (actualizado) => {
      refrescar();
      setDialogo(null);
      notifications.show({
        color: 'success',
        title: actualizado.estado === 'Activo' ? 'Proveedor activado' : 'Proveedor desactivado',
        message: `${actualizado.razonSocial} quedó ${actualizado.estado === 'Activo' ? 'activo' : 'inactivo'}.`,
      });
    },
  });

  const enviar = form.onSubmit((valores) => {
    if (inactivo) return;
    setRucExistente(null);
    guardar.mutate(valores);
  });

  const bloqueo = inactivo ? 'Activar el proveedor para modificarlo.' : null;

  return (
    <>
      <FormPage
        title={proveedor ? proveedor.razonSocial : 'Registrar proveedor'}
        description={
          proveedor ? 'Datos, condiciones y productos que suministra.' : 'Datos del nuevo proveedor.'
        }
        breadcrumbs={[
          { label: 'Proveedores', href: '/proveedores' },
          { label: proveedor ? proveedor.razonSocial : 'Nuevo proveedor' },
        ]}
        submitLabel={proveedor ? 'Guardar cambios' : 'Registrar proveedor'}
        submitting={guardar.isPending}
        canSubmit={!inactivo}
        reason={bloqueo}
        onSubmit={enviar}
        onCancel={() => void navigate('/proveedores')}
      >
        {proveedor && (
          <Card title="Estado del proveedor">
            <Stack gap="md">
              <div>
                <StatusBadge entity="registro" value={proveedor.estado} />
              </div>
              {inactivo && (
                <Alert tone="warning" title="Este proveedor está inactivo y no puede usarse en nuevas órdenes de compra" />
              )}
              <Group>
                {inactivo ? (
                  <Button variant="default" onClick={() => setDialogo('activar')}>
                    Activar proveedor
                  </Button>
                ) : (
                  <Button color="danger" variant="outline" onClick={() => setDialogo('desactivar')}>
                    Desactivar proveedor
                  </Button>
                )}
              </Group>
            </Stack>
          </Card>
        )}

        <Card title="Datos del proveedor">
          <Stack gap="md">
            <TextInput
              label="Razón social"
              required
              disabled={inactivo}
              {...form.getInputProps('razonSocial')}
            />
            <TextInput
              label="RUC"
              required
              inputMode="numeric"
              maxLength={11}
              disabled={inactivo}
              description="Once dígitos, empieza en 10 o 20."
              {...form.getInputProps('ruc')}
            />
            {rucExistente && (
              <Anchor component={Link} to={`/proveedores/${rucExistente}`}>
                Modificar el proveedor existente
              </Anchor>
            )}
          </Stack>
        </Card>

        <Card title="Contacto y condiciones">
          <Stack gap="md">
            <TextInput
              label="Persona de contacto"
              required
              disabled={inactivo}
              {...form.getInputProps('contacto')}
            />
            <TextInput
              label="Teléfono"
              inputMode="tel"
              maxLength={9}
              disabled={inactivo}
              {...form.getInputProps('telefono')}
            />
            <TextInput
              label="Correo electrónico"
              type="email"
              disabled={inactivo}
              {...form.getInputProps('correo')}
            />
            <TextInput
              label="Condiciones comerciales"
              required
              placeholder="Por ejemplo, Pago a 30 días"
              disabled={inactivo}
              {...form.getInputProps('condiciones')}
            />
          </Stack>
        </Card>

        <Card title="Productos que suministra">
          <Stack gap="md">
            <MultiSelect
              label="Productos del catálogo"
              placeholder="Buscar por SKU o nombre"
              data={opciones}
              searchable
              searchValue={busqueda}
              onSearchChange={setBusqueda}
              nothingFoundMessage={sinCoincidencia ? undefined : 'Sin coincidencias'}
              disabled={inactivo}
              comboboxProps={{ width: 360, position: 'bottom-start' }}
              {...form.getInputProps('skus')}
            />
            {sinCoincidencia && (
              <Alert tone="warning" title="El producto no está en el catálogo">
                <div className={classes.acciones}>
                  <Anchor component={Link} to="/catalogo/nuevo">
                    Dar de alta en el catálogo
                  </Anchor>
                  <Button variant="default" size="compact-md" onClick={() => setBusqueda('')}>
                    Continuar sin asociar
                  </Button>
                </div>
              </Alert>
            )}
          </Stack>
        </Card>
      </FormPage>

      {proveedor && (
        <>
          <ConfirmDialog
            open={dialogo === 'desactivar'}
            title={`Desactivar a ${proveedor.razonSocial}`}
            tone="danger"
            consequences={[
              'El proveedor no podrá usarse en nuevas órdenes de compra.',
              'Sus datos y sus órdenes existentes se conservan.',
            ]}
            confirmLabel="Desactivar proveedor"
            loading={cambiarEstado.isPending}
            onConfirm={() => cambiarEstado.mutate('Inactivo')}
            onCancel={() => setDialogo(null)}
          />
          <ConfirmDialog
            open={dialogo === 'activar'}
            title={`Activar a ${proveedor.razonSocial}`}
            consequences={['El proveedor podrá usarse en nuevas órdenes de compra.']}
            confirmLabel="Activar proveedor"
            loading={cambiarEstado.isPending}
            onConfirm={() => cambiarEstado.mutate('Activo')}
            onCancel={() => setDialogo(null)}
          />
        </>
      )}
    </>
  );
}
