import { vi } from 'vitest';

/** Respuesta que devuelve el servidor simulado. Sin `cuerpo` la respuesta no tiene contenido. */
export interface RespuestaSimulada {
  estado?: number;
  cuerpo?: unknown;
}

/** Petición recibida por el servidor simulado, con la URL ya analizada. */
export interface PeticionSimulada {
  metodo: string;
  url: URL;
  cabeceras: Headers;
  cuerpo: unknown;
}

export type ManejadorSimulado = (peticion: PeticionSimulada) => RespuestaSimulada;

/**
 * Activa la rama de API real de los módulos: fija VITE_API_REAL=true y recarga los módulos para que
 * `API_REAL` se evalúe de nuevo. Los módulos deben importarse después de llamar a esta función.
 */
export function activarApiReal(): void {
  vi.stubEnv('VITE_API_REAL', 'true');
  vi.resetModules();
}

/**
 * Sustituye `fetch` por un servidor simulado. Cada llamada se registra en el resultado para comprobar ruta,
 * método, cabeceras y cuerpo. El cuerpo FormData se devuelve tal cual; el JSON se decodifica.
 */
export function simularServidor(manejador: ManejadorSimulado) {
  const llamada = vi.fn(async (entrada: string, inicio: RequestInit = {}) => {
    const cuerpo =
      inicio.body instanceof FormData || inicio.body === undefined
        ? inicio.body
        : JSON.parse(String(inicio.body));
    const { estado = 200, cuerpo: respuesta } = manejador({
      metodo: inicio.method ?? 'GET',
      url: new URL(entrada, 'http://localhost'),
      cabeceras: new Headers(inicio.headers),
      cuerpo,
    });
    if (respuesta === undefined)
      return new Response(null, { status: estado === 200 ? 204 : estado });
    return new Response(JSON.stringify(respuesta), {
      status: estado,
      headers: { 'Content-Type': 'application/json' },
    });
  });
  vi.stubGlobal('fetch', llamada);
  return llamada;
}

/** Problema RFC 9457 que devuelve la API ante un error. */
export function problema(estado: number, codigo: string, extra: Record<string, unknown> = {}) {
  return {
    estado,
    cuerpo: {
      type: 'https://migal.example/problemas/' + codigo.toLowerCase(),
      title: codigo,
      status: estado,
      detail: `Error ${codigo}.`,
      instance: '/api/v1',
      code: codigo,
      ...extra,
    },
  };
}
