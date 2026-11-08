import type { ProductoDisponibilidad } from './types';

/** Catálogo de prueba hasta contar con la API de inventario. */
const base: [prefix: string, nombre: string, categoria: string, unidad: string, minimo: number][] =
  [
    ['CEM', 'Cemento Sol tipo I, bolsa 42.5 kg', 'Cemento', 'bolsa', 200],
    ['LAD', 'Ladrillo King Kong 18 huecos', 'Ladrillos', 'unidad', 800],
    ['FIE', 'Fierro corrugado 1/2 in x 9 m', 'Fierros', 'varilla', 100],
    ['AGR', 'Arena gruesa, metro cúbico', 'Agregados', 'm³', 10],
    ['PIN', 'Pintura látex blanco, balde 4 L', 'Otras', 'balde', 15],
    ['TUB', 'Tubo PVC desagüe 4 in x 3 m', 'Gasfitería', 'unidad', 30],
    ['CAB', 'Cable THW 12 AWG, rollo 100 m', 'Electricidad', 'rollo', 12],
    ['CLA', 'Clavos de 3 in, bolsa 1 kg', 'Ferretería', 'bolsa', 40],
  ];

/** 48 productos: cada variante repite la familia con distintas existencias para ejercitar filtros y paginación. */
export const PRODUCTOS: ProductoDisponibilidad[] = Array.from({ length: 48 }, (_, i) => {
  const [prefix, nombre, categoria, unidad, minimo] = base[i % base.length]!;
  const v = Math.floor(i / base.length);
  const factor = [1, 0.4, 0, 2.5, 0.1, 1.5][(i * 7) % 6]!;
  const tienda = Math.round(minimo * 0.5 * factor);
  const almacen1 = Math.round(minimo * 3 * factor);
  const almacen2 = Math.round(minimo * 2 * factor);
  return {
    sku: `${prefix}-${String(i + 1).padStart(3, '0')}`,
    nombre: v === 0 ? nombre : `${nombre} (lote ${v + 1})`,
    categoria,
    unidad,
    tienda,
    almacen1,
    almacen2,
    reservado: Math.round((tienda + almacen1 + almacen2) * [0.2, 0.6, 0.95, 1][(i * 3) % 4]!),
    minimo,
  };
});
