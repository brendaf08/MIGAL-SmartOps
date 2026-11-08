import { mockRequest } from '@/api/mock';
import type { CambioStock } from '@/components/domain/Constancia/Constancia';
import { PRODUCTOS } from '@/features/inventario/fixtures';
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

/** Orden con la salida confirmada y la existencia física total antes y después de cada línea. */
export interface ResultadoSalida {
  orden: OrdenDespacho;
  cambios: CambioStock[];
}

/** Efecto de la salida sobre la existencia física consolidada; la orden no indica la ubicación de origen. */
function cambiosDeSalida(lineas: LineaDespacho[]): CambioStock[] {
  return lineas.flatMap((l) => {
    const producto = PRODUCTOS.find((p) => p.sku === l.sku);
    if (!producto) return [];
    const fisico = producto.tienda + producto.almacen1 + producto.almacen2;
    return [
      {
        producto: l.nombre,
        ubicacion: 'Todas las ubicaciones',
        antes: fisico,
        despues: fisico - l.cantidad,
        unidad: producto.unidad,
      },
    ];
  });
}

/**
 * CU-06. La salida de Kardex se registra aquí, al confirmar la salida física, no al aprobar la venta ni al imprimir la guía.
 * El inventario simulado no se modifica; la API real devolverá la existencia antes y después.
 */
export const confirmarSalida = (id: string) =>
  mockRequest<ResultadoSalida>(() => {
    const found = ORDENES.find((o) => o.id === id);
    if (!found || found.estado !== 'Emitida')
      throw new Error('La orden no está lista para confirmar su salida.');
    if (!reservaCompleta(found.lineas))
      throw new Error('Hay cantidades sin reservar; la entrega no puede ser completa.');
    found.estado = 'SalidaConfirmada';
    return { orden: found, cambios: cambiosDeSalida(found.lineas) };
  });
