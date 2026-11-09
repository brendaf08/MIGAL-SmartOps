import { Button, SimpleGrid } from '@mantine/core';
import { useDebouncedValue } from '@mantine/hooks';
import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { useNavigate } from 'react-router';
import { StatusBadge } from '@/components/domain/StatusBadge/StatusBadge';
import { StockBar } from '@/components/domain/StockBar/StockBar';
import { PageHeader } from '@/components/layout/PageHeader/PageHeader';
import type { Column } from '@/components/ui/DataTable/DataTable';
import { EmptyState } from '@/components/ui/EmptyState/EmptyState';
import { QueryTable } from '@/components/ui/QueryTable/QueryTable';
import { SearchInput } from '@/components/ui/SearchInput/SearchInput';
import { Stat } from '@/components/ui/Stat/Stat';
import { Tabs } from '@/components/ui/Tabs/Tabs';
import { useSession } from '@/features/seguridad/session';
import { formatNumber } from '@/lib/format';
import { listarAlertas, type Alerta } from '../api';
import classes from './AlertasPage.module.css';

/** Filtro por nivel de la lista de alertas. */
const NIVELES = {
  todas: () => true,
  critico: (a: Alerta) => a.nivel === 'Critico',
  minimo: (a: Alerta) => a.nivel === 'Minimo',
} as const;

/** P12. Alertas de stock ordenadas por gravedad; el rol Reponedor puede crear la orden de compra del producto. */
export function AlertasPage() {
  const navigate = useNavigate();
  const { user } = useSession();
  const reponedor = user?.role === 'Reponedor';
  const [tab, setTab] = useState<keyof typeof NIVELES>('todas');
  const [q, setQ] = useState('');
  const [debouncedQ] = useDebouncedValue(q, 300);

  const { data, isLoading, isError } = useQuery({
    queryKey: ['alertas'],
    queryFn: listarAlertas,
  });

  const alertas = data ?? [];
  const criticos = alertas.filter(NIVELES.critico).length;
  const minimos = alertas.filter(NIVELES.minimo).length;
  const term = debouncedQ.trim().toLowerCase();
  const filtradas = alertas.filter(
    (a) => NIVELES[tab](a) && (!term || `${a.sku} ${a.nombre}`.toLowerCase().includes(term)),
  );

  const limpiar = () => {
    setTab('todas');
    setQ('');
  };

  const columns: Column<Alerta>[] = [
    {
      key: 'nombre',
      header: 'Producto',
      primary: true,
      render: (a) => (
        <>
          <div>{a.nombre}</div>
          <small className={classes.sku}>{a.sku}</small>
        </>
      ),
    },
    {
      key: 'libre',
      header: 'Libre',
      numeric: true,
      render: (a) => formatNumber(a.libre),
    },
    {
      key: 'minimo',
      header: 'Mínimo',
      numeric: true,
      phone: false,
      render: (a) => formatNumber(a.minimo),
    },
    {
      key: 'faltante',
      header: 'Faltante',
      numeric: true,
      render: (a) => formatNumber(Math.max(a.faltante, 0)),
    },
    {
      key: 'barra',
      header: 'Stock frente al mínimo',
      phone: false,
      render: (a) => (
        <StockBar
          legend={false}
          libre={Math.max(a.libre, 0)}
          reservado={a.reservado}
          minimo={a.minimo}
          unidad={a.unidad}
          label={`Stock de ${a.nombre}`}
        />
      ),
    },
    {
      key: 'nivel',
      header: 'Nivel',
      render: (a) => <StatusBadge entity="stock" value={a.nivel} />,
    },
    {
      key: 'acciones',
      header: 'Acciones',
      render: (a) => (
        <div className={classes.acciones}>
          <Button variant="default" size="compact-sm" onClick={() => navigate('/disponibilidad')}>
            Ver disponibilidad
          </Button>
          {reponedor && (
            <Button
              size="compact-sm"
              onClick={() => navigate(`/compras/nueva?sku=${encodeURIComponent(a.sku)}`)}
            >
              Crear orden de compra
            </Button>
          )}
        </div>
      ),
    },
  ];

  return (
    <>
      <PageHeader
        title="Alertas de stock"
        description="Productos con stock libre crítico o en el mínimo, del más grave al menos grave."
      />
      <SimpleGrid cols={{ base: 1, sm: 3 }} mb="md">
        <Stat label="Críticos" tone="danger" value={data ? formatNumber(criticos) : undefined} />
        <Stat
          label="En el mínimo"
          tone="warning"
          value={data ? formatNumber(minimos) : undefined}
        />
        <Stat label="Total en alerta" value={data ? formatNumber(alertas.length) : undefined} />
      </SimpleGrid>
      <div className={classes.toolbar}>
        <Tabs
          aria-label="Nivel de alerta"
          value={tab}
          onChange={(v) => setTab(v as keyof typeof NIVELES)}
          items={[
            { value: 'todas', label: 'Todas', count: alertas.length },
            { value: 'critico', label: 'Críticas', count: criticos },
            { value: 'minimo', label: 'En el mínimo', count: minimos },
          ]}
        />
        <SearchInput
          className={classes.search}
          aria-label="Buscar producto en alerta"
          placeholder="Nombre o SKU"
          value={q}
          onChange={setQ}
        />
      </div>
      <QueryTable
        query={{ data: filtradas, isLoading, isError }}
        errorTitle="No se pudieron cargar las alertas"
        caption="Productos en alerta"
        columns={columns}
        rowKey="sku"
        empty={
          alertas.length === 0 ? (
            <EmptyState
              icon="status-ok"
              title="No hay productos en alerta"
              description="Todas las existencias libres están por encima del mínimo."
            />
          ) : (
            <EmptyState
              title="Sin resultados"
              description="Ningún producto coincide con la búsqueda o el nivel elegido."
              action={
                <Button variant="default" onClick={limpiar}>
                  Limpiar filtros
                </Button>
              }
            />
          )
        }
      />
    </>
  );
}
