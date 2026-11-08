/**
 * Simula la latencia de la API REST mientras el backend no existe (backend/ solo contiene su README).
 * Cada función de `api.ts` de cada módulo que lo usa se sustituye por una llamada HTTP sin cambiar su firma.
 */
export const mockRequest = <T>(compute: () => T, ms = 250): Promise<T> =>
  new Promise((resolve, reject) =>
    setTimeout(() => {
      try {
        resolve(compute());
      } catch (error) {
        reject(error);
      }
    }, ms),
  );

/** Respuesta paginada del servidor. */
export interface Page<T> {
  items: T[];
  total: number;
}
