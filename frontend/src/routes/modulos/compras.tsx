import { ComprasPage } from '@/features/abastecimiento/compras/ComprasPage';
import { OrdenCompraDetallePage } from '@/features/abastecimiento/compras/OrdenCompraDetallePage';
import { OrdenCompraFormPage } from '@/features/abastecimiento/compras/OrdenCompraFormPage';
import type { RutaModulo } from './tipos';

/** Rutas del módulo `compras`; la rama dueña del módulo agrega aquí sus pantallas. */
export const RUTAS: RutaModulo[] = [
  { path: '', element: <ComprasPage /> },
  { path: 'nueva', element: <OrdenCompraFormPage />, roles: ['Reponedor'] },
  { path: ':id', element: <OrdenCompraDetallePage /> },
];
