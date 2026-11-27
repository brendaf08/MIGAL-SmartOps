import { Skeleton } from '@mantine/core';
import { useQuery } from '@tanstack/react-query';
import { useParams } from 'react-router';
import { STATUS } from '@/components/domain/StatusBadge/status';
import { DocumentoImprimible } from '@/components/domain/DocumentoImprimible/DocumentoImprimible';
import { Alert } from '@/components/ui/Alert/Alert';
import { formatCurrency, formatDate, formatNumber } from '@/lib/format';
import { obtenerCotizacion } from './api';
import { totales } from './calculos';

/** Cotización imprimible en una hoja A4, para entregar al cliente o guardar en PDF. */
export function CotizacionImprimiblePage() {
  const { id = '' } = useParams();
  const query = useQuery({ queryKey: ['cotizacion', id], queryFn: () => obtenerCotizacion(id) });

  if (query.isError) {
    return (
      <Alert tone="warning" title="La cotización no existe">
        Volver al listado y elegir otra cotización.
      </Alert>
    );
  }
  const cotizacion = query.data;
  if (!cotizacion) return <Skeleton height={240} />;

  const t = totales(cotizacion);
  return (
    <DocumentoImprimible
      titulo={`Cotización ${cotizacion.id}`}
      datos={[
        { etiqueta: 'Fecha de emisión', valor: formatDate(cotizacion.emision) },
        { etiqueta: 'Vigencia hasta', valor: formatDate(cotizacion.vencimiento) },
        { etiqueta: 'Cliente', valor: cotizacion.cliente?.nombre ?? 'Sin cliente identificado' },
        { etiqueta: 'Estado', valor: STATUS.cotizacion[cotizacion.estado][1] },
        ...(cotizacion.formaPago
          ? [{ etiqueta: 'Forma de pago', valor: cotizacion.formaPago }]
          : []),
        ...(cotizacion.condiciones
          ? [{ etiqueta: 'Condiciones', valor: cotizacion.condiciones }]
          : []),
      ]}
      tablaTitulo="Productos cotizados"
      columnas={[
        { clave: 'sku', titulo: 'SKU', mono: true, celda: (l) => l.sku },
        { clave: 'nombre', titulo: 'Producto', celda: (l) => l.nombre },
        {
          clave: 'cantidad',
          titulo: 'Cantidad',
          numerica: true,
          celda: (l) => formatNumber(l.cantidad),
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
          celda: (l) => formatCurrency(l.cantidad * l.precio),
        },
      ]}
      filas={cotizacion.lineas}
      clave={(l) => String(l.id)}
      pie={[
        { rotulo: 'Subtotal', valor: formatCurrency(t.subtotal) },
        ...(cotizacion.descuento
          ? [
              {
                rotulo: `Descuento (${formatNumber(cotizacion.descuento * 100)} %)`,
                valor: `− ${formatCurrency(t.descuento)}`,
              },
            ]
          : []),
        { rotulo: 'Total', valor: formatCurrency(t.total) },
      ]}
      volverA={`/cotizaciones/${cotizacion.id}`}
      volverEtiqueta="Volver a la cotización"
    />
  );
}
