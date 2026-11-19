import { describe, expect, it } from 'vitest';
import type { LineaOcr } from '@/features/documentos-ocr/api';
import type { OrdenRecepcionable } from './api';
import { coincidenciasConGuia, ordenSugerida } from './lineas';

const orden = (id: string, lineas: [string, number, number][]): OrdenRecepcionable => ({
  id,
  ordenCompraId: id,
  proveedor: 'Proveedor',
  estado: 'Pendiente',
  lineas: lineas.map(([sku, cantidad, recibido]) => ({ sku, nombre: sku, cantidad, recibido })),
});

const guia = (...skus: string[]): LineaOcr[] =>
  skus.map((sku, i) => ({ id: String(i), texto: sku, sku, score: 1, cantidad: 5 }));

describe('sugerencia de orden de compra desde la guía leída', () => {
  const a = orden('OC-1', [
    ['CEM-001', 10, 0],
    ['FIE-002', 10, 0],
  ]);
  const b = orden('OC-2', [['CEM-001', 10, 0]]);
  const c = orden('OC-3', [['CEM-001', 10, 10]]);

  it('cuenta solo los productos de la guía que la orden aún espera recibir', () => {
    expect(coincidenciasConGuia(a, guia('CEM-001', 'FIE-002', 'XXX-000'))).toBe(2);
    expect(coincidenciasConGuia(c, guia('CEM-001'))).toBe(0);
  });

  it('sugiere la orden con más productos en común', () => {
    expect(ordenSugerida([b, a, c], guia('CEM-001', 'FIE-002'))).toBe('OC-1');
  });

  it('no sugiere ninguna si hay empate o ninguna coincide', () => {
    expect(ordenSugerida([a, b], guia('CEM-001'))).toBeNull();
    expect(ordenSugerida([a, b], guia('XXX-000'))).toBeNull();
  });
});
