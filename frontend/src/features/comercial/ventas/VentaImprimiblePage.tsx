import { Skeleton } from '@mantine/core';
import { useQuery } from '@tanstack/react-query';
import { useParams } from 'react-router';
import { STATUS } from '@/components/domain/StatusBadge/status';
import { DocumentoImprimible } from '@/components/domain/DocumentoImprimible/DocumentoImprimible';
import { Alert } from '@/components/ui/Alert/Alert';
import { CLIENTES } from '@/features/comercial/clientes/fixtures';
import { formatCurrency, formatDate, formatNumber } from '@/lib/format';
import { obtenerVenta, totalesVenta } from './api';

/** Venta imprimible en una hoja A4, con lo solicitado y lo atendido por producto. */
export function VentaImprimiblePage() {
  const { id = '' } = useParams();
  const query = useQuery({ queryKey: ['venta', id], queryFn: () => obtenerVenta(id) });

  if (query.isError) {
    return (
      <Alert tone="warning" title="La venta no existe">
        Volver al listado y elegir otra venta.
      </Alert>
    );
  }
  const venta = query.data;
  if (!venta) return <Skeleton height={240} />;

  const cliente =
    venta.clienteNombre ?? CLIENTES.find((c) => c.id === venta.clienteId)?.nombre ?? 'Sin cliente';
  const t = totalesVenta(venta);
  return (
    <DocumentoImprimible
      titulo={`Venta ${venta.id}`}
      datos={[
        { etiqueta: 'Fecha', valor: formatDate(venta.fecha) },
        { etiqueta: 'Cliente', valor: cliente },
        ...(venta.entregaRequerida
          ? [{ etiqueta: 'Entrega requerida', valor: formatDate(venta.entregaRequerida) }]
          : []),
        { etiqueta: 'Estado', valor: STATUS.venta[venta.estado][1] },
        ...(venta.cotizacionId
          ? [{ etiqueta: 'Cotización', valor: venta.cotizacionId, mono: true }]
          : []),
      ]}
      tablaTitulo="Productos de la venta"
      columnas={[
        { clave: 'sku', titulo: 'SKU', mono: true, celda: (l) => l.sku },
        { clave: 'nombre', titulo: 'Producto', celda: (l) => l.nombre },
        {
          clave: 'solicitada',
          titulo: 'Solicitada',
          numerica: true,
          celda: (l) => formatNumber(l.solicitada),
        },
        {
          clave: 'atendida',
          titulo: 'Atendida',
          numerica: true,
          celda: (l) => formatNumber(l.atendida),
        },
        {
          clave: 'precio',
          titulo: 'Precio unit.',
          numerica: true,
          celda: (l) => formatCurrency(l.precio),
        },
        {
          clave: 'importe',
          titulo: 'Importe',
          numerica: true,
          celda: (l) => formatCurrency(l.solicitada * l.precio),
        },
      ]}
      filas={venta.lineas}
      clave={(l) => l.sku}
      pie={[
        { rotulo: 'Subtotal', valor: formatCurrency(t.subtotal) },
        ...(t.descuento > 0
          ? [{ rotulo: 'Descuento', valor: `− ${formatCurrency(t.descuento)}` }]
          : []),
        { rotulo: 'Total', valor: formatCurrency(t.total) },
      ]}
      volverA={`/ventas/${venta.id}`}
      volverEtiqueta="Volver a la venta"
    />
  );
}
