import { describe, expect, it } from 'vitest';
import {
  calcularAtencion,
  tieneLineasSinCobertura,
  type LineaSolicitada,
  type ModalidadAtencion,
} from './registro/atencion';

const stock = { CEM: 40, FIE: 10, TUB: 0 };
const linea = (sku: string, cantidad: number): LineaSolicitada => ({ sku, cantidad });

describe('calcularAtencion', () => {
  it('atiende todas las líneas y deja la venta pendiente de despacho si hay stock', () => {
    const r = calcularAtencion([linea('CEM', 40), linea('FIE', 10)], stock, null);

    expect(r.lineas.map((l) => [l.atendida, l.pendiente])).toEqual([
      [40, 0],
      [10, 0],
    ]);
    expect(r).toMatchObject({
      totalPendiente: 0,
      estado: 'PendienteDespacho',
      entrega: 'Completa',
    });
  });

  it('con modalidad parcial atiende lo disponible y registra la diferencia como pendiente', () => {
    const r = calcularAtencion([linea('CEM', 55)], stock, 'parcial');

    expect(r.lineas[0]).toEqual({ sku: 'CEM', solicitada: 55, atendida: 40, pendiente: 15 });
    expect(r).toMatchObject({
      totalPendiente: 15,
      estado: 'PendienteAbastecimiento',
      entrega: 'Parcial',
    });
  });

  it.each<ModalidadAtencion>(['pendienteAbastecimiento', 'pendienteCliente'])(
    'con %s no atiende las líneas sin cobertura, pero sí las cubiertas',
    (modalidad) => {
      const r = calcularAtencion([linea('CEM', 40), linea('FIE', 11)], stock, modalidad);

      expect(r.lineas.map((l) => l.atendida)).toEqual([40, 0]);
      expect(r.lineas[1]!.pendiente).toBe(11);
      expect(r).toMatchObject({ estado: 'PendienteAbastecimiento', entrega: 'Parcial' });
    },
  );

  it('sin modalidad no atiende lo no cubierto', () => {
    expect(calcularAtencion([linea('TUB', 3)], stock, null).lineas[0]!.atendida).toBe(0);
  });

  it('una cantidad exactamente igual al stock queda cubierta', () => {
    expect(tieneLineasSinCobertura([linea('CEM', 40)], stock)).toBe(false);
    expect(tieneLineasSinCobertura([linea('CEM', 41)], stock)).toBe(true);
  });

  it('con stock cero o negativo no atiende nada en modo parcial', () => {
    const r = calcularAtencion([linea('TUB', 5), linea('SIN', 2)], { TUB: 0, SIN: -4 }, 'parcial');

    expect(r.lineas.map((l) => l.atendida)).toEqual([0, 0]);
  });

  it('varias líneas del mismo SKU comparten el stock y nunca lo superan', () => {
    const r = calcularAtencion([linea('CEM', 25), linea('CEM', 25)], stock, 'parcial');

    expect(r.lineas.map((l) => l.atendida)).toEqual([25, 15]);
    expect(r.lineas.reduce((s, l) => s + l.atendida, 0)).toBe(40);
  });

  it('ninguna combinación atiende más que el stock ni más que lo solicitado', () => {
    const existencias = [0, 1, 5, 10];
    const cantidades = [1, 2, 5, 9, 10, 11];
    const modalidades: (ModalidadAtencion | null)[] = [
      null,
      'parcial',
      'pendienteAbastecimiento',
      'pendienteCliente',
    ];
    for (const libre of existencias)
      for (const cantidad of cantidades)
        for (const m of modalidades)
          for (const cantidad2 of cantidades) {
            const r = calcularAtencion(
              [linea('X', cantidad), linea('X', cantidad2)],
              { X: libre },
              m,
            );
            const atendido = r.lineas.reduce((s, l) => s + l.atendida, 0);
            expect(atendido).toBeLessThanOrEqual(libre);
            r.lineas.forEach((l) => {
              expect(l.atendida).toBeLessThanOrEqual(l.solicitada);
              expect(l.atendida + l.pendiente).toBe(l.solicitada);
            });
          }
  });
});
