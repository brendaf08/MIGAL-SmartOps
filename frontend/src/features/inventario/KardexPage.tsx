import {
  Button,
  Group,
  Select,
  SimpleGrid,
  Stack,
  Text,
  TextInput,
  useMatches,
} from '@mantine/core';
import { useDebouncedValue } from '@mantine/hooks';
import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router';
import { MovementBadge } from '@/components/domain/MovementBadge/MovementBadge';
import { PageHeader } from '@/components/layout/PageHeader/PageHeader';
import type { Column } from '@/components/ui/DataTable/DataTable';
import { EmptyState } from '@/components/ui/EmptyState/EmptyState';
import { Icon } from '@/components/ui/Icon/Icon';
import { Modal } from '@/components/ui/Modal/Modal';
import { Pagination } from '@/components/ui/Pagination/Pagination';
import { QueryTable } from '@/components/ui/QueryTable/QueryTable';
import { SearchInput } from '@/components/ui/SearchInput/SearchInput';
import { formatDate } from '@/lib/format';
import { UBICACIONES } from './movimientos/esquema';
import { listarMovimientosFiltrados, SIN_FILTROS_KARDEX, type FiltrosKardex } from './api';
import { MOVIMIENTOS } from './fixtures';
import type { Movimiento } from './types';

const PAGE_SIZE = 20;

const ORIGEN_TEXTO: Record<Movimiento['origen'], string> = {
  Despacho: 'Despacho',
  Recepcion: 'Recepción',
  Ajuste: 'Ajuste',
  Traslado: 'Traslado',
};

const ORIGENES = Object.entries(ORIGEN_TEXTO).map(([value, label]) => ({ value, label }));

/** Par de un movimiento de traslado: la salida y la entrada se registran juntas, con igual fecha, producto y cantidad. */
const parDe = (m: Movimiento) =>
  m.origen === 'Traslado'
    ? MOVIMIENTOS.find(
        (o) =>
          o.id !== m.id &&
          o.origen === 'Traslado' &&
          o.sku === m.sku &&
          o.fecha === m.fecha &&
          o.cantidad === m.cantidad &&
          o.tipo !== m.tipo,
      )
    : undefined;

const columns: Column<Movimiento>[] = [
  { key: 'fecha', header: 'Fecha', render: (m) => formatDate(m.fecha) },
  { key: 'sku', header: 'SKU', mono: true, primary: true },
  { key: 'producto', header: 'Producto', phone: false },
  {
    key: 'movimiento',
    header: 'Movimiento',
    render: (m) => <MovementBadge tipo={m.tipo} cantidad={m.cantidad} />,
  },
  {
    key: 'origen',
    header: 'Origen',
    render: (m) => {
      const par = parDe(m);
      return (
        <>
          <span>{ORIGEN_TEXTO[m.origen]}</span>
          {par && (
            <small>
              {m.tipo === 'Salida' ? `Hacia ${par.ubicacion}` : `Desde ${par.ubicacion}`}
            </small>
          )}
        </>
      );
    },
  },
  { key: 'ubicacion', header: 'Ubicación', phone: false },
  { key: 'usuario', header: 'Responsable', phone: false },
];

/** Rótulo de cada filtro activo y la clave que lo limpia. */
function etiquetasDe(f: FiltrosKardex) {
  const etiquetas: { clave: keyof FiltrosKardex; texto: string }[] = [];
  if (f.q.trim()) etiquetas.push({ clave: 'q', texto: `Producto o SKU: ${f.q.trim()}` });
  if (f.tipo) etiquetas.push({ clave: 'tipo', texto: `Tipo: ${f.tipo}` });
  if (f.origen) etiquetas.push({ clave: 'origen', texto: `Origen: ${ORIGEN_TEXTO[f.origen]}` });
  if (f.ubicacion) etiquetas.push({ clave: 'ubicacion', texto: `Ubicación: ${f.ubicacion}` });
  if (f.desde) etiquetas.push({ clave: 'desde', texto: `Desde ${formatDate(f.desde)}` });
  if (f.hasta) etiquetas.push({ clave: 'hasta', texto: `Hasta ${formatDate(f.hasta)}` });
  return etiquetas;
}

