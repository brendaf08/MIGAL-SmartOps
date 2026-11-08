import { Button, Group, Stack } from '@mantine/core';
import { useDebouncedValue } from '@mantine/hooks';
import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { Link } from 'react-router';
import { StatusBadge } from '@/components/domain/StatusBadge/StatusBadge';
import { PageHeader } from '@/components/layout/PageHeader/PageHeader';
import type { Column } from '@/components/ui/DataTable/DataTable';
import { EmptyState } from '@/components/ui/EmptyState/EmptyState';
import { QueryTable } from '@/components/ui/QueryTable/QueryTable';
import { SearchInput } from '@/components/ui/SearchInput/SearchInput';
import { Tabs } from '@/components/ui/Tabs/Tabs';
import { listarUsuarios } from './usuarios/api';
import type { UsuarioRegistro } from './usuarios-tipos';

type Filtro = 'Todos' | 'Activos' | 'Inactivos';

const FILTROS: Filtro[] = ['Todos', 'Activos', 'Inactivos'];

const columns: Column<UsuarioRegistro>[] = [
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
      </Group>
    ),
  },
];

/** CU-02: lista de usuarios con búsqueda, filtro por estado y acciones por fila. */
export function UsuariosPage() {
  const [q, setQ] = useState('');
  const [debouncedQ] = useDebouncedValue(q, 300);
  const [filtro, setFiltro] = useState<Filtro>('Todos');
  const query = useQuery({ queryKey: ['usuarios'], queryFn: listarUsuarios });

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
          <Button component={Link} to="/usuarios/nuevo">
            Registrar usuario
          </Button>
        }
      />
      <Stack gap="md">
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
          maw={320}
        />
        <QueryTable
          query={{ data: filas, isLoading: query.isLoading, isError: query.isError }}
          errorTitle="No se pudieron cargar los usuarios"
          caption="Usuarios"
          columns={columns}
          rowKey="id"
          empty={
            <EmptyState
              title="No hay usuarios que coincidan"
              description="Cambiar el texto de búsqueda o el filtro de estado."
            />
          }
        />
      </Stack>
    </>
  );
}
