import { API_REAL, api } from '@/api/cliente';
import { mockRequest } from '@/api/mock';
import type { CambioStock } from '@/components/domain/Constancia/Constancia';
import { paginas } from '@/features/comercial/ventas/api';
import { PRODUCTOS } from '@/features/inventario/fixtures';
import { ORDENES } from './fixtures';
import type { EstadoGuia, LineaDespacho, OrdenDespacho } from './types';

/** Ubicación de salida de una línea de la guía (GuiaUbicacionSalida). */
export interface UbicacionSalidaApi {
  ubicacionId: string;
  nombre: string;
  cantidad: number;
}

/** Línea de una guía de despacho (GuiaLinea). */
export interface GuiaLineaApi {
  productoId: string;
  sku: string;
  nombre: string;
  unidad: string;
  cantidad: number;
  ubicacionesSalida: UbicacionSalidaApi[];
}

/** Guía de despacho con sus líneas (GuiaDespacho). Los listados devuelven solo los campos del resumen. */
export interface GuiaApi {
  id: string;
  codigo: string;
  idVenta: string;
  codigoVenta: string;
  cliente: string;
  estado: 'preparacion' | 'emitida' | 'salida_confirmada' | 'entregada' | 'anulada';
  fechaEmision: string;
  fechaSalidaReal: string | null;
  fechaEntrega: string | null;
  usuarioResponsableId: string | null;
  direccionEntrega: string;
  lineas: GuiaLineaApi[];
}

/** Resultado de confirmar la salida (GuiaResultadoSalida): la guía, los movimientos y las existencias. */
interface GuiaResultadoSalidaApi {
  guia: GuiaApi;
  movimientos: unknown[];
  cambios: { productoId: string; sku: string; ubicacionId: string; unidad: string; antes: number; despues: number }[];
}

const ESTADO_DE_API: Record<Exclude<GuiaApi['estado'], 'anulada'>, EstadoGuia> = {
  preparacion: 'Preparacion',
  emitida: 'Emitida',
  salida_confirmada: 'SalidaConfirmada',
  entregada: 'Entregada',
};

/**
 * Convierte una guía de la API a la orden de la interfaz. La guía representa un despacho único y completo, por lo que
 * la entrega es completa y lo reservado de cada línea es la suma de sus ubicaciones de salida. Las guías anuladas no
 * tienen equivalente en la interfaz y deben filtrarse antes.
 */
export const ordenDesde = (g: GuiaApi): OrdenDespacho => {
  if (g.estado === 'anulada') throw new Error('La guía está anulada.');
  return {
    id: g.id,
    venta: g.codigoVenta,
    cliente: g.cliente,
    estado: ESTADO_DE_API[g.estado],
    entrega: 'Completa',
    lineas: g.lineas.map(
      (l): LineaDespacho => ({
        sku: l.sku,
        nombre: l.nombre,
        cantidad: l.cantidad,
        reservado: l.ubicacionesSalida.reduce((suma, u) => suma + u.cantidad, 0),
      }),
    ),
  };
};

const ruta = (id: string, sufijo = '') => `/guias-despacho/${encodeURIComponent(id)}${sufijo}`;

/** Lista las guías no anuladas con su detalle, porque la interfaz necesita las líneas para cada orden. */
async function listarOrdenesReal(): Promise<OrdenDespacho[]> {
  const resumenes = await paginas<{ id: string; estado: GuiaApi['estado'] }>('/guias-despacho');
  const detalles = await Promise.all(
    resumenes.filter((g) => g.estado !== 'anulada').map((g) => api.get<GuiaApi>(ruta(g.id))),
  );
  return detalles.map(ordenDesde);
}

export const listarOrdenes = (): Promise<OrdenDespacho[]> =>
  API_REAL ? listarOrdenesReal() : mockRequest(() => [...ORDENES]);

export const obtenerOrden = (id: string): Promise<OrdenDespacho> =>
  API_REAL
    ? api.get<GuiaApi>(ruta(id)).then(ordenDesde)
    : mockRequest(() => {
        const found = ORDENES.find((o) => o.id === id);
        if (!found) throw new Error('Orden no encontrada');
        return found;
      });

/** Una orden puede despacharse completa solo si cada línea tiene reservada toda su cantidad. */
export const reservaCompleta = (lineas: LineaDespacho[]) =>
  lineas.every((l) => l.reservado >= l.cantidad);

/** Orden con la salida confirmada y la existencia física total antes y después de cada línea. */
export interface ResultadoSalida {
  orden: OrdenDespacho;
  cambios: CambioStock[];
}

/** Efecto de la salida sobre la existencia física consolidada; la orden no indica la ubicación de origen. */
function cambiosDeSalida(lineas: LineaDespacho[]): CambioStock[] {
  return lineas.flatMap((l) => {
    const producto = PRODUCTOS.find((p) => p.sku === l.sku);
    if (!producto) return [];
    // Una reserva implica existencia física al menos igual; el inventario simulado no siempre lo cumple.
    const fisico = Math.max(producto.tienda + producto.almacen1 + producto.almacen2, l.reservado);
    return [
      {
        producto: l.nombre,
        ubicacion: 'Todas las ubicaciones',
        antes: fisico,
        despues: fisico - l.cantidad,
        unidad: producto.unidad,
      },
    ];
  });
}

/** Convierte el resultado de la API en la salida de la interfaz; los nombres salen de la guía. */
const resultadoDesde = (r: GuiaResultadoSalidaApi): ResultadoSalida => {
  const nombreDeSku = (sku: string) => r.guia.lineas.find((l) => l.sku === sku)?.nombre ?? sku;
  const nombreDeUbicacion = (ubicacionId: string) =>
    r.guia.lineas
      .flatMap((l) => l.ubicacionesSalida)
      .find((u) => u.ubicacionId === ubicacionId)?.nombre ?? ubicacionId;
  return {
    orden: ordenDesde(r.guia),
    cambios: r.cambios.map((c) => ({
      producto: nombreDeSku(c.sku),
      ubicacion: nombreDeUbicacion(c.ubicacionId),
      antes: c.antes,
      despues: c.despues,
      unidad: c.unidad,
    })),
  };
};

/**
 * CU-06. La salida de Kardex se registra al confirmar la salida física, no al aprobar la venta ni al imprimir la guía.
 * La clave de idempotencia identifica la intención del usuario y se reutiliza en sus reintentos.
 */
export const confirmarSalida = (id: string, claveIdempotencia: string): Promise<ResultadoSalida> =>
  API_REAL
    ? api
        .post<GuiaResultadoSalidaApi>(ruta(id, '/salida'), undefined, claveIdempotencia)
        .then(resultadoDesde)
    : mockRequest<ResultadoSalida>(() => {
        const found = ORDENES.find((o) => o.id === id);
        if (!found || found.estado !== 'Emitida')
          throw new Error('La orden no está lista para confirmar su salida.');
        if (!reservaCompleta(found.lineas))
          throw new Error('Hay cantidades sin reservar; la entrega no puede ser completa.');
        found.estado = 'SalidaConfirmada';
        return { orden: found, cambios: cambiosDeSalida(found.lineas) };
      });
