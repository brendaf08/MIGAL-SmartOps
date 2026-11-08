import type { LineaOcr } from '@/features/documentos-ocr/api';
import type { OrdenRecepcionable } from './api';
import type { LineaRecepcion } from './types';

/**
 * Filas de conteo de una recepción: una por línea de la orden, con lo que leyó el OCR para ese producto, y una
 * por cada línea del documento que no pertenece a la orden (sobrante). Sin documento, lo recibido parte en 0.
 */
export function construirLineas(
  orden: OrdenRecepcionable,
  ocr: LineaOcr[] | null,
): LineaRecepcion[] {
  const deOrden: LineaRecepcion[] = orden.lineas.map((linea, i) => {
    const leidas = (ocr ?? []).filter((l) => l.sku === linea.sku);
    return {
      id: i + 1,
      texto: linea.nombre,
      sku: linea.sku,
      score: leidas.length > 0 ? Math.max(...leidas.map((l) => l.score)) : 1,
      pedido: linea.cantidad - linea.recibido,
      recibido: leidas.reduce((suma, l) => suma + l.cantidad, 0),
    };
  });

  const sobrantes: LineaRecepcion[] = (ocr ?? [])
    .filter((l) => !l.sku || !orden.lineas.some((o) => o.sku === l.sku))
    .map((l, i) => ({
      id: 100 + i,
      texto: l.texto,
      sku: l.sku,
      score: l.score,
      pedido: 0,
      recibido: l.cantidad,
    }));

  return [...deOrden, ...sobrantes];
}
