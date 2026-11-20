import { Skeleton } from '@mantine/core';
import { useQuery } from '@tanstack/react-query';
import { useParams } from 'react-router';
import { DocumentoImprimible } from '@/components/domain/DocumentoImprimible/DocumentoImprimible';
import { Alert } from '@/components/ui/Alert/Alert';
import { formatDate, formatNumber } from '@/lib/format';
import { obtenerDocumentoOrden } from './api';

/** CU-06, orden de despacho imprimible en una hoja A4. La barra de acciones queda fuera del documento y no se imprime. */
export function OrdenImprimiblePage() {
  const { id = '' } = useParams();
  const query = useQuery({
    queryKey: ['orden-imprimible', id],
    queryFn: () => obtenerDocumentoOrden(id),
  });

  if (query.isError) {
    return (
      <Alert tone="warning" title="La orden no existe">
        Volver al listado y elegir otra orden.
      </Alert>
    );
  }
  if (!query.data) return <Skeleton height={240} />;

  const { orden, fecha, direccion, lineas } = query.data;

  return (
    <DocumentoImprimible
      titulo={`Orden de despacho ${orden.id}`}
      datos={[
        { etiqueta: 'Fecha', valor: formatDate(fecha) },
        { etiqueta: 'Venta', valor: orden.venta, mono: true },
        { etiqueta: 'Cliente', valor: orden.cliente },
        { etiqueta: 'Dirección de entrega', valor: direccion },
      ]}
      tablaTitulo="Productos a despachar"
      columnas={[
        { clave: 'sku', titulo: 'SKU', mono: true, celda: (l) => l.sku },
        { clave: 'nombre', titulo: 'Producto', celda: (l) => l.nombre },
        {
          clave: 'cantidad',
          titulo: 'Cantidad',
          numerica: true,
          celda: (l) => formatNumber(l.cantidad),
        },
        { clave: 'unidad', titulo: 'Unidad', celda: (l) => l.unidad },
      ]}
      filas={lineas}
      clave={(l) => l.sku}
      pie={[{ rotulo: 'Total de líneas', valor: formatNumber(lineas.length), columna: 2 }]}
      firmas={['Despachó', 'Chofer', 'Recibió']}
      volverA={`/despachos/${orden.id}`}
      volverEtiqueta="Volver a la orden"
    />
  );
}
