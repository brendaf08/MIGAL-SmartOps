import { mockRequest } from '@/api/mock';

export interface Proveedor {
  id: string;
  nombre: string;
  ruc: string;
  contacto: string;
  estado: 'Activo' | 'Inactivo';
}

export interface OrdenCompra {
  id: string;
  proveedor: string;
  fecha: string;
  total: number;
  estado: 'Pendiente' | 'Parcial' | 'Completada';
}

/** Datos de prueba hasta contar con la API de abastecimiento. */
const PROVEEDORES: Proveedor[] = [
  {
    id: 'P-001',
    nombre: 'Distribuidora Andina S.A.C.',
    ruc: '20123456789',
    contacto: 'ventas@andina.example',
    estado: 'Activo',
  },
  {
    id: 'P-002',
    nombre: 'Aceros del Sur S.A.',
    ruc: '20234567890',
    contacto: 'pedidos@acerossur.example',
    estado: 'Activo',
  },
  {
    id: 'P-003',
    nombre: 'Cementos Arequipa E.I.R.L.',
    ruc: '20345678901',
    contacto: 'comercial@cementosaq.example',
    estado: 'Inactivo',
  },
];

const ORDENES: OrdenCompra[] = [
  {
    id: 'OC-0087',
    proveedor: 'Distribuidora Andina S.A.C.',
    fecha: '2026-11-03',
    total: 8450,
    estado: 'Pendiente',
  },
  {
    id: 'OC-0086',
    proveedor: 'Aceros del Sur S.A.',
    fecha: '2026-10-30',
    total: 15200.5,
    estado: 'Parcial',
  },
  {
    id: 'OC-0084',
    proveedor: 'Distribuidora Andina S.A.C.',
    fecha: '2026-10-25',
    total: 4300,
    estado: 'Completada',
  },
];

export const listarProveedores = () => mockRequest(() => [...PROVEEDORES]);
export const listarOrdenesCompra = () => mockRequest(() => [...ORDENES]);
