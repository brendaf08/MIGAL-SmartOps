import { useQuery } from '@tanstack/react-query';
import { Card } from '@/components/ui/Card/Card';
import { DataTable, type Column } from '@/components/ui/DataTable/DataTable';
import { listarFaltantesVentas, type FaltanteVenta } from '@/features/inventario/api';
import { formatNumber } from '@/lib/format';

const columnas: Column<FaltanteVenta>[] = [
  { key: 'nombre', header: 'Producto', primary: true },
  { key: 'sku', header: 'SKU', mono: true, phone: false },
  {
    key: 'ventas',
    header: 'Ventas',
    numeric: true,
    phone: false,
    render: (f) => formatNumber(f.ventas),
  },
  {
    key: 'faltante',
    header: 'Falta para ventas',
    numeric: true,
    render: (f) => `${formatNumber(f.faltante)} ${f.unidad}`,
  },
  {
    key: 'planificado',
    header: 'Ya pedido',
    numeric: true,
    render: (f) => formatNumber(f.planificado),
  },
  {
    key: 'porCubrir',
    header: 'Sin orden de compra',
    numeric: true,
    render: (f) => <strong>{formatNumber(f.porCubrir)}</strong>,
  },
];

/**
 * Referencia para quien prepara una orden de compra: lo que falta para atender las ventas pendientes de abastecimiento,
 * descontado lo que ya está pedido. Solo informa; no agrega nada a la orden.
 */
export function FaltantesVentasCard() {
  const consulta = useQuery({ queryKey: ['faltantes-ventas'], queryFn: listarFaltantesVentas });
  const filas = consulta.data ?? [];
  if (filas.length === 0) return null;

  return (
    <Card
      title="Faltantes de ventas pendientes de abastecimiento"
      description="Lo que falta para atender las ventas activas, descontado lo que ya está pedido. Solo como referencia."
      padded={false}
    >
      <DataTable
        caption="Productos que faltan para las ventas pendientes de abastecimiento"
        columns={columnas}
        rows={filas}
        rowKey="sku"
      />
    </Card>
  );
}
