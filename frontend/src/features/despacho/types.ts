/** Ubicación de la que sale parte de una línea, con la cantidad. */
export interface SalidaUbicacion {
  ubicacion: string;
  cantidad: number;
}

export interface LineaDespacho {
  sku: string;
  nombre: string;
  cantidad: number;
  reservado: number;
  /** De qué ubicaciones sale la línea (las reservas de la venta); lo informa la API. */
  salidas?: SalidaUbicacion[];
}

/** Valores de `EstadoGuia` del backend; sus nombres definitivos están pendientes de armonizar. */
export type EstadoGuia = 'Preparacion' | 'Emitida' | 'SalidaConfirmada' | 'Entregada';

/** Forma de entrega de la orden. */
export type TipoEntrega = 'envio' | 'retiro_tienda';

export interface OrdenDespacho {
  id: string;
  venta: string;
  cliente: string;
  estado: EstadoGuia;
  entrega: 'Completa' | 'Parcial';
  /** Envío a una dirección o retiro en tienda (venta de mostrador). Sin dato se trata como envío. */
  tipoEntrega?: TipoEntrega;
  /** Dirección del envío o punto de retiro. */
  direccion?: string;
  lineas: LineaDespacho[];
}
