import { KardexPage } from '@/features/inventario/KardexPage';
import type { RutaModulo } from './tipos';

/** Rutas del módulo `kardex`; la rama dueña del módulo agrega aquí sus pantallas. */
export const RUTAS: RutaModulo[] = [{ path: '', element: <KardexPage /> }];
