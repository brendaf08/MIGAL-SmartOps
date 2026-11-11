import { API_REAL, ApiError, api } from '@/api/cliente';
import { mockRequest } from '@/api/mock';
import { PRODUCTOS } from '@/features/inventario/fixtures';
import type { ProductoDisponibilidad } from '@/features/inventario/types';
import { siguienteCodigo } from '@/lib/codigos';
import { ORDENES_COMPRA, PROVEEDORES } from '../fixtures';
import { listarProveedores, obtenerProveedor } from '../proveedores/api';
import type { LineaOrdenCompra, OrdenCompra, Proveedor } from '../tipos';

/** Fila del listado: la orden con su proveedor y el avance de recepción calculados. */
export interface FilaOrdenCompra {
  id: string;
  proveedor: string;
  fecha: string;
  estado: OrdenCompra['estado'];
  productos: number;
  pedidas: number;
  recibidas: number;
  total: number;
}

/** Orden con los datos del proveedor para el detalle. */
export interface DetalleOrdenCompra {
  orden: OrdenCompra;
  proveedor: Proveedor;
}

/** Línea de una orden nueva, tal como la entrega el formulario. */
export interface LineaNuevaOrden {
  sku: string;
  cantidad: number;
  precio: number;
}

export interface NuevaOrdenCompra {
  proveedorId: string;
  fecha: string;
  lineas: LineaNuevaOrden[];
}

/** Orden de compra tal como la devuelve la API (contrato abastecimiento.yaml, esquema OrdenCompra). */
interface OrdenCompraApi {
  id: string;
  codigo: string;
  proveedorId: string;
  fechaEmision: string;
  estado: 'pendiente' | 'parcial' | 'completada';
  lineas: {
    sku: string;
    nombre: string;
    cantidadComprada: number;
    costoUnitario: number;
    cantidadRecibida: number;
  }[];
  total: number;
}

interface PaginaApi<T> {
  items: T[];
  total: number;
  page: number;
  pageSize: number;
}

interface ProveedorNombreApi {
  id: string;
  razonSocial: string;
}

const TAMANO_PAGINA = 100;

const ESTADO_UI: Record<OrdenCompraApi['estado'], OrdenCompra['estado']> = {
  pendiente: 'Pendiente',
  parcial: 'Parcial',
  completada: 'Completada',
};

/** Importe de una línea: cantidad por precio unitario, redondeado a céntimos. */
export const subtotalLineaOrden = (l: Pick<LineaOrdenCompra, 'cantidad' | 'precio'>) =>
  Math.round(l.cantidad * l.precio * 100) / 100;

/** Total de la orden a partir de sus líneas. */
export const totalOrden = (lineas: readonly LineaOrdenCompra[]) =>
  Math.round(lineas.reduce((suma, l) => suma + subtotalLineaOrden(l), 0) * 100) / 100;

/** Nombre del proveedor de una orden, o su identificador si ya no existe en el directorio. */
const nombreProveedor = (proveedorId: string) =>
  PROVEEDORES.find((p) => p.id === proveedorId)?.razonSocial ?? proveedorId;

const filaDe = (o: OrdenCompra): FilaOrdenCompra => ({
  id: o.id,
  proveedor: nombreProveedor(o.proveedorId),
  fecha: o.fecha,
  estado: o.estado,
  productos: o.lineas.length,
  pedidas: o.lineas.reduce((s, l) => s + l.cantidad, 0),
  recibidas: o.lineas.reduce((s, l) => s + l.recibido, 0),
  total: totalOrden(o.lineas),
});

/*
 * Adaptadores entre el contrato y la interfaz. La interfaz identifica la orden por su código (OC-0087), que es
 * el valor visible y el que usan las rutas y los enlaces; el contrato usa el identificador técnico en las rutas
 * y expone el código en el campo codigo. Por ello el código se usa como identificador de la interfaz.
 */
