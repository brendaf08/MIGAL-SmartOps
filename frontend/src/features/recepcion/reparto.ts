import type { LineaRecepcion } from './types';

const suma = (reparto: Record<string, number>) =>
  Object.values(reparto).reduce((total, cantidad) => total + cantidad, 0);

/**
 * Reparto de una línea por ubicación. Una línea que aún no se repartió (`reparto` ausente) cuenta como recibida
 * entera en `porDefecto`; una con reparto vacío tiene todo su recibido sin ubicar.
 */
export const repartoDe = (linea: LineaRecepcion, porDefecto: string): Record<string, number> =>
  linea.reparto ?? (linea.recibido > 0 ? { [porDefecto]: linea.recibido } : {});

/** Unidades de la línea ya ubicadas en alguna ubicación. */
export const ubicado = (linea: LineaRecepcion): number => suma(linea.reparto ?? {});

/** Unidades recibidas que aún no tienen ubicación. */
export const sinUbicar = (linea: LineaRecepcion): number =>
  Math.max(linea.recibido - ubicado(linea), 0);

/** Lo ubicado en una ubicación. */
export const cantidadEn = (linea: LineaRecepcion, ubicacion: string): number =>
  linea.reparto?.[ubicacion] ?? 0;

/** Lo máximo que admite una ubicación: lo recibido menos lo ya ubicado en las demás. */
export const maximoEn = (linea: LineaRecepcion, ubicacion: string): number =>
  Math.max(linea.recibido - (ubicado(linea) - cantidadEn(linea, ubicacion)), 0);

/** Ubica unidades en una ubicación sin pasar de lo que queda por ubicar; con 0 la quita. */
export function ubicarEn(
  linea: LineaRecepcion,
  ubicacion: string,
  cantidad: number,
): LineaRecepcion {
  const valida = Math.min(Math.max(cantidad, 0), maximoEn(linea, ubicacion));
  return { ...linea, reparto: { ...linea.reparto, [ubicacion]: valida } };
}

/**
 * Cambia el total recibido de la línea. Si lo ubicado supera el nuevo total, el exceso se quita primero de la
 * ubicación que se está viendo y luego de las demás.
 */
export function recibirTotal(
  linea: LineaRecepcion,
  total: number,
  preferida: string,
): LineaRecepcion {
  const recibido = Math.max(total, 0);
  const reparto = { ...linea.reparto };
  let exceso = suma(reparto) - recibido;
  for (const ubicacion of [preferida, ...Object.keys(reparto).filter((u) => u !== preferida)]) {
    if (exceso <= 0) break;
    const quitar = Math.min(reparto[ubicacion] ?? 0, exceso);
    if (quitar > 0) {
      reparto[ubicacion] = (reparto[ubicacion] ?? 0) - quitar;
      exceso -= quitar;
    }
  }
  return { ...linea, recibido, reparto };
}

/** Ubica en una ubicación todo lo que cada línea tiene sin ubicar. */
export const ubicarTodoEn = (lineas: LineaRecepcion[], ubicacion: string): LineaRecepcion[] =>
  lineas.map((l) => ubicarEn(l, ubicacion, maximoEn(l, ubicacion)));

/** Punto de partida de un conteo: nada ubicado todavía. */
export const sinReparto = (lineas: LineaRecepcion[]): LineaRecepcion[] =>
  lineas.map((l) => ({ ...l, reparto: {} }));

/** Total de unidades recibidas sin ubicar en todas las líneas. */
export const totalSinUbicar = (lineas: LineaRecepcion[]): number =>
  lineas.reduce((total, l) => total + (l.reparto ? sinUbicar(l) : 0), 0);

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
