import type { LineaRecepcion } from './types';

const suma = (reparto: Record<string, number>) =>
  Object.values(reparto).reduce((total, cantidad) => total + cantidad, 0);

/** Lo recibido de una línea en una ubicación; sin reparto, todo se toma como recibido en `porDefecto`. */
export const cantidadEn = (linea: LineaRecepcion, ubicacion: string, porDefecto: string): number =>
  linea.reparto ? (linea.reparto[ubicacion] ?? 0) : ubicacion === porDefecto ? linea.recibido : 0;

/** Reparto de una línea, con lo recibido en `porDefecto` si aún no se repartió. */
export const repartoDe = (linea: LineaRecepcion, porDefecto: string): Record<string, number> =>
  linea.reparto ?? (linea.recibido > 0 ? { [porDefecto]: linea.recibido } : {});

/** Asigna a una ubicación todo lo recibido de cada línea (punto de partida de un conteo nuevo). */
export const conRepartoInicial = (lineas: LineaRecepcion[], ubicacion: string): LineaRecepcion[] =>
  lineas.map((l) => ({ ...l, reparto: l.recibido > 0 ? { [ubicacion]: l.recibido } : {} }));

/** Registra lo recibido en una ubicación; el total de la línea es la suma de todas sus ubicaciones. */
export function fijarEnUbicacion(
  linea: LineaRecepcion,
  ubicacion: string,
  cantidad: number,
  porDefecto: string,
): LineaRecepcion {
  const reparto = { ...repartoDe(linea, porDefecto), [ubicacion]: Math.max(cantidad, 0) };
  return { ...linea, reparto, recibido: suma(reparto) };
}

/** Lleva el total recibido de la línea a `total`, ajustando solo la ubicación indicada y respetando las demás. */
export function fijarTotal(
  linea: LineaRecepcion,
  ubicacion: string,
  total: number,
  porDefecto: string,
): LineaRecepcion {
  const reparto = repartoDe(linea, porDefecto);
  const enOtras = suma(reparto) - (reparto[ubicacion] ?? 0);
  return fijarEnUbicacion(linea, ubicacion, Math.max(total - enOtras, 0), porDefecto);
}

/** Ubicaciones con mercadería recibida y el total de unidades en cada una, en orden de aparición. */
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

/** Cantidad de movimientos de entrada: uno por producto y ubicación con unidades recibidas. */
export const movimientosDeEntrada = (lineas: LineaRecepcion[], porDefecto: string): number =>
  lineas.reduce(
    (total, l) => total + Object.values(repartoDe(l, porDefecto)).filter((c) => c > 0).length,
    0,
  );
