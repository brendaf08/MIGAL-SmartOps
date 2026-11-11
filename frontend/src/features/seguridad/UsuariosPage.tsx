import { Button, Group, Stack, Text } from '@mantine/core';
import { useDebouncedValue } from '@mantine/hooks';
import { notifications } from '@mantine/notifications';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { Link } from 'react-router';
import { StatusBadge } from '@/components/domain/StatusBadge/StatusBadge';
import { PageHeader } from '@/components/layout/PageHeader/PageHeader';
import type { Column } from '@/components/ui/DataTable/DataTable';
import { Icon } from '@/components/ui/Icon/Icon';
import { EmptyState } from '@/components/ui/EmptyState/EmptyState';
import { ConfirmDialog } from '@/components/ui/Modal/ConfirmDialog';
import { QueryTable } from '@/components/ui/QueryTable/QueryTable';
import { SearchInput } from '@/components/ui/SearchInput/SearchInput';
import { Tabs } from '@/components/ui/Tabs/Tabs';
import { useSession } from './session';
import { listarUsuarios, revocarAcceso } from './usuarios/api';
import type { UsuarioRegistro } from './usuarios-tipos';

type Filtro = 'Todos' | 'Activos' | 'Inactivos';

const FILTROS: Filtro[] = ['Todos', 'Activos', 'Inactivos'];

const MENSAJE_PROPIA_CUENTA = 'No se puede revocar la propia cuenta';

/** Columnas de la tabla; las acciones de revocar dependen de la sesión y del diálogo de la página. */
function columnas(
  propia: (u: UsuarioRegistro) => boolean,
  onRevocar: (u: UsuarioRegistro) => void,
): Column<UsuarioRegistro>[] {
  return [
    { key: 'nombre', header: 'Nombre', primary: true },
    { key: 'usuario', header: 'Usuario', mono: true },
    { key: 'rol', header: 'Rol' },
    {
      key: 'estado',
      header: 'Estado',
      render: (u) => <StatusBadge entity="registro" value={u.estado} />,
    },
    {
      key: 'acciones',
      header: 'Acciones',
      block: true,
      render: (u) => (
        <Group gap="xs">
          <Button
            variant="default"
            size="compact-md"
            component={Link}
            to={`/usuarios/${u.id}`}
            aria-label={`Modificar rol de ${u.nombre}`}
          >
            Modificar rol
          </Button>
          {u.estado === 'Activo' &&
            (propia(u) ? (
              <>
                <Button
                  variant="default"
                  size="compact-md"
                  color="danger"
                  disabled
                  title={MENSAJE_PROPIA_CUENTA}
                  aria-label={`Revocar acceso de ${u.nombre}: ${MENSAJE_PROPIA_CUENTA}`}
                >
                  Revocar acceso
                </Button>
                <Text size="sm" c="dimmed">
                  {MENSAJE_PROPIA_CUENTA}
                </Text>
              </>
            ) : (
              <Button
                variant="default"
                size="compact-md"
                color="danger"
                onClick={() => onRevocar(u)}
                aria-label={`Revocar acceso de ${u.nombre}`}
              >
                Revocar acceso
              </Button>
            ))}
        </Group>
      ),
    },
  ];
}

/** CU-02: lista de usuarios con búsqueda, filtro por estado, acciones por fila y revocación de acceso. */
export function UsuariosPage() {
  const [q, setQ] = useState('');
  const [debouncedQ] = useDebouncedValue(q, 300);
  const [filtro, setFiltro] = useState<Filtro>('Todos');
  const [revocando, setRevocando] = useState<UsuarioRegistro | null>(null);
  const { user } = useSession();
  const queryClient = useQueryClient();
  const query = useQuery({ queryKey: ['usuarios'], queryFn: listarUsuarios });

  const revocar = useMutation({
    mutationFn: (id: string) => revocarAcceso(id),
    onSuccess: async (usuario) => {
      await queryClient.invalidateQueries({ queryKey: ['usuarios'] });
      notifications.show({ color: 'success', message: `Acceso revocado a ${usuario.usuario}.` });
      setRevocando(null);
    },
  });

  const propia = (u: UsuarioRegistro) => u.nombre === user?.name;

  const todos = query.data ?? [];
  const porEstado = (f: Filtro) =>
    todos.filter((u) => f === 'Todos' || u.estado === (f === 'Activos' ? 'Activo' : 'Inactivo'));
  const termino = debouncedQ.trim().toLowerCase();
  const filas = porEstado(filtro).filter(
    (u) => !termino || `${u.nombre} ${u.usuario}`.toLowerCase().includes(termino),
  );

  return (
    <>
      <PageHeader
        title="Usuarios y roles"
        description="Usuarios internos y el rol que define sus permisos."
        actions={
          <Button component={Link} to="/usuarios/nuevo" leftSection={<Icon name="plus" />}>
            Registrar usuario
          </Button>
        }
      />
      <Stack gap="md">
        <Group justify="space-between" align="flex-end" gap="md">
          <Tabs
            aria-label="Filtrar usuarios por estado"
            items={FILTROS.map((f) => ({ value: f, label: f, count: porEstado(f).length }))}
            value={filtro}
            onChange={(v) => setFiltro(v as Filtro)}
          />
          <SearchInput
            aria-label="Buscar usuario"
            placeholder="Nombre o usuario"
            value={q}
            onChange={setQ}
            w={{ base: '100%', sm: 320 }}
            maw="100%"
          />
        </Group>
        <QueryTable
          query={{ data: filas, isLoading: query.isLoading, isError: query.isError }}
          errorTitle="No se pudieron cargar los usuarios"
          caption="Usuarios"
          columns={columnas(propia, setRevocando)}
          rowKey="id"
          empty={
            <EmptyState
              title="No hay usuarios que coincidan"
              description="Cambiar el texto de búsqueda o el filtro de estado."
            />
          }
        />
      </Stack>
      <ConfirmDialog
        open={!!revocando}
        title={`Revocar acceso de ${revocando?.usuario ?? ''}`}
        consequences={[
          'El usuario no podrá iniciar sesión.',
          'Su historial de movimientos se conserva.',
          'El acceso puede restituirse editando al usuario.',
        ]}
        tone="danger"
        confirmLabel="Revocar acceso"
        loading={revocar.isPending}
        onConfirm={() => revocando && revocar.mutate(revocando.id)}
        onCancel={() => setRevocando(null)}
      />
    </>
  );
}
