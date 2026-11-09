import { ProveedoresPage } from '@/features/abastecimiento/ProveedoresPage';
import { ProveedorFormPage } from '@/features/abastecimiento/proveedores/ProveedorFormPage';
import type { RutaModulo } from './tipos';

/** Rutas del módulo `proveedores`: lista, registro (`nueva`) y ficha de edición (`:id`). */
export const RUTAS: RutaModulo[] = [
  { path: '', element: <ProveedoresPage /> },
  { path: 'nueva', element: <ProveedorFormPage /> },
  { path: ':id', element: <ProveedorFormPage /> },
];
