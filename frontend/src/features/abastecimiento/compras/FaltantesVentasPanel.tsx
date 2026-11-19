import { Anchor, Group, Select, Stack, Text } from '@mantine/core';
import { useQuery } from '@tanstack/react-query';
import { useMemo, useState } from 'react';
import { Link } from 'react-router';
import { Card } from '@/components/ui/Card/Card';
import { DataTable, type Column } from '@/components/ui/DataTable/DataTable';
import { Tabs } from '@/components/ui/Tabs/Tabs';
import { hoyIso } from '@/features/comercial/calculos';
import { listarFaltantesVentas, type FaltanteVenta } from '@/features/inventario/api';
import { formatDate, formatNumber } from '@/lib/format';
import classes from './Compras.module.css';
import {
  agruparPorVenta,
  diasHasta,
  filtrarPorProducto,
  VENTANAS,
  type Ventana,
  type VentaConFaltantes,
} from './faltantes';

type Vista = 'ventas' | 'productos';

const columnasDeVenta: Column<VentaConFaltantes['productos'][number]>[] = [
  { key: 'nombre', header: 'Producto', primary: true },
  { key: 'sku', header: 'SKU', mono: true, phone: false },
  {
    key: 'faltante',
    header: 'Falta',
    numeric: true,
    render: (p) => `${formatNumber(p.faltante)} ${p.unidad}`,
  },
];

const columnasPorProducto: Column<FaltanteVenta>[] = [
  { key: 'nombre', header: 'Producto', primary: true },
  { key: 'sku', header: 'SKU', mono: true, phone: false },
  {
    key: 'ventas',
    header: 'Ventas',
    render: (p) => p.origenes.map((o) => o.ventaCodigo).join(', '),
  },
  {
    key: 'faltante',
    header: 'Falta para ventas',
    numeric: true,
    render: (p) => `${formatNumber(p.faltante)} ${p.unidad}`,
  },
  {
    key: 'planificado',
    header: 'Ya pedido',
    numeric: true,
    render: (p) => formatNumber(p.planificado),
  },
  {
    key: 'porCubrir',
    header: 'Sin orden de compra',
    numeric: true,
    render: (p) => <strong>{formatNumber(p.porCubrir)}</strong>,
  },
];

/** Texto de la urgencia de una venta: cuántos días faltan para la entrega requerida. */
function textoEntrega(fecha: string | null, hoy: string): string {
  const dias = diasHasta(fecha, hoy);
  if (fecha === null || dias === null) return 'Sin fecha de entrega';
  if (dias < 0) return `Entrega requerida ${formatDate(fecha)} (vencida hace ${-dias} d)`;
  if (dias === 0) return `Entrega requerida hoy, ${formatDate(fecha)}`;
  return `Entrega requerida ${formatDate(fecha)} (en ${dias} d)`;
}

/**
 * Referencia para planificar compras: lo que falta para atender las ventas pendientes de abastecimiento, por venta (con
 * su cliente, su cotización de origen y la fecha en que el cliente necesita la entrega) o consolidado por producto.
 * Solo informa; no agrega nada a ninguna orden.
 */
export function FaltantesVentasPanel() {
  const consulta = useQuery({ queryKey: ['faltantes-ventas'], queryFn: listarFaltantesVentas });
  const [vista, setVista] = useState<Vista>('ventas');
  const [ventana, setVentana] = useState<Ventana>('todas');
  const hoy = hoyIso();
  const items = useMemo(() => consulta.data ?? [], [consulta.data]);
  const ventas = useMemo(() => agruparPorVenta(items, ventana, hoy), [items, ventana, hoy]);
  const productos = useMemo(() => filtrarPorProducto(items, ventana, hoy), [items, ventana, hoy]);

  if (consulta.isLoading || consulta.isError) return null;
  if (items.length === 0) {
    return (
      <Card title="Faltantes de ventas pendientes de abastecimiento">
        <Text size="sm" c="dimmed">
          No hay ventas pendientes de abastecimiento: todo lo vendido tiene stock reservado.
        </Text>
      </Card>
    );
  }

  return (
    <Card
      title="Faltantes de ventas pendientes de abastecimiento"
      description="Lo que falta para atender las ventas activas. Solo como referencia para planificar compras."
    >
      <Stack gap="md">
        <Group justify="space-between" align="flex-end" wrap="wrap">
          <Tabs
            aria-label="Vista de los faltantes"
            value={vista}
            onChange={(v) => setVista(v as Vista)}
            items={[
              { value: 'ventas', label: 'Por venta', count: ventas.length },
              { value: 'productos', label: 'Por producto', count: productos.length },
            ]}
          />
          <Select
            label="Fecha de entrega"
            data={VENTANAS}
            value={ventana}
            onChange={(v) => setVentana((v ?? 'todas') as Ventana)}
            allowDeselect={false}
            w={220}
          />
        </Group>
        {vista === 'ventas' ? (
          ventas.length === 0 ? (
            <Text size="sm" c="dimmed">
              Ninguna venta pendiente coincide con esa fecha de entrega.
            </Text>
          ) : (
            ventas.map((venta) => (
              <section key={venta.ventaId} className={classes.faltanteVenta}>
                <header className={classes.faltanteCabecera}>
                  <Anchor component={Link} to={`/ventas/${venta.ventaId}`} fw={700}>
                    {venta.ventaCodigo}
                  </Anchor>
                  <Text span size="sm">
                    {venta.cliente}
                  </Text>
                  <Text span size="sm" c="dimmed">
                    cotización{' '}
                    <Anchor component={Link} to={`/cotizaciones/${venta.cotizacionId}`} size="sm">
                      {venta.cotizacionCodigo}
                    </Anchor>
                    , vendida el {formatDate(venta.fechaVenta)}
                  </Text>
                  <Text span size="sm" fw={600}>
                    {textoEntrega(venta.fechaEntregaRequerida, hoy)}
                  </Text>
                </header>
                <DataTable
                  caption={`Productos que faltan para la venta ${venta.ventaCodigo}`}
                  columns={columnasDeVenta}
                  rows={venta.productos}
                  rowKey="sku"
                />
              </section>
            ))
          )
        ) : (
          <DataTable
            caption="Productos que faltan para las ventas pendientes de abastecimiento"
            columns={columnasPorProducto}
            rows={productos}
            rowKey="sku"
          />
        )}
      </Stack>
    </Card>
  );
}
