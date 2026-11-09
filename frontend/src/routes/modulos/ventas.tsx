import { VentaDetallePage } from '@/features/comercial/ventas/VentaDetallePage';
import { VentasPage } from '@/features/comercial/ventas/VentasPage';
import type { RutaModulo } from './tipos';

/** Rutas del módulo `ventas`: el listado y el detalle con atención parcial. El registro lo agrega `ventas.registro`. */
export const RUTAS: RutaModulo[] = [
  { path: '', element: <VentasPage /> },
  { path: ':id', element: <VentaDetallePage /> },
];
