import type { OrdenCompra, Proveedor } from './tipos';

/** Proveedores de prueba. Se modifican en memoria. */
export const PROVEEDORES: Proveedor[] = [
  {
    id: 'P-001',
    ruc: '20123456789',
    razonSocial: 'Distribuidora Andina S.A.C.',
    contacto: 'Ana Villanueva',
    telefono: '954123456',
    correo: 'ventas@andina.example',
    condiciones: 'Pago a 30 días',
    skus: ['CEM-001', 'CLA-008', 'PIN-005'],
    estado: 'Activo',
  },
  {
    id: 'P-002',
    ruc: '20234567890',
    razonSocial: 'Aceros del Sur S.A.',
    contacto: 'Raúl Medina',
    telefono: '959876543',
    correo: 'pedidos@acerossur.example',
    condiciones: 'Pago a 15 días',
    skus: ['FIE-003'],
    estado: 'Activo',
  },
  {
    id: 'P-003',
    ruc: '20345678901',
    razonSocial: 'Cementos Arequipa E.I.R.L.',
    contacto: 'Carla Núñez',
    telefono: '951112233',
    correo: 'comercial@cementosaq.example',
    condiciones: 'Pago al contado',
    skus: ['CEM-001'],
    estado: 'Inactivo',
  },
  {
    id: 'P-004',
    ruc: '20456789012',
    razonSocial: 'Ladrillera Sur S.A.C.',
    contacto: 'Hugo Salas',
    telefono: '954998877',
    correo: 'ventas@ladrilleras.example',
    condiciones: 'Pago a 30 días',
    skus: ['LAD-002'],
    estado: 'Activo',
  },
  {
    id: 'P-005',
    ruc: '20567890123',
    razonSocial: 'Gasfitería Industrial E.I.R.L.',
    contacto: 'Lidia Paz',
    telefono: '956554433',
    correo: 'lpaz@gasfiteria.example',
    condiciones: 'Pago a 15 días',
    skus: ['TUB-006'],
    estado: 'Activo',
  },
  {
    id: 'P-006',
    ruc: '10678901234',
    razonSocial: 'Electro Andes',
    contacto: 'Iván Cuba',
    telefono: '957001122',
    correo: 'icuba@electroandes.example',
    condiciones: 'Pago al contado',
    skus: ['CAB-007'],
    estado: 'Activo',
  },
];

/** Órdenes de compra de prueba. OC-0084, OC-0086 y OC-0087 coinciden con las recepciones. Se modifican en memoria. */
export const ORDENES_COMPRA: OrdenCompra[] = [
  {
    id: 'OC-0087',
    proveedorId: 'P-001',
    fecha: '2026-11-03',
    estado: 'Pendiente',
    lineas: [
      {
        sku: 'CEM-001',
        nombre: 'Cemento Sol tipo I, bolsa 42.5 kg',
        cantidad: 100,
        recibido: 0,
        precio: 26.5,
      },
      {
        sku: 'CLA-008',
        nombre: 'Clavos de 3 in, bolsa 1 kg',
        cantidad: 25,
        recibido: 0,
        precio: 7.2,
      },
      {
        sku: 'PIN-005',
        nombre: 'Pintura látex blanco, balde 4 L',
        cantidad: 60,
        recibido: 0,
        precio: 98,
      },
    ],
  },
  {
    id: 'OC-0086',
    proveedorId: 'P-002',
    fecha: '2026-10-30',
    estado: 'Parcial',
    lineas: [
      {
        sku: 'FIE-003',
        nombre: 'Fierro corrugado 1/2 in x 9 m',
        cantidad: 300,
        recibido: 180,
        precio: 38.4,
      },
    ],
  },
  {
    id: 'OC-0084',
    proveedorId: 'P-001',
    fecha: '2026-10-25',
    estado: 'Completada',
    lineas: [
      {
        sku: 'CEM-001',
        nombre: 'Cemento Sol tipo I, bolsa 42.5 kg',
        cantidad: 150,
        recibido: 150,
        precio: 26.5,
      },
    ],
  },
  {
    id: 'OC-0088',
    proveedorId: 'P-004',
    fecha: '2026-11-05',
    estado: 'Pendiente',
    lineas: [
      {
        sku: 'LAD-002',
        nombre: 'Ladrillo King Kong 18 huecos',
        cantidad: 4000,
        recibido: 0,
        precio: 0.95,
      },
    ],
  },
  {
    id: 'OC-0089',
    proveedorId: 'P-005',
    fecha: '2026-11-06',
    estado: 'Parcial',
    lineas: [
      {
        sku: 'TUB-006',
        nombre: 'Tubo PVC desagüe 4 in x 3 m',
        cantidad: 120,
        recibido: 70,
        precio: 14.8,
      },
    ],
  },
  {
    id: 'OC-0090',
    proveedorId: 'P-006',
    fecha: '2026-11-07',
    estado: 'Pendiente',
    lineas: [
      {
        sku: 'CAB-007',
        nombre: 'Cable THW 12 AWG, rollo 100 m',
        cantidad: 20,
        recibido: 0,
        precio: 185,
      },
    ],
  },
];
