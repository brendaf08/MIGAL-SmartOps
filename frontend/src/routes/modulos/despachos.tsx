import { DespachoPage } from '@/features/despacho/DespachoPage';
import { DespachosPage } from '@/features/despacho/DespachosPage';
import type { RutaModulo } from './tipos';

/** Rutas del módulo `despachos`; la rama dueña del módulo agrega aquí sus pantallas. */
export const RUTAS: RutaModulo[] = [
  { path: '', element: <DespachosPage /> },
  { path: ':id', element: <DespachoPage /> },
];