const aOrden = (orden: OrdenCompraApi): OrdenCompra => ({
  id: orden.codigo,
  proveedorId: orden.proveedorId,
  fecha: orden.fechaEmision.slice(0, 10),
  estado: ESTADO_UI[orden.estado],
  lineas: orden.lineas.map((l) => ({
    sku: l.sku,
    nombre: l.nombre,
    cantidad: l.cantidadComprada,
    recibido: l.cantidadRecibida,
    precio: l.costoUnitario,
  })),
});

const aFila = (orden: OrdenCompraApi, nombres: Map<string, string>): FilaOrdenCompra => ({
  id: orden.codigo,
  proveedor: nombres.get(orden.proveedorId) ?? orden.proveedorId,
  fecha: orden.fechaEmision.slice(0, 10),
  estado: ESTADO_UI[orden.estado],
  productos: orden.lineas.length,
  pedidas: orden.lineas.reduce((s, l) => s + l.cantidadComprada, 0),
  recibidas: orden.lineas.reduce((s, l) => s + l.cantidadRecibida, 0),
  total: orden.total,
});

/** Recorre todas las páginas de un listado paginado de la API. */
async function recorrerPaginas<T>(ruta: string, consulta: Record<string, string>): Promise<T[]> {
  const elementos: T[] = [];
  for (let page = 1; ; page += 1) {
    const pagina = await api.get<PaginaApi<T>>(ruta, {
      ...consulta,
      page,
      pageSize: TAMANO_PAGINA,
    });
    elementos.push(...pagina.items);
    if (pagina.items.length === 0 || elementos.length >= pagina.total) return elementos;
  }
}

/** Busca la orden por su código. El parámetro q de la API localiza el código; la coincidencia exacta la confirma. */
async function buscarOrdenPorCodigo(codigo: string): Promise<OrdenCompraApi | undefined> {
  for (let page = 1; ; page += 1) {
    const pagina = await api.get<PaginaApi<OrdenCompraApi>>('/ordenes-compra', {
      q: codigo,
      page,
      pageSize: TAMANO_PAGINA,
    });
    const encontrada = pagina.items.find((o) => o.codigo === codigo);
    if (encontrada) return encontrada;
    if (pagina.items.length === 0 || page * TAMANO_PAGINA >= pagina.total) return undefined;
  }
}

const listarOrdenesReal = async (): Promise<FilaOrdenCompra[]> => {
  const [ordenes, proveedores] = await Promise.all([
    recorrerPaginas<OrdenCompraApi>('/ordenes-compra', {}),
    recorrerPaginas<ProveedorNombreApi>('/proveedores', {}),
  ]);
  const nombres = new Map(proveedores.map((p) => [p.id, p.razonSocial] as const));
  return ordenes.map((o) => aFila(o, nombres));
};

/** Proveedor del detalle; la API responde RECURSO_NO_ENCONTRADO si el proveedor ya no existe. */
const obtenerProveedorDetalle = (proveedorId: string): Promise<Proveedor> =>
  obtenerProveedor(proveedorId).catch((error: unknown) => {
    if (error instanceof Error && error.message === 'NO_ENCONTRADO') {
      throw new Error('PROVEEDOR_NO_ENCONTRADO');
    }
    throw error;
  });

const obtenerOrdenReal = async (codigo: string): Promise<DetalleOrdenCompra> => {
  const orden = await buscarOrdenPorCodigo(codigo);
  if (!orden) throw new Error('ORDEN_NO_ENCONTRADA');
  const proveedor = await obtenerProveedorDetalle(orden.proveedorId);
  return { orden: aOrden(orden), proveedor };
};

