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

const problema = (estado: number, code: string) =>
  new Response(
    JSON.stringify({
      type: 'https://migal.example/errores/' + code.toLowerCase(),
      title: 'Error',
      status: estado,
      detail: 'Mensaje de prueba.',
      instance: '/api/v1/cotizaciones',
      code,
    }),
    { status: estado, headers: { 'Content-Type': 'application/problem+json' } },
  );

/** Cotización registrada en la API (CotizacionRegistrada), con sobrescritos opcionales. */
const cotizacionApi = (sobrescritos: Record<string, unknown> = {}) => ({
  id: '812',
  estado: 'emitida',
  cliente: { id: '57', nombreRazonSocial: 'Constructora Andina S.A.C.', estadoActivo: true },
  usuarioResponsableId: '7',
  fechaEmision: '2026-11-02T15:00:00-05:00',
  fechaVencimiento: '2026-11-09',
  condicionesComerciales: 'Forma de pago: Contado. Entrega en obra.',
  descuento: 25,
  lineas: [
    {
      productoId: '101',
      sku: 'CEM-049',
      nombreProducto: 'Cemento Sol tipo I 42.5 kg',
      cantidad: 40,
      precioUnitario: 28.5,
      subtotal: 1140,
    },
  ],
  subtotal: 1140,
  total: 1115,
  fechaAceptacionCliente: null,
  fechaValidacionMigal: null,
  rechazo: null,
  ...sobrescritos,
});

const fetchMock = vi.fn<(url: string, init?: RequestInit) => Promise<Response>>();

/** URL y opciones de la llamada indicada a fetch. */
const llamada = (indice: number): [string, RequestInit | undefined] => {
  const parametros = fetchMock.mock.calls[indice];
  if (!parametros) throw new Error(`No hay llamada a fetch en la posición ${indice}.`);
  return [parametros[0], parametros[1]];
};
const cabeceras = (indice: number) => (llamada(indice)[1]?.headers ?? {}) as Record<string, string>;
const cuerpo = (indice: number) => JSON.parse(String(llamada(indice)[1]?.body));

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

