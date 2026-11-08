export interface Cliente {
  nombre: string;
  ruc: string;
  direccion: string;
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
  'Emitida' | 'AceptadaCliente' | 'ValidadaMigal' | 'Rechazada' | 'Vencida';

export interface Cotizacion {
  id: string;
  estado: EstadoCotizacion;
  emision: string;
  vencimiento: string;
  /** El cliente es opcional (RF-CLI-04): su ausencia no bloquea la emisión. */
  cliente?: Cliente;
  condiciones?: string;
  /** Descuento global, de 0 a 1. */
  descuento: number;
  lineas: LineaCotizacion[];
}
