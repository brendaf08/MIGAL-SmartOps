import { api } from './cliente';
import { recorrerPaginas } from './paginacion';

/*
 * Resolución del identificador técnico de un producto a partir de su SKU. Se consulta la disponibilidad (CU-04,
 * GET /inventario/disponibilidad), que admite a los cuatro roles; el catálogo maestro (CU-03, GET /productos) solo
 * admite al Almacenero y al Administrador, y el Vendedor recibía 403 al registrar ventas o validar documentos.
 */

interface ProductoDisponibleApi {
  productoId: string;
  sku: string;
}

/** Identificador técnico del producto vigente con el SKU indicado, o `null` si no existe. */
export async function buscarIdProducto(sku: string): Promise<string | null> {
  const pagina = await api.get<{ items: ProductoDisponibleApi[] }>('/inventario/disponibilidad', {
    sku,
    pageSize: 1,
  });
  return pagina.items.find((p) => p.sku === sku)?.productoId ?? null;
}

/** Identificador técnico del producto con el SKU indicado. Lanza un error si el producto no existe. */
export async function productoIdDeSku(sku: string): Promise<string> {
  const id = await buscarIdProducto(sku);
  if (!id) throw new Error(`No se encontró el producto con SKU ${sku} en el catálogo.`);
  return id;
}

/** Identificadores de todos los productos vigentes, por SKU. */
export async function idsDeProductosPorSku(): Promise<Map<string, string>> {
  const productos = await recorrerPaginas<ProductoDisponibleApi>('/inventario/disponibilidad', {
    q: '',
    soloAlerta: false,
  });
  return new Map(productos.map((p) => [p.sku, p.productoId] as const));
}
