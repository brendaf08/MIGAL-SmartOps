import { API_REAL, api } from '@/api/cliente';
import { mockRequest } from '@/api/mock';
import { siguienteCodigo } from '@/lib/codigos';
import { resumen } from '@/features/inventario/calculos';
import { PRODUCTOS } from '@/features/inventario/fixtures';
import { COTIZACIONES } from '../../fixtures';
import type { Cotizacion } from '../../types';
import {
  paginas,
  productoIdDeSku,
  ventaDesde,
  type VentaApi,
  type VentaResumenApi,
} from '../api';
import { VENTAS } from '../fixtures';
import type { LineaVenta, Venta } from '../tipos';
import { calcularAtencion, type ModalidadAtencion, type ResultadoAtencion } from './atencion';
import type { CambioStock } from '@/components/domain/Constancia/Constancia';

/** Stock libre por SKU del catálogo vigente; la validación del formulario se calcula con estas cifras. */
export const stockLibre = (): Record<string, number> =>
  Object.fromEntries(PRODUCTOS.map((p) => [p.sku, resumen(p).libre]));

/** Cotización aceptada en la API (CotizacionRegistrada), con los campos que usa el registro de venta. */
interface CotizacionApi {
  id: string;
  cliente: { id: string; nombreRazonSocial: string };
  fechaEmision: string;
  fechaVencimiento: string;
  condicionesComerciales: string | null;
  descuento: number;
  subtotal: number;
  lineas: { productoId: string; sku: string; nombreProducto: string; cantidad: number; precioUnitario: number }[];
}

/** Convierte una cotización validada de la API al tipo de la interfaz, sin detalle de condiciones. */
const cotizacionDeApi = (c: CotizacionApi): Cotizacion => ({
  id: c.id,
  estado: 'ValidadaMigal',
  emision: c.fechaEmision.slice(0, 10),
  vencimiento: c.fechaVencimiento,
  cliente: { id: c.cliente.id, nombre: c.cliente.nombreRazonSocial },
  descuento: c.subtotal > 0 ? c.descuento / c.subtotal : 0,
  lineas: c.lineas.map((l, i) => ({
    id: i + 1,
    sku: l.sku,
    nombre: l.nombreProducto,
    cantidad: l.cantidad,
    precio: l.precioUnitario,
  })),
});

/**
 * Cotizaciones validadas por MIGAL que aún no se han convertido en venta (flujo alternativo 2). La API solo
 * convierte cotizaciones validadas; la cotización aceptada por el cliente debe validarse antes.
 */
async function listarCotizacionesAceptadasReal(): Promise<Cotizacion[]> {
  const [validadas, ventas] = await Promise.all([
    paginas<CotizacionApi>('/cotizaciones', { estado: 'validada' }),
    paginas<VentaResumenApi>('/ventas'),
  ]);
  const convertidas = new Set(ventas.map((v) => v.cotizacionId));
  return validadas.filter((c) => !convertidas.has(c.id)).map(cotizacionDeApi);
}

/** Cotizaciones aceptadas que aún no se han convertido en venta (flujo alternativo 2). */
export const listarCotizacionesAceptadas = (): Promise<Cotizacion[]> =>
  API_REAL
    ? listarCotizacionesAceptadasReal()
    : mockRequest(() =>
        COTIZACIONES.filter(
          (c) => c.estado === 'AceptadaCliente' && !VENTAS.some((v) => v.cotizacionId === c.id),
        ),
      );

/** Busca una cotización aceptada no convertida; devuelve `undefined` si no se puede usar. Solo con datos simulados. */
export const cotizacionUtilizable = (id: string): Cotizacion | undefined =>
  API_REAL
    ? undefined
    : COTIZACIONES.find(
    (c) =>
      c.id === id && c.estado === 'AceptadaCliente' && !VENTAS.some((v) => v.cotizacionId === c.id),
  );

