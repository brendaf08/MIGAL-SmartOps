// Módulo usuarios · Gabriel · P01–P07 (P04 es un modal dentro de P02, sin ruta)
// Reemplaza cada <EnConstruccion /> por tu página de ./pages/. Mantén codigo y path.
import type { RutaModulo } from '../../shared/tipos';
import EnConstruccion from '../../shared/components/EnConstruccion';
import P01Login from './pages/P01Login';

export const rutas: RutaModulo[] = [
  { codigo: 'P01', path: '/login', titulo: 'Inicio de sesión', elemento: <P01Login />, enMenu: false },
  { codigo: 'P02', path: '/usuarios', titulo: 'Usuarios', elemento: <EnConstruccion codigo="P02" titulo="Usuarios" />, enMenu: true },
  { codigo: 'P03', path: '/usuarios/nuevo', titulo: 'Nuevo usuario', elemento: <EnConstruccion codigo="P03" titulo="Nuevo usuario" />, enMenu: false },
  { codigo: 'P05', path: '/agente', titulo: 'Agente de IA', elemento: <EnConstruccion codigo="P05" titulo="Agente de IA" />, enMenu: true },
  { codigo: 'P06', path: '/proveedores', titulo: 'Proveedores', elemento: <EnConstruccion codigo="P06" titulo="Proveedores" />, enMenu: true },
  { codigo: 'P07', path: '/proveedores/nuevo', titulo: 'Nuevo proveedor', elemento: <EnConstruccion codigo="P07" titulo="Nuevo proveedor" />, enMenu: false },
];
