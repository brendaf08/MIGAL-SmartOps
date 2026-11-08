import { Alert, Button, PasswordInput, Stack, Text, TextInput, Title } from '@mantine/core';
import { useForm } from '@mantine/form';
import { useMutation } from '@tanstack/react-query';
import { zod4Resolver } from 'mantine-form-zod-resolver';
import { Navigate, useLocation, useNavigate } from 'react-router';
import { z } from 'zod';
import logo from '@/assets/progresol-logo.png';
import { Card } from '@/components/ui/Card/Card';
import { Icon } from '@/components/ui/Icon/Icon';
import { iniciarSesion } from './api';
import { useSession } from './session';
import classes from './LoginPage.module.css';

const schema = z.object({
  usuario: z.string().trim().min(1, 'Ingresar el usuario.'),
  clave: z.string().min(1, 'Ingresar la contraseña.'),
});

/** CU-01. Una sola columna centrada, campos grandes para uso táctil y un único botón principal. */
export function LoginPage() {
  const { user, login } = useSession();
  const navigate = useNavigate();
  const from = (useLocation().state as { from?: string } | null)?.from ?? '/';
  const form = useForm({
    initialValues: { usuario: '', clave: '' },
    validate: zod4Resolver(schema),
  });
  const mutation = useMutation({
    mutationFn: ({ usuario, clave }: typeof form.values) => iniciarSesion(usuario, clave),
    onSuccess: (next) => {
      login(next);
      void navigate(from, { replace: true });
    },
  });

  if (user) return <Navigate to="/" replace />;

  return (
    <main className={classes.root}>
      <div className={classes.box}>
        <Stack align="center" gap="xs" ta="center" mb="lg">
          <img src={logo} alt="" width={72} height={72} />
          <Title order={1}>MIGAL SmartOps</Title>
          <Text size="lg" c="dimmed">
            Ferretería Progresol Migal
          </Text>
        </Stack>
        <Card>
          <form onSubmit={form.onSubmit((values) => mutation.mutate(values))} noValidate>
            <Stack gap="sm">
              {mutation.isError && (
                <Alert color="danger" title="Credenciales incorrectas" role="alert">
                  Verificar el usuario y la contraseña. Tras cinco intentos fallidos la cuenta se
                  bloquea temporalmente.
                </Alert>
              )}
              <TextInput
                label="Usuario"
                size="lg"
                leftSection={<Icon name="user" />}
                autoComplete="username"
                required
                {...form.getInputProps('usuario')}
              />
              <PasswordInput
                label="Contraseña"
                size="lg"
                leftSection={<Icon name="lock" />}
                autoComplete="current-password"
                required
                visibilityToggleButtonProps={{ 'aria-label': 'Mostrar contraseña' }}
                {...form.getInputProps('clave')}
              />
              <Button type="submit" size="lg" fullWidth loading={mutation.isPending}>
                Iniciar sesión
              </Button>
            </Stack>
          </form>
        </Card>
      </div>
    </main>
  );
}
