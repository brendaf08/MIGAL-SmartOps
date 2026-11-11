import { Button, Group, Stack, Text } from '@mantine/core';
import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { useNavigate } from 'react-router';
import { StatusBadge } from '@/components/domain/StatusBadge/StatusBadge';
import { PageHeader } from '@/components/layout/PageHeader/PageHeader';
import { type Column } from '@/components/ui/DataTable/DataTable';
import { Icon } from '@/components/ui/Icon/Icon';
import { QueryTable } from '@/components/ui/QueryTable/QueryTable';
import { SearchInput } from '@/components/ui/SearchInput/SearchInput';
import { Tabs } from '@/components/ui/Tabs/Tabs';
import { formatCurrency, formatDate } from '@/lib/format';
import { listarCotizaciones } from './api';
import { diasEntre, hoyIso, totales } from './calculos';
import type { Cotizacion, EstadoCotizacion } from './types';

type Filtro = 'todas' | EstadoCotizacion;

/** Una cotización próxima a vencer (tres días o menos) se destaca con ícono y texto, no solo con color. */
function Vencimiento({ cotizacion }: { cotizacion: Cotizacion }) {
  const dias = diasEntre(hoyIso(), cotizacion.vencimiento);
  const pronto = cotizacion.estado === 'Emitida' && dias >= 0 && dias <= 3;
  if (!pronto) return <>{formatDate(cotizacion.vencimiento)}</>;

  const cuando = dias === 0 ? 'hoy' : dias === 1 ? 'en 1 día' : `en ${dias} días`;
  return (
    <Text component="span" size="sm" fw={600} c="warning">
      <Icon name="alert-triangle" size={16} />
      {formatDate(cotizacion.vencimiento)}, vence {cuando}
    </Text>
  );
}

const columns: Column<Cotizacion>[] = [
  { key: 'id', header: 'Número', mono: true, primary: true },
  { key: 'cliente', header: 'Cliente', render: (c) => c.cliente?.nombre ?? 'Sin cliente' },
  { key: 'emision', header: 'Fecha', phone: false, render: (c) => formatDate(c.emision) },
  {
    key: 'vencimiento',
    header: 'Vence',
    render: (c) => <Vencimiento cotizacion={c} />,
  },
  {
    key: 'total',
    header: 'Total',
    numeric: true,
    render: (c) => formatCurrency(totales(c).total),
  },
  {
    key: 'estado',
    header: 'Estado',
    render: (c) => <StatusBadge entity="cotizacion" value={c.estado} />,
  },
];

const FILTROS: { value: Filtro; label: string }[] = [
  { value: 'todas', label: 'Todas' },
  { value: 'Emitida', label: 'Emitidas' },
  { value: 'AceptadaCliente', label: 'Aceptadas' },
  { value: 'Rechazada', label: 'Rechazadas' },
  { value: 'Vencida', label: 'Vencidas' },
];

/** CU-09: listado de cotizaciones por estado; cada fila abre su detalle. */
export function CotizacionesPage() {
  const navigate = useNavigate();
  const query = useQuery({ queryKey: ['cotizaciones'], queryFn: listarCotizaciones });
  const [filtro, setFiltro] = useState<Filtro>('todas');
  const [busqueda, setBusqueda] = useState('');

  const todas = query.data ?? [];
  const texto = busqueda.trim().toLowerCase();
  const visibles = todas.filter(
    (c) =>
      (filtro === 'todas' || c.estado === filtro) &&
      (!texto ||
        c.id.toLowerCase().includes(texto) ||
        (c.cliente?.nombre ?? 'Sin cliente').toLowerCase().includes(texto)),
  );

  return (
    <>
      <PageHeader
        title="Cotizaciones"
        description="Cotizaciones emitidas a clientes y su estado."
        actions={
          <Button
            leftSection={<Icon name="plus" />}
            onClick={() => void navigate('/cotizaciones/nueva')}
          >
            Nueva cotización
          </Button>
        }
      />
      <Stack gap="md">
        <Group justify="space-between" align="flex-end">
          <Tabs
            aria-label="Filtrar cotizaciones por estado"
            value={filtro}
            onChange={(v) => setFiltro(v as Filtro)}
            items={FILTROS.map(({ value, label }) => ({
              value,
              label,
              count:
                value === 'todas' ? todas.length : todas.filter((c) => c.estado === value).length,
            }))}
          />
          <SearchInput
            aria-label="Buscar por número o cliente"
            placeholder="Buscar por número o cliente"
            value={busqueda}
            onChange={setBusqueda}
            w={{ base: '100%', sm: 320 }}
            maw="100%"
          />
        </Group>
        <div role="tabpanel">
          <QueryTable
            query={{ data: visibles, isLoading: query.isLoading, isError: query.isError }}
            errorTitle="No se pudieron cargar las cotizaciones"
            caption="Cotizaciones"
            columns={columns}
            rowKey="id"
            onRowClick={(c) => void navigate(`/cotizaciones/${c.id}`)}
          />
        </div>
      </Stack>
    </>
  );
}
