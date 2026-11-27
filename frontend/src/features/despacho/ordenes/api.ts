import { API_REAL, api } from '@/api/cliente';
import { mockRequest } from '@/api/mock';
import { CLIENTES } from '@/features/comercial/clientes/fixtures';
import { obtenerVenta, paginas } from '@/features/comercial/ventas/api';
import { VENTAS } from '@/features/comercial/ventas/fixtures';
import type { Venta } from '@/features/comercial/ventas/tipos';
import { PRODUCTOS } from '@/features/inventario/fixtures';
import { siguienteCodigo } from '@/lib/codigos';
import { ORDENES } from '../fixtures';
import { obtenerOrden, ordenDesde, type GuiaApi } from '../api';
import type { OrdenDespacho, TipoEntrega } from '../types';

export type { TipoEntrega };

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
  /** retiro_tienda para un cliente de mostrador; envio para los demás. */
  tipoEntregaSugerido: TipoEntrega;
  orden?: OrdenDespacho;
}

const nombreCliente = (clienteId?: string) =>
  CLIENTES.find((c) => c.id === clienteId)?.nombre ?? 'Sin cliente registrado';

export const ordenDeVenta = (ventaId: string) => ORDENES.find((o) => o.venta === ventaId);

/** Venta pendiente de despacho en el listado de la API (DespachoVentaPendiente). */
interface VentaPendienteApi {
  idVenta: string;
  cliente: string;
  direccionEntrega: string;
  tipoEntregaSugerido?: TipoEntrega;
}

/**
 * Ventas con todas sus cantidades reservadas y sin guía emitida. La API no las lista con sus líneas, por lo que se
 * consulta el detalle de cada una. Una venta sin guía no tiene orden todavía.
 */
async function ventasParaOrdenReal(): Promise<VentaParaOrden[]> {
  const pendientes = await paginas<VentaPendienteApi>('/ventas-pendientes-despacho');
  return Promise.all(
    pendientes.map(async (p) => ({
      venta: await obtenerVenta(p.idVenta),
      cliente: p.cliente,
      direccion: p.direccionEntrega,
      tipoEntregaSugerido: p.tipoEntregaSugerido ?? 'envio',
    })),
  );
}

/** Ventas en estado `ListaDespacho`, con o sin orden previa. */
export const ventasParaOrden = (): Promise<VentaParaOrden[]> =>
  API_REAL
    ? ventasParaOrdenReal()
    : mockRequest<VentaParaOrden[]>(() =>
        VENTAS.filter((v) => v.estado === 'ListaDespacho').map((venta) => ({
          venta,
          cliente: nombreCliente(venta.clienteId),
          direccion: DIRECCIONES_ENTREGA[venta.clienteId ?? ''] ?? DIRECCION_NO_REGISTRADA,
          tipoEntregaSugerido: 'envio',
          orden: ordenDeVenta(venta.id),
        })),
      );

/** Ubicación desde la que puede salir un producto: lo disponible y lo que la línea ya tiene reservado allí. */
export interface OpcionUbicacion {
  ubicacionId: string;
  nombre: string;
  disponible: number;
  asignada: number;
}

/** Producto de la venta con las ubicaciones posibles de salida (GuiaOpcionProducto). */
export interface OpcionProducto {
  productoId: string;
  sku: string;
  nombre: string;
  unidad: string;
  cantidad: number;
  ubicaciones: OpcionUbicacion[];
}

/** Datos que se eligen al generar la orden: cómo se entrega y de dónde sale cada producto. */
export interface DatosEmision {
  tipoEntrega: TipoEntrega;
  /** Dirección del envío; en un retiro en tienda no se envía. */
  direccion?: string;
  salidas: { productoId: string; ubicaciones: { ubicacionId: string; cantidad: number }[] }[];
}

/** De qué ubicaciones puede salir cada producto de una venta lista para despachar. */
export const opcionesSalida = (ventaId: string): Promise<OpcionProducto[]> =>
  API_REAL
    ? api
        .get<{ productos: OpcionProducto[] }>(
          `/ventas-pendientes-despacho/${encodeURIComponent(ventaId)}/salidas`,
        )
        .then((r) => r.productos)
    : mockRequest<OpcionProducto[]>(() => {
        const venta = VENTAS.find((v) => v.id === ventaId);
        if (!venta) throw new Error('Venta no encontrada');
        return venta.lineas.map((l) => {
          const p = PRODUCTOS.find((x) => x.sku === l.sku);
          return {
            productoId: l.sku,
            sku: l.sku,
            nombre: l.nombre,
            unidad: p?.unidad ?? 'unidad',
            cantidad: l.atendida,
            ubicaciones: [
              {
                ubicacionId: 'tienda',
                nombre: 'Tienda principal',
                disponible: Math.max(p?.tienda ?? 0, l.atendida),
                asignada: l.atendida,
              },
              {
                ubicacionId: 'almacen1',
                nombre: 'Almacén 1',
                disponible: p?.almacen1 ?? 0,
                asignada: 0,
              },
            ],
          };
        });
      });

/**
 * CU-06, pasos 2 a 6. Crea la orden en estado `Emitida` a partir de una venta lista; la venta conserva su estado.
 * Se elige la forma de entrega y las ubicaciones de salida de cada producto (las reservas se reasignan a ellas).
 */
export const generarOrden = (ventaId: string, datos?: DatosEmision): Promise<OrdenDespacho> =>
  API_REAL
    ? api
        .post<GuiaApi>('/guias-despacho', {
          idVenta: ventaId,
          ...(datos
            ? {
                tipoEntrega: datos.tipoEntrega,
                ...(datos.direccion ? { direccionEntrega: datos.direccion } : {}),
                salidas: datos.salidas,
              }
            : {}),
        })
        .then(ordenDesde)
    : mockRequest<OrdenDespacho>(() => {
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
          tipoEntrega: datos?.tipoEntrega ?? 'envio',
          direccion: datos?.tipoEntrega === 'retiro_tienda' ? 'Retiro en tienda' : datos?.direccion,
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

/**
 * Reimpresión de una orden existente. Exige un motivo no vacío (flujo alternativo 1 de CU-06). La API no devuelve la
 * guía en la respuesta de la reimpresión, por lo que se consulta después.
 */
export const reimprimirOrden = (ordenId: string, motivo: string): Promise<OrdenDespacho> =>
  API_REAL
    ? (async () => {
        const texto = motivo.trim();
        if (!texto) throw new Error('Indicar el motivo de la reimpresión.');
        await api.post(`/guias-despacho/${encodeURIComponent(ordenId)}/reimpresiones`, {
          motivo: texto,
        });
        return obtenerOrden(ordenId);
      })()
    : mockRequest<OrdenDespacho>(() => {
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

export const obtenerDocumentoOrden = (id: string): Promise<DocumentoOrden> =>
  API_REAL
    ? api.get<GuiaApi>(`/guias-despacho/${encodeURIComponent(id)}/documento`).then((g) => ({
        orden: ordenDesde(g),
        // La guía registra la fecha de emisión; la fecha de la venta no llega con el documento.
        fecha: g.fechaEmision.slice(0, 10),
        direccion: g.direccionEntrega,
        lineas: g.lineas.map((l) => ({
          sku: l.sku,
          nombre: l.nombre,
          cantidad: l.cantidad,
          reservado: l.ubicacionesSalida.reduce((suma, u) => suma + u.cantidad, 0),
          unidad: l.unidad,
        })),
      }))
    : mockRequest<DocumentoOrden>(() => {
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
