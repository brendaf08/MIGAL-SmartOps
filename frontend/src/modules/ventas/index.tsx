// Módulo ventas · Pinedo · P19–P22
// Reemplaza cada <EnConstruccion /> por tu página de ./pages/. Mantén codigo y path.
import type { RutaModulo } from '../../shared/tipos';
import EnConstruccion from '../../shared/components/EnConstruccion';

export const rutas: RutaModulo[] = [
  { codigo: 'P19', path: '/ventas', titulo: 'Ventas', elemento: <EnConstruccion codigo="P19" titulo="Ventas" />, enMenu: true },
  { codigo: 'P20', path: '/ventas/nueva', titulo: 'Registrar venta', elemento: <EnConstruccion codigo="P20" titulo="Registrar venta" />, enMenu: true },
  { codigo: 'P21', path: '/ventas/:id/atencion', titulo: 'Atención de venta', elemento: <EnConstruccion codigo="P21" titulo="Atención de venta" />, enMenu: false },
  { codigo: 'P22', path: '/clientes', titulo: 'Clientes', elemento: <EnConstruccion codigo="P22" titulo="Clientes" />, enMenu: true },
];
