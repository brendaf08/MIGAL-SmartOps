import type { OrdenDespacho } from './types';

/** Órdenes de despacho de prueba hasta contar con la API. Se modifican en memoria. */
export const ORDENES: OrdenDespacho[] = [
  {
    id: 'D-0031',
    venta: 'V-0038',
    cliente: 'Constructora Los Andes S.A.C.',
    estado: 'Emitida',
    entrega: 'Completa',
    lineas: [
      {
        sku: 'CEM-001',
        nombre: 'Cemento Sol tipo I, bolsa 42.5 kg',
        cantidad: 200,
        reservado: 200,
      },
      { sku: 'FIE-003', nombre: 'Fierro corrugado 1/2 in x 9 m', cantidad: 60, reservado: 60 },
    ],
  },
  {
    id: 'D-0030',
    venta: 'V-0036',
    cliente: 'Ferrocentro Quispe E.I.R.L.',
    estado: 'Emitida',
    entrega: 'Completa',
    lineas: [
      { sku: 'TUB-006', nombre: 'Tubo PVC desagüe 4 in x 3 m', cantidad: 80, reservado: 50 },
    ],
  },
  {
    id: 'D-0029',
    venta: 'V-0035',
    cliente: 'Obras Sur S.A.',
    estado: 'Entregada',
    entrega: 'Completa',
    lineas: [
      { sku: 'LAD-002', nombre: 'Ladrillo King Kong 18 huecos', cantidad: 500, reservado: 500 },
    ],
  },
];
