import type { EstadoVenta } from '../tipos';

/** Modalidad elegida cuando alguna línea no tiene existencia suficiente (RF-VEN-12). */
export type ModalidadAtencion = 'parcial' | 'pendienteAbastecimiento' | 'pendienteCliente';

export const MODALIDADES: Record<ModalidadAtencion, string> = {
  parcial: 'Atender parcialmente',
  pendienteAbastecimiento: 'Venta pendiente por abastecimiento',
  pendienteCliente: 'Solicitud pendiente del cliente',
};

export interface LineaSolicitada {
  sku: string;
  cantidad: number;
}

export interface LineaAtendida {
  sku: string;
  solicitada: number;
  atendida: number;
  pendiente: number;
}

export interface ResultadoAtencion {
  lineas: LineaAtendida[];
  /** Suma de las cantidades pendientes en todas las líneas. */
  totalPendiente: number;
  estado: EstadoVenta;
  entrega: 'Completa' | 'Parcial';
}

/**
 * Calcula cuánto se atiende de cada línea según el stock libre por SKU. Una línea está cubierta si su cantidad
 * cabe en el stock que queda; varias líneas del mismo SKU comparten ese stock, de modo que nunca se atiende más
 * de lo que existe. Con modalidad `parcial`, una línea no cubierta atiende lo disponible; con las otras dos
 * modalidades, no atiende nada. Sin modalidad (`null`), tampoco atiende lo no cubierto: la interfaz exige elegir
 * una modalidad cuando `tieneLineasSinCobertura` es verdadero.
 */
export function calcularAtencion(
  lineas: readonly LineaSolicitada[],
  stock: Readonly<Record<string, number>>,
  modalidad: ModalidadAtencion | null,
): ResultadoAtencion {
  const restante = new Map<string, number>();
  const resultado = lineas.map((l): LineaAtendida => {
    const disponible = restante.get(l.sku) ?? Math.max(0, stock[l.sku] ?? 0);
    const cubierta = l.cantidad <= disponible;
    const atendida = cubierta ? l.cantidad : modalidad === 'parcial' ? disponible : 0;
    restante.set(l.sku, disponible - atendida);
    return { sku: l.sku, solicitada: l.cantidad, atendida, pendiente: l.cantidad - atendida };
  });
  const totalPendiente = resultado.reduce((suma, l) => suma + l.pendiente, 0);
  return {
    lineas: resultado,
    totalPendiente,
    estado: totalPendiente > 0 ? 'PendienteAbastecimiento' : 'PendienteDespacho',
    entrega: totalPendiente > 0 ? 'Parcial' : 'Completa',
  };
}

/** Verdadero si alguna línea no se cubre con el stock, por lo que la venta exige una modalidad. */
export const tieneLineasSinCobertura = (
  lineas: readonly LineaSolicitada[],
  stock: Readonly<Record<string, number>>,
) => calcularAtencion(lineas, stock, null).totalPendiente > 0;
