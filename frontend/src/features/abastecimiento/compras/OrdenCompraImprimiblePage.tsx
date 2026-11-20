import { Skeleton } from '@mantine/core';
import { useQuery } from '@tanstack/react-query';
import { useParams } from 'react-router';
import { STATUS } from '@/components/domain/StatusBadge/status';
import { DocumentoImprimible } from '@/components/domain/DocumentoImprimible/DocumentoImprimible';
import { Alert } from '@/components/ui/Alert/Alert';
import { formatCurrency, formatDate, formatNumber } from '@/lib/format';
import { obtenerOrdenCompra, subtotalLineaOrden, totalOrden } from './api';

/** Orden de compra imprimible en una hoja A4, para enviar al proveedor o guardar en PDF. */
export function OrdenCompraImprimiblePage() {
  const { id = '' } = useParams();
  const query = useQuery({ queryKey: ['orden-compra', id], queryFn: () => obtenerOrdenCompra(id) });

  if (query.isError) {
    return (
      <Alert tone="warning" title="La orden de compra no existe">
        Volver al listado y elegir otra orden.
      </Alert>
    );
  }
  if (!query.data) return <Skeleton height={240} />;

  const { orden, proveedor } = query.data;
  return (
    <DocumentoImprimible
      titulo={`Orden de compra ${orden.id}`}
      datos={[
        { etiqueta: 'Fecha', valor: formatDate(orden.fecha) },
        { etiqueta: 'Estado', valor: STATUS.compra[orden.estado][1] },
        { etiqueta: 'Proveedor', valor: proveedor.razonSocial },
        { etiqueta: 'RUC', valor: proveedor.ruc, mono: true },
      ]}
      tablaTitulo="Productos pedidos"
      columnas={[
        { clave: 'sku', titulo: 'SKU', mono: true, celda: (l) => l.sku },
        { clave: 'nombre', titulo: 'Producto', celda: (l) => l.nombre },
        {
          clave: 'cantidad',
          titulo: 'Pedida',
          numerica: true,
          celda: (l) => formatNumber(l.cantidad),
        },
        {
          clave: 'precio',
          titulo: 'Precio',
          numerica: true,
          celda: (l) => formatCurrency(l.precio),
        },
        {
          clave: 'subtotal',
          titulo: 'Subtotal',
          numerica: true,
          celda: (l) => formatCurrency(subtotalLineaOrden(l)),
        },
      ]}
      filas={orden.lineas}
      clave={(l) => l.sku}
      pie={[{ rotulo: 'Total de la orden', valor: formatCurrency(totalOrden(orden.lineas)) }]}
      volverA={`/compras/${orden.id}`}
      volverEtiqueta="Volver a la orden"
    />
  );
}
