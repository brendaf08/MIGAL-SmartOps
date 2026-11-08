import { mockRequest, type Page } from '@/api/mock';
import { PRODUCTOS } from '@/features/inventario/fixtures';
import type { ProductoDisponibilidad } from '@/features/inventario/types';

export type EstadoProducto = 'Activo' | 'Inactivo';

/** Producto del catálogo maestro: datos de registro, sin existencias. */
export interface ProductoCatalogo {
  sku: string;
  nombre: string;
  descripcion: string;
  categoria: string;
  unidad: string;
  minimo: number;
  estado: EstadoProducto;
}

/** Datos que el formulario envía al registrar o modificar un producto. */
export type DatosProducto = Omit<ProductoCatalogo, 'estado'> & { estado: EstadoProducto };

export interface ConsultaCatalogo {
  q: string;
  categoria: string;
  estado: EstadoProducto | '';
  page: number;
  pageSize: number;
}

/** Error de negocio: el SKU ya existe en el catálogo (flujo alternativo 1.1 de CU-03). */
export class SkuDuplicadoError extends Error {
  constructor() {
    super('El SKU ya existe. Ingresar un código distinto.');
    this.name = 'SkuDuplicadoError';
  }
}

/** Datos que el catálogo conserva fuera de `PRODUCTOS` (descripción y estado), por SKU. */
const DATOS_EXTRA = new Map<string, { descripcion: string; estado: EstadoProducto }>();

const extraDe = (sku: string) => DATOS_EXTRA.get(sku) ?? { descripcion: '', estado: 'Activo' as const };

const aCatalogo = (p: ProductoDisponibilidad): ProductoCatalogo => {
  const extra = extraDe(p.sku);
  return {
    sku: p.sku,
    nombre: p.nombre,
    descripcion: extra.descripcion,
    categoria: p.categoria,
    unidad: p.unidad,
    minimo: p.minimo,
    estado: extra.estado,
  };
};

/** CU-03. Búsqueda por nombre o SKU, filtros por categoría y estado, y paginación en memoria. */
export const buscarCatalogo = (consulta: ConsultaCatalogo) =>
  mockRequest<Page<ProductoCatalogo>>(() => {
    const term = consulta.q.trim().toLowerCase();
    const filtrados = PRODUCTOS.map(aCatalogo).filter(
      (p) =>
        (!term || `${p.sku} ${p.nombre}`.toLowerCase().includes(term)) &&
        (!consulta.categoria || p.categoria === consulta.categoria) &&
        (!consulta.estado || p.estado === consulta.estado),
    );
    const inicio = (consulta.page - 1) * consulta.pageSize;
    return {
      items: filtrados.slice(inicio, inicio + consulta.pageSize),
      total: filtrados.length,
    };
  });

/** Producto por SKU, o `null` si no existe. */
export const obtenerProducto = (sku: string) =>
  mockRequest<ProductoCatalogo | null>(() => {
    const producto = PRODUCTOS.find((p) => p.sku === sku);
    return producto ? aCatalogo(producto) : null;
  });

/** Categorías presentes en el catálogo, en orden alfabético. */
export const listarCategorias = () =>
  mockRequest(() => [...new Set(PRODUCTOS.map((p) => p.categoria))].sort((a, b) => a.localeCompare(b, 'es')));

/** Registra un producto nuevo. Sin existencias iniciales: las cantidades se registran por recepción. */
export const registrarProducto = (datos: DatosProducto) =>
  mockRequest(() => {
    if (PRODUCTOS.some((p) => p.sku === datos.sku)) throw new SkuDuplicadoError();
    PRODUCTOS.push({
      sku: datos.sku,
      nombre: datos.nombre,
      categoria: datos.categoria,
      unidad: datos.unidad,
      tienda: 0,
      almacen1: 0,
      almacen2: 0,
      reservado: 0,
      minimo: datos.minimo,
    });
    DATOS_EXTRA.set(datos.sku, { descripcion: datos.descripcion, estado: datos.estado });
    return aCatalogo(PRODUCTOS[PRODUCTOS.length - 1]!);
  });

/** Modifica un producto existente. El SKU no cambia. */
export const actualizarProducto = (sku: string, datos: DatosProducto) =>
  mockRequest(() => {
    const producto = PRODUCTOS.find((p) => p.sku === sku);
    if (!producto) throw new Error('El producto no existe.');
    Object.assign(producto, {
      nombre: datos.nombre,
      categoria: datos.categoria,
      unidad: datos.unidad,
      minimo: datos.minimo,
    });
    DATOS_EXTRA.set(sku, { descripcion: datos.descripcion, estado: datos.estado });
    return aCatalogo(producto);
  });
