import { useQuery } from '@tanstack/react-query';
import { StatusBadge } from '@/components/domain/StatusBadge/StatusBadge';
import { PageHeader } from '@/components/layout/PageHeader/PageHeader';
import type { Column } from '@/components/ui/DataTable/DataTable';
import { QueryTable } from '@/components/ui/QueryTable/QueryTable';
import { listarUsuarios, type UsuarioResumen } from './api';

const columns: Column<UsuarioResumen>[] = [
  { key: 'name', header: 'Nombre', primary: true },
  { key: 'usuario', header: 'Usuario', mono: true },
  { key: 'role', header: 'Rol' },
  {
    key: 'estado',
    header: 'Estado',
    render: () => <StatusBadge entity="registro" value="Activo" />,
  },
];

/** CU-02: usuarios y roles. */
export function UsuariosPage() {
  const query = useQuery({ queryKey: ['usuarios'], queryFn: listarUsuarios });

  return (
    <>
      <PageHeader
        title="Usuarios y roles"
        description="Usuarios internos y el rol que define sus permisos."
      />
      <QueryTable
        query={query}
        errorTitle="No se pudieron cargar los usuarios"
        caption="Usuarios"
        columns={columns}
        rowKey="usuario"
      />
    </>
  );
}
