import { CambiarClavePage } from '@/features/seguridad/CambiarClavePage';
import type { RutaModulo } from './tipos';

/** Rutas del módulo `cuenta`: cambio de la contraseña propia, para todos los roles. */
export const RUTAS: RutaModulo[] = [{ path: '', element: <CambiarClavePage /> }];
