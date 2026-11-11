import { z } from 'zod';
import { formatNumber } from '@/lib/format';
import { PRODUCTOS } from '../fixtures';
import type { ProductoDisponibilidad } from '../types';

export const TIPOS_MOVIMIENTO = [
  { value: 'entrada', label: 'Entrada por ajuste', origen: 'Ajuste', signo: 'Entrada' },
  { value: 'salida', label: 'Salida por merma o ajuste', origen: 'Ajuste', signo: 'Salida' },
  { value: 'traslado', label: 'Traslado interno', origen: 'Traslado', signo: 'Traslado' },
] as const;

export type TipoMovimiento = (typeof TIPOS_MOVIMIENTO)[number]['value'];

/** Ubicaciones de existencia, en el orden fijo Tienda, Almacén 1, Almacén 2. */
export const UBICACIONES = ['Tienda principal', 'Almacén 1', 'Almacén 2'] as const;
export type Ubicacion = (typeof UBICACIONES)[number];

/** Campo del producto que guarda la existencia física de cada ubicación. */
export const CAMPO_UBICACION = {
  'Tienda principal': 'tienda',
  'Almacén 1': 'almacen1',
  'Almacén 2': 'almacen2',
} as const satisfies Record<Ubicacion, keyof ProductoDisponibilidad>;

export const buscarProducto = (sku: string) => PRODUCTOS.find((p) => p.sku === sku);

/** Existencia física de una ubicación: base de las salidas y de los traslados (no el stock libre). */
export const existenciaEn = (p: ProductoDisponibilidad, ubicacion: Ubicacion) =>
  p[CAMPO_UBICACION[ubicacion]];

/** Texto del bloqueo cuando la salida o el traslado superan la existencia del origen; `null` si alcanza. */
export function bloqueoDeExistencia(
  producto: ProductoDisponibilidad | null | undefined,
  ubicacion: string,
  cantidad: number,
): string | null {
  if (!producto || !(ubicacion in CAMPO_UBICACION) || !(cantidad > 0)) return null;
  const hay = existenciaEn(producto, ubicacion as Ubicacion);
  return cantidad > hay
    ? `Stock insuficiente: hay ${formatNumber(hay)} y se solicitan ${formatNumber(cantidad)}`
    : null;
}

/** Bloqueo para un producto del catálogo de prueba, identificado por SKU. */
export const bloqueoPorStock = (sku: string, ubicacion: string, cantidad: number) =>
  bloqueoDeExistencia(buscarProducto(sku), ubicacion, cantidad);

/** Esquema del formulario de movimiento. Los campos según el tipo se exigen en `superRefine`. */
export const movimientoSchema = z
  .object({
    tipo: z.enum(['entrada', 'salida', 'traslado']),
    sku: z.string().min(1, 'Seleccionar un producto.'),
    ubicacion: z.string(),
    origen: z.string(),
    destino: z.string(),
    cantidad: z.coerce
      .number({ error: 'Ingresar la cantidad.' })
      .int('La cantidad debe ser un número entero.')
      .gt(0, 'La cantidad debe ser mayor que cero.'),
    motivo: z.string().trim().min(10, 'Ingresar el motivo con al menos 10 caracteres.'),
  })
  .superRefine((v, ctx) => {
    if (v.tipo === 'traslado') {
      if (!v.origen) {
        ctx.addIssue({
          code: 'custom',
          path: ['origen'],
          message: 'Seleccionar el almacén de origen.',
        });
      }
      if (!v.destino) {
        ctx.addIssue({
          code: 'custom',
          path: ['destino'],
          message: 'Seleccionar el almacén de destino.',
        });
      } else if (v.destino === v.origen) {
        ctx.addIssue({
          code: 'custom',
          path: ['destino'],
          message: 'El almacén de destino debe ser distinto del de origen.',
        });
      }
    } else if (!v.ubicacion) {
      ctx.addIssue({ code: 'custom', path: ['ubicacion'], message: 'Seleccionar el almacén.' });
    }
  });

/* El stock insuficiente no se valida aquí: depende de las existencias del producto, que el formulario obtiene en cada
 * caso (`bloqueoDeExistencia`) y que la API valida al registrar. */

export type DatosFormulario = z.infer<typeof movimientoSchema>;
