import type { Venta } from './tipos';

/** Ventas de prueba, una o más por estado. V-0036 y V-0038 coinciden con las órdenes de despacho. Se modifican en memoria. */
export const VENTAS: Venta[] = [
  {
    id: 'V-0038',
    fecha: '2026-11-05',
    clienteId: 'CLI-001',
    cotizacionId: 'COT-0229',
    estado: 'ListaDespacho',
    entrega: 'Completa',
    descuento: 0,
    lineas: [
      {
        sku: 'CEM-001',
        nombre: 'Cemento Sol tipo I, bolsa 42.5 kg',
        solicitada: 200,
        atendida: 200,
        precio: 28.9,
      },
      {
        sku: 'FIE-003',
        nombre: 'Fierro corrugado 1/2 in x 9 m',
        solicitada: 60,
        atendida: 60,
        precio: 42.5,
      },
    ],
  },
  {
    id: 'V-0036',
    fecha: '2026-11-04',
    clienteId: 'CLI-002',
    estado: 'PendienteDespacho',
    entrega: 'Completa',
    descuento: 0,
    lineas: [
      {
        sku: 'TUB-006',
        nombre: 'Tubo PVC desagüe 4 in x 3 m',
        solicitada: 80,
        atendida: 50,
        precio: 16.9,
      },
    ],
  },
  {
    id: 'V-0040',
    fecha: '2026-11-06',
    clienteId: 'CLI-003',
    estado: 'PendienteAbastecimiento',
    entrega: 'Parcial',
    descuento: 20,
    lineas: [
      {
        sku: 'LAD-002',
        nombre: 'Ladrillo King Kong 18 huecos',
        solicitada: 2000,
        atendida: 1200,
        precio: 1.15,
      },
      {
        sku: 'CAB-007',
        nombre: 'Cable THW 12 AWG, rollo 100 m',
        solicitada: 10,
        atendida: 0,
        precio: 210,
      },
    ],
  },
  {
    id: 'V-0035',
    fecha: '2026-10-30',
    clienteId: 'CLI-004',
    estado: 'Despachada',
    entrega: 'Completa',
    descuento: 50,
    lineas: [
      {
        sku: 'PIN-005',
        nombre: 'Pintura látex blanco, balde 4 L',
        solicitada: 12,
        atendida: 12,
        precio: 112,
      },
    ],
  },
  {
    id: 'V-0034',
    fecha: '2026-10-29',
    estado: 'Despachada',
    entrega: 'Completa',
    descuento: 0,
    lineas: [
      {
        sku: 'CLA-008',
        nombre: 'Clavos de 3 in, bolsa 1 kg',
        solicitada: 5,
        atendida: 5,
        precio: 9.5,
      },
    ],
  },
  {
    id: 'V-0033',
    fecha: '2026-10-28',
    clienteId: 'CLI-005',
    estado: 'Cancelada',
    entrega: 'Completa',
    descuento: 0,
    lineas: [
      {
        sku: 'AGR-004',
        nombre: 'Arena gruesa, metro cúbico',
        solicitada: 4,
        atendida: 4,
        precio: 75,
      },
    ],
  },
  {
    id: 'V-0041',
    fecha: '2026-11-07',
    clienteId: 'CLI-006',
    estado: 'ListaDespacho',
    entrega: 'Completa',
    descuento: 0,
    lineas: [
      {
        sku: 'CEM-001',
        nombre: 'Cemento Sol tipo I, bolsa 42.5 kg',
        solicitada: 40,
        atendida: 40,
        precio: 28.9,
      },
    ],
  },
  {
    id: 'V-0042',
    fecha: '2026-11-07',
    clienteId: 'CLI-007',
    estado: 'PendienteAbastecimiento',
    entrega: 'Parcial',
    descuento: 0,
    lineas: [
      {
        sku: 'FIE-003',
        nombre: 'Fierro corrugado 1/2 in x 9 m',
        solicitada: 100,
        atendida: 0,
        precio: 42.5,
      },
    ],
  },
];
