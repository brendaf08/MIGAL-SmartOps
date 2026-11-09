import { CotizacionPage } from '@/features/comercial/CotizacionPage';
import { CotizacionesPage } from '@/features/comercial/CotizacionesPage';
import { CotizacionFormPage } from '@/features/comercial/cotizaciones/CotizacionFormPage';
import type { RutaModulo } from './tipos';

/** Rutas del módulo `cotizaciones`; la rama dueña del módulo agrega aquí sus pantallas. */
export const RUTAS: RutaModulo[] = [
  { path: '', element: <CotizacionesPage /> },
  { path: 'nueva', element: <CotizacionFormPage /> },
  { path: ':id', element: <CotizacionPage /> },
  { path: ':id/editar', element: <CotizacionFormPage /> },
];
