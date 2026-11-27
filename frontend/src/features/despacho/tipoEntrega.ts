import type { TipoEntrega } from './types';

/** Texto con el que se nombra cada forma de entrega en las pantallas. */
export const TIPO_ENTREGA_ETIQUETA: Record<TipoEntrega, string> = {
  envio: 'Envío a dirección',
  retiro_tienda: 'Retiro en tienda',
};
