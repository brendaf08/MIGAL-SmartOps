import { describe, expect, it } from 'vitest';
import { consolidarLineas } from './calculos';

describe('consolidarLineas', () => {
  it('suma las cantidades de un producto repetido y lo informa', () => {
    const { lineas, repetidos } = consolidarLineas([
      { id: 1, sku: 'FIE-001', cantidad: 5, precio: 20 },
      { id: 2, sku: 'CEM-001', cantidad: 2, precio: 30 },
      { id: 3, sku: 'FIE-001', cantidad: 3, precio: 0 },
    ]);

    expect(lineas).toEqual([
      { id: 1, sku: 'FIE-001', cantidad: 8, precio: 20 },
      { id: 2, sku: 'CEM-001', cantidad: 2, precio: 30 },
    ]);
    expect(repetidos).toEqual(['FIE-001']);
  });

  it('no toca las filas sin producto ni modifica la lista original', () => {
    const original = [
      { id: 1, sku: '', cantidad: 1, precio: 0 },
      { id: 2, sku: '', cantidad: 1, precio: 0 },
    ];
    const { lineas, repetidos } = consolidarLineas(original);

    expect(lineas).toHaveLength(2);
    expect(repetidos).toEqual([]);
    expect(original[0]!.cantidad).toBe(1);
  });
});
