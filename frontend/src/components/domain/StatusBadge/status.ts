import type { IconName } from '../../ui/Icon/icons';

export type Tone = 'info' | 'brand' | 'success' | 'warning' | 'danger' | 'neutral';
export type Definition = [tone: Tone, label: string, icon: IconName];

/**
 * Mapa central de estados: valor de la enumeración del backend a tono, texto e ícono.
 * Un valor nuevo se agrega aquí una sola vez. Los nombres de los estados de cotización, venta y guía
 * están pendientes de armonizar con los casos de uso.
 */
export const STATUS = {
  cotizacion: {
    Emitida: ['info', 'Emitida', 'info'],
    AceptadaCliente: ['brand', 'Aceptada por el cliente', 'check'],
    ValidadaMigal: ['success', 'Validada por MIGAL', 'status-ok'],
    Rechazada: ['danger', 'Rechazada', 'status-danger'],
    Vencida: ['neutral', 'Vencida', 'clock'],
  },
  venta: {
    PendienteAbastecimiento: ['warning', 'Pendiente de abastecimiento', 'alert-triangle'],
    PendienteDespacho: ['info', 'Pendiente de despacho', 'info'],
    ListaDespacho: ['brand', 'Lista para despacho', 'check'],
    Despachada: ['success', 'Despachada', 'status-ok'],
    Cancelada: ['danger', 'Cancelada', 'status-danger'],
  },
  guia: {
    Preparacion: ['neutral', 'En preparación', 'clock'],
    Emitida: ['info', 'Emitida', 'info'],
    SalidaConfirmada: ['brand', 'Salida confirmada', 'despacho'],
    Entregada: ['success', 'Entregada', 'status-ok'],
  },
  compra: {
    Pendiente: ['warning', 'Pendiente', 'alert-triangle'],
    Parcial: ['info', 'Recepción parcial', 'info'],
    Completada: ['success', 'Completada', 'status-ok'],
  },
  recepcion: {
    Pendiente: ['warning', 'Pendiente de validación', 'alert-triangle'],
    Validada: ['success', 'Validada', 'status-ok'],
  },
  documento: {
    Pendiente: ['neutral', 'Pendiente', 'clock'],
    Procesado_OCR: ['info', 'Procesado por OCR', 'info'],
    Validado: ['success', 'Validado', 'status-ok'],
  },
  reserva: {
    Activa: ['brand', 'Reserva activa', 'reserva'],
    Liberada: ['neutral', 'Liberada', 'arrow-up'],
    Consumida: ['success', 'Consumida', 'status-ok'],
  },
  stock: {
    Normal: ['success', 'Stock suficiente', 'status-ok'],
    Minimo: ['warning', 'En el mínimo', 'alert-triangle'],
    Critico: ['danger', 'Stock crítico', 'status-danger'],
  },
  registro: {
    Activo: ['success', 'Activo', 'status-ok'],
    Inactivo: ['neutral', 'Inactivo', 'x-circle'],
  },
} satisfies Record<string, Record<string, Definition>>;

export type StatusEntity = keyof typeof STATUS;
export type StockLevel = keyof typeof STATUS.stock;

/** Nivel de alerta sobre el stock libre (supuesto pendiente de confirmar: libre o físico). */
export function stockLevel(libre: number, minimo?: number): StockLevel {
  if (libre <= 0) return 'Critico';
  return minimo !== undefined && libre <= minimo ? 'Minimo' : 'Normal';
}
