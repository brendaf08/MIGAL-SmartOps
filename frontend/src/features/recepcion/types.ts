/** Línea precargada por OCR y Fuzzy Matching; el usuario la revisa y corrige antes de confirmar. */
export interface LineaRecepcion {
  id: number;
  /** Texto tal como lo leyó el OCR. */
  texto: string;
  /** SKU sugerido o elegido; vacío si no hubo coincidencia. */
  sku: string;
  /** Similitud con el catálogo, de 0 a 1. */
  score: number;
  pedido: number;
  recibido: number;
}

export interface DocumentoProcesado {
  orden: string;
  proveedor: string;
  almacen: string;
  lineas: LineaRecepcion[];
}

export interface Recepcion {
  id: string;
  orden: string;
  proveedor: string;
  almacen: string;
  fecha: string;
  estado: 'Pendiente' | 'Validada';
}
