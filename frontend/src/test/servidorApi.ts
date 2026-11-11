import { vi } from 'vitest';

/** Llamada recibida por el `fetch` simulado, con la ruta, la consulta, las cabeceras y el cuerpo ya interpretado. */
export interface LlamadaApi {
  metodo: string;
  ruta: string;
  consulta: URLSearchParams;
  cabeceras: Headers;
  cuerpo: unknown;
}

/** Respuesta JSON del servidor simulado. */
export const json = (cuerpo: unknown, estado = 200) =>
  new Response(JSON.stringify(cuerpo), { status: estado, headers: { 'Content-Type': 'application/json' } });

/** Error de la API con la forma `application/problem+json` del contrato. */
export const problema = (estado: number, codigo: string, detalle: string) =>
  json({ type: 'about:blank', title: codigo, status: estado, detail: detalle, instance: '/', code: codigo }, estado);

/**
 * Sustituye `fetch` por un servidor simulado. Cada clave tiene la forma "METODO /ruta" y recibe la llamada para
 * construir la respuesta, por ejemplo según el parámetro `page`. Las llamadas sin ruta simulada fallan en la prueba.
 * Devuelve el arreglo donde se registra cada llamada.
 */
export function simularServidor(respuestas: Record<string, (llamada: LlamadaApi) => Response>): LlamadaApi[] {
  const llamadas: LlamadaApi[] = [];
  vi.stubGlobal(
    'fetch',
    vi.fn(async (url: string, init: RequestInit = {}) => {
      const destino = new URL(url, 'http://localhost');
      const metodo = init.method ?? 'GET';
      const llamada: LlamadaApi = {
        metodo,
        ruta: destino.pathname,
        consulta: destino.searchParams,
        cabeceras: new Headers(init.headers),
        cuerpo: init.body === undefined ? undefined : JSON.parse(String(init.body)),
      };
      llamadas.push(llamada);
      const respuesta = respuestas[`${metodo} ${destino.pathname}`];
      if (!respuesta) throw new Error(`Ruta no simulada: ${metodo} ${destino.pathname}`);
      return respuesta(llamada);
    }),
  );
  return llamadas;
}

/** Activa la rama de la API real con un token de sesión; debe llamarse antes de cargar el módulo. */
export function activarApiReal() {
  vi.stubEnv('VITE_API_REAL', 'true');
  window.sessionStorage.setItem('migal-token', 'TOKEN-PRUEBA');
}

/** Restablece el entorno de las pruebas de la API real. */
export function restablecerApiReal() {
  vi.unstubAllEnvs();
  vi.unstubAllGlobals();
  window.sessionStorage.clear();
}

/** Vuelve a evaluar el módulo para que `API_REAL` tome el valor del entorno activo. */
export async function cargarModulo<T>(importar: () => Promise<T>): Promise<T> {
  vi.resetModules();
  return importar();
}
