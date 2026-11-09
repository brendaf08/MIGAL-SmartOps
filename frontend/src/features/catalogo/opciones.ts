import type { OpcionProducto } from '@/components/ui/ProductoSelect/ProductoSelect';
import { PRODUCTOS } from '@/features/inventario/fixtures';

/** Productos del catálogo vigente para `ProductoSelect` y `LineasEditor`. Se consulta en cada render para ver las altas nuevas. */
export const opcionesProducto = (): OpcionProducto[] =>
  PRODUCTOS.map(({ sku, nombre }) => ({ sku, nombre }));
