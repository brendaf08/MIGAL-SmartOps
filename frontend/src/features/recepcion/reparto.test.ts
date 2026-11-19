import { describe, expect, it } from 'vitest';
import {
  cantidadEn,
  conRepartoInicial,
  fijarEnUbicacion,
  fijarTotal,
  movimientosDeEntrada,
  totalesPorUbicacion,
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
  it('parte con todo lo recibido en una ubicación', () => {
    const [l] = conRepartoInicial([linea(60)], 'Almacén 1');

    expect(l!.reparto).toEqual({ 'Almacén 1': 60 });
    expect(cantidadEn(l!, 'Almacén 2', 'Almacén 1')).toBe(0);
  });

  it('el total de la línea suma lo registrado en cada ubicación', () => {
    const [inicial] = conRepartoInicial([linea(60)], 'Almacén 1');
    const repartida = fijarEnUbicacion(inicial!, 'Almacén 2', 15, 'Almacén 1');

    expect(repartida.recibido).toBe(75);
    expect(totalesPorUbicacion([repartida], 'Almacén 1')).toEqual([
      ['Almacén 1', 60],
      ['Almacén 2', 15],
    ]);
    expect(movimientosDeEntrada([repartida], 'Almacén 1')).toBe(2);
  });

  it('igualar al pedido respeta lo ya registrado en las otras ubicaciones', () => {
    const [inicial] = conRepartoInicial([linea(30)], 'Almacén 1');
    const conSegunda = fijarEnUbicacion(inicial!, 'Almacén 2', 20, 'Almacén 1');
    const igualada = fijarTotal(conSegunda, 'Almacén 1', 100, 'Almacén 1');

    expect(igualada.reparto).toEqual({ 'Almacén 1': 80, 'Almacén 2': 20 });
    expect(igualada.recibido).toBe(100);
  });

  it('una línea sin reparto cuenta como recibida en la ubicación por defecto', () => {
    expect(cantidadEn(linea(40), 'Almacén 1', 'Almacén 1')).toBe(40);
    expect(movimientosDeEntrada([linea(40), linea(0)], 'Almacén 1')).toBe(1);
  });
});
