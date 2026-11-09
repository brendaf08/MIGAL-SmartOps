import { VentaFormPage } from '@/features/comercial/ventas/registro/VentaFormPage';
import type { RutaModulo } from './tipos';

/** Rutas del módulo `ventas.registro`; la rama dueña del módulo agrega aquí sus pantallas. */
export const RUTAS: RutaModulo[] = [{ path: 'nueva', element: <VentaFormPage /> }];
