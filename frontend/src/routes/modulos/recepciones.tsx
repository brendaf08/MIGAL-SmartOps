import { OrdenRecepcionPage } from '@/features/recepcion/OrdenRecepcionPage';
import { RecepcionesPage } from '@/features/recepcion/RecepcionesPage';
import type { RutaModulo } from './tipos';

/** Rutas del módulo `recepciones`; la rama dueña del módulo agrega aquí sus pantallas. */
export const RUTAS: RutaModulo[] = [
  { path: '', element: <RecepcionesPage /> },
  { path: 'nueva', element: <OrdenRecepcionPage /> },
];
