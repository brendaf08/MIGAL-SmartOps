import { Button, Progress } from '@mantine/core';
import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { useNavigate } from 'react-router';
import { StatusBadge } from '@/components/domain/StatusBadge/StatusBadge';
import { useSession } from '@/features/seguridad/session';
import { PageHeader } from '@/components/layout/PageHeader/PageHeader';
import { type Column } from '@/components/ui/DataTable/DataTable';
import { Icon } from '@/components/ui/Icon/Icon';
import { QueryTable } from '@/components/ui/QueryTable/QueryTable';
import { SearchInput } from '@/components/ui/SearchInput/SearchInput';
import { Tabs } from '@/components/ui/Tabs/Tabs';
import { formatCurrency, formatDate, formatNumber } from '@/lib/format';
import { listarOrdenesCompra, type FilaOrdenCompra } from './api';
import classes from './Compras.module.css';

type Filtro = 'Todas' | FilaOrdenCompra['estado'];

const FILTROS: { value: Filtro; label: string }[] = [
  { value: 'Todas', label: 'Todas' },
  { value: 'Pendiente', label: 'Pendientes' },
  { value: 'Parcial', label: 'Recepción parcial' },
  { value: 'Completada', label: 'Completadas' },
];

const columns: Column<FilaOrdenCompra>[] = [
  { key: 'id', header: 'Orden', mono: true, primary: true },
  { key: 'proveedor', header: 'Proveedor' },
  { key: 'fecha', header: 'Fecha', phone: false, render: (o) => formatDate(o.fecha) },
  {
    key: 'productos',
    header: 'Productos',
    numeric: true,
    phone: false,
    render: (o) => formatNumber(o.productos),
  },
  {
    key: 'avance',
    header: 'Avance de recepción',
    block: true,
    render: (o) => (
      <div className={classes.avance}>
        <span>
          {formatNumber(o.recibidas)} de {formatNumber(o.pedidas)} unidades
        </span>
        <Progress
          value={o.pedidas ? (o.recibidas / o.pedidas) * 100 : 0}
          size="sm"
          aria-hidden="true"
        />
      </div>
    ),
  },
  { key: 'total', header: 'Total', numeric: true, render: (o) => formatCurrency(o.total) },
  {
    key: 'estado',
    header: 'Estado',
    render: (o) => <StatusBadge entity="compra" value={o.estado} />,
  },
];

/** P25: órdenes de compra con su estado y avance de recepción, búsqueda y pestañas por estado (CU-12). */
export function ComprasPage() {
  const { user } = useSession();
  const navigate = useNavigate();
  const query = useQuery({ queryKey: ['ordenes-compra'], queryFn: listarOrdenesCompra });
  const [filtro, setFiltro] = useState<Filtro>('Todas');
  const [busqueda, setBusqueda] = useState('');

  const todas = query.data ?? [];
  const termino = busqueda.trim().toLocaleLowerCase('es-PE');
  const visibles = todas.filter(
    (o) =>
      (filtro === 'Todas' || o.estado === filtro) &&
      (!termino ||
        o.id.toLocaleLowerCase('es-PE').includes(termino) ||
        o.proveedor.toLocaleLowerCase('es-PE').includes(termino)),
  );
  const conteo = (estado: Filtro) =>
    estado === 'Todas' ? todas.length : todas.filter((o) => o.estado === estado).length;

  return (
    <>
      <PageHeader
        title="Órdenes de compra"
        description="Órdenes emitidas a proveedores y su avance de recepción."
        actions={
          user?.role === 'Reponedor' && (
            <Button
              leftSection={<Icon name="plus" />}
              onClick={() => void navigate('/compras/nueva')}
            >
              Nueva orden de compra
            </Button>
          )
        }
      />
      <Tabs
        aria-label="Filtrar órdenes por estado"
        value={filtro}
        onChange={(v) => setFiltro(v as Filtro)}
        items={FILTROS.map((f) => ({ value: f.value, label: f.label, count: conteo(f.value) }))}
      />
      <div className={classes.busqueda}>
        <SearchInput
          aria-label="Buscar orden o proveedor"
          placeholder="Buscar por orden o proveedor"
          value={busqueda}
          onChange={setBusqueda}
        />
      </div>
      <QueryTable
        query={{ ...query, data: visibles }}
        errorTitle="No se pudieron cargar las órdenes de compra"
        caption="Órdenes de compra"
        columns={columns}
        rowKey="id"
        empty={<p>No hay órdenes que coincidan con el filtro.</p>}
        onRowClick={(o) => void navigate(`/compras/${o.id}`)}
      />
    </>
  );
}
