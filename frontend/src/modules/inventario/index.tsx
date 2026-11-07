// Módulo inventario · Nuñez · P12–P16
// Reemplaza cada <EnConstruccion /> por tu página de ./pages/. Mantén codigo y path.
import type { RutaModulo } from '../../shared/tipos';
import EnConstruccion from '../../shared/components/EnConstruccion';

export const rutas: RutaModulo[] = [
  { codigo: 'P12', path: '/inventario/alertas', titulo: 'Alertas de stock', elemento: <EnConstruccion codigo="P12" titulo="Alertas de stock" />, enMenu: true },
  { codigo: 'P13', path: '/inventario/movimientos/nuevo', titulo: 'Registrar movimiento', elemento: <EnConstruccion codigo="P13" titulo="Registrar movimiento" />, enMenu: true },
  { codigo: 'P14', path: '/inventario/kardex', titulo: 'Kardex', elemento: <EnConstruccion codigo="P14" titulo="Kardex" />, enMenu: true },
  { codigo: 'P15', path: '/despachos', titulo: 'Despachos', elemento: <EnConstruccion codigo="P15" titulo="Despachos" />, enMenu: true },
  { codigo: 'P16', path: '/despachos/:id/imprimir', titulo: 'Guía de despacho', elemento: <EnConstruccion codigo="P16" titulo="Guía de despacho" />, enMenu: false },
];
