import { describe, expect, it } from 'vitest';
import { diferenciaLinea, igualarCoincidenciasAltas, resumirRecepcion } from './resumen';
import type { LineaRecepcion } from './types';

const linea = (parcial: Partial<LineaRecepcion>): LineaRecepcion => ({
  id: 1,
  texto: 'CEMENTO SOL TIPO 1',
  sku: 'CEM-SOL-001',
  score: 0.94,
  pedido: 100,
  recibido: 100,
  ...parcial,
});

describe('diferenciaLinea', () => {
  it('distingue completa, faltante, sobrante y sin asociar', () => {
    expect(diferenciaLinea(linea({}))).toEqual({ tipo: 'completa' });
    expect(diferenciaLinea(linea({ recibido: 40, pedido: 60 }))).toEqual({
      tipo: 'faltan',
      cantidad: 20,
    });
    expect(diferenciaLinea(linea({ recibido: 105 }))).toEqual({ tipo: 'sobran', cantidad: 5 });
    expect(diferenciaLinea(linea({ sku: '' }))).toEqual({ tipo: 'sinAsociar' });
  });
});

describe('resumirRecepcion', () => {
  it('cuenta por estado y bloquea solo por líneas sin asociar', () => {
    const r = resumirRecepcion([
      linea({ id: 1 }),
      linea({ id: 2, recibido: 40, pedido: 60 }),
      linea({ id: 3, sku: '', score: 0.31 }),
    ]);

    expect(r).toMatchObject({ total: 3, completas: 1, parciales: 1, sobrantes: 0, sinAsociar: 1 });
    expect(r.puedeConfirmar).toBe(false);
    expect(r.motivoBloqueo).toBe('Falta asociar 1 línea a un producto del catálogo.');
  });

  it('admite la recepción parcial y los sobrantes', () => {
    const r = resumirRecepcion([linea({ recibido: 40, pedido: 60 }), linea({ id: 2, recibido: 110 })]);

    expect(r.puedeConfirmar).toBe(true);
    expect(r.motivoBloqueo).toBeNull();
  });

  it('pluraliza el motivo y rechaza una recepción sin líneas', () => {
    expect(
      resumirRecepcion([linea({ sku: '' }), linea({ id: 2, sku: '' })]).motivoBloqueo,
    ).toBe('Falta asociar 2 líneas a un producto del catálogo.');
    expect(resumirRecepcion([]).motivoBloqueo).toBe('No hay líneas para confirmar.');
  });
});

describe('igualarCoincidenciasAltas', () => {
  it('copia el pedido solo en líneas con producto y coincidencia alta', () => {
    const resultado = igualarCoincidenciasAltas([
      linea({ id: 1, recibido: 30, score: 0.9 }),
      linea({ id: 2, recibido: 30, score: 0.72 }),
      linea({ id: 3, recibido: 30, score: 0.95, sku: '' }),
    ]);

    expect(resultado.map((l) => l.recibido)).toEqual([100, 30, 30]);
  });
});
