import type { Cotizacion, EstadoCotizacion } from './types';

/** Subtotal, descuento y total de una cotización. */
export function totales({ lineas, descuento }: Pick<Cotizacion, 'lineas' | 'descuento'>) {
  const subtotal = lineas.reduce((sum, l) => sum + l.cantidad * l.precio, 0);
  const monto = subtotal * descuento;
  return { subtotal, descuento: monto, total: subtotal - monto };
}

const aIso = (fecha: Date) =>
  `${fecha.getFullYear()}-${String(fecha.getMonth() + 1).padStart(2, '0')}-${String(fecha.getDate()).padStart(2, '0')}`;

const aDiaUtc = (iso: string) => {
  const [anio, mes, dia] = iso.split('-').map(Number);
  return Date.UTC(anio!, mes! - 1, dia!);
};

/** Fecha de hoy en formato `aaaa-mm-dd`, según la hora local. */
export const hoyIso = () => aIso(new Date());

/** Suma días a una fecha `aaaa-mm-dd` y devuelve otra en el mismo formato. */
export function sumarDias(iso: string, dias: number) {
  const [anio, mes, dia] = iso.split('-').map(Number);
  return aIso(new Date(anio!, mes! - 1, dia! + dias));
}

/** Días naturales de `desde` a `hasta`; negativo si `hasta` ya pasó. */
export const diasEntre = (desde: string, hasta: string) =>
  Math.round((aDiaUtc(hasta) - aDiaUtc(desde)) / 86_400_000);

/**
 * Estado vigente de una cotización: una emitida cuyo vencimiento ya pasó se muestra como vencida (flujo alternativo 3).
 * El vencimiento es inclusivo: la cotización sigue vigente el día en que vence.
 */
export function estadoVigente(
  { estado, vencimiento }: Pick<Cotizacion, 'estado' | 'vencimiento'>,
  hoy: string,
): EstadoCotizacion {
  return estado === 'Emitida' && vencimiento < hoy ? 'Vencida' : estado;
}

/** Ciclo de vida mostrado en el `Stepper`; las cotizaciones rechazadas o vencidas quedan en el primer paso. */
export const PASOS = ['Emitida', 'Aceptada', 'Validada', 'Venta'].map((label) => ({ label }));
const AVANCE: Record<EstadoCotizacion, number> = {
  Emitida: 0,
  AceptadaCliente: 1,
  ValidadaMigal: 2,
  Rechazada: 0,
  Vencida: 0,
};
export const pasoActual = (estado: EstadoCotizacion) => AVANCE[estado];
