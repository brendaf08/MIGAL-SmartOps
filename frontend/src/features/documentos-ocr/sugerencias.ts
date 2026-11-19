const PALABRAS_VACIAS = new Set([
  'de',
  'del',
  'la',
  'el',
  'los',
  'las',
  'con',
  'para',
  'por',
  'una',
  'uno',
  'unidad',
]);

const normalizar = (texto: string) =>
  texto
    .normalize('NFD')
    .replace(/\p{M}/gu, '')
    .toLowerCase()
    .replace(/[^a-z0-9/.]+/g, ' ')
    .trim();

const palabras = (texto: string) =>
  normalizar(texto)
    .split(' ')
    .filter((p) => p.length >= 3 && !PALABRAS_VACIAS.has(p));

/**
 * SKU de los productos del catálogo relacionados con un texto leído por el OCR, del más al menos parecido: los que
 * comparten más palabras (o medidas) con él. Sin palabras en común no se sugiere nada, de modo que el selector no
 * ofrece el catálogo entero.
 */
export function sugerirSkus(
  texto: string,
  productos: readonly { sku: string; nombre: string }[],
  maximo = 12,
): string[] {
  const buscadas = palabras(texto);
  if (buscadas.length === 0) return [];
  return productos
    .map((p) => {
      const nombre = normalizar(`${p.sku} ${p.nombre}`);
      const puntos = buscadas.reduce(
        (suma, palabra) => suma + (nombre.includes(palabra) ? (palabra.length >= 4 ? 2 : 1) : 0),
        0,
      );
      return { sku: p.sku, puntos, largo: p.nombre.length };
    })
    .filter((p) => p.puntos > 0)
    .sort((a, b) => b.puntos - a.puntos || a.largo - b.largo)
    .slice(0, maximo)
    .map((p) => p.sku);
}

/** Coincide si todas las palabras escritas aparecen en el texto de la opción. */
export function coincideBusqueda(etiqueta: string, busqueda: string): boolean {
  const texto = normalizar(etiqueta);
  return normalizar(busqueda)
    .split(' ')
    .filter(Boolean)
    .every((p) => texto.includes(p));
}
