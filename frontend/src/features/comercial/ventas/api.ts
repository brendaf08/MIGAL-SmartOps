import { API_REAL, api } from '@/api/cliente';
import { mockRequest } from '@/api/mock';
import { resumen } from '@/features/inventario/calculos';
import { PRODUCTOS } from '@/features/inventario/fixtures';
import { VENTAS } from './fixtures';
import type { EstadoVenta, LineaVenta, Venta } from './tipos';

/** Estados con cantidades por atender o por despachar; las ventas despachadas y canceladas no son activas. */
export const ESTADOS_ACTIVOS: EstadoVenta[] = [
  'PendienteAbastecimiento',
  'PendienteDespacho',
  'ListaDespacho',
];

/** Estados en los que la atención de la venta puede completarse. */
export const ESTADOS_COMPLETABLES: EstadoVenta[] = ['PendienteAbastecimiento', 'PendienteDespacho'];

/**
 * Variación de unidades reservadas por SKU desde que se carga el módulo. Completar suma las unidades atendidas;
 * cancelar las resta. Se guarda aquí porque los datos de inventario son de otro módulo y no se modifican.
 */
const ajusteReservado = new Map<string, number>();

/** Existencia libre y reservada de un SKU, con los ajustes de esta sesión. */
export function existenciaDe(sku: string): { libre: number; reservado: number } {
  const producto = PRODUCTOS.find((p) => p.sku === sku);
  if (!producto) return { libre: 0, reservado: 0 };
  const ajuste = ajusteReservado.get(sku) ?? 0;
  return { libre: resumen(producto).libre - ajuste, reservado: producto.reservado + ajuste };
}

/** Unidades que quedan pendientes en una línea (solicitada menos atendida). */
export const pendienteDe = (linea: LineaVenta) => linea.solicitada - linea.atendida;

/** Unidades pendientes de la venta, en todas sus líneas. */
export const unidadesPendientes = (venta: Venta) =>
  venta.lineas.reduce((suma, l) => suma + pendienteDe(l), 0);

/** Una venta tiene pendientes solo mientras está activa; una cancelada no atiende nada más. */
export const tienePendientes = (venta: Venta) =>
  ESTADOS_ACTIVOS.includes(venta.estado) && unidadesPendientes(venta) > 0;

export interface LineaAtencion {
  sku: string;
  nombre: string;
  pendiente: number;
  libre: number;
  /** Unidades que se atenderían con el stock libre actual: nunca más que lo pendiente ni que el libre. */
  atendible: number;
}

/** Atención posible de cada línea con pendientes, según el stock libre actual. */
export function planAtencion(venta: Venta): LineaAtencion[] {
  return venta.lineas
    .filter((l) => pendienteDe(l) > 0)
    .map((l) => {
      const { libre } = existenciaDe(l.sku);
      return {
        sku: l.sku,
        nombre: l.nombre,
        pendiente: pendienteDe(l),
        libre,
        atendible: Math.max(0, Math.min(pendienteDe(l), libre)),
      };
    });
}

/** Subtotal por lo solicitado, descuento global y total, redondeados a céntimos. */
export function totalesVenta(venta: Venta) {
  const subtotal =
    Math.round(venta.lineas.reduce((s, l) => s + l.solicitada * l.precio, 0) * 100) / 100;
  const total = Math.round((subtotal - venta.descuento) * 100) / 100;
  return { subtotal, descuento: venta.descuento, total };
}

/* Adaptadores de la API comercial (ventas.yaml). */

/** Venta con sus líneas, su cobertura y sus reservas (VentaDetalle). */
export interface VentaApi {
  id: string;
  fechaVenta: string;
  cotizacionId: string;
  clienteId: string;
  clienteNombre: string;
  estado: 'pendiente_abastecimiento' | 'pendiente_despacho' | 'lista_para_despacho' | 'despachada' | 'cancelada';
  descuento: number;
  subtotal: number;
  total: number;
  lineas: {
    id: string;
    productoId: string;
    sku: string;
    nombreProducto: string;
    cantidadVendida: number;
    precioUnitario: number;
    subtotal: number;
    cantidadReservadaActual: number;
    cantidadPendienteAbastecimiento: number;
  }[];
}

/** Venta en el listado (VentaResumen), sin líneas. */
export interface VentaResumenApi {
  id: string;
  cotizacionId: string;
  clienteId: string;
  estado: VentaApi['estado'];
  total: number;
}

const ESTADO_DE_API: Record<VentaApi['estado'], EstadoVenta> = {
  pendiente_abastecimiento: 'PendienteAbastecimiento',
  pendiente_despacho: 'PendienteDespacho',
  lista_para_despacho: 'ListaDespacho',
  despachada: 'Despachada',
  cancelada: 'Cancelada',
};

/**
 * Convierte una venta de la API al tipo de la interfaz. La cantidad atendida es la reservada actual; la entrega es
 * parcial mientras alguna línea tenga cantidad pendiente de abastecimiento.
 */
export function ventaDesde(v: VentaApi): Venta {
  return {
    id: v.id,
    fecha: v.fechaVenta.slice(0, 10),
    clienteId: v.clienteId,
    clienteNombre: v.clienteNombre,
    cotizacionId: v.cotizacionId,
    estado: ESTADO_DE_API[v.estado],
    entrega: v.lineas.some((l) => l.cantidadPendienteAbastecimiento > 0) ? 'Parcial' : 'Completa',
    descuento: v.descuento,
    lineas: v.lineas.map((l) => ({
      sku: l.sku,
      nombre: l.nombreProducto,
      solicitada: l.cantidadVendida,
      atendida: l.cantidadReservadaActual,
      precio: l.precioUnitario,
    })),
  };
}

