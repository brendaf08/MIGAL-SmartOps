import { PasswordInput, Select, Stack, TextInput } from '@mantine/core';
import { useForm } from '@mantine/form';
import { notifications } from '@mantine/notifications';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { zod4Resolver } from 'mantine-form-zod-resolver';
import { useNavigate, useParams } from 'react-router';
import { FormPage } from '@/components/ui/FormPage/FormPage';
import { Card } from '@/components/ui/Card/Card';
import { ROLES } from '@/features/seguridad/roles';
import { actualizarUsuario, obtenerUsuario, registrarUsuario } from './api';
import { esquemaEdicion, esquemaRegistro, type DatosEdicion, type DatosRegistro } from './esquema';
import type { UsuarioRegistro } from '@/features/seguridad/usuarios-tipos';

const MENSAJE_DUPLICADO = 'El usuario ya está registrado. Ingresar uno distinto.';

/** CU-02: registro de un usuario nuevo o edición de su nombre y rol. */
export function UsuarioFormPage() {
  const { id } = useParams();
  const query = useQuery({
    queryKey: ['usuario', id],
    queryFn: () => obtenerUsuario(id!),
    enabled: !!id,
  });

  if (!id) return <FormularioUsuario />;
  if (query.isError) {
    return <p role="alert">No se encontró el usuario solicitado.</p>;
  }
  if (!query.data) return null;
  return <FormularioUsuario key={query.data.id} usuario={query.data} />;
}

interface FormularioUsuarioProps {
  usuario?: UsuarioRegistro;
}

function FormularioUsuario({ usuario }: FormularioUsuarioProps) {
  const editando = !!usuario;
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const form = useForm<DatosRegistro & { clave: string }>({
    initialValues: {
      nombre: usuario?.nombre ?? '',
      usuario: usuario?.usuario ?? '',
      clave: '',
      rol: (usuario?.rol ?? '') as DatosRegistro['rol'],
    },
    validate: zod4Resolver(editando ? esquemaEdicion : esquemaRegistro, {
      errorPriority: 'first',
    }),
  });

  const mutation = useMutation({
    mutationFn: (datos: DatosRegistro | DatosEdicion) =>
      editando
        ? actualizarUsuario(usuario.id, datos as DatosEdicion)
        : registrarUsuario(datos as DatosRegistro),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['usuarios'] });
      notifications.show({
        color: 'success',
        message: editando ? 'Usuario actualizado.' : 'Usuario registrado.',
      });
      void navigate('/usuarios');
    },
    onError: (error) => {
      if (error instanceof Error && error.message === 'USUARIO_DUPLICADO') {
        form.setFieldError('usuario', MENSAJE_DUPLICADO);
      }
    },
  });

  const guardar = (valores: typeof form.values) => {
    if (editando) {
      mutation.mutate({ nombre: valores.nombre, rol: valores.rol as DatosEdicion['rol'] });
    } else {
      mutation.mutate(valores as DatosRegistro);
    }
  };

  return (
    <FormPage
      title={editando ? 'Modificar usuario' : 'Registrar usuario'}
      description={
        editando
          ? 'Cambiar el nombre o el rol. El usuario y la contraseña no se modifican.'
          : 'Completar los datos personales y el acceso del nuevo usuario.'
      }
      breadcrumbs={[
        { label: 'Usuarios', href: '/usuarios' },
        { label: editando ? usuario.nombre : 'Nuevo usuario' },
      ]}
      submitLabel={editando ? 'Guardar cambios' : 'Registrar usuario'}
      submitting={mutation.isPending}
      onSubmit={form.onSubmit(guardar)}
      onCancel={() => navigate('/usuarios')}
    >
      <Stack gap="md">
        <Card title="Datos personales">
          <TextInput label="Nombre completo" required {...form.getInputProps('nombre')} />
        </Card>
        <Card title="Acceso">
          <Stack gap="md">
            <TextInput
              label="Usuario"
              required={!editando}
              disabled={editando}
              description={editando ? undefined : 'Minúsculas, sin espacios, de 4 a 20 caracteres.'}
              autoComplete="off"
              {...form.getInputProps('usuario')}
            />
            {!editando && (
              <PasswordInput
                label="Contraseña temporal"
                required
                description="Mínimo 8 caracteres, con una letra y un número."
                autoComplete="new-password"
                {...form.getInputProps('clave')}
              />
            )}
            <Select
              label="Rol"
              required
              data={[...ROLES]}
              allowDeselect={false}
              placeholder="Seleccionar un rol"
              {...form.getInputProps('rol')}
              value={form.values.rol || null}
              onChange={(v) => form.setFieldValue('rol', (v ?? '') as DatosRegistro['rol'])}
            />
          </Stack>
        </Card>
      </Stack>
    </FormPage>
  );
}
