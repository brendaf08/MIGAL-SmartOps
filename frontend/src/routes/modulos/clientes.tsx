import { ClientesPage } from '@/features/comercial/clientes/ClientesPage';
import type { RutaModulo } from './tipos';

/** Rutas del módulo `clientes`; la rama dueña del módulo agrega aquí sus pantallas. */
export const RUTAS: RutaModulo[] = [{ path: '', element: <ClientesPage /> }];