export interface EntradaVenta {
  clienteId?: string;
  cotizacionId?: string;
  descuento: number;
  lineas: { sku: string; nombre: string; cantidad: number; precio: number }[];
  modalidad: ModalidadAtencion | null;
  /** Clave de idempotencia de la intención de registro; se reutiliza en sus reintentos. */
  claveIdempotencia: string;
}

/** Resultado de confirmar: la venta, el reparto de la atención y la existencia libre antes y después. */
export interface ResultadoVenta {
  venta: Venta;
  atencion: ResultadoAtencion;
  cambios: CambioStock[];
}

/** Atención de la venta según la respuesta de la API. La modalidad la resuelve el servidor, no la interfaz. */
const atencionDe = (venta: Venta): ResultadoAtencion => {
  const lineas = venta.lineas.map((l: LineaVenta) => ({
    sku: l.sku,
    solicitada: l.solicitada,
    atendida: l.atendida,
    pendiente: l.solicitada - l.atendida,
  }));
  const totalPendiente = lineas.reduce((suma, l) => suma + l.pendiente, 0);
  return { lineas, totalPendiente, estado: venta.estado, entrega: venta.entrega };
};

/** Registra la venta a partir de la cotización validada (POST /ventas) o como venta directa (POST /ventas/directas). */
async function registrarVentaReal(entrada: EntradaVenta): Promise<ResultadoVenta> {
  let respuesta: VentaApi;
  if (entrada.cotizacionId) {
    respuesta = await api.post<VentaApi>(
      '/ventas',
      { idCotizacion: entrada.cotizacionId },
      entrada.claveIdempotencia,
    );
  } else {
    const lineas = await Promise.all(
      entrada.lineas.map(async (l) => ({
        productoId: await productoIdDeSku(l.sku),
        cantidad: l.cantidad,
        precioUnitario: l.precio,
      })),
    );
    respuesta = await api.post<VentaApi>(
      '/ventas/directas',
      { clienteId: entrada.clienteId ?? null, descuento: entrada.descuento, lineas },
      entrada.claveIdempotencia,
    );
  }
  const venta = ventaDesde(respuesta);
  return { venta, atencion: atencionDe(venta), cambios: [] };
}

/**
 * CU-10. Registra la venta en memoria con el siguiente código. La cantidad atendida se calcula con el stock
 * libre actual; la venta queda vinculada a la cotización de origen, que deja de estar disponible para otra venta.
 */
export const registrarVenta = (entrada: EntradaVenta): Promise<ResultadoVenta> =>
  API_REAL
    ? registrarVentaReal(entrada)
    : mockRequest<ResultadoVenta>(() => {
        const stock = stockLibre();
        const atencion = calcularAtencion(
          entrada.lineas.map((l) => ({ sku: l.sku, cantidad: l.cantidad })),
          stock,
          entrada.modalidad,
        );
        const lineas: LineaVenta[] = entrada.lineas.map((l, i) => ({
          sku: l.sku,
          nombre: l.nombre,
          solicitada: l.cantidad,
          atendida: atencion.lineas[i]!.atendida,
          precio: l.precio,
        }));
        const venta: Venta = {
          id: siguienteCodigo(
            'V',
            VENTAS.map((v) => v.id),
            4,
          ),
          fecha: new Date().toISOString().slice(0, 10),
          clienteId: entrada.clienteId,
          cotizacionId: entrada.cotizacionId,
          estado: atencion.estado,
          entrega: atencion.entrega,
          descuento: entrada.descuento,
          lineas,
        };
        VENTAS.unshift(venta);
        const cambios: CambioStock[] = lineas
          .filter((l) => l.atendida > 0)
          .map((l) => ({
            producto: l.nombre,
            ubicacion: 'Stock libre',
            antes: stock[l.sku] ?? 0,
            despues: (stock[l.sku] ?? 0) - l.atendida,
            unidad: PRODUCTOS.find((p) => p.sku === l.sku)?.unidad ?? '',
          }));
        return { venta, atencion, cambios };
      });
