import { KardexPage } from '@/features/inventario/KardexPage';
import { MovimientoFormPage } from '@/features/inventario/movimientos/MovimientoFormPage';
import type { RutaModulo } from './tipos';

/** Rutas del módulo `kardex`: listado del Kardex (P14) y registro de movimiento (P13). */
export const RUTAS: RutaModulo[] = [
  { path: '', element: <KardexPage /> },
  { path: 'nuevo', element: <MovimientoFormPage /> },
];
