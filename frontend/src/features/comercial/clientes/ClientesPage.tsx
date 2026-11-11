import { Button } from '@mantine/core';
import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { StatusBadge } from '@/components/domain/StatusBadge/StatusBadge';
import { PageHeader } from '@/components/layout/PageHeader/PageHeader';
import type { Column } from '@/components/ui/DataTable/DataTable';
import { Icon } from '@/components/ui/Icon/Icon';
import { QueryTable } from '@/components/ui/QueryTable/QueryTable';
import { SearchInput } from '@/components/ui/SearchInput/SearchInput';
import { Tabs } from '@/components/ui/Tabs/Tabs';
import { ClienteFormModal } from './ClienteFormModal';
import { listarClientes } from './api';
import type { Cliente } from './tipos';
import classes from './Clientes.module.css';

const columns: Column<Cliente>[] = [
  { key: 'nombre', header: 'Nombre', primary: true },
  { key: 'documento', header: 'Documento', mono: true },
  { key: 'telefono', header: 'Teléfono', render: (c) => c.telefono || '—' },
  { key: 'correo', header: 'Correo', render: (c) => c.correo || '—' },
  {
    key: 'estado',
    header: 'Estado',
    render: (c) => <StatusBadge entity="registro" value={c.estado} />,
  },
];

/** CU-10. Lista de clientes con búsqueda y pestañas por estado; una fila abre el diálogo de modificación. */
export function ClientesPage() {
  const query = useQuery({ queryKey: ['clientes'], queryFn: listarClientes });
  const [tab, setTab] = useState('activos');
  const [texto, setTexto] = useState('');
  const [abierto, setAbierto] = useState(false);
  const [seleccionado, setSeleccionado] = useState<Cliente | null>(null);
  const [ultimoCambio, setUltimoCambio] = useState<string | undefined>(undefined);

  const clientes = query.data ?? [];
  const activos = clientes.filter((c) => c.estado === 'Activo');
  const inactivos = clientes.filter((c) => c.estado === 'Inactivo');
  const termino = texto.trim().toLowerCase();
  const filas = (tab === 'activos' ? activos : inactivos).filter(
    (c) => !termino || `${c.nombre} ${c.documento}`.toLowerCase().includes(termino),
  );

  const abrirRegistro = () => {
    setSeleccionado(null);
    setAbierto(true);
  };
  const abrirModificacion = (cliente: Cliente) => {
    setSeleccionado(cliente);
    setAbierto(true);
  };
  const marcarCambio = (cliente: Cliente) => setUltimoCambio(cliente.id);

  return (
    <>
      <PageHeader
        title="Clientes"
        description="Clientes registrados para cotizar y vender."
        actions={
          <Button leftSection={<Icon name="plus" size={16} />} onClick={abrirRegistro}>
            Registrar cliente
          </Button>
        }
      />
      <div className={classes.filtros}>
        <Tabs
          aria-label="Estado de los clientes"
          value={tab}
          onChange={setTab}
          items={[
            { value: 'activos', label: 'Activos', count: activos.length },
            { value: 'inactivos', label: 'Inactivos', count: inactivos.length },
          ]}
        />
        <SearchInput
          className={classes.buscar}
          aria-label="Buscar cliente"
          placeholder="Nombre o documento"
          value={texto}
          onChange={setTexto}
        />
      </div>
      <div role="tabpanel" style={{ marginTop: 'var(--space-3)' }}>
        <QueryTable
          query={{ data: filas, isLoading: query.isLoading, isError: query.isError }}
          errorTitle="No se pudieron cargar los clientes"
          caption="Clientes"
          columns={columns}
          rowKey="id"
          onRowClick={abrirModificacion}
          selectedKey={seleccionado?.id}
          changedKey={ultimoCambio}
        />
      </div>
      <ClienteFormModal
        open={abierto}
        onClose={() => setAbierto(false)}
        cliente={seleccionado}
        onGuardado={marcarCambio}
        onDesactivado={marcarCambio}
      />
    </>
  );
}
