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

function buscarVenta(id: string): Venta {
  const venta = VENTAS.find((v) => v.id === id);
  if (!venta) throw new Error('Venta no encontrada');
  return venta;
}

export const listarVentas = () => mockRequest(() => [...VENTAS]);

export const obtenerVenta = (id: string) => mockRequest(() => buscarVenta(id));

/** Resultado de completar: la venta ya actualizada y la atención aplicada línea por línea. */
export interface ResultadoCompletar {
  venta: Venta;
  atencion: LineaAtencion[];
}

/**
 * CU-10, atención parcial. Atiende lo posible con el stock libre; la venta pasa a `ListaDespacho` solo si
 * ya no tiene pendientes, y en otro caso conserva su estado.
 */
export const completarVenta = (id: string) =>
  mockRequest<ResultadoCompletar>(() => {
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

/** Cancela una venta activa y libera las unidades que tenía atendidas. */
export const cancelarVenta = (id: string) =>
  mockRequest(() => {
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
