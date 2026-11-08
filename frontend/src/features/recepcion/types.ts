import type { OrdenCompra } from '@/features/abastecimiento/tipos';

/** Línea de conteo: lo pendiente de la orden frente a lo efectivamente recibido (CU-13). */
export interface LineaRecepcion {
  id: number;
  /** Texto de la línea: el leído por el OCR o el nombre del producto de la orden. */
  texto: string;
  /** SKU del producto; vacío si el OCR no encontró coincidencia. */
  sku: string;
  /** Similitud con el catálogo, de 0 a 1. */
  score: number;
  /** Cantidad pendiente en la orden; 0 para los sobrantes. */
  pedido: number;
  recibido: number;
}

export interface Recepcion {
  id: string;
  orden: string;
  proveedor: string;
  almacen: string;
  fecha: string;
  estado: 'Pendiente' | 'Validada';
  /** Motivo de los sobrantes, si los hubo. */
  nota?: string;
}

/** Recepción con el estado vigente de su orden de compra. */
export interface RecepcionListada extends Recepcion {
  estadoOrden: OrdenCompra['estado'];
}