/** Recorre todas las páginas de un listado paginado de la API. */
export async function paginas<T>(
  ruta: string,
  consulta: Record<string, string> = {},
): Promise<T[]> {
  const todas: T[] = [];
  let pagina = 1;
  for (;;) {
    const respuesta = await api.get<{ items: T[]; total: number }>(ruta, {
      ...consulta,
      page: pagina,
      pageSize: 100,
    });
    todas.push(...respuesta.items);
    if (respuesta.items.length === 0 || todas.length >= respuesta.total) return todas;
    pagina += 1;
  }
}

/** Resuelve el identificador técnico del producto a partir de su SKU (GET /productos con el filtro sku). */
export async function productoIdDeSku(sku: string): Promise<string> {
  const pagina = await api.get<{ items: { id: string; sku: string }[] }>('/productos', {
    sku,
    pageSize: 1,
  });
  const producto = pagina.items.find((p) => p.sku === sku);
  if (!producto) throw new Error(`No se encontró el producto con SKU ${sku} en el catálogo.`);
  return producto.id;
}

/** Consulta una venta con su detalle. */
const obtenerVentaApi = (id: string) =>
  api.get<VentaApi>(`/ventas/${encodeURIComponent(id)}`).then(ventaDesde);

async function listarVentasReal(): Promise<Venta[]> {
  const resumenes = await paginas<VentaResumenApi>('/ventas');
  return Promise.all(resumenes.map((r) => obtenerVentaApi(r.id)));
}

async function completarVentaReal(id: string, claveIdempotencia: string): Promise<ResultadoCompletar> {
  const antes = await obtenerVentaApi(id);
  const despues = ventaDesde(
    await api.post<VentaApi>(`/ventas/${encodeURIComponent(id)}/cobertura`, undefined, claveIdempotencia),
  );
  const atencion = antes.lineas
    .filter((l) => pendienteDe(l) > 0)
    .map((l): LineaAtencion => {
      const atendida = despues.lineas.find((x) => x.sku === l.sku)?.atendida ?? l.atendida;
      const atendible = atendida - l.atendida;
      return { sku: l.sku, nombre: l.nombre, pendiente: pendienteDe(l), libre: atendible, atendible };
    });
  return { venta: despues, atencion };
}

async function cancelarVentaReal(id: string, motivo: string, claveIdempotencia: string): Promise<Venta> {
  const texto = motivo.trim();
  if (!texto) throw new Error('Indicar el motivo de la cancelación.');
  return ventaDesde(
    await api.post<VentaApi>(
      `/ventas/${encodeURIComponent(id)}/cancelacion`,
      { motivo: texto },
      claveIdempotencia,
    ),
  );
}

function buscarVenta(id: string): Venta {
  const venta = VENTAS.find((v) => v.id === id);
  if (!venta) throw new Error('Venta no encontrada');
  return venta;
}

export const listarVentas = (): Promise<Venta[]> =>
  API_REAL ? listarVentasReal() : mockRequest(() => [...VENTAS]);

export const obtenerVenta = (id: string): Promise<Venta> =>
  API_REAL ? obtenerVentaApi(id) : mockRequest(() => buscarVenta(id));

/** Resultado de completar: la venta ya actualizada y la atención aplicada línea por línea. */
export interface ResultadoCompletar {
  venta: Venta;
  atencion: LineaAtencion[];
}

/**
 * CU-10, atención parcial. Atiende lo posible con el stock libre; la venta pasa a `ListaDespacho` solo si
 * ya no tiene pendientes, y en otro caso conserva su estado. La clave de idempotencia identifica la intención
 * del usuario y se reutiliza en sus reintentos.
 */
export const completarVenta = (id: string, claveIdempotencia: string): Promise<ResultadoCompletar> =>
  API_REAL
    ? completarVentaReal(id, claveIdempotencia)
    : mockRequest<ResultadoCompletar>(() => {
        const venta = buscarVenta(id);
        if (!ESTADOS_COMPLETABLES.includes(venta.estado))
          throw new Error('La venta no admite completar su atención.');
        const atencion = planAtencion(venta);
        if (atencion.length === 0) throw new Error('La venta no tiene cantidades pendientes.');
        if (atencion.every((a) => a.atendible === 0))
          throw new Error('Sin stock disponible para las unidades pendientes.');

        for (const a of atencion) {
          if (a.atendible === 0) continue;
          const linea = venta.lineas.find((l) => l.sku === a.sku);
          if (linea) linea.atendida += a.atendible;
          ajusteReservado.set(a.sku, (ajusteReservado.get(a.sku) ?? 0) + a.atendible);
        }
        if (venta.lineas.every((l) => pendienteDe(l) === 0)) venta.estado = 'ListaDespacho';
        return { venta, atencion };
      });

/** Cancela una venta activa y libera las unidades que tenía atendidas. El motivo es obligatorio en la API. */
export const cancelarVenta = (id: string, motivo: string, claveIdempotencia: string): Promise<Venta> =>
  API_REAL
    ? cancelarVentaReal(id, motivo, claveIdempotencia)
    : mockRequest(() => {
        const venta = buscarVenta(id);
        if (!ESTADOS_ACTIVOS.includes(venta.estado))
          throw new Error('La venta no se puede cancelar en su estado actual.');
        for (const l of venta.lineas) {
          if (l.atendida > 0)
            ajusteReservado.set(l.sku, (ajusteReservado.get(l.sku) ?? 0) - l.atendida);
        }
        venta.estado = 'Cancelada';
        return venta;
      });

/** Restablece los ajustes de reserva de esta sesión (uso en pruebas). */
export function reiniciarAjustes() {
  ajusteReservado.clear();
}
