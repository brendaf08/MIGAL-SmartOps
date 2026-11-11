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

/** Venta con su detalle (VentaDetalle), con sobrescritos opcionales. */
const ventaApi = (sobrescritos: Record<string, unknown> = {}) => ({
  id: '501',
  fechaVenta: '2026-11-07T16:45:00-05:00',
  cotizacionId: '301',
  clienteId: '12',
  clienteNombre: 'Constructora Ejemplo SAC',
  usuarioResponsableId: '5',
  estado: 'pendiente_abastecimiento',
  descuento: 18,
  subtotal: 1368,
  total: 1350,
  fechaCancelacion: null,
  motivoCancelacion: null,
  usuarioAutorizanteId: null,
  lineas: [
    {
      id: '901',
      productoId: '42',
      sku: 'CEM-001',
      nombreProducto: 'Cemento Sol tipo I, bolsa 42.5 kg',
      cantidadVendida: 40,
      precioUnitario: 28.5,
      subtotal: 1140,
      cantidadReservadaActual: 40,
      cantidadPendienteAbastecimiento: 0,
    },
    {
      id: '902',
      productoId: '44',
      sku: 'AGR-004',
      nombreProducto: 'Arena gruesa, metro cúbico',
      cantidadVendida: 10,
      precioUnitario: 22.8,
      subtotal: 228,
      cantidadReservadaActual: 4,
      cantidadPendienteAbastecimiento: 6,
    },
  ],
  reservas: [],
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

describe('ventas (API real)', () => {
  it('lista las ventas leyendo el detalle de cada una y convierte los estados', async () => {
    const api = await cargarApiReal();
    fetchMock
      .mockResolvedValueOnce(
        respuesta({
          items: [{ id: '501', cotizacionId: '301', clienteId: '12', estado: 'pendiente_abastecimiento', total: 1350 }],
          total: 1,
          page: 1,
          pageSize: 100,
        }),
      )
      .mockResolvedValueOnce(respuesta(ventaApi()));

    const ventas = await api.listarVentas();

    expect(llamada(0)[0]).toBe('/api/v1/ventas?page=1&pageSize=100');
    expect(llamada(1)[0]).toBe('/api/v1/ventas/501');
    expect(ventas).toHaveLength(1);
    expect(ventas[0]).toEqual({
      id: '501',
      fecha: '2026-11-07',
      clienteId: '12',
      clienteNombre: 'Constructora Ejemplo SAC',
      cotizacionId: '301',
      estado: 'PendienteAbastecimiento',
      entrega: 'Parcial',
      descuento: 18,
      lineas: [
        { sku: 'CEM-001', nombre: 'Cemento Sol tipo I, bolsa 42.5 kg', solicitada: 40, atendida: 40, precio: 28.5 },
        { sku: 'AGR-004', nombre: 'Arena gruesa, metro cúbico', solicitada: 10, atendida: 4, precio: 22.8 },
      ],
    });
  });

  it('completa la atención con POST de cobertura y calcula lo atendido por línea', async () => {
    const api = await cargarApiReal();
    fetchMock
      .mockResolvedValueOnce(respuesta(ventaApi()))
      .mockResolvedValueOnce(
        respuesta(
          ventaApi({
            estado: 'pendiente_abastecimiento',
            lineas: [
              { ...ventaApi().lineas[0], cantidadReservadaActual: 40 },
              { ...ventaApi().lineas[1], cantidadReservadaActual: 8, cantidadPendienteAbastecimiento: 2 },
            ],
          }),
        ),
      );

    const resultado = await api.completarVenta('501', 'clave-completar-1');

    expect(llamada(1)[0]).toBe('/api/v1/ventas/501/cobertura');
    expect(llamada(1)[1]?.method).toBe('POST');
    expect(cabeceras(1)['Idempotency-Key']).toBe('clave-completar-1');
    expect(resultado.atencion).toEqual([
      { sku: 'AGR-004', nombre: 'Arena gruesa, metro cúbico', pendiente: 6, libre: 4, atendible: 4 },
    ]);
    expect(resultado.venta.lineas[1]?.atendida).toBe(8);
  });

  it('cancela la venta con POST de cancelación, motivo recortado y clave de idempotencia', async () => {
    const api = await cargarApiReal();
    fetchMock.mockResolvedValueOnce(respuesta(ventaApi({ estado: 'cancelada' })));

    const cancelada = await api.cancelarVenta('501', '  Cliente desistió  ', 'clave-cancelar-1');

    expect(llamada(0)[0]).toBe('/api/v1/ventas/501/cancelacion');
    expect(llamada(0)[1]?.method).toBe('POST');
    expect(cabeceras(0)['Idempotency-Key']).toBe('clave-cancelar-1');
    expect(JSON.parse(String(llamada(0)[1]?.body))).toEqual({ motivo: 'Cliente desistió' });
    expect(cancelada.estado).toBe('Cancelada');
  });

  it('no envía la cancelación sin motivo', async () => {
    const api = await cargarApiReal();

    await expect(api.cancelarVenta('501', '   ', 'clave-cancelar-2')).rejects.toThrow(
      'Indicar el motivo de la cancelación.',
    );
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it('propaga VENTA_NO_CANCELABLE de la API', async () => {
    const api = await cargarApiReal();
    fetchMock.mockResolvedValueOnce(
      new Response(JSON.stringify({ code: 'VENTA_NO_CANCELABLE', detail: 'No cancelable.', status: 409 }), {
        status: 409,
        headers: { 'Content-Type': 'application/problem+json' },
      }),
    );

    await expect(api.cancelarVenta('501', 'Motivo', 'clave-cancelar-3')).rejects.toMatchObject({
      codigo: 'VENTA_NO_CANCELABLE',
    });
  });
});