describe('cotizaciones (API real)', () => {
  it('lista todas las páginas y convierte estado, fechas, descuento y forma de pago', async () => {
    const api = await cargarApiReal();
    fetchMock.mockResolvedValueOnce(
      respuesta({
        items: [cotizacionApi(), cotizacionApi({ id: '813', estado: 'aceptada', descuento: 0, condicionesComerciales: null })],
        total: 2,
        page: 1,
        pageSize: 100,
      }),
    );

    const cotizaciones = await api.listarCotizaciones();

    expect(llamada(0)[0]).toBe('/api/v1/cotizaciones?page=1&pageSize=100');
    expect(cabeceras(0).Authorization).toBe('Bearer token-de-prueba');
    expect(cotizaciones).toHaveLength(2);
    expect(cotizaciones[0]).toEqual({
      id: '812',
      estado: 'Emitida',
      emision: '2026-11-02',
      vencimiento: '2026-11-09',
      cliente: { id: '57', nombre: 'Constructora Andina S.A.C.' },
      formaPago: 'Contado',
      condiciones: 'Entrega en obra.',
      descuento: 25 / 1140,
      lineas: [{ id: 1, sku: 'CEM-049', nombre: 'Cemento Sol tipo I 42.5 kg', cantidad: 40, precio: 28.5 }],
    });
    expect(cotizaciones[1]?.estado).toBe('AceptadaCliente');
    expect(cotizaciones[1]?.descuento).toBe(0);
    expect(cotizaciones[1]?.formaPago).toBeUndefined();
  });

  it('crea una cotización resolviendo el productoId por SKU y convirtiendo el descuento a monto', async () => {
    const api = await cargarApiReal();
    fetchMock
      .mockResolvedValueOnce(respuesta({ items: [{ id: '101', sku: 'CEM-049' }], total: 1, page: 1, pageSize: 1 }))
      .mockResolvedValueOnce(respuesta(cotizacionApi(), 201));

    const creada = await api.crearCotizacion({
      clienteId: '57',
      lineas: [{ sku: 'CEM-049', cantidad: 40, precio: 28.5 }],
      descuento: 25 / 1140,
      validezDias: 7,
      formaPago: 'Contado',
      condiciones: 'Entrega en obra.',
    });

    expect(llamada(0)[0]).toBe('/api/v1/productos?sku=CEM-049&pageSize=1');
    expect(llamada(1)[0]).toBe('/api/v1/cotizaciones');
    expect(llamada(1)[1]?.method).toBe('POST');
    expect(cuerpo(1)).toEqual({
      clienteId: '57',
      lineas: [{ productoId: '101', cantidad: 40, precioUnitario: 28.5 }],
      descuento: 25,
      validezDias: 7,
      condicionesComerciales: 'Forma de pago: Contado. Entrega en obra.',
    });
    expect(creada.id).toBe('812');
  });

  it('envía clienteId nulo cuando la cotización no tiene cliente', async () => {
    const api = await cargarApiReal();
    fetchMock
      .mockResolvedValueOnce(respuesta({ items: [{ id: '101', sku: 'CEM-049' }], total: 1, page: 1, pageSize: 1 }))
      .mockResolvedValueOnce(respuesta(cotizacionApi(), 201));

    await api.crearCotizacion({
      lineas: [{ sku: 'CEM-049', cantidad: 1, precio: 10 }],
      descuento: 0,
      validezDias: 7,
      formaPago: 'Contado',
    });

    expect(cuerpo(1).clienteId).toBeNull();
    expect(cuerpo(1).condicionesComerciales).toBe('Forma de pago: Contado.');
  });

  it('falla con un mensaje claro cuando un SKU no existe en el catálogo', async () => {
    const api = await cargarApiReal();
    fetchMock.mockResolvedValueOnce(respuesta({ items: [], total: 0, page: 1, pageSize: 1 }));

    await expect(
      api.crearCotizacion({
        clienteId: '57',
        lineas: [{ sku: 'XXX-000', cantidad: 1, precio: 10 }],
        descuento: 0,
        validezDias: 7,
        formaPago: 'Contado',
      }),
    ).rejects.toThrow('No se encontró el producto con SKU XXX-000 en el catálogo.');
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });

  it('actualiza una cotización con PUT', async () => {
    const api = await cargarApiReal();
    fetchMock
      .mockResolvedValueOnce(respuesta({ items: [{ id: '101', sku: 'CEM-049' }], total: 1, page: 1, pageSize: 1 }))
      .mockResolvedValueOnce(respuesta(cotizacionApi()));

    await api.actualizarCotizacion('812', {
      clienteId: '57',
      lineas: [{ sku: 'CEM-049', cantidad: 40, precio: 28.5 }],
      descuento: 0,
      validezDias: 7,
      formaPago: 'Contado',
    });

    expect(llamada(1)[0]).toBe('/api/v1/cotizaciones/812');
    expect(llamada(1)[1]?.method).toBe('PUT');
  });

  it('registra la aceptación con POST, cabecera Idempotency-Key y sin cuerpo', async () => {
    const api = await cargarApiReal();
    fetchMock.mockResolvedValueOnce(respuesta(cotizacionApi({ estado: 'aceptada' })));

    const aceptada = await api.registrarAceptacion('812', 'clave-aceptacion-1');

    expect(llamada(0)[0]).toBe('/api/v1/cotizaciones/812/aceptacion');
    expect(llamada(0)[1]?.method).toBe('POST');
    expect(cabeceras(0)['Idempotency-Key']).toBe('clave-aceptacion-1');
    expect(llamada(0)[1]?.body).toBeUndefined();
    expect(aceptada.estado).toBe('AceptadaCliente');
  });

  it('registra el rechazo con origen cliente, motivo y cabecera Idempotency-Key', async () => {
    const api = await cargarApiReal();
    fetchMock.mockResolvedValueOnce(respuesta(cotizacionApi({ estado: 'rechazada' })));

    const rechazada = await api.registrarRechazo('812', '  Plazo distinto. ', 'clave-rechazo-1');

    expect(llamada(0)[0]).toBe('/api/v1/cotizaciones/812/rechazo');
    expect(cabeceras(0)['Idempotency-Key']).toBe('clave-rechazo-1');
    expect(cuerpo(0)).toEqual({ origen: 'cliente', motivo: 'Plazo distinto.' });
    expect(rechazada.estado).toBe('Rechazada');
  });

  it('envía un motivo por defecto en el rechazo cuando el usuario no lo informa', async () => {
    const api = await cargarApiReal();
    fetchMock.mockResolvedValueOnce(respuesta(cotizacionApi({ estado: 'rechazada' })));

    await api.registrarRechazo('812', '', 'clave-rechazo-2');

    expect(cuerpo(0).motivo).toBe('Sin motivo informado.');
  });

  it('propaga los códigos de error de la API, como COTIZACION_VENCIDA en la aceptación', async () => {
    const api = await cargarApiReal();
    fetchMock.mockResolvedValueOnce(problema(409, 'COTIZACION_VENCIDA'));

    await expect(api.registrarAceptacion('812', 'clave-3')).rejects.toMatchObject({
      codigo: 'COTIZACION_VENCIDA',
      estado: 409,
    });
  });

  it('consulta una cotización por su identificador', async () => {
    const api = await cargarApiReal();
    fetchMock.mockResolvedValueOnce(respuesta(cotizacionApi()));

    const cotizacion = await api.obtenerCotizacion('812');

    expect(llamada(0)[0]).toBe('/api/v1/cotizaciones/812');
    expect(cotizacion.cliente).toEqual({ id: '57', nombre: 'Constructora Andina S.A.C.' });
  });
});
