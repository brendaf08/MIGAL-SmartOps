export interface LineaDespacho {
  sku: string;
  nombre: string;
  cantidad: number;
  reservado: number;
}

/** Valores de `EstadoGuia` del backend; sus nombres definitivos están pendientes de armonizar. */
export type EstadoGuia = 'Preparacion' | 'Emitida' | 'SalidaConfirmada' | 'Entregada';

export interface OrdenDespacho {
  id: string;
  venta: string;
  cliente: string;
  estado: EstadoGuia;
  entrega: 'Completa' | 'Parcial';
  lineas: LineaDespacho[];
}
