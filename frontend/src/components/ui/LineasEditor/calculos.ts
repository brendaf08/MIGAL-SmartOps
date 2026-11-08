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