/** P14. Kardex con filtros combinables; en teléfono los filtros se agrupan en una hoja. */
export function KardexPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const enHoja = useMatches({ base: true, sm: false });
  const [filtros, setFiltros] = useState<FiltrosKardex>(SIN_FILTROS_KARDEX);
  const [page, setPage] = useState(1);
  const [hojaAbierta, setHojaAbierta] = useState(false);
  /** Fila creada por el registro de movimiento; se monta sin resaltado y lo recibe al entrar. */
  const [resaltado, setResaltado] = useState<number>();
  const [debouncedQ] = useDebouncedValue(filtros.q, 300);
  const consulta = { ...filtros, q: debouncedQ };

  const query = useQuery({
    queryKey: ['kardex', consulta],
    queryFn: () => listarMovimientosFiltrados(consulta),
    placeholderData: keepPreviousData,
  });

  /* El resaltado se aplica tras el montaje: la tabla recibe la clave después de su primer render. */
  useEffect(() => {
    const id = (location.state as { changedKey?: number } | null)?.changedKey;
    if (id === undefined) return;
    const timer = window.setTimeout(() => setResaltado(id), 0);
    return () => window.clearTimeout(timer);
  }, [location.state]);

  const movimientos = query.data ?? [];
  const pagina = movimientos.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE);
  const etiquetas = etiquetasDe(filtros);

  /** Cambiar un filtro vuelve a la primera página. */
  const cambiar = (parcial: Partial<FiltrosKardex>) => {
    setFiltros((actual) => ({ ...actual, ...parcial }));
    setPage(1);
  };
  const limpiar = () => cambiar(SIN_FILTROS_KARDEX);

  /* Campos de filtro como arreglo: en escritorio forman una cuadrícula y en teléfono se apilan en la hoja. */
  const campos = [
    <SearchInput
      key="q"
      aria-label="Buscar por SKU o producto"
      placeholder="SKU o producto"
      value={filtros.q}
      onChange={(q) => cambiar({ q })}
    />,
    <Select
      key="tipo"
      label="Tipo"
      placeholder="Todos"
      clearable
      data={['Entrada', 'Salida']}
      value={filtros.tipo || null}
      onChange={(v) => cambiar({ tipo: (v ?? '') as FiltrosKardex['tipo'] })}
    />,
    <Select
      key="origen"
      label="Origen"
      placeholder="Todos"
      clearable
      data={ORIGENES}
      value={filtros.origen || null}
      onChange={(v) => cambiar({ origen: (v ?? '') as FiltrosKardex['origen'] })}
    />,
    <Select
      key="ubicacion"
      label="Ubicación"
      placeholder="Todas"
      clearable
      data={[...UBICACIONES]}
      value={filtros.ubicacion || null}
      onChange={(v) => cambiar({ ubicacion: (v ?? '') as FiltrosKardex['ubicacion'] })}
    />,
    <TextInput
      key="desde"
      type="date"
      label="Desde"
      value={filtros.desde}
      onChange={(e) => cambiar({ desde: e.currentTarget.value })}
    />,
    <TextInput
      key="hasta"
      type="date"
      label="Hasta"
      value={filtros.hasta}
      onChange={(e) => cambiar({ hasta: e.currentTarget.value })}
    />,
  ];

  return (
    <>
      <PageHeader
        title="Movimientos y Kardex"
        description="Entradas y salidas de mercadería por producto y ubicación."
        actions={
          <Button leftSection={<Icon name="plus" />} onClick={() => void navigate('/kardex/nuevo')}>
            Registrar movimiento
          </Button>
        }
      />
      {enHoja ? (
        <Group gap="sm" mb="sm" wrap="wrap">
          <Button
            variant="default"
            leftSection={<Icon name="filter" />}
            onClick={() => setHojaAbierta(true)}
          >
            {etiquetas.length > 0 ? `Filtros (${etiquetas.length})` : 'Filtros'}
          </Button>
        </Group>
      ) : (
        <SimpleGrid cols={{ base: 1, sm: 3, lg: 6 }} mb="sm">
          {campos}
        </SimpleGrid>
      )}
      {etiquetas.length > 0 && (
        <Group gap="xs" mb="md" wrap="wrap" role="group" aria-label="Filtros aplicados">
          {etiquetas.map(({ clave, texto }) => (
            <Button
              key={clave}
              variant="light"
              size="compact-sm"
              aria-label={`Quitar filtro ${texto}`}
              rightSection={<Icon name="close" size={16} />}
              onClick={() => cambiar({ [clave]: SIN_FILTROS_KARDEX[clave] })}
            >
              {texto}
            </Button>
          ))}
          <Button variant="subtle" size="compact-sm" onClick={limpiar}>
            Limpiar filtros
          </Button>
        </Group>
      )}
      <QueryTable
        query={{ data: pagina, isLoading: query.isLoading, isError: query.isError }}
        errorTitle="No se pudo cargar el Kardex"
        caption="Movimientos de Kardex"
        columns={columns}
        rowKey="id"
        changedKey={resaltado}
        empty={
          <EmptyState
            title="Sin movimientos"
            description="Ningún movimiento coincide con los filtros aplicados."
            action={
              etiquetas.length > 0 ? (
                <Button variant="default" onClick={limpiar}>
                  Limpiar filtros
                </Button>
              ) : undefined
            }
          />
        }
      />
      {!query.isError && (
        <div>
          <Pagination
            page={page}
            total={movimientos.length}
            pageSize={PAGE_SIZE}
            onChange={setPage}
          />
        </div>
      )}
      <Modal
        open={hojaAbierta}
        title="Filtros"
        onClose={() => setHojaAbierta(false)}
        footer={
          <>
            <Button variant="default" onClick={limpiar}>
              Limpiar filtros
            </Button>
            <Button onClick={() => setHojaAbierta(false)}>Ver resultados</Button>
          </>
        }
      >
        <Stack gap="md">
          {campos}
          <Text size="sm" c="dimmed">
            {movimientos.length} movimientos con los filtros aplicados.
          </Text>
        </Stack>
      </Modal>
    </>
  );
}
