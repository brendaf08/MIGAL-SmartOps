// Módulo productos · Brenda · P08–P11, P25–P26
// Reemplaza cada <EnConstruccion /> por tu página de ./pages/. Mantén codigo y path.
import type { RutaModulo } from '../../shared/tipos';
import EnConstruccion from '../../shared/components/EnConstruccion';

export const rutas: RutaModulo[] = [
  { codigo: 'P08', path: '/productos', titulo: 'Productos', elemento: <EnConstruccion codigo="P08" titulo="Productos" />, enMenu: true },
  { codigo: 'P09', path: '/productos/nuevo', titulo: 'Nuevo producto', elemento: <EnConstruccion codigo="P09" titulo="Nuevo producto" />, enMenu: false },
  { codigo: 'P10', path: '/productos/:id', titulo: 'Ficha de producto', elemento: <EnConstruccion codigo="P10" titulo="Ficha de producto" />, enMenu: false },
  { codigo: 'P11', path: '/disponibilidad', titulo: 'Disponibilidad', elemento: <EnConstruccion codigo="P11" titulo="Disponibilidad" />, enMenu: true },
  { codigo: 'P25', path: '/compras', titulo: 'Órdenes de compra', elemento: <EnConstruccion codigo="P25" titulo="Órdenes de compra" />, enMenu: true },
  { codigo: 'P26', path: '/compras/nueva', titulo: 'Nueva orden de compra', elemento: <EnConstruccion codigo="P26" titulo="Nueva orden de compra" />, enMenu: false },
];
