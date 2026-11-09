/**
 * Siguiente identificador correlativo con un prefijo, por ejemplo `siguienteCodigo('OC', ['OC-0087', 'OC-0090'])`
 * devuelve `OC-0091`. Los códigos que no tienen el prefijo se ignoran.
 */
export function siguienteCodigo(prefijo: string, existentes: readonly string[], ancho = 4): string {
  const inicio = `${prefijo}-`;
  const numeros = existentes
    .filter((codigo) => codigo.startsWith(inicio))
    .map((codigo) => Number(codigo.slice(inicio.length)))
    .filter((n) => Number.isInteger(n) && n >= 0);
  const siguiente = (numeros.length ? Math.max(...numeros) : 0) + 1;
  return `${inicio}${String(siguiente).padStart(ancho, '0')}`;
}
