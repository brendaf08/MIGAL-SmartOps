import { api, type OpcionesPeticion } from './cliente';

/** Página de un listado de la API: el sobre de paginación común del contrato (esquema Pagina). */
export interface PaginaApi<T> {
  items: T[];
  total: number;
  page: number;
  pageSize: number;
}

/** Tamaño máximo de página que admite la API. */
export const TAMANO_PAGINA_MAXIMO = 100;

/**
 * Recorre todas las páginas de un listado paginado, en base 1, y devuelve sus elementos en el orden recibido. Se
 * detiene al alcanzar el total informado o cuando una página llega vacía.
 */
export async function recorrerPaginas<T>(
  ruta: string,
  consulta: NonNullable<OpcionesPeticion['consulta']> = {},
): Promise<T[]> {
  const elementos: T[] = [];
  for (let page = 1; ; page += 1) {
    const pagina = await api.get<PaginaApi<T>>(ruta, {
      ...consulta,
      page,
      pageSize: TAMANO_PAGINA_MAXIMO,
    });
    elementos.push(...pagina.items);
    if (pagina.items.length === 0 || elementos.length >= pagina.total) return elementos;
  }
}
