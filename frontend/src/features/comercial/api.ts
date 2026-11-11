import { API_REAL, api } from '@/api/cliente';
import { mockRequest } from '@/api/mock';
import { PRODUCTOS } from '@/features/inventario/fixtures';
import { siguienteCodigo } from '@/lib/codigos';
import { CLIENTES } from './clientes/fixtures';
import { estadoVigente, hoyIso, sumarDias } from './calculos';
import { COTIZACIONES } from './fixtures';
import type { Cliente, Cotizacion, EstadoCotizacion, LineaCotizacion } from './types';

/** Copia de una cotización con el estado vigente a la fecha de consulta (CU-09, flujo alternativo 3). */
const conEstadoVigente = (c: Cotizacion): Cotizacion => ({
  ...c,
  estado: estadoVigente(c, hoyIso()),
});

/** Tamaño máximo de página admitido por la API (parámetro pageSize). */
const TAMANO_PAGINA = 100;

/** Forma de una cotización en la API (esquema CotizacionRegistrada de comercial.yaml). */
interface CotizacionApi {
  id: string;
  estado: 'emitida' | 'aceptada' | 'validada' | 'rechazada' | 'vencida';
  cliente: { id: string; nombreRazonSocial: string; estadoActivo: boolean };
  fechaEmision: string;
  fechaVencimiento: string;
  condicionesComerciales: string | null;
  descuento: number;
  lineas: {
    productoId: string;
    sku: string;
    nombreProducto: string;
    cantidad: number;
    precioUnitario: number;
    subtotal: number;
  }[];
  subtotal: number;
  total: number;
}

interface CotizacionPaginaApi {
  items: CotizacionApi[];
  total: number;
  page: number;
  pageSize: number;
}

const ESTADO_DE_API: Record<CotizacionApi['estado'], EstadoCotizacion> = {
  emitida: 'Emitida',
  aceptada: 'AceptadaCliente',
  validada: 'ValidadaMigal',
  rechazada: 'Rechazada',
  vencida: 'Vencida',
};

/** La forma de pago no tiene campo propio en la API: se incorpora al inicio de `condicionesComerciales`. */
const PREFIJO_FORMA_PAGO = /^Forma de pago: ([^.\n]+)\.\s*/;

/** Separa la forma de pago y las observaciones del texto de `condicionesComerciales`. */
const separarCondiciones = (texto: string | null): Pick<Cotizacion, 'formaPago' | 'condiciones'> => {
  if (!texto) return {};
  const coincidencia = PREFIJO_FORMA_PAGO.exec(texto);
  if (!coincidencia) return { condiciones: texto };
  const resto = texto.slice(coincidencia[0].length).trim();
  return { formaPago: coincidencia[1], condiciones: resto || undefined };
};

const unirCondiciones = (formaPago?: string, condiciones?: string): string | null =>
  [formaPago ? `Forma de pago: ${formaPago}.` : '', condiciones ?? ''].filter(Boolean).join(' ') ||
  null;

/** Redondea a céntimos, como exige el contrato para los importes. */
const aCentimos = (importe: number) => Math.round(importe * 100) / 100;

/**
 * Convierte una cotización de la API al tipo de la interfaz. El descuento de la API es un monto y la interfaz lo
 * maneja como fracción del subtotal. La fecha de emisión se toma del día indicado en la marca de tiempo.
 */
const cotizacionDesde = (c: CotizacionApi): Cotizacion => ({
  id: c.id,
  estado: ESTADO_DE_API[c.estado],
  emision: c.fechaEmision.slice(0, 10),
  vencimiento: c.fechaVencimiento,
  cliente: { id: c.cliente.id, nombre: c.cliente.nombreRazonSocial },
  ...separarCondiciones(c.condicionesComerciales),
  descuento: c.subtotal > 0 ? c.descuento / c.subtotal : 0,
  lineas: c.lineas.map(
    (l, i): LineaCotizacion => ({
      id: i + 1,
      sku: l.sku,
      nombre: l.nombreProducto,
      cantidad: l.cantidad,
      precio: l.precioUnitario,
    }),
  ),
});

/**
 * Resuelve el identificador técnico del producto a partir de su SKU, que es lo que captura la interfaz. Se consulta
 * el catálogo de productos (GET /productos con el filtro sku).
 */
async function productoIdDeSku(sku: string): Promise<string> {
  const pagina = await api.get<{ items: { id: string; sku: string }[] }>('/productos', {
    sku,
    pageSize: 1,
  });
  const producto = pagina.items.find((p) => p.sku === sku);
  if (!producto) throw new Error(`No se encontró el producto con SKU ${sku} en el catálogo.`);
  return producto.id;
}

/** Cuerpo de CotizacionDatos. El descuento de la interfaz es una fracción que se convierte en monto. */
async function cuerpoDesde(datos: DatosCotizacion) {
  const ids = await Promise.all(datos.lineas.map((l) => productoIdDeSku(l.sku)));
  const subtotal = datos.lineas.reduce((suma, l) => suma + l.cantidad * l.precio, 0);
  return {
    clienteId: datos.clienteId ?? null,
    lineas: datos.lineas.map((l, i) => ({
      productoId: ids[i],
      cantidad: l.cantidad,
      precioUnitario: l.precio,
    })),
    descuento: aCentimos(datos.descuento * subtotal),
    validezDias: datos.validezDias,
    condicionesComerciales: unirCondiciones(datos.formaPago, datos.condiciones),
  };
}

