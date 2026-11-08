import { mockRequest } from '@/api/mock';
import { PRODUCTOS } from '@/features/inventario/fixtures';
import { siguienteCodigo } from '@/lib/codigos';
import { estadoVigente, hoyIso, sumarDias } from './calculos';
import { COTIZACIONES } from './fixtures';
import type { Cliente, Cotizacion, LineaCotizacion } from './types';

/** Copia de una cotización con el estado vigente a la fecha de consulta (CU-09, flujo alternativo 3). */
const conEstadoVigente = (c: Cotizacion): Cotizacion => ({
  ...c,
  estado: estadoVigente(c, hoyIso()),
});

export const listarCotizaciones = () => mockRequest(() => COTIZACIONES.map(conEstadoVigente));

export const obtenerCotizacion = (id: string) =>
  mockRequest(() => {
    const found = COTIZACIONES.find((c) => c.id === id);
    if (!found) throw new Error('Cotización no encontrada');
    return conEstadoVigente(found);
  });

/** CU-09, flujo alternativo 1. El stock no se consulta ni se reserva hasta convertirla en venta (RF-COT-08). */
export const registrarAceptacion = (id: string) =>
  mockRequest<Cotizacion>(() => {
    const found = COTIZACIONES.find((c) => c.id === id);
    if (!found || found.estado !== 'Emitida')
      throw new Error('Solo se puede aceptar una cotización emitida.');
    found.estado = 'AceptadaCliente';
    return found;
  });

/** CU-09, flujo alternativo 2. El rechazo no libera ni reserva existencias, porque la cotización nunca las reservó. */
export const registrarRechazo = (id: string) =>
  mockRequest<Cotizacion>(() => {
    const found = COTIZACIONES.find((c) => c.id === id);
    if (!found || found.estado !== 'Emitida')
      throw new Error('Solo se puede rechazar una cotización emitida.');
    found.estado = 'Rechazada';
    return found;
  });

/** Datos capturados en el formulario de cotización. El descuento es una fracción de 0 a 1 (ver `Cotizacion.descuento`). */
export interface DatosCotizacion {
  /** Ausente cuando la cotización se emite sin cliente. */
  cliente?: Cliente;
  lineas: Pick<LineaCotizacion, 'sku' | 'cantidad' | 'precio'>[];
  descuento: number;
  validezDias: number;
  formaPago: string;
  condiciones?: string;
}

const lineasDesde = (lineas: DatosCotizacion['lineas']): LineaCotizacion[] =>
  lineas.map((l, i) => ({
    id: i + 1,
    sku: l.sku,
    nombre: PRODUCTOS.find((p) => p.sku === l.sku)?.nombre ?? l.sku,
    cantidad: l.cantidad,
    precio: l.precio,
  }));

/** CU-09. Emitir la cotización no modifica el stock; el vencimiento se calcula desde la fecha de emisión. */
export const crearCotizacion = (datos: DatosCotizacion) =>
  mockRequest<Cotizacion>(() => {
    const hoy = hoyIso();
    const nueva: Cotizacion = {
      id: siguienteCodigo(
        'COT',
        COTIZACIONES.map((c) => c.id),
      ),
      estado: 'Emitida',
      emision: hoy,
      vencimiento: sumarDias(hoy, datos.validezDias),
      cliente: datos.cliente,
      condiciones: datos.condiciones,
      formaPago: datos.formaPago,
      descuento: datos.descuento,
      lineas: lineasDesde(datos.lineas),
    };
    COTIZACIONES.unshift(nueva);
    return nueva;
  });

/** Modifica una cotización emitida. La fecha de emisión se conserva y el vencimiento se recalcula desde ella. */
export const actualizarCotizacion = (id: string, datos: DatosCotizacion) =>
  mockRequest<Cotizacion>(() => {
    const found = COTIZACIONES.find((c) => c.id === id);
    if (!found || found.estado !== 'Emitida')
      throw new Error('Solo se puede modificar una cotización emitida.');
    Object.assign(found, {
      vencimiento: sumarDias(found.emision, datos.validezDias),
      cliente: datos.cliente,
      condiciones: datos.condiciones,
      formaPago: datos.formaPago,
      descuento: datos.descuento,
      lineas: lineasDesde(datos.lineas),
    });
    return found;
  });
