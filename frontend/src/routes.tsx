// ARCHIVO DE INTEGRACIÓN: ya importa los 5 módulos. No se modifica.
import type { RutaModulo } from './shared/tipos';
import { rutas as usuarios } from './modules/usuarios';
import { rutas as productos } from './modules/productos';
import { rutas as inventario } from './modules/inventario';
import { rutas as ventas } from './modules/ventas';
import { rutas as cotizaciones } from './modules/cotizaciones';

export const todasLasRutas: RutaModulo[] = [
  ...usuarios, ...productos, ...inventario, ...ventas, ...cotizaciones,
];
