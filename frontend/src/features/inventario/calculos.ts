import { stockLevel } from '@/components/domain/StatusBadge/status';
import type { ProductoDisponibilidad } from './types';

/** Existencia por ubicación en el orden fijo Tienda, Almacén 1, Almacén 2; el reservado es consolidado. */
export function ubicacionesDe(p: ProductoDisponibilidad) {
  return [
    { nombre: 'Tienda principal', cantidad: p.tienda },
    { nombre: 'Almacén 1', cantidad: p.almacen1 },
    { nombre: 'Almacén 2', cantidad: p.almacen2 },
  ];
}

/** Cantidades derivadas: físico consolidado, libre y nivel de alerta sobre el stock libre. */
export function resumen(p: ProductoDisponibilidad) {
  const fisico = p.tienda + p.almacen1 + p.almacen2;
  const libre = fisico - p.reservado;
  return { fisico, libre, nivel: stockLevel(libre, p.minimo) };
}
