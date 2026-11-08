import { mockRequest } from '@/api/mock';
import { ORDENES } from './fixtures';
import type { LineaDespacho, OrdenDespacho } from './types';

export const listarOrdenes = () => mockRequest(() => [...ORDENES]);

export const obtenerOrden = (id: string) =>
  mockRequest(() => {
    const found = ORDENES.find((o) => o.id === id);
    if (!found) throw new Error('Orden no encontrada');
    return found;
  });

/** Una orden puede despacharse completa solo si cada línea tiene reservada toda su cantidad. */
export const reservaCompleta = (lineas: LineaDespacho[]) =>
  lineas.every((l) => l.reservado >= l.cantidad);

/** CU-06. La salida de Kardex se registra aquí, al confirmar la salida física, no al aprobar la venta ni al imprimir la guía. */
export const confirmarSalida = (id: string) =>
  mockRequest<OrdenDespacho>(() => {
    const found = ORDENES.find((o) => o.id === id);
    if (!found || found.estado !== 'Emitida')
      throw new Error('La orden no está lista para confirmar su salida.');
    if (!reservaCompleta(found.lineas))
      throw new Error('Hay cantidades sin reservar; la entrega no puede ser completa.');
    found.estado = 'SalidaConfirmada';
    return found;
  });