const crearOrdenReal = async (datos: NuevaOrdenCompra): Promise<OrdenCompra> => {
  try {
    // La fecha de emisión la asigna el servidor, por lo que no se envía el campo fecha del formulario.
    const creada = await api.post<OrdenCompraApi>('/ordenes-compra', {
      proveedorId: datos.proveedorId,
      lineas: datos.lineas.map((l) => ({
        sku: l.sku,
        cantidadComprada: l.cantidad,
        costoUnitario: l.precio,
      })),
    });
    return aOrden(creada);
  } catch (error) {
    if (error instanceof ApiError && error.codigo === 'PROVEEDOR_INACTIVO') {
      throw new Error('PROVEEDOR_INACTIVO');
    }
    throw error;
  }
};

/** Órdenes de compra para el listado. */
export const listarOrdenesCompra = (): Promise<FilaOrdenCompra[]> =>
  API_REAL ? listarOrdenesReal() : mockRequest(() => ORDENES_COMPRA.map(filaDe));

/** Proveedores activos e inactivos para el formulario. */
export const listarProveedoresCompra = (): Promise<Proveedor[]> =>
  API_REAL ? listarProveedores() : mockRequest(() => [...PROVEEDORES]);

/** Orden y su proveedor. Rechaza con `ORDEN_NO_ENCONTRADA` si el identificador no existe. */
export const obtenerOrdenCompra = (id: string): Promise<DetalleOrdenCompra> =>
  API_REAL
    ? obtenerOrdenReal(id)
    : mockRequest<DetalleOrdenCompra>(() => {
        const orden = ORDENES_COMPRA.find((o) => o.id === id);
        if (!orden) throw new Error('ORDEN_NO_ENCONTRADA');
        const proveedor = PROVEEDORES.find((p) => p.id === orden.proveedorId);
        if (!proveedor) throw new Error('PROVEEDOR_NO_ENCONTRADO');
        return { orden, proveedor };
      });

/**
 * Crea la orden en estado Pendiente. Rechaza con `PROVEEDOR_INACTIVO` si el proveedor no está activo; la
 * validación del cliente impide llegar a este caso.
 */
export const crearOrdenCompra = (datos: NuevaOrdenCompra): Promise<OrdenCompra> =>
  API_REAL
    ? crearOrdenReal(datos)
    : mockRequest<OrdenCompra>(() => {
        const proveedor = PROVEEDORES.find((p) => p.id === datos.proveedorId);
        if (!proveedor) throw new Error('PROVEEDOR_NO_ENCONTRADO');
        if (proveedor.estado === 'Inactivo') throw new Error('PROVEEDOR_INACTIVO');
        const orden: OrdenCompra = {
          id: siguienteCodigo(
            'OC',
            ORDENES_COMPRA.map((o) => o.id),
          ),
          proveedorId: datos.proveedorId,
          fecha: datos.fecha,
          estado: 'Pendiente',
          lineas: datos.lineas.map((l) => ({
            sku: l.sku,
            nombre: PRODUCTOS.find((p) => p.sku === l.sku)?.nombre ?? l.sku,
            cantidad: l.cantidad,
            recibido: 0,
            precio: l.precio,
          })),
        };
        ORDENES_COMPRA.unshift(orden);
        return orden;
      }, 400);

/** Último precio de compra registrado para un SKU; 0 si nunca se ha comprado. */
export const precioReferencia = (sku: string) =>
  ORDENES_COMPRA.find((o) => o.lineas.some((l) => l.sku === sku))?.lineas.find((l) => l.sku === sku)
    ?.precio ?? 0;

/** Existencias libres de un producto: tienda y almacenes menos lo reservado. */
const existenciaLibre = (p: ProductoDisponibilidad) =>
  p.tienda + p.almacen1 + p.almacen2 - p.reservado;

/** Cantidad que lleva el producto por encima de su stock mínimo (al menos una unidad). */
export const cantidadParaMinimo = (sku: string) => {
  const p = PRODUCTOS.find((x) => x.sku === sku);
  if (!p) return 1;
  return Math.max(1, p.minimo - existenciaLibre(p) + 1);
};
