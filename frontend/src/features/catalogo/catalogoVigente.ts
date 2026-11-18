import { useQuery } from '@tanstack/react-query';
import { API_REAL } from '@/api/cliente';
import { recorrerPaginas } from '@/api/paginacion';
import { aDisponibilidad, type ProductoExistenciasDto } from '@/features/inventario/adaptadores';
import { PRODUCTOS } from '@/features/inventario/fixtures';
import type { ProductoDisponibilidad } from '@/features/inventario/types';

/** Clave de la consulta que sincroniza el catálogo vigente con la API. */
export const CLAVE_CATALOGO_VIGENTE = ['catalogo-vigente'] as const;

/*
 * Con la API real, `PRODUCTOS` deja de ser el catálogo de prueba y pasa a ser la copia local del catálogo vigente:
 * los selectores de producto (cotización, venta, orden de compra, proveedor, revisión del OCR) y las búsquedas de
 * nombre, unidad y stock libre por SKU lo leen. Se vacía al cargar el módulo para no mostrar productos de prueba
 * mientras llega la primera respuesta.
 */
if (API_REAL) PRODUCTOS.splice(0, PRODUCTOS.length);

/**
 * Consulta los productos vigentes con sus existencias (GET /inventario/disponibilidad) y devuelve una lista nueva. La
 * copia en `PRODUCTOS` se mantiene para las búsquedas por SKU que se hacen fuera de un componente.
 */
export async function sincronizarCatalogoVigente(): Promise<ProductoDisponibilidad[]> {
  const respuesta = await recorrerPaginas<ProductoExistenciasDto>('/inventario/disponibilidad', {
    q: '',
    soloAlerta: false,
  });
  const productos = respuesta.map(aDisponibilidad);
  PRODUCTOS.splice(0, PRODUCTOS.length, ...productos);
  return productos;
}

/**
 * Catálogo vigente con sus existencias. Cada pantalla que elige productos lo usa: al montarse vuelve a consultar el
 * catálogo (las existencias cambian con cada venta o recepción) y se vuelve a pintar cuando cambia cualquier dato, no
 * solo la cantidad de productos. Con los datos simulados devuelve el catálogo de prueba sin consultar nada.
 */
export function useCatalogoVigente(activo = true) {
  const consulta = useQuery({
    queryKey: CLAVE_CATALOGO_VIGENTE,
    queryFn: sincronizarCatalogoVigente,
    enabled: API_REAL && activo,
    refetchOnMount: 'always',
  });
  return {
    cargando: API_REAL && activo && consulta.isPending,
    error: consulta.isError,
    productos: consulta.data ?? PRODUCTOS,
  };
}
