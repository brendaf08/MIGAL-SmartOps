import { stockLevel } from '@/components/domain/StatusBadge/status';
import type { ProductoDisponibilidad } from './types';

/** Cantidades derivadas: físico consolidado, libre y nivel de alerta sobre el stock libre. */
export function resumen(p: ProductoDisponibilidad) {
  const fisico = p.tienda + p.almacen1 + p.almacen2;
  const libre = fisico - p.reservado;
  return { fisico, libre, nivel: stockLevel(libre, p.minimo) };
}
