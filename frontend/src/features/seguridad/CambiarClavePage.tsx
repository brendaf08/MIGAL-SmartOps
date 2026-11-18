import { PasswordInput, Stack } from '@mantine/core';
import { useForm } from '@mantine/form';
import { notifications } from '@mantine/notifications';
import { useMutation } from '@tanstack/react-query';
import { zod4Resolver } from 'mantine-form-zod-resolver';
import { useNavigate } from 'react-router';
import { z } from 'zod';
import { ApiError } from '@/api/cliente';
import { Card } from '@/components/ui/Card/Card';
import { FormPage } from '@/components/ui/FormPage/FormPage';
import { cambiarClave } from './api';

const esquema = z
  .object({
    claveActual: z.string().min(1, 'Ingresar la contraseña actual.'),
    claveNueva: z
      .string()
      .min(8, 'La nueva contraseña debe tener al menos 8 caracteres.')
      .regex(/[A-Za-z]/, 'La nueva contraseña debe incluir una letra.')
      .regex(/\d/, 'La nueva contraseña debe incluir un número.'),
    confirmacion: z.string(),
  })
  .refine((v) => v.confirmacion === v.claveNueva, {
    path: ['confirmacion'],
    message: 'La confirmación no coincide con la nueva contraseña.',
  });

/** Cambio de la contraseña propia. El Administrador inicial lo usa tras el primer ingreso. */
export function CambiarClavePage() {
  const navigate = useNavigate();
  const form = useForm({
    initialValues: { claveActual: '', claveNueva: '', confirmacion: '' },
    validate: zod4Resolver(esquema, { errorPriority: 'first' }),
  });

  const mutation = useMutation({
    mutationFn: (v: typeof form.values) => cambiarClave(v.claveActual, v.claveNueva),
    onSuccess: () => {
      notifications.show({ color: 'success', message: 'Contraseña actualizada.' });
      form.reset();
      void navigate('/');
    },
    onError: (error) => {
      const campo = error instanceof ApiError ? error.errores[0] : undefined;
      if (campo && (campo.campo === 'claveActual' || campo.campo === 'claveNueva')) {
        form.setFieldError(campo.campo, campo.mensaje);
        return;
      }
      notifications.show({
        color: 'danger',
        title: 'No se pudo cambiar la contraseña',
        message: error.message,
      });
    },
  });

  return (
    <FormPage
      title="Cambiar contraseña"
      description="Reemplazar la contraseña propia. Se pide la actual para confirmar la identidad."
      submitLabel="Guardar contraseña"
      submitting={mutation.isPending}
      onSubmit={form.onSubmit((v) => mutation.mutate(v))}
      onCancel={() => navigate('/')}
    >
      <Card title="Contraseña">
        <Stack gap="md">
          <PasswordInput
            label="Contraseña actual"
            required
            autoComplete="current-password"
            {...form.getInputProps('claveActual')}
          />
          <PasswordInput
            label="Nueva contraseña"
            required
            description="Mínimo 8 caracteres, con una letra y un número."
            autoComplete="new-password"
            {...form.getInputProps('claveNueva')}
          />
          <PasswordInput
            label="Confirmar la nueva contraseña"
            required
            autoComplete="new-password"
            {...form.getInputProps('confirmacion')}
          />
        </Stack>
      </Card>
    </FormPage>
  );
}
