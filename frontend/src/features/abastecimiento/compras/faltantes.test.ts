import { describe, expect, it } from 'vitest';
import type { FaltanteVenta, OrigenFaltante } from '@/features/inventario/api';
import { agruparPorVenta, dentroDeVentana, diasHasta, filtrarPorProducto } from './faltantes';

const origen = (venta: number, entrega: string | null, faltante: number): OrigenFaltante => ({
  ventaId: String(venta),
  ventaCodigo: `V-000${venta}`,
  cotizacionId: String(venta),
  cotizacionCodigo: `COT-000${venta}`,
  cliente: 'Cliente',
  fechaVenta: '2026-11-19T10:00:00Z',
  fechaEntregaRequerida: entrega,
  faltante,
});

const producto = (sku: string, planificado: number, origenes: OrigenFaltante[]): FaltanteVenta => ({
  sku,
  nombre: `Producto ${sku}`,
  unidad: 'unidad',
  faltante: origenes.reduce((s, o) => s + o.faltante, 0),
  planificado,
  porCubrir: 0,
  ventas: origenes.length,
  origenes,
});

const HOY = '2026-11-19';
const ITEMS = [
  producto('A', 20, [origen(8, '2026-11-25', 30), origen(7, null, 10)]),
  producto('B', 0, [origen(8, '2026-11-25', 5)]),
];

describe('faltantes de ventas', () => {
  it('cuenta los días hasta la entrega', () => {
    expect(diasHasta('2026-11-25', HOY)).toBe(6);
    expect(diasHasta('2026-11-18', HOY)).toBe(-1);
    expect(diasHasta(null, HOY)).toBeNull();
  });

  it('agrupa por venta con la entrega más próxima primero y las ventas sin fecha al final', () => {
    const ventas = agruparPorVenta(ITEMS, 'todas', HOY);

    expect(ventas.map((v) => v.ventaCodigo)).toEqual(['V-0008', 'V-0007']);
    expect(ventas[0]!.productos.map((p) => [p.sku, p.faltante])).toEqual([
      ['A', 30],
      ['B', 5],
    ]);
  });

  it('la ventana de tiempo filtra por fecha de entrega', () => {
    expect(dentroDeVentana('2026-11-25', '7', HOY)).toBe(true);
    expect(dentroDeVentana('2026-12-30', '30', HOY)).toBe(false);
    expect(agruparPorVenta(ITEMS, 'sin-fecha', HOY).map((v) => v.ventaCodigo)).toEqual(['V-0007']);
  });

  it('por producto recalcula lo que falta pedir con las ventas de la ventana', () => {
    const [a] = filtrarPorProducto(ITEMS, '7', HOY);

    expect(a).toMatchObject({ sku: 'A', faltante: 30, ventas: 1, porCubrir: 10 });
  });
});