/** Recorre todas las páginas de GET /cotizaciones. La pantalla filtra por estado en el cliente. */
async function listarCotizacionesReal(): Promise<Cotizacion[]> {
  const cotizaciones: Cotizacion[] = [];
  let pagina = 1;
  for (;;) {
    const respuesta = await api.get<CotizacionPaginaApi>('/cotizaciones', {
      page: pagina,
      pageSize: TAMANO_PAGINA,
    });
    cotizaciones.push(...respuesta.items.map(cotizacionDesde));
    if (respuesta.items.length === 0 || cotizaciones.length >= respuesta.total) return cotizaciones;
    pagina += 1;
  }
}

const ruta = (id: string, sufijo = '') => `/cotizaciones/${encodeURIComponent(id)}${sufijo}`;

/** Cliente embebido en una cotización simulada a partir del identificador elegido en el formulario. */
const clienteSimulado = (clienteId?: string): Cliente | undefined => {
  const cliente = CLIENTES.find((c) => c.id === clienteId);
  return cliente ? { id: cliente.id, nombre: cliente.nombre, ruc: cliente.documento } : undefined;
};

export const listarCotizaciones = (): Promise<Cotizacion[]> =>
  API_REAL ? listarCotizacionesReal() : mockRequest(() => COTIZACIONES.map(conEstadoVigente));

export const obtenerCotizacion = (id: string): Promise<Cotizacion> =>
  API_REAL
    ? api.get<CotizacionApi>(ruta(id)).then(cotizacionDesde)
    : mockRequest(() => {
        const found = COTIZACIONES.find((c) => c.id === id);
        if (!found) throw new Error('Cotización no encontrada');
        return conEstadoVigente(found);
      });

/**
 * CU-09, flujo alternativo 1. El stock no se consulta ni se reserva hasta convertirla en venta (RF-COT-08).
 * La clave de idempotencia identifica la intención del usuario y se reutiliza en sus reintentos.
 */
export const registrarAceptacion = (id: string, claveIdempotencia: string): Promise<Cotizacion> =>
  API_REAL
    ? api
        .post<CotizacionApi>(ruta(id, '/aceptacion'), undefined, claveIdempotencia)
        .then(cotizacionDesde)
    : mockRequest<Cotizacion>(() => {
        const found = COTIZACIONES.find((c) => c.id === id);
        if (!found || found.estado !== 'Emitida')
          throw new Error('Solo se puede aceptar una cotización emitida.');
        found.estado = 'AceptadaCliente';
        return found;
      });

/**
 * CU-09, flujo alternativo 2. El rechazo no libera ni reserva existencias, porque la cotización nunca las reservó.
 * Con la API, el motivo es obligatorio: sin texto no se envía la solicitud.
 */
export const registrarRechazo = (
  id: string,
  motivo: string | undefined,
  claveIdempotencia: string,
): Promise<Cotizacion> =>
  API_REAL
    ? (async () => {
        const texto = motivo?.trim() ?? '';
        if (!texto) throw new Error('Indicar el motivo del rechazo.');
        return api
          .post<CotizacionApi>(
            ruta(id, '/rechazo'),
            { origen: 'cliente', motivo: texto },
            claveIdempotencia,
          )
          .then(cotizacionDesde);
      })()
    : mockRequest<Cotizacion>(() => {
        const found = COTIZACIONES.find((c) => c.id === id);
        if (!found || found.estado !== 'Emitida')
          throw new Error('Solo se puede rechazar una cotización emitida.');
        found.estado = 'Rechazada';
        return found;
      });

/**
 * Datos capturados en el formulario de cotización. El descuento es una fracción de 0 a 1 (ver `Cotizacion.descuento`).
 * Ausente `clienteId` cuando la cotización se emite sin cliente.
 */
export interface DatosCotizacion {
  clienteId?: string;
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
export const crearCotizacion = (datos: DatosCotizacion): Promise<Cotizacion> =>
  API_REAL
    ? cuerpoDesde(datos)
        .then((cuerpo) => api.post<CotizacionApi>('/cotizaciones', cuerpo))
        .then(cotizacionDesde)
    : mockRequest<Cotizacion>(() => {
        const hoy = hoyIso();
        const nueva: Cotizacion = {
          id: siguienteCodigo(
            'COT',
            COTIZACIONES.map((c) => c.id),
          ),
          estado: 'Emitida',
          emision: hoy,
          vencimiento: sumarDias(hoy, datos.validezDias),
          cliente: clienteSimulado(datos.clienteId),
          condiciones: datos.condiciones,
          formaPago: datos.formaPago,
          descuento: datos.descuento,
          lineas: lineasDesde(datos.lineas),
        };
        COTIZACIONES.unshift(nueva);
        return nueva;
      });

/** Modifica una cotización emitida. La fecha de emisión se conserva y el vencimiento se recalcula desde ella. */
export const actualizarCotizacion = (id: string, datos: DatosCotizacion): Promise<Cotizacion> =>
  API_REAL
    ? cuerpoDesde(datos)
        .then((cuerpo) => api.put<CotizacionApi>(ruta(id), cuerpo))
        .then(cotizacionDesde)
    : mockRequest<Cotizacion>(() => {
        const found = COTIZACIONES.find((c) => c.id === id);
        if (!found || found.estado !== 'Emitida')
          throw new Error('Solo se puede modificar una cotización emitida.');
        Object.assign(found, {
          vencimiento: sumarDias(found.emision, datos.validezDias),
          cliente: clienteSimulado(datos.clienteId),
          condiciones: datos.condiciones,
          formaPago: datos.formaPago,
          descuento: datos.descuento,
          lineas: lineasDesde(datos.lineas),
        });
        return found;
      });
