import { afterEach, describe, expect, it, vi } from 'vitest';
import { ApiError, api, guardarToken } from './cliente';
import { problema, simularServidor } from '@/test/apiReal';

afterEach(() => {
  vi.unstubAllGlobals();
  window.sessionStorage.clear();
});

describe('api.subir', () => {
  it('envía el formulario con POST, el token de la sesión y sin fijar Content-Type', async () => {
    guardarToken('token-de-prueba');
    const llamada = simularServidor(() => ({ estado: 202, cuerpo: { id: '31' } }));
    const formulario = new FormData();
    formulario.append('tipoDocumento', 'solicitud_cliente');
    formulario.append('archivo', new File(['%PDF'], 'pedido.pdf', { type: 'application/pdf' }));

    const respuesta = await api.subir<{ id: string }>('/documentos', formulario);

    expect(respuesta).toEqual({ id: '31' });
    const [url, inicio] = (llamada.mock.calls[0] ?? []) as [string, RequestInit?];
    expect(url).toBe('/api/v1/documentos');
    expect(inicio?.method).toBe('POST');
    expect(inicio?.body).toBe(formulario);
    const cabeceras = new Headers(inicio?.headers);
    expect(cabeceras.get('Authorization')).toBe('Bearer token-de-prueba');
    expect(cabeceras.has('Content-Type')).toBe(false);
  });

  it('convierte un problema RFC 9457 en ApiError con código, campos y extensiones', async () => {
    simularServidor(() =>
      problema(422, 'VALIDACION_FALLIDA', {
        detail: 'El archivo no es válido.',
        errores: [{ campo: 'archivo', codigo: 'FORMATO', mensaje: 'Formato no admitido.' }],
      }),
    );

    const error = await api.subir('/documentos', new FormData()).catch((e: unknown) => e);

    expect(error).toBeInstanceOf(ApiError);
    expect(error).toMatchObject({
      estado: 422,
      codigo: 'VALIDACION_FALLIDA',
      message: 'El archivo no es válido.',
      errores: [{ campo: 'archivo', codigo: 'FORMATO', mensaje: 'Formato no admitido.' }],
    });
  });

  it('informa SERVICIO_NO_DISPONIBLE cuando la red falla', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('red caída')));

    await expect(api.subir('/documentos', new FormData())).rejects.toMatchObject({
      estado: 0,
      codigo: 'SERVICIO_NO_DISPONIBLE',
    });
  });
});
