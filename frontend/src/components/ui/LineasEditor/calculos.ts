/** Línea de un documento comercial o de abastecimiento mientras se edita. `sku` vacío significa sin producto. */
export interface LineaEditable {
  id: number;
  sku: string;
  cantidad: number;
  precio: number;
}

const redondear = (n: number) => Math.round(n * 100) / 100;

export const subtotalLinea = (l: LineaEditable) => redondear(l.cantidad * l.precio);

export const subtotalLineas = (lineas: readonly LineaEditable[]) =>
  redondear(lineas.reduce((suma, l) => suma + subtotalLinea(l), 0));

/** Total tras el descuento en soles; nunca es negativo. */
export const totalLineas = (lineas: readonly LineaEditable[], descuento = 0) =>
  Math.max(0, redondear(subtotalLineas(lineas) - descuento));

/** El descuento es válido si no es negativo y no supera el subtotal. */
export const descuentoValido = (lineas: readonly LineaEditable[], descuento: number) =>
  descuento >= 0 && descuento <= subtotalLineas(lineas);

/** Siguiente identificador de línea dentro de la lista. */
export const siguienteIdLinea = (lineas: readonly LineaEditable[]) =>
  lineas.reduce((mayor, l) => Math.max(mayor, l.id), 0) + 1;

/**
 * Suma las cantidades de los productos repetidos (conserva la primera fila de cada producto y su precio) y devuelve los
 * SKU que estaban repetidos. Las filas sin producto se dejan como están.
 */
export function consolidarLineas(lineas: readonly LineaEditable[]): {
  lineas: LineaEditable[];
  repetidos: string[];
} {
  const porSku = new Map<string, LineaEditable>();
  const repetidos = new Set<string>();
  const resultado: LineaEditable[] = [];
  for (const l of lineas) {
    const previa = l.sku ? porSku.get(l.sku) : undefined;
    if (previa) {
      previa.cantidad = Math.round((previa.cantidad + l.cantidad) * 1000) / 1000;
      repetidos.add(l.sku);
      continue;
    }
    const copia = { ...l };
    if (copia.sku) porSku.set(copia.sku, copia);
    resultado.push(copia);
  }
  return { lineas: resultado, repetidos: [...repetidos] };
}
