/** Cliente embebido en una cotización. El RUC y la dirección no los devuelve la API comercial y se omiten en ese caso. */
export interface Cliente {
  id: string;
  nombre: string;
  ruc?: string;
  direccion?: string;
}

export interface LineaCotizacion {
  id: number;
  sku: string;
  nombre: string;
  cantidad: number;
  precio: number;
}

/** Valores de `EstadoCotizacion` del backend; sus nombres definitivos están pendientes de armonizar. */
export type EstadoCotizacion =
  'Emitida' | 'AceptadaCliente' | 'ValidadaMigal' | 'Rechazada' | 'Cancelada' | 'Vencida';

export interface Cotizacion {
  id: string;
  estado: EstadoCotizacion;
  emision: string;
  vencimiento: string;
  /** El cliente es opcional (RF-CLI-04): su ausencia no bloquea la emisión. */
  cliente?: Cliente;
  condiciones?: string;
  /** Forma de pago elegida al emitir (por ejemplo, "Contado"). */
  formaPago?: string;
  /** Descuento global, de 0 a 1. */
  descuento: number;
  lineas: LineaCotizacion[];
  /** Venta que originó la cotización; con venta no se modifica ni se cancela. Solo la informa el detalle. */
  ventaId?: string;
  /** Rechazo o cancelación registrada. */
  rechazo?: { origen: 'cliente' | 'migal'; motivo: string };
  /** Modificaciones y cancelaciones, de la más reciente a la más antigua. Solo las informa el detalle. */
  historial?: CambioCotizacion[];
}

/** Cambio de estado por modificación o cancelación, con su motivo. */
export interface CambioCotizacion {
  fecha: string;
  usuario: string;
  estadoAnterior: EstadoCotizacion;
  estadoNuevo: EstadoCotizacion;
  motivo: string;
}
