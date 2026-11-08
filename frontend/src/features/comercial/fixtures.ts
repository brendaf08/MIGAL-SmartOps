import type { Cliente, Cotizacion } from './types';

const andes: Cliente = {
  nombre: 'Constructora Los Andes S.A.C.',
  ruc: '20456789012',
  direccion: 'Av. Los Constructores 120, Arequipa',
};

/** Cotizaciones de prueba hasta contar con la API comercial. Se modifican en memoria. */
export const COTIZACIONES: Cotizacion[] = [
  {
    id: 'COT-0231',
    estado: 'Emitida',
    emision: '2026-11-04',
    vencimiento: '2026-11-18',
    cliente: andes,
    condiciones: 'Entrega completa en obra. Pago al contado.',
    descuento: 0.03,
    lineas: [
      {
        id: 1,
        sku: 'CEM-001',
        nombre: 'Cemento Sol tipo I, bolsa 42.5 kg',
        cantidad: 200,
        precio: 28.9,
      },
      {
        id: 2,
        sku: 'FIE-003',
        nombre: 'Fierro corrugado 1/2 in x 9 m',
        cantidad: 60,
        precio: 42.5,
      },
      {
        id: 3,
        sku: 'LAD-002',
        nombre: 'Ladrillo King Kong 18 huecos',
        cantidad: 1500,
        precio: 1.15,
      },
    ],
  },
  {
    id: 'COT-0230',
    estado: 'AceptadaCliente',
    emision: '2026-11-03',
    vencimiento: '2026-11-17',
    cliente: {
      nombre: 'Ferrocentro Quispe E.I.R.L.',
      ruc: '20512345678',
      direccion: 'Jr. Comercio 45, Cusco',
    },
    descuento: 0,
    lineas: [
      { id: 1, sku: 'TUB-006', nombre: 'Tubo PVC desagüe 4 in x 3 m', cantidad: 80, precio: 18.5 },
    ],
  },
  {
    id: 'COT-0229',
    estado: 'Vencida',
    emision: '2026-10-12',
    vencimiento: '2026-10-26',
    descuento: 0,
    lineas: [
      {
        id: 1,
        sku: 'PIN-005',
        nombre: 'Pintura látex blanco, balde 4 L',
        cantidad: 12,
        precio: 64.9,
      },
    ],
  },
];
