/**
 * Cliente HTTP de la API REST de MIGAL SmartOps (contrato en backend/api). Añade el token de la sesión, interpreta las
 * respuestas de error `application/problem+json` (RFC 9457) y las convierte en `ApiError`. Con `VITE_API_REAL` distinto
 * de `true` (o en las pruebas), los módulos usan los datos simulados de `mock.ts`.
 */

/**
 * Indica si los módulos deben usar la API real en lugar de los datos simulados. Por defecto es verdadero en el
 * servidor de desarrollo y falso en las pruebas; `VITE_API_REAL=true|false` fuerza el valor en cualquier modo.
 */
export const API_REAL =
  import.meta.env.VITE_API_REAL !== undefined
    ? import.meta.env.VITE_API_REAL === 'true'
    : import.meta.env.MODE === 'development';

const BASE = '/api/v1';
const CLAVE_TOKEN = 'migal-token';

/** Error de la API con el código del catálogo (`code`) y el estado HTTP. */
export class ApiError extends Error {
  constructor(
    readonly estado: number,
    readonly codigo: string,
    mensaje: string,
    readonly errores: { campo: string; codigo: string; mensaje: string }[] = [],
    readonly extensiones: Record<string, unknown> = {},
  ) {
    super(mensaje);
    this.name = 'ApiError';
  }
}

export const guardarToken = (token: string): void => window.sessionStorage.setItem(CLAVE_TOKEN, token);
export const borrarToken = (): void => window.sessionStorage.removeItem(CLAVE_TOKEN);
const leerToken = (): string | null => window.sessionStorage.getItem(CLAVE_TOKEN);

export interface OpcionesPeticion {
  /** Parámetros de consulta; se omiten los valores nulos, indefinidos o vacíos. */
  consulta?: Record<string, string | number | boolean | null | undefined>;
  cuerpo?: unknown;
  /** Valor de la cabecera `Idempotency-Key`, obligatoria en las operaciones críticas. */
  claveIdempotencia?: string;
}

function construirUrl(ruta: string, consulta?: OpcionesPeticion['consulta']): string {
  const parametros = new URLSearchParams();
  Object.entries(consulta ?? {}).forEach(([nombre, valor]) => {
    if (valor !== null && valor !== undefined && valor !== '') parametros.set(nombre, String(valor));
  });
  const texto = parametros.toString();
  return `${BASE}${ruta}${texto ? `?${texto}` : ''}`;
}

/** Ejecuta la petición y devuelve el cuerpo JSON, o `undefined` si la respuesta no tiene cuerpo. */
export async function peticion<T>(
  metodo: 'GET' | 'POST' | 'PUT' | 'DELETE',
  ruta: string,
  { consulta, cuerpo, claveIdempotencia }: OpcionesPeticion = {},
): Promise<T> {
  const cabeceras: Record<string, string> = { Accept: 'application/json' };
  const token = leerToken();
  if (token) cabeceras.Authorization = `Bearer ${token}`;
  if (cuerpo !== undefined) cabeceras['Content-Type'] = 'application/json';
  if (claveIdempotencia) cabeceras['Idempotency-Key'] = claveIdempotencia;

  let respuesta: Response;
  try {
    respuesta = await fetch(construirUrl(ruta, consulta), {
      method: metodo,
      headers: cabeceras,
      body: cuerpo === undefined ? undefined : JSON.stringify(cuerpo),
    });
  } catch {
    throw new ApiError(0, 'SERVICIO_NO_DISPONIBLE', 'No fue posible comunicarse con el servidor.');
  }

  if (respuesta.ok) {
    return (respuesta.status === 204 ? undefined : await respuesta.json()) as T;
  }
  const problema = (await respuesta.json().catch(() => ({}))) as {
    code?: string;
    detail?: string;
    errores?: ApiError['errores'];
  } & Record<string, unknown>;
  const { code, detail, errores, ...extensiones } = problema;
  throw new ApiError(
    respuesta.status,
    code ?? 'ERROR_INTERNO',
    detail ?? 'Ocurrió un error inesperado.',
    errores ?? [],
    extensiones,
  );
}

export const api = {
  get: <T>(ruta: string, consulta?: OpcionesPeticion['consulta']) => peticion<T>('GET', ruta, { consulta }),
  post: <T>(ruta: string, cuerpo?: unknown, claveIdempotencia?: string) =>
    peticion<T>('POST', ruta, { cuerpo, claveIdempotencia }),
  put: <T>(ruta: string, cuerpo?: unknown) => peticion<T>('PUT', ruta, { cuerpo }),
  delete: <T>(ruta: string) => peticion<T>('DELETE', ruta),
};
