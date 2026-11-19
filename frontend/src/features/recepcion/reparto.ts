import type { LineaRecepcion } from './types';

const suma = (reparto: Record<string, number>) =>
  Object.values(reparto).reduce((total, cantidad) => total + cantidad, 0);

/**
 * Lo recibido de una línea en cada ubicación. Una línea que aún no tiene reparto (`reparto` ausente) cuenta como
 * recibida entera en `porDefecto`.
 */
export const repartoDe = (linea: LineaRecepcion, porDefecto: string): Record<string, number> =>
  linea.reparto ?? (linea.recibido > 0 ? { [porDefecto]: linea.recibido } : {});

/** Lo recibido en una ubicación. */
export const cantidadEn = (linea: LineaRecepcion, ubicacion: string): number =>
  linea.reparto?.[ubicacion] ?? 0;

/**
 * Tope de una ubicación: lo pendiente de la orden menos lo recibido en las demás. Nunca se recibe más de lo pedido, de
 * modo que un producto que no está en la orden (pendiente 0) no admite cantidad.
 */
export const maximoEn = (linea: LineaRecepcion, ubicacion: string): number =>
  Math.max(linea.pedido - (linea.recibido - cantidadEn(linea, ubicacion)), 0);

/** Registra lo recibido en una ubicación, sin pasar del tope; el total de la línea es la suma de sus ubicaciones. */
export function recibirEn(
  linea: LineaRecepcion,
  ubicacion: string,
  cantidad: number,
): LineaRecepcion {
  const valida = Math.min(Math.max(cantidad, 0), maximoEn(linea, ubicacion));
  const reparto = { ...linea.reparto, [ubicacion]: valida };
  return { ...linea, reparto, recibido: suma(reparto) };
}

/** Todo lo que le falta a la línea para completar lo pedido llegó a esta ubicación. */
export const completarEn = (linea: LineaRecepcion, ubicacion: string): LineaRecepcion =>
  recibirEn(linea, ubicacion, maximoEn(linea, ubicacion));

/** Punto de partida sin documento: nada recibido en ninguna ubicación. */
export const sinReparto = (lineas: LineaRecepcion[]): LineaRecepcion[] =>
  lineas.map((l) => ({ ...l, recibido: 0, reparto: {} }));

/** Punto de partida con la guía leída: lo que dice la guía queda recibido en la ubicación elegida, hasta el tope. */
export const repartoDeLaGuia = (lineas: LineaRecepcion[], ubicacion: string): LineaRecepcion[] =>
  lineas.map((l) => recibirEn({ ...l, recibido: 0, reparto: {} }, ubicacion, l.recibido));

/** Ubicaciones con mercadería y el total de unidades en cada una, en orden de aparición. */
export function totalesPorUbicacion(
  lineas: LineaRecepcion[],
  porDefecto: string,
): [string, number][] {
  const totales = new Map<string, number>();
  for (const linea of lineas) {
    for (const [ubicacion, cantidad] of Object.entries(repartoDe(linea, porDefecto))) {
      if (cantidad > 0) totales.set(ubicacion, (totales.get(ubicacion) ?? 0) + cantidad);
    }
  }
  return [...totales];
}

/** Cantidad de movimientos de entrada: uno por producto y ubicación con unidades. */
export const movimientosDeEntrada = (lineas: LineaRecepcion[], porDefecto: string): number =>
  lineas.reduce(
    (total, l) => total + Object.values(repartoDe(l, porDefecto)).filter((c) => c > 0).length,
    0,
  );
