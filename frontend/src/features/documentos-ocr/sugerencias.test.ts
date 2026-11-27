import { describe, expect, it } from 'vitest';
import { coincideBusqueda, sugerirSkus } from './sugerencias';

const CATALOGO = [
  { sku: '00771', nombre: 'CLAVO ACERO PRODAC (1 1/2")' },
  { sku: '00082', nombre: 'CLAVO DE ALBAÑIL CON CABEZA (4")' },
  { sku: '00064', nombre: 'CEMENTO SOL TIPO I - 42.5 KG' },
  { sku: '00762', nombre: 'FIERRO CORRUGADO 5/8 SIDERPERU' },
];

describe('sugerencias del OCR', () => {
  it('ofrece solo los productos que comparten palabras con el texto leído', () => {
    expect(sugerirSkus('kg clavo', CATALOGO)).toEqual(['00771', '00082']);
  });

  it('no sugiere nada si no hay palabras en común', () => {
    expect(sugerirSkus('brocha', CATALOGO)).toEqual([]);
  });

  it('la búsqueda exige todas las palabras escritas, sin importar tildes ni mayúsculas', () => {
    expect(coincideBusqueda('00082, CLAVO DE ALBAÑIL CON CABEZA (4")', 'clavo albanil')).toBe(true);
    expect(coincideBusqueda('00064, CEMENTO SOL', 'clavo')).toBe(false);
  });
});

describe('símbolo de diámetro', () => {
  const FIERROS = [
    { sku: '00763', nombre: 'FIERRO CORRUGADO 1/4 SIDERPERU' },
    { sku: '00064', nombre: 'CEMENTO SOL TIPO I - 42.5 KG' },
  ];

  it.each(['fi 1/4', 'Ø 1/4', 'phi 1/4', 'φ1/4'])('«%s» se relaciona con los fierros', (texto) => {
    expect(sugerirSkus(texto, FIERROS)).toEqual(['00763']);
  });
});
