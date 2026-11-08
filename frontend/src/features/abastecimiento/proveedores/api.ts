import { mockRequest } from '@/api/mock';
import { siguienteCodigo } from '@/lib/codigos';
import { PROVEEDORES } from '../fixtures';
import type { Proveedor } from '../tipos';

/** Datos que se registran o modifican; el identificador y el estado los asigna el servicio. */
export type DatosProveedor = Omit<Proveedor, 'id' | 'estado'>;

export interface FiltroProveedores {
  /** Razón social o RUC, sin distinguir mayúsculas. */
  q?: string;
  estado?: Proveedor['estado'];
}

/** Error de RUC duplicado (`Error('RUC_DUPLICADO')`) con el identificador del proveedor que ya lo usa. */
export class RucDuplicadoError extends Error {
  readonly proveedorId: string;

  constructor(proveedorId: string) {
    super('RUC_DUPLICADO');
    this.name = 'RucDuplicadoError';
    this.proveedorId = proveedorId;
  }
}

/** Proveedores de prueba. Se modifican en memoria. */
export const listarProveedores = (filtro: FiltroProveedores = {}) =>
  mockRequest(() => {
    const term = (filtro.q ?? '').trim().toLowerCase();
    return PROVEEDORES.filter(
      (p) =>
        (!filtro.estado || p.estado === filtro.estado) &&
        (!term || `${p.razonSocial} ${p.ruc}`.toLowerCase().includes(term)),
    ).map((p) => ({ ...p, skus: [...p.skus] }));
  });

export const obtenerProveedor = (id: string) =>
  mockRequest(() => {
    const proveedor = PROVEEDORES.find((p) => p.id === id);
    if (!proveedor) throw new Error('NO_ENCONTRADO');
    return { ...proveedor, skus: [...proveedor.skus] };
  });

/** CU-11. Falla con `RucDuplicadoError` si el RUC ya está registrado. */
export const registrarProveedor = (datos: DatosProveedor) =>
  mockRequest(() => {
    const existente = PROVEEDORES.find((p) => p.ruc === datos.ruc);
    if (existente) throw new RucDuplicadoError(existente.id);
    const nuevo: Proveedor = {
      ...datos,
      skus: [...datos.skus],
      id: siguienteCodigo('P', PROVEEDORES.map((p) => p.id), 3),
      estado: 'Activo',
    };
    PROVEEDORES.push(nuevo);
    return { ...nuevo };
  });

export const actualizarProveedor = (id: string, datos: DatosProveedor) =>
  mockRequest(() => {
    const proveedor = PROVEEDORES.find((p) => p.id === id);
    if (!proveedor) throw new Error('NO_ENCONTRADO');
    const duplicado = PROVEEDORES.find((p) => p.ruc === datos.ruc && p.id !== id);
    if (duplicado) throw new RucDuplicadoError(duplicado.id);
    Object.assign(proveedor, { ...datos, skus: [...datos.skus] });
    return { ...proveedor, skus: [...proveedor.skus] };
  });

export const cambiarEstadoProveedor = (id: string, estado: Proveedor['estado']) =>
  mockRequest(() => {
    const proveedor = PROVEEDORES.find((p) => p.id === id);
    if (!proveedor) throw new Error('NO_ENCONTRADO');
    proveedor.estado = estado;
    return { ...proveedor, skus: [...proveedor.skus] };
  });
