import { mockRequest } from '@/api/mock';
import { CLIENTES } from '@/features/comercial/clientes/fixtures';
import { VENTAS } from '@/features/comercial/ventas/fixtures';
import type { Venta } from '@/features/comercial/ventas/tipos';
import { PRODUCTOS } from '@/features/inventario/fixtures';
import { siguienteCodigo } from '@/lib/codigos';
import { ORDENES } from '../fixtures';
import type { OrdenDespacho } from '../types';

/** Dirección de entrega de prueba por cliente; el modelo de cliente aún no registra direcciones. */
const DIRECCIONES_ENTREGA: Record<string, string> = {
  'CLI-001': 'Av. Javier Prado Este 4200, San Borja, Lima',
  'CLI-002': 'Jr. Los Pinos 215, Huancayo, Junín',
  'CLI-006': 'Av. Argentina 1870, Callao',
};

const DIRECCION_NO_REGISTRADA = 'Dirección no registrada';

/** Reimpresiones de órdenes con el motivo indicado. Se modifican en memoria. */
export const REIMPRESIONES: { ordenId: string; motivo: string }[] = [];

/** Venta lista para despacho, con el cliente y la orden que ya tiene, si existe. */
export interface VentaParaOrden {
  venta: Venta;
  cliente: string;
  direccion: string;
  orden?: OrdenDespacho;
}

const nombreCliente = (clienteId?: string) =>
  CLIENTES.find((c) => c.id === clienteId)?.nombre ?? 'Sin cliente registrado';

export const ordenDeVenta = (ventaId: string) => ORDENES.find((o) => o.venta === ventaId);

/** Ventas en estado `ListaDespacho`, con o sin orden previa. */
export const ventasParaOrden = () =>
  mockRequest<VentaParaOrden[]>(() =>
    VENTAS.filter((v) => v.estado === 'ListaDespacho').map((venta) => ({
      venta,
      cliente: nombreCliente(venta.clienteId),
      direccion: DIRECCIONES_ENTREGA[venta.clienteId ?? ''] ?? DIRECCION_NO_REGISTRADA,
      orden: ordenDeVenta(venta.id),
    })),
  );

/**
 * CU-06, pasos 2 a 6. Crea la orden en estado `Emitida` a partir de una venta lista; la venta conserva su estado.
 * La cantidad despachable de cada línea es la atendida, que ya cuenta con reserva.
 */
export const generarOrden = (ventaId: string) =>
  mockRequest<OrdenDespacho>(() => {
    const venta = VENTAS.find((v) => v.id === ventaId);
    if (!venta || venta.estado !== 'ListaDespacho')
      throw new Error('La venta no está lista para despacho.');
    if (ordenDeVenta(ventaId)) throw new Error('La venta ya tiene una orden de despacho.');
    const orden: OrdenDespacho = {
      id: siguienteCodigo(
        'D',
        ORDENES.map((o) => o.id),
      ),
      venta: venta.id,
      cliente: nombreCliente(venta.clienteId),
      estado: 'Emitida',
      entrega: venta.entrega,
      lineas: venta.lineas.map((l) => ({
        sku: l.sku,
        nombre: l.nombre,
        cantidad: l.atendida,
        reservado: l.atendida,
      })),
    };
    ORDENES.push(orden);
    return orden;
  });

/** Reimpresión de una orden existente. Exige un motivo no vacío (flujo alternativo 1 de CU-06). */
export const reimprimirOrden = (ordenId: string, motivo: string) =>
  mockRequest<OrdenDespacho>(() => {
    const orden = ORDENES.find((o) => o.id === ordenId);
    if (!orden) throw new Error('Orden no encontrada');
    const texto = motivo.trim();
    if (!texto) throw new Error('Indicar el motivo de la reimpresión.');
    REIMPRESIONES.push({ ordenId, motivo: texto });
    return orden;
  });

/** Datos del documento imprimible: la fecha es la de la venta, porque la orden no registra fecha propia. */
export interface DocumentoOrden {
  orden: OrdenDespacho;
  fecha: string;
  direccion: string;
  lineas: (OrdenDespacho['lineas'][number] & { unidad: string })[];
}

export const obtenerDocumentoOrden = (id: string) =>
  mockRequest<DocumentoOrden>(() => {
    const orden = ORDENES.find((o) => o.id === id);
    if (!orden) throw new Error('Orden no encontrada');
    const venta = VENTAS.find((v) => v.id === orden.venta);
    return {
      orden,
      fecha: venta?.fecha ?? '',
      direccion: DIRECCIONES_ENTREGA[venta?.clienteId ?? ''] ?? DIRECCION_NO_REGISTRADA,
      lineas: orden.lineas.map((l) => ({
        ...l,
        unidad: PRODUCTOS.find((p) => p.sku === l.sku)?.unidad ?? 'unidad',
      })),
    };
  });
