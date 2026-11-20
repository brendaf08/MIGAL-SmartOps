import { resumen } from '@/features/inventario/calculos';
import type { ProductoDisponibilidad } from '@/features/inventario/types';

/** Con qué se cubre una línea: lo que hay libre, lo que está por recibir o nada de lo anterior. */
export type Cobertura = 'stock' | 'recibir' | 'comprar';

export interface DesgloseLinea {
  libre: number;
  reservado: number;
  /** Lo pedido en órdenes de compra abiertas. */
  porRecibir: number;
  /** Fecha estimada más próxima de llegada, si alguna orden la indica. */
  proximaLlegada: string | null;
  /** Lo por recibir que queda tras atender lo que otras ventas ya reclaman. */
  previstoNeto: number;
  /** Otras ventas pendientes compiten por lo por recibir. */
  compite: boolean;
  cobertura: Cobertura;
}

/**
 * Desglose del stock de un producto para decidir si se puede prometer una cantidad. Es informativo: la cotización no
 * reserva existencias, así que el vendedor ve aquí con qué se atendería.
 */
export function desglosarLinea(cantidad: number, producto: ProductoDisponibilidad): DesgloseLinea {
  const libre = Math.max(resumen(producto).libre, 0);
  const faltanteVentas = producto.faltanteVentas ?? 0;
  const previstoNeto = Math.max(producto.planificado - faltanteVentas, 0);
  const cobertura: Cobertura =
    cantidad <= libre ? 'stock' : cantidad <= libre + previstoNeto ? 'recibir' : 'comprar';
  return {
    libre,
    reservado: producto.reservado,
    porRecibir: producto.planificado,
    proximaLlegada: producto.proximaLlegada ?? null,
    previstoNeto,
    compite: faltanteVentas > 0 && producto.planificado > 0,
    cobertura,
  };
}

export const ETIQUETA_COBERTURA: Record<Cobertura, string> = {
  stock: 'Cubierto con stock',
  recibir: 'Cubierto con lo por recibir',
  comprar: 'Falta comprar',
};
