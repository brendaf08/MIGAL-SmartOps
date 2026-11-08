import type { LineaRecepcion } from './types';

/** Umbral de ejemplo para considerar alta una coincidencia; el valor definitivo lo define el servicio Python. */
export const UMBRAL_COINCIDENCIA_ALTA = 0.85;

export type DiferenciaLinea =
  | { tipo: 'sinAsociar' }
  | { tipo: 'completa' }
  | { tipo: 'faltan'; cantidad: number }
  | { tipo: 'sobran'; cantidad: number };

export interface ResumenRecepcion {
  total: number;
  completas: number;
  parciales: number;
  sobrantes: number;
  sinAsociar: number;
  /** Solo las líneas sin producto asociado impiden confirmar; la recepción parcial se admite (RF-COM-11). */
  puedeConfirmar: boolean;
  /** Texto del bloqueo para mostrar junto a la acción principal; `null` si no hay bloqueo. */
  motivoBloqueo: string | null;
}

/** Compara lo recibido con lo pedido; una línea sin producto no tiene diferencia que evaluar. */
export function diferenciaLinea(linea: LineaRecepcion): DiferenciaLinea {
  if (!linea.sku) return { tipo: 'sinAsociar' };
  if (linea.recibido < linea.pedido) return { tipo: 'faltan', cantidad: linea.pedido - linea.recibido };
  if (linea.recibido > linea.pedido) return { tipo: 'sobran', cantidad: linea.recibido - linea.pedido };
  return { tipo: 'completa' };
}

export function resumirRecepcion(lineas: LineaRecepcion[]): ResumenRecepcion {
  const tipos = lineas.map((l) => diferenciaLinea(l).tipo);
  const cuenta = (tipo: DiferenciaLinea['tipo']) => tipos.filter((t) => t === tipo).length;
  const sinAsociar = cuenta('sinAsociar');

  let motivoBloqueo: string | null = null;
  if (lineas.length === 0) motivoBloqueo = 'No hay líneas para confirmar.';
  else if (sinAsociar > 0) {
    motivoBloqueo = `Falta asociar ${sinAsociar} ${sinAsociar === 1 ? 'línea' : 'líneas'} a un producto del catálogo.`;
  }

  return {
    total: lineas.length,
    completas: cuenta('completa'),
    parciales: cuenta('faltan'),
    sobrantes: cuenta('sobran'),
    sinAsociar,
    puedeConfirmar: motivoBloqueo === null,
    motivoBloqueo,
  };
}

/** Unidades que faltan por recibir respecto del pedido, solo en líneas con producto asociado. */
export function faltanteTotal(lineas: LineaRecepcion[]): number {
  return lineas.reduce((suma, l) => {
    const d = diferenciaLinea(l);
    return d.tipo === 'faltan' ? suma + d.cantidad : suma;
  }, 0);
}

/** Iguala lo recibido al pedido solo en las líneas con producto asociado y coincidencia alta. */
export function igualarCoincidenciasAltas(
  lineas: LineaRecepcion[],
  umbral = UMBRAL_COINCIDENCIA_ALTA,
): LineaRecepcion[] {
  return lineas.map((l) => (l.sku && l.score >= umbral ? { ...l, recibido: l.pedido } : l));
}
