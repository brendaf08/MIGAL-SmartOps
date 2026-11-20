import { Badge, Stack, Text } from '@mantine/core';
import { Alert } from '@/components/ui/Alert/Alert';
import { Card } from '@/components/ui/Card/Card';
import { DataTable, type Column } from '@/components/ui/DataTable/DataTable';
import { useCatalogoVigente } from '@/features/catalogo/catalogoVigente';
import { formatDate, formatNumber } from '@/lib/format';
import {
  desglosarLinea,
  ETIQUETA_COBERTURA,
  type Cobertura,
  type DesgloseLinea,
} from './coberturaStock';

interface LineaStock {
  sku: string;
  nombre?: string;
  cantidad: number;
}

type Fila = {
  id: string;
  sku: string;
  nombre: string;
  unidad: string;
  cantidad: number;
} & DesgloseLinea;

const COLOR: Record<Cobertura, string> = {
  stock: 'success',
  recibir: 'warning',
  comprar: 'danger',
};

const columnas: Column<Fila>[] = [
  { key: 'nombre', header: 'Producto', primary: true },
  { key: 'cantidad', header: 'Cotizado', numeric: true, render: (f) => formatNumber(f.cantidad) },
  { key: 'libre', header: 'Stock libre', numeric: true, render: (f) => formatNumber(f.libre) },
  {
    key: 'reservado',
    header: 'Reservado',
    numeric: true,
    render: (f) => formatNumber(f.reservado),
  },
  {
    key: 'porRecibir',
    header: 'Por recibir',
    numeric: true,
    render: (f) =>
      f.porRecibir > 0
        ? `${formatNumber(f.porRecibir)}${f.proximaLlegada ? ` (próx. ${formatDate(f.proximaLlegada)})` : ''}`
        : '0',
  },
  {
    key: 'previstoNeto',
    header: 'Previsto neto',
    numeric: true,
    render: (f) => formatNumber(f.previstoNeto),
  },
  {
    key: 'cobertura',
    header: 'Cobertura',
    render: (f) => (
      <Badge color={COLOR[f.cobertura]} variant="light">
        {ETIQUETA_COBERTURA[f.cobertura]}
      </Badge>
    ),
  },
];

/**
 * Desglose del stock de cada línea para el vendedor: libre, reservado, por recibir con su próxima llegada, previsto
 * neto y con qué se cubriría. Es informativo: la cotización no reserva existencias.
 */
export function DesgloseStock({ lineas }: { lineas: LineaStock[] }) {
  const { productos } = useCatalogoVigente();
  const filas: Fila[] = lineas.flatMap((l, i) => {
    const producto = productos.find((p) => p.sku === l.sku);
    if (!producto) return [];
    return [
      {
        id: `${l.sku}-${i}`,
        sku: l.sku,
        nombre: l.nombre ?? producto.nombre,
        unidad: producto.unidad,
        cantidad: l.cantidad,
        ...desglosarLinea(l.cantidad, producto),
      },
    ];
  });
  if (filas.length === 0) return null;
  const compite = filas.some((f) => f.compite);
  return (
    <Stack>
      <Card title="Stock para decidir" padded={false}>
        <DataTable
          caption="Stock por producto cotizado"
          columns={columnas}
          rows={filas}
          rowKey="id"
        />
      </Card>
      <Text size="sm" c="dimmed">
        La cotización no reserva existencias: el stock se reserva al convertirla en venta. Previsto
        neto es lo por recibir menos lo que otras ventas pendientes ya reclaman.
      </Text>
      {compite && (
        <Alert tone="warning" title="Otras ventas compiten por este stock">
          Parte de lo por recibir ya lo esperan ventas pendientes de abastecimiento. El previsto
          neto descuenta esa cantidad.
        </Alert>
      )}
    </Stack>
  );
}
