import type { STATUS } from '@/components/domain/StatusBadge/status';

/** Estados de una venta, tomados del mapa central de estados. */
export type EstadoVenta = keyof (typeof STATUS)['venta'];

export interface LineaVenta {
  sku: string;
  nombre: string;
  solicitada: number;
  /** Cantidad atendida con stock; el resto queda pendiente. */
  atendida: number;
  precio: number;
}

/** Venta (CU-10). La diferencia entre solicitada y atendida es la cantidad pendiente. */
export interface Venta {
  id: string;
  fecha: string;
  /** Vacío si la venta se registró sin cliente. */
  clienteId?: string;
  /** Cotización aceptada de la que proviene, si corresponde. */
  cotizacionId?: string;
  estado: EstadoVenta;
  entrega: 'Completa' | 'Parcial';
  /** Descuento global en soles. */
  descuento: number;
  lineas: LineaVenta[];
}
