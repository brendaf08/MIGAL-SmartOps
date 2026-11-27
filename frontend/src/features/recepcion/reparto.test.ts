import { describe, expect, it } from 'vitest';
import {
  cantidadEn,
  completarEn,
  maximoEn,
  movimientosDeEntrada,
  recibirEn,
  repartoDeLaGuia,
  sinReparto,
  totalesPorUbicacion,
} from './reparto';
import type { LineaRecepcion } from './types';

const linea = (pedido: number, recibido = 0): LineaRecepcion => ({
  id: 1,
  texto: 'Cemento',
  sku: 'CEM-001',
  score: 1,
  pedido,
  recibido,
});

describe('recibido por ubicación con tope duro', () => {
  it('sin documento no hay nada recibido en ninguna ubicación', () => {
    const [l] = sinReparto([linea(60, 25)]);

    expect(l!.recibido).toBe(0);
    expect(cantidadEn(l!, 'Tienda')).toBe(0);
    expect(maximoEn(l!, 'Tienda')).toBe(60);
  });

  it('cada ubicación guarda sus cantidades y el total de la línea es su suma', () => {
    const [inicial] = sinReparto([linea(60)]);
    const repartida = recibirEn(recibirEn(inicial!, 'Tienda', 20), 'Almacén 1', 15);

    expect(repartida.recibido).toBe(35);
    expect(cantidadEn(repartida, 'Tienda')).toBe(20);
    expect(totalesPorUbicacion([repartida], 'Tienda')).toEqual([
      ['Tienda', 20],
      ['Almacén 1', 15],
    ]);
    expect(movimientosDeEntrada([repartida], 'Tienda')).toBe(2);
  });

  it('nunca se recibe más de lo pendiente: lo que ya está en otras ubicaciones reduce el tope', () => {
    const [inicial] = sinReparto([linea(60)]);
    const enTienda = recibirEn(inicial!, 'Tienda', 20);

    expect(maximoEn(enTienda, 'Almacén 1')).toBe(40);
    expect(recibirEn(enTienda, 'Almacén 1', 500).reparto).toEqual({ Tienda: 20, 'Almacén 1': 40 });
    expect(recibirEn(enTienda, 'Almacén 1', 500).recibido).toBe(60);
  });

  it('completo el pedido, las demás ubicaciones quedan bloqueadas (tope cero)', () => {
    const [inicial] = sinReparto([linea(60)]);
    const completa = completarEn(inicial!, 'Almacén 1');

    expect(completa.recibido).toBe(60);
    expect(maximoEn(completa, 'Almacén 2')).toBe(0);
    expect(maximoEn(completa, 'Almacén 1')).toBe(60);
  });

  it('«todo lo pedido llegó» completa solo lo que falta respetando las otras ubicaciones', () => {
    const [inicial] = sinReparto([linea(60)]);
    const enTienda = recibirEn(inicial!, 'Tienda', 25);

    expect(completarEn(enTienda, 'Almacén 2').reparto).toEqual({ Tienda: 25, 'Almacén 2': 35 });
  });

  it('un producto que no está en la orden (pendiente 0) no admite cantidad', () => {
    const [inicial] = sinReparto([linea(0)]);

    expect(maximoEn(inicial!, 'Tienda')).toBe(0);
    expect(recibirEn(inicial!, 'Tienda', 10).recibido).toBe(0);
  });

  it('la guía leída precarga lo que dice, en la ubicación elegida y hasta el tope', () => {
    const [dentro, fuera] = repartoDeLaGuia([linea(60, 40), linea(30, 80)], 'Almacén 1');

    expect(dentro!.reparto).toEqual({ 'Almacén 1': 40 });
    expect(fuera!.reparto).toEqual({ 'Almacén 1': 30 });
    expect(fuera!.recibido).toBe(30);
  });

  it('una línea sin reparto cuenta como recibida en la ubicación por defecto', () => {
    expect(movimientosDeEntrada([linea(60, 40), linea(60, 0)], 'Almacén 1')).toBe(1);
  });
});
