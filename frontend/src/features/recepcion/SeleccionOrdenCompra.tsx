import { Button, Group, Text } from '@mantine/core';
import { StatusBadge } from '@/components/domain/StatusBadge/StatusBadge';
import { Card } from '@/components/ui/Card/Card';
import { DataTable, type Column } from '@/components/ui/DataTable/DataTable';
import { Icon } from '@/components/ui/Icon/Icon';
import type { LineaOrdenCompra } from '@/features/abastecimiento/tipos';
import { formatNumber } from '@/lib/format';
import type { OrdenRecepcionable } from './api';

const porRecibir = (linea: LineaOrdenCompra) => Math.max(linea.cantidad - linea.recibido, 0);

const columnasOrdenes: Column<OrdenRecepcionable>[] = [
  { key: 'id', header: 'Orden de compra', mono: true, primary: true },
  { key: 'proveedor', header: 'Proveedor' },
  {
    key: 'productos',
    header: 'Productos',
    numeric: true,
    phone: false,
    render: (o) => o.lineas.length,
  },
  {
    key: 'pendiente',
    header: 'Por recibir',
    numeric: true,
    render: (o) => formatNumber(o.lineas.reduce((suma, l) => suma + porRecibir(l), 0)),
  },
  {
    key: 'estado',
    header: 'Estado',
    render: (o) => <StatusBadge entity="compra" value={o.estado} />,
  },
];

const columnasProductos: Column<LineaOrdenCompra>[] = [
  { key: 'sku', header: 'Producto', mono: true, primary: true },
  { key: 'nombre', header: 'Descripción', phone: false },
  { key: 'cantidad', header: 'Pedido', numeric: true, render: (l) => formatNumber(l.cantidad) },
  { key: 'recibido', header: 'Recibido', numeric: true, render: (l) => formatNumber(l.recibido) },
  {
    key: 'pendiente',
    header: 'Por recibir',
    numeric: true,
    render: (l) => formatNumber(porRecibir(l)),
  },
];

export interface SeleccionOrdenCompraProps {
  ordenes: OrdenRecepcionable[];
  seleccionada: string | null;
  onSeleccionar: (id: string) => void;
}

/**
 * P27, paso 1: órdenes pendientes o con recepción parcial. Al elegir una, se muestran sus productos esperados
 * y lo que falta recibir de cada uno.
 */
export function SeleccionOrdenCompra({
  ordenes,
  seleccionada,
  onSeleccionar,
}: SeleccionOrdenCompraProps) {
  const columnas: Column<OrdenRecepcionable>[] = [
    ...columnasOrdenes,
    {
      key: 'accion',
      header: 'Acción',
      render: (o) =>
        o.id === seleccionada ? (
          <Group gap={4} wrap="nowrap">
            <Icon name="check" size={16} />
            <Text size="sm" fw={600}>
              Elegida
            </Text>
          </Group>
        ) : (
          <Button
            variant="default"
            size="compact-md"
            aria-label={`Elegir orden ${o.id}`}
            onClick={() => onSeleccionar(o.id)}
          >
            Elegir
          </Button>
        ),
    },
  ];

  const elegida = ordenes.find((o) => o.id === seleccionada);

  return (
    <>
      <Card
        title="Orden de compra"
        description="Órdenes pendientes o con recepción parcial."
        padded={false}
      >
        <DataTable
          caption="Órdenes de compra para recibir"
          columns={columnas}
          rows={ordenes}
          rowKey="id"
        />
      </Card>
      {elegida && (
        <Card
          title="Productos esperados"
          description={`${elegida.id}, ${elegida.proveedor}`}
          padded={false}
        >
          <DataTable
            caption={`Productos esperados de ${elegida.id}`}
            columns={columnasProductos}
            rows={elegida.lineas}
            rowKey="sku"
          />
        </Card>
      )}
    </>
  );
}
