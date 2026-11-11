import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

/** Carga el módulo con la API real activada. Debe llamarse tras fijar el entorno, porque API_REAL se evalúa al importar. */
async function cargarApiReal() {
  vi.stubEnv('VITE_API_REAL', 'true');
  vi.resetModules();
  return import('./api');
}

/** Respuesta JSON simulada con el estado indicado. */
const respuesta = (cuerpo: unknown, estado = 200) =>
  new Response(JSON.stringify(cuerpo), {
    status: estado,
    headers: { 'Content-Type': 'application/json' },
  });

/** Problema RFC 9457 simulado para un código de negocio. */
const problema = (estado: number, code: string, extensiones: Record<string, unknown> = {}) =>
  new Response(
    JSON.stringify({
      type: 'https://migal.example/errores/' + code.toLowerCase(),
      title: 'Error',
      status: estado,
      detail: 'Mensaje de prueba.',
      instance: '/api/v1/clientes',
      code,
      ...extensiones,
    }),
    { status: estado, headers: { 'Content-Type': 'application/problem+json' } },
  );

const clienteApi = (sobrescritos: Record<string, unknown> = {}) => ({
  id: '57',
  tipoDocumento: 'ruc',
  numeroDocumento: '20601234567',
  nombreRazonSocial: 'Constructora Andina S.A.C.',
  telefonoContacto: '987654321',
  direccionReferencia: null,
  estadoActivo: true,
  fechaRegistro: '2026-02-10T10:20:00-05:00',
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

describe('clientes (API real)', () => {
  it('lista todas las páginas y convierte los campos al tipo de la interfaz', async () => {
    const api = await cargarApiReal();
    fetchMock
      .mockResolvedValueOnce(
        respuesta({
          items: [clienteApi()],
          total: 2,
          page: 1,
          pageSize: 100,
        }),
      )
      .mockResolvedValueOnce(
        respuesta({
          items: [clienteApi({ id: '58', tipoDocumento: 'dni', numeroDocumento: '41234567', telefonoContacto: null, estadoActivo: false, nombreRazonSocial: 'Juan Pérez' })],
          total: 2,
          page: 2,
          pageSize: 100,
        }),
      );

    const clientes = await api.listarClientes();

    expect(fetchMock).toHaveBeenCalledTimes(2);
    expect(llamada(0)[0]).toBe('/api/v1/clientes?page=1&pageSize=100');
    expect(llamada(1)[0]).toBe('/api/v1/clientes?page=2&pageSize=100');
    expect(llamada(0)[1]?.method).toBe('GET');
    expect(cabeceras(0).Authorization).toBe('Bearer token-de-prueba');
    expect(clientes).toEqual([
      {
        id: '57',
        tipoDocumento: 'RUC',
        documento: '20601234567',
        nombre: 'Constructora Andina S.A.C.',
        telefono: '987654321',
        correo: '',
        estado: 'Activo',
      },
      {
        id: '58',
        tipoDocumento: 'DNI',
        documento: '41234567',
        nombre: 'Juan Pérez',
        telefono: '',
        correo: '',
        estado: 'Inactivo',
      },
    ]);
  });

  it('registra un cliente con POST y envía los vacíos como nulos', async () => {
    const api = await cargarApiReal();
    fetchMock.mockResolvedValueOnce(respuesta(clienteApi({ id: '99' }), 201));

    const creado = await api.registrarCliente({
      tipoDocumento: 'RUC',
      documento: '20601234567',
      nombre: 'Constructora Andina S.A.C.',
      telefono: '',
      correo: 'compras@andina.example',
    });

    expect(llamada(0)[0]).toBe('/api/v1/clientes');
    expect(llamada(0)[1]?.method).toBe('POST');
    expect(cabeceras(0)['Content-Type']).toBe('application/json');
    expect(cuerpo(0)).toEqual({
      tipoDocumento: 'ruc',
      numeroDocumento: '20601234567',
      nombreRazonSocial: 'Constructora Andina S.A.C.',
      telefonoContacto: null,
    });
    expect(creado.id).toBe('99');
    expect(creado.estado).toBe('Activo');
  });

  it('traduce DOCUMENTO_CLIENTE_DUPLICADO a DocumentoDuplicadoError con el cliente existente', async () => {
    const api = await cargarApiReal();
    fetchMock
      .mockResolvedValueOnce(problema(409, 'DOCUMENTO_CLIENTE_DUPLICADO', { clienteId: '57' }))
      .mockResolvedValueOnce(respuesta(clienteApi()));

    await expect(
      api.registrarCliente({
        tipoDocumento: 'RUC',
        documento: '20601234567',
        nombre: 'Otra razón social',
        telefono: '',
        correo: '',
      }),
    ).rejects.toMatchObject({
      name: 'DocumentoDuplicadoError',
      cliente: { id: '57', documento: '20601234567', estado: 'Activo' },
    });
    expect(llamada(1)[0]).toBe('/api/v1/clientes/57');
  });

  it('traduce el documento duplicado también al modificar un cliente', async () => {
    const api = await cargarApiReal();
    fetchMock
      .mockResolvedValueOnce(problema(409, 'DOCUMENTO_CLIENTE_DUPLICADO', { clienteId: '57' }))
      .mockResolvedValueOnce(respuesta(clienteApi()));

    await expect(
      api.actualizarCliente('58', {
        tipoDocumento: 'RUC',
        documento: '20601234567',
        nombre: 'Juan Pérez',
        telefono: '',
        correo: '',
      }),
    ).rejects.toMatchObject({ name: 'DocumentoDuplicadoError', cliente: { id: '57' } });
    expect(llamada(0)[0]).toBe('/api/v1/clientes/58');
    expect(llamada(0)[1]?.method).toBe('PUT');
  });

  it('propaga los demás errores de la API sin traducirlos', async () => {
    const api = await cargarApiReal();
    fetchMock.mockResolvedValueOnce(
      problema(422, 'VALIDACION_FALLIDA', { errores: [{ campo: 'nombreRazonSocial', codigo: 'min', mensaje: 'Obligatorio.' }] }),
    );

    await expect(
      api.registrarCliente({ tipoDocumento: 'DNI', documento: '12345678', nombre: '', telefono: '', correo: '' }),
    ).rejects.toMatchObject({ codigo: 'VALIDACION_FALLIDA', estado: 422 });
  });

  it('actualiza un cliente con PUT y envía el cuerpo de ClienteDatos', async () => {
    const api = await cargarApiReal();
    fetchMock.mockResolvedValueOnce(respuesta(clienteApi({ telefonoContacto: '912345678' })));

    const actualizado = await api.actualizarCliente('57', {
      tipoDocumento: 'RUC',
      documento: '20601234567',
      nombre: 'Constructora Andina S.A.C.',
      telefono: '912345678',
      correo: '',
    });

    expect(llamada(0)[0]).toBe('/api/v1/clientes/57');
    expect(llamada(0)[1]?.method).toBe('PUT');
    expect(cuerpo(0)).toEqual({
      tipoDocumento: 'ruc',
      numeroDocumento: '20601234567',
      nombreRazonSocial: 'Constructora Andina S.A.C.',
      telefonoContacto: '912345678',
    });
    expect(actualizado.telefono).toBe('912345678');
  });

  it('desactiva un cliente con PUT sobre el recurso estado', async () => {
    const api = await cargarApiReal();
    fetchMock.mockResolvedValueOnce(respuesta(clienteApi({ estadoActivo: false })));

    const baja = await api.desactivarCliente('57');

    expect(llamada(0)[0]).toBe('/api/v1/clientes/57/estado');
    expect(llamada(0)[1]?.method).toBe('PUT');
    expect(cuerpo(0)).toEqual({ estadoActivo: false });
    expect(baja.estado).toBe('Inactivo');
  });
});
