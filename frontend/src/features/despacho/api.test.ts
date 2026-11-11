import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

/** Carga los módulos con la API real activada. Debe llamarse tras fijar el entorno, porque API_REAL se evalúa al importar. */
async function cargarApiReal() {
  vi.stubEnv('VITE_API_REAL', 'true');
  vi.resetModules();
  const despacho = await import('./api');
  const ordenes = await import('./ordenes/api');
  return { ...despacho, ...ordenes };
}

const respuesta = (cuerpo: unknown, estado = 200) =>
  new Response(cuerpo === undefined ? null : JSON.stringify(cuerpo), {
    status: estado,
    headers: { 'Content-Type': 'application/json' },
  });

/** Guía con sus líneas (GuiaDespacho), con sobrescritos opcionales. */
const guiaApi = (sobrescritos: Record<string, unknown> = {}) => ({
  id: '31',
  codigo: 'D-0031',
  idVenta: '38',
  codigoVenta: 'V-0038',
  cliente: 'Constructora Los Andes S.A.C.',
  estado: 'emitida',
  fechaEmision: '2026-11-06T09:15:00-05:00',
  fechaSalidaReal: null,
  fechaEntrega: null,
  usuarioResponsableId: null,
  direccionEntrega: 'Av. Javier Prado Este 4200, San Borja, Lima',
  fechaDespacho: null,
  lineas: [
    {
      productoId: '42',
      sku: 'CEM-001',
      nombre: 'Cemento Sol tipo I, bolsa 42.5 kg',
      unidad: 'bolsa',
      cantidad: 200,
      ubicacionesSalida: [
        { ubicacionId: '101', nombre: 'Tienda principal', cantidad: 40 },
        { ubicacionId: '102', nombre: 'Almacén 1', cantidad: 160 },
      ],
    },
  ],
  ...sobrescritos,
});

const fetchMock = vi.fn<(url: string, init?: RequestInit) => Promise<Response>>();

const llamada = (indice: number): [string, RequestInit | undefined] => {
  const parametros = fetchMock.mock.calls[indice];
  if (!parametros) throw new Error(`No hay llamada a fetch en la posición ${indice}.`);
  return [parametros[0], parametros[1]];
};
const cabeceras = (indice: number) => (llamada(indice)[1]?.headers ?? {}) as Record<string, string>;

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

describe('despacho (API real)', () => {
  it('lista las guías no anuladas con su detalle y convierte las líneas', async () => {
    const api = await cargarApiReal();
    fetchMock
      .mockResolvedValueOnce(
        respuesta({
          items: [
            { id: '31', estado: 'emitida' },
            { id: '32', estado: 'anulada' },
          ],
          total: 2,
          page: 1,
          pageSize: 100,
        }),
      )
      .mockResolvedValueOnce(respuesta(guiaApi()));

    const ordenes = await api.listarOrdenes();

    expect(llamada(0)[0]).toBe('/api/v1/guias-despacho?page=1&pageSize=100');
    expect(llamada(1)[0]).toBe('/api/v1/guias-despacho/31');
    expect(fetchMock).toHaveBeenCalledTimes(2);
    expect(ordenes).toEqual([
      {
        id: '31',
        venta: 'V-0038',
        cliente: 'Constructora Los Andes S.A.C.',
        estado: 'Emitida',
        entrega: 'Completa',
        lineas: [{ sku: 'CEM-001', nombre: 'Cemento Sol tipo I, bolsa 42.5 kg', cantidad: 200, reservado: 200 }],
      },
    ]);
  });

  it('confirma la salida con POST, Idempotency-Key y mapea los cambios de existencia', async () => {
    const api = await cargarApiReal();
    fetchMock.mockResolvedValueOnce(
      respuesta({
        guia: guiaApi({ estado: 'salida_confirmada', usuarioResponsableId: '7', fechaSalidaReal: '2026-11-07T14:32:10-05:00' }),
        movimientos: [],
        cambios: [
          { productoId: '42', sku: 'CEM-001', ubicacionId: '101', unidad: 'bolsa', antes: 120, despues: 80 },
        ],
      }),
    );

    const resultado = await api.confirmarSalida('31', 'clave-salida-1');

    expect(llamada(0)[0]).toBe('/api/v1/guias-despacho/31/salida');
    expect(llamada(0)[1]?.method).toBe('POST');
    expect(cabeceras(0)['Idempotency-Key']).toBe('clave-salida-1');
    expect(resultado.orden.estado).toBe('SalidaConfirmada');
    expect(resultado.cambios).toEqual([
      { producto: 'Cemento Sol tipo I, bolsa 42.5 kg', ubicacion: 'Tienda principal', antes: 120, despues: 80, unidad: 'bolsa' },
    ]);
  });

  it('propaga RESERVA_INCOMPLETA al confirmar la salida', async () => {
    const api = await cargarApiReal();
    fetchMock.mockResolvedValueOnce(
      new Response(JSON.stringify({ code: 'RESERVA_INCOMPLETA', detail: 'Reserva incompleta.', status: 409 }), {
        status: 409,
        headers: { 'Content-Type': 'application/problem+json' },
      }),
    );

    await expect(api.confirmarSalida('31', 'clave-salida-2')).rejects.toMatchObject({
      codigo: 'RESERVA_INCOMPLETA',
    });
  });

  it('emite la guía de la venta con POST y el identificador de la venta', async () => {
    const api = await cargarApiReal();
    fetchMock.mockResolvedValueOnce(respuesta(guiaApi(), 201));

    const orden = await api.generarOrden('38');

    expect(llamada(0)[0]).toBe('/api/v1/guias-despacho');
    expect(llamada(0)[1]?.method).toBe('POST');
    expect(JSON.parse(String(llamada(0)[1]?.body))).toEqual({ idVenta: '38' });
    expect(orden.id).toBe('31');
  });

  it('registra la reimpresión con motivo y luego consulta la guía', async () => {
    const api = await cargarApiReal();
    fetchMock
      .mockResolvedValueOnce(respuesta({ id: '18', idGuia: '31', fechaHora: '2026-11-07T16:05:44-05:00', usuarioId: '7', motivo: 'Documento dañado' }, 201))
      .mockResolvedValueOnce(respuesta(guiaApi()));

    const orden = await api.reimprimirOrden('31', '  Documento dañado  ');

    expect(llamada(0)[0]).toBe('/api/v1/guias-despacho/31/reimpresiones');
    expect(JSON.parse(String(llamada(0)[1]?.body))).toEqual({ motivo: 'Documento dañado' });
    expect(llamada(1)[0]).toBe('/api/v1/guias-despacho/31');
    expect(orden.id).toBe('31');
  });

  it('no registra la reimpresión sin motivo', async () => {
    const api = await cargarApiReal();

    await expect(api.reimprimirOrden('31', '   ')).rejects.toThrow('Indicar el motivo de la reimpresión.');
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it('obtiene los datos imprimibles con la unidad de cada línea y la dirección de entrega', async () => {
    const api = await cargarApiReal();
    fetchMock.mockResolvedValueOnce(respuesta(guiaApi()));

    const documento = await api.obtenerDocumentoOrden('31');

    expect(llamada(0)[0]).toBe('/api/v1/guias-despacho/31/documento');
    expect(documento.direccion).toBe('Av. Javier Prado Este 4200, San Borja, Lima');
    expect(documento.fecha).toBe('2026-11-06');
    expect(documento.lineas[0]).toMatchObject({ sku: 'CEM-001', unidad: 'bolsa', cantidad: 200 });
  });
});
