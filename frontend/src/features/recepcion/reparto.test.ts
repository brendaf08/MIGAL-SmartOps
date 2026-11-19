import { describe, expect, it } from 'vitest';
import {
  cantidadEn,
  maximoEn,
  movimientosDeEntrada,
  recibirTotal,
  sinReparto,
  sinUbicar,
  totalesPorUbicacion,
  totalSinUbicar,
  ubicarEn,
  ubicarTodoEn,
} from './reparto';
import type { LineaRecepcion } from './types';

const linea = (recibido: number, pedido = 100): LineaRecepcion => ({
  id: 1,
  texto: 'Cemento',
  sku: 'CEM-001',
  score: 1,
  pedido,
  recibido,
});

describe('reparto de lo recibido entre ubicaciones', () => {
  it('parte con todo lo recibido sin ubicar', () => {
    const [l] = sinReparto([linea(60)]);

    expect(sinUbicar(l!)).toBe(60);
    expect(maximoEn(l!, 'Almacén 1')).toBe(60);
    expect(totalSinUbicar([l!])).toBe(60);
  });

  it('cada ubicación admite como máximo lo que quedó sin ubicar', () => {
    const [inicial] = sinReparto([linea(60)]);
    const enA1 = ubicarEn(inicial!, 'Almacén 1', 40);

    expect(maximoEn(enA1, 'Almacén 2')).toBe(20);
    expect(ubicarEn(enA1, 'Almacén 2', 50).reparto).toEqual({ 'Almacén 1': 40, 'Almacén 2': 20 });
    expect(sinUbicar(ubicarEn(enA1, 'Almacén 2', 50))).toBe(0);
  });

  it('con todo ubicado, las demás ubicaciones quedan sin cupo (bloqueadas)', () => {
    const [inicial] = sinReparto([linea(60)]);
    const todo = ubicarTodoEn([inicial!], 'Almacén 1')[0]!;

    expect(maximoEn(todo, 'Almacén 2')).toBe(0);
    expect(cantidadEn(todo, 'Almacén 1')).toBe(60);
    // La ubicación que tiene la cantidad sigue admitiendo cambios: puede bajar para liberar cupo.
    expect(maximoEn(todo, 'Almacén 1')).toBe(60);
  });

  it('ubicar lo que falta respeta lo ya ubicado en otras ubicaciones', () => {
    const [inicial] = sinReparto([linea(60)]);
    const enA1 = ubicarEn(inicial!, 'Almacén 1', 25);

    expect(ubicarTodoEn([enA1], 'Almacén 2')[0]!.reparto).toEqual({
      'Almacén 1': 25,
      'Almacén 2': 35,
    });
  });

  it('al bajar el total recibido se quita el exceso de la ubicación que se está viendo primero', () => {
    const [inicial] = sinReparto([linea(60)]);
    const repartida = ubicarEn(ubicarEn(inicial!, 'Almacén 1', 40), 'Almacén 2', 20);

    expect(recibirTotal(repartida, 50, 'Almacén 2').reparto).toEqual({
      'Almacén 1': 40,
      'Almacén 2': 10,
    });
    expect(recibirTotal(repartida, 30, 'Almacén 2').reparto).toEqual({
      'Almacén 1': 30,
      'Almacén 2': 0,
    });
  });

  it('cuenta los movimientos y los totales por ubicación', () => {
    const [inicial] = sinReparto([linea(60)]);
    const repartida = ubicarEn(ubicarEn(inicial!, 'Almacén 1', 40), 'Tienda', 20);

    expect(totalesPorUbicacion([repartida], 'Almacén 1')).toEqual([
      ['Almacén 1', 40],
      ['Tienda', 20],
    ]);
    expect(movimientosDeEntrada([repartida], 'Almacén 1')).toBe(2);
  });

  it('una línea sin reparto cuenta como recibida en la ubicación por defecto', () => {
    expect(movimientosDeEntrada([linea(40), linea(0)], 'Almacén 1')).toBe(1);
    expect(totalesPorUbicacion([linea(40)], 'Almacén 1')).toEqual([['Almacén 1', 40]]);
  });
});
