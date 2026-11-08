import { mockRequest } from '@/api/mock';
import { PRODUCTOS } from '@/features/inventario/fixtures';

/** CU-03. Búsqueda por nombre o SKU; el servidor real resuelve además los términos coloquiales con Fuzzy Matching. */
export const buscarCatalogo = (q: string) =>
  mockRequest(() => {
    const term = q.trim().toLowerCase();
    return PRODUCTOS.filter((p) => !term || `${p.sku} ${p.nombre}`.toLowerCase().includes(term));
  });
