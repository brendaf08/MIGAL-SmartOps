import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

/** Carga el módulo con la API real activada. Debe llamarse tras fijar el entorno, porque API_REAL se evalúa al importar. */
async function cargarApiReal() {
  vi.stubEnv('VITE_API_REAL', 'true');
  vi.resetModules();
  return import('./api');
}

const respuesta = (cuerpo: unknown, estado = 200) =>
  new Response(JSON.stringify(cuerpo), {
    status: estado,
    headers: { 'Content-Type': 'application/json' },
  });

const ventaApi = {
  id: '502',
  fechaVenta: '2026-11-08T10:00:00-05:00',
  cotizacionId: '812',
  clienteId: '57',
  clienteNombre: 'Constructora Andina S.A.C.',
  usuarioResponsableId: '5',
  estado: 'pendiente_despacho',
  descuento: 25,
  subtotal: 1140,
  total: 1115,
  fechaCancelacion: null,
  motivoCancelacion: null,
  usuarioAutorizanteId: null,
  lineas: [
    {
      id: '903',
      productoId: '101',
      sku: 'CEM-049',
      nombreProducto: 'Cemento Sol tipo I 42.5 kg',
      cantidadVendida: 40,
      precioUnitario: 28.5,
      subtotal: 1140,
      cantidadReservadaActual: 40,
      cantidadPendienteAbastecimiento: 0,
    },
  ],
  reservas: [],
};

const fetchMock = vi.fn<(url: string, init?: RequestInit) => Promise<Response>>();

const llamada = (indice: number): [string, RequestInit | undefined] => {
  const parametros = fetchMock.mock.calls[indice];
  if (!parametros) throw new Error(`No hay llamada a fetch en la posición ${indice}.`);
  return [parametros[0], parametros[1]];
};
const cabeceras = (indice: number) => (llamada(indice)[1]?.headers ?? {}) as Record<string, string>;
const cuerpo = (indice: number) => JSON.parse(String(llamada(indice)[1]?.body));

const entrada = {
  clienteId: '57',
  cotizacionId: '812',
  descuento: 25,
  lineas: [{ sku: 'CEM-049', nombre: 'Cemento Sol tipo I 42.5 kg', cantidad: 40, precio: 28.5 }],
  modalidad: null,
  claveIdempotencia: 'clave-registro-1',
};

beforeEach(() => {
  fetchMock.mockReset();
  vi.stubGlobal('fetch', fetchMock);
  window.sessionStorage.setItem('migal-token', 'token-de-prueba');
});

afterEach(() => {
  vi.unstubAllGlobals();
  vi.unstubAllEnvs();
  window.sessionStorage.clear();
});

describe('registro de ventas (API real)', () => {
  it('registra la venta desde la cotización con POST /ventas y clave de idempotencia', async () => {
    const api = await cargarApiReal();
    fetchMock.mockResolvedValueOnce(respuesta(ventaApi, 201));

    const resultado = await api.registrarVenta(entrada);

    expect(llamada(0)[0]).toBe('/api/v1/ventas');
    expect(llamada(0)[1]?.method).toBe('POST');
    expect(cabeceras(0)['Idempotency-Key']).toBe('clave-registro-1');
    expect(cuerpo(0)).toEqual({ idCotizacion: '812' });
    expect(resultado.venta.estado).toBe('PendienteDespacho');
    expect(resultado.atencion).toEqual({
      lineas: [{ sku: 'CEM-049', solicitada: 40, atendida: 40, pendiente: 0 }],
      totalPendiente: 0,
      estado: 'PendienteDespacho',
      entrega: 'Completa',
    });
  });

  it('registra una venta directa resolviendo los productos por SKU', async () => {
    const api = await cargarApiReal();
    fetchMock
      .mockResolvedValueOnce(respuesta({ items: [{ id: '101', sku: 'CEM-049' }], total: 1, page: 1, pageSize: 1 }))
      .mockResolvedValueOnce(respuesta(ventaApi, 201));

    await api.registrarVenta({ ...entrada, cotizacionId: undefined, claveIdempotencia: 'clave-directa-1' });

    expect(llamada(1)[0]).toBe('/api/v1/ventas/directas');
    expect(cabeceras(1)['Idempotency-Key']).toBe('clave-directa-1');
    expect(cuerpo(1)).toEqual({
      clienteId: '57',
      descuento: 25,
      lineas: [{ productoId: '101', cantidad: 40, precioUnitario: 28.5 }],
    });
  });

  it('lista solo las cotizaciones validadas que no tienen venta', async () => {
    const api = await cargarApiReal();
    const cotizacion = (id: string) => ({
      id,
      cliente: { id: '57', nombreRazonSocial: 'Constructora Andina S.A.C.' },
      fechaEmision: '2026-11-02T15:00:00-05:00',
      fechaVencimiento: '2026-11-09',
      condicionesComerciales: null,
      descuento: 0,
      subtotal: 1140,
      lineas: [{ productoId: '101', sku: 'CEM-049', nombreProducto: 'Cemento', cantidad: 40, precioUnitario: 28.5 }],
    });
    fetchMock
      .mockResolvedValueOnce(respuesta({ items: [cotizacion('812'), cotizacion('813')], total: 2, page: 1, pageSize: 100 }))
      .mockResolvedValueOnce(respuesta({ items: [{ id: '502', cotizacionId: '812', clienteId: '57', estado: 'pendiente_despacho', total: 1115 }], total: 1, page: 1, pageSize: 100 }));

    const cotizaciones = await api.listarCotizacionesAceptadas();

    expect(llamada(0)[0]).toBe('/api/v1/cotizaciones?estado=validada&page=1&pageSize=100');
    expect(llamada(1)[0]).toBe('/api/v1/ventas?page=1&pageSize=100');
    expect(cotizaciones.map((c) => c.id)).toEqual(['813']);
    expect(cotizaciones[0]?.cliente).toEqual({ id: '57', nombre: 'Constructora Andina S.A.C.' });
  });
});
