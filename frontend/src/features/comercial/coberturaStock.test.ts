import { describe, expect, it } from 'vitest';
import type { ProductoDisponibilidad } from '@/features/inventario/types';
import { desglosarLinea } from './coberturaStock';

const producto = (parcial: Partial<ProductoDisponibilidad>): ProductoDisponibilidad => ({
  sku: 'CEM-001',
  nombre: 'Cemento',
  categoria: 'Cemento',
  unidad: 'bolsa',
  tienda: 40,
  almacen1: 60,
  almacen2: 0,
  reservado: 20,
  planificado: 0,
  precio: 30,
  minimo: 10,
  ...parcial,
});

describe('desglosarLinea', () => {
  it('cubre con stock libre (físico menos reservado)', () => {
    const d = desglosarLinea(80, producto({}));
    expect(d.libre).toBe(80);
    expect(d.reservado).toBe(20);
    expect(d.cobertura).toBe('stock');
  });

  it('cubre con lo por recibir una vez descontado lo que otras ventas reclaman', () => {
    const p = producto({ planificado: 100, faltanteVentas: 60, proximaLlegada: '2026-12-05' });
    const d = desglosarLinea(110, p);
    expect(d.previstoNeto).toBe(40);
    expect(d.compite).toBe(true);
    expect(d.proximaLlegada).toBe('2026-12-05');
    expect(d.cobertura).toBe('recibir');
  });

  it('indica que falta comprar cuando ni el stock ni el previsto neto alcanzan', () => {
    const p = producto({ planificado: 100, faltanteVentas: 100 });
    const d = desglosarLinea(81, p);
    expect(d.previstoNeto).toBe(0);
    expect(d.cobertura).toBe('comprar');
  });
});
