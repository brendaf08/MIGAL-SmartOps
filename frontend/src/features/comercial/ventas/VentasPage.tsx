import { Button, Stack, Text } from '@mantine/core';
import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { useNavigate } from 'react-router';
import { STATUS } from '@/components/domain/StatusBadge/status';
import { StatusBadge } from '@/components/domain/StatusBadge/StatusBadge';
import { PageHeader } from '@/components/layout/PageHeader/PageHeader';
import { type Column } from '@/components/ui/DataTable/DataTable';
import { Icon } from '@/components/ui/Icon/Icon';
import { QueryTable } from '@/components/ui/QueryTable/QueryTable';
import { SearchInput } from '@/components/ui/SearchInput/SearchInput';
import { Tabs, type TabItem } from '@/components/ui/Tabs/Tabs';
import { formatCurrency, formatDate, formatNumber } from '@/lib/format';
import { CLIENTES } from '@/features/comercial/clientes/fixtures';
import { listarVentas, tienePendientes, totalesVenta, unidadesPendientes } from './api';
import type { EstadoVenta, Venta } from './tipos';
import classes from './Ventas.module.css';

/** Estados que aparecen como pestañas, en el orden del flujo de la venta. */
const ESTADOS: EstadoVenta[] = [
  'PendienteAbastecimiento',
  'PendienteDespacho',
  'ListaDespacho',
  'Despachada',
  'Cancelada',
];

const nombreCliente = (venta: Venta) =>
  CLIENTES.find((c) => c.id === venta.clienteId)?.nombre ?? 'Sin cliente';

const columns: Column<Venta>[] = [
  { key: 'id', header: 'Venta', mono: true, primary: true },
  { key: 'fecha', header: 'Fecha', phone: false, render: (v) => formatDate(v.fecha) },
  { key: 'cliente', header: 'Cliente', render: nombreCliente },
  {
    key: 'entrega',
    header: 'Entrega',
    render: (v) => (
      <Stack gap={0}>
        <Text size="sm">{v.entrega}</Text>
        {tienePendientes(v) && (
          <span className={classes.pendiente}>
            <Icon name="alert-triangle" size={16} />
            Pendiente: {formatNumber(unidadesPendientes(v))} unidades
          </span>
        )}
      </Stack>
    ),
  },
  {
    key: 'total',
    header: 'Total',
    numeric: true,
    render: (v) => formatCurrency(totalesVenta(v).total),
  },
  {
    key: 'estado',
    header: 'Estado',
    render: (v) => <StatusBadge entity="venta" value={v.estado} />,
  },
];

/** CU-10: lista de ventas por estado, con búsqueda por número o cliente. Clic abre el detalle. */
export function VentasPage() {
  const navigate = useNavigate();
  const [tab, setTab] = useState('todas');
  const [q, setQ] = useState('');
  const query = useQuery({ queryKey: ['ventas'], queryFn: listarVentas });
  const ventas = query.data ?? [];

  const tabs: TabItem[] = [
    { value: 'todas', label: 'Todas', count: ventas.length },
    ...ESTADOS.map((estado) => ({
      value: estado,
      label: STATUS.venta[estado][1],
      count: ventas.filter((v) => v.estado === estado).length,
    })),
  ];

  const termino = q.trim().toLowerCase();
  const visibles = ventas
    .filter((v) => tab === 'todas' || v.estado === tab)
    .filter(
      (v) =>
        termino === '' ||
        v.id.toLowerCase().includes(termino) ||
        nombreCliente(v).toLowerCase().includes(termino),
    );

  return (
    <>
      <PageHeader
        title="Ventas"
        description="Ventas por estado, con sus cantidades pendientes de atención."
        actions={
          <Button leftSection={<Icon name="plus" />} onClick={() => void navigate('/ventas/nueva')}>
            Registrar venta
          </Button>
        }
      />
      <div className={classes.toolbar}>
        <Tabs items={tabs} value={tab} onChange={setTab} aria-label="Filtrar ventas por estado" />
        <SearchInput
          className={classes.search}
          aria-label="Buscar por número o cliente"
          placeholder="Buscar por número o cliente"
          value={q}
          onChange={setQ}
        />
      </div>
      <QueryTable
        query={{ ...query, data: visibles }}
        errorTitle="No se pudieron cargar las ventas"
        caption="Ventas"
        columns={columns}
        rowKey="id"
        onRowClick={(v) => void navigate(`/ventas/${v.id}`)}
      />
    </>
  );
}
