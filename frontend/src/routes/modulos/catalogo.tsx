import { CatalogoPage } from '@/features/catalogo/CatalogoPage';
import { ProductoFormPage } from '@/features/catalogo/ProductoFormPage';
import type { RutaModulo } from './tipos';

/** Rutas del módulo `catalogo`; la rama dueña del módulo agrega aquí sus pantallas. */
export const RUTAS: RutaModulo[] = [
  { path: '', element: <CatalogoPage /> },
  { path: 'nuevo', element: <ProductoFormPage /> },
  { path: ':sku/editar', element: <ProductoFormPage /> },
];
