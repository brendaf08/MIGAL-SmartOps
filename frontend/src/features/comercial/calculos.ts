import type { Cotizacion, EstadoCotizacion } from './types';

/** Subtotal, descuento y total de una cotización. */
export function totales({ lineas, descuento }: Pick<Cotizacion, 'lineas' | 'descuento'>) {
  const subtotal = lineas.reduce((sum, l) => sum + l.cantidad * l.precio, 0);
  const monto = subtotal * descuento;
  return { subtotal, descuento: monto, total: subtotal - monto };
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
