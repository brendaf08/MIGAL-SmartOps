/** Página de un listado del contrato, con el sobre de paginación común. */
export interface PaginaApi<T> {
  items: T[];
  total: number;
}

/** Tamaño de página máximo admitido por el contrato (`pageSize`). */
export const TAMANO_PAGINA_MAXIMO = 100;

/**
 * Recorre todas las páginas de un listado paginado (base 1) y devuelve sus elementos en el orden del servidor. Se usa
 * cuando la interfaz necesita el conjunto completo para calcular contadores o agrupar elementos.
 */
export async function todosLosElementos<T>(
  cargarPagina: (page: number, pageSize: number) => Promise<PaginaApi<T>>,
): Promise<T[]> {
  const elementos: T[] = [];
  for (let page = 1; ; page += 1) {
    const pagina = await cargarPagina(page, TAMANO_PAGINA_MAXIMO);
    elementos.push(...pagina.items);
    if (pagina.items.length === 0 || elementos.length >= pagina.total) return elementos;
  }
}
