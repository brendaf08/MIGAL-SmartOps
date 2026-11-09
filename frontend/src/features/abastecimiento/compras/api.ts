import { mockRequest } from '@/api/mock';
import { PRODUCTOS } from '@/features/inventario/fixtures';
import type { ProductoDisponibilidad } from '@/features/inventario/types';
import { siguienteCodigo } from '@/lib/codigos';
import { ORDENES_COMPRA, PROVEEDORES } from '../fixtures';
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

/** Órdenes de compra para el listado. */
export const listarOrdenesCompra = () => mockRequest(() => ORDENES_COMPRA.map(filaDe));

/** Proveedores activos e inactivos para el formulario. */
export const listarProveedoresCompra = () => mockRequest(() => [...PROVEEDORES]);

/** Orden y su proveedor. Rechaza con `ORDEN_NO_ENCONTRADA` si el identificador no existe. */
export const obtenerOrdenCompra = (id: string) =>
  mockRequest<DetalleOrdenCompra>(() => {
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
export const crearOrdenCompra = (datos: NuevaOrdenCompra) =>
  mockRequest<OrdenCompra>(() => {
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
