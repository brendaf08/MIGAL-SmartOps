import type { ReactElement } from 'react';

/**
 * Ruta de un módulo, relativa a su clave de navegación: la cadena vacía es la pantalla principal (`/ventas`) y
 * `nueva`, `:id` o `:id/editar` son pantallas secundarias (`/ventas/nueva`).
 */
export interface RutaModulo {
  path: string;
  element: ReactElement;
  /** Si se omite, rigen los roles del módulo definidos en `NAV`. */
  roles?: string[];
}
