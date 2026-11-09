import { mockRequest } from '@/api/mock';
import { siguienteCodigo } from '@/lib/codigos';
import { resumen } from '@/features/inventario/calculos';
import { PRODUCTOS } from '@/features/inventario/fixtures';
import { COTIZACIONES } from '../../fixtures';
import type { Cotizacion } from '../../types';
import { VENTAS } from '../fixtures';
import type { LineaVenta, Venta } from '../tipos';
import { calcularAtencion, type ModalidadAtencion, type ResultadoAtencion } from './atencion';
import type { CambioStock } from '@/components/domain/Constancia/Constancia';

/** Stock libre por SKU del catálogo vigente; la validación del formulario se calcula con estas cifras. */
export const stockLibre = (): Record<string, number> =>
  Object.fromEntries(PRODUCTOS.map((p) => [p.sku, resumen(p).libre]));

/** Cotizaciones aceptadas que aún no se han convertido en venta (flujo alternativo 2). */
export const listarCotizacionesAceptadas = () =>
  mockRequest(() =>
    COTIZACIONES.filter(
      (c) => c.estado === 'AceptadaCliente' && !VENTAS.some((v) => v.cotizacionId === c.id),
    ),
  );

/** Busca una cotización aceptada no convertida; devuelve `undefined` si no se puede usar. */
export const cotizacionUtilizable = (id: string): Cotizacion | undefined =>
  COTIZACIONES.find(
    (c) =>
      c.id === id && c.estado === 'AceptadaCliente' && !VENTAS.some((v) => v.cotizacionId === c.id),
  );

export interface EntradaVenta {
  clienteId?: string;
  cotizacionId?: string;
  descuento: number;
  lineas: { sku: string; nombre: string; cantidad: number; precio: number }[];
  modalidad: ModalidadAtencion | null;
}

/** Resultado de confirmar: la venta, el reparto de la atención y la existencia libre antes y después. */
export interface ResultadoVenta {
  venta: Venta;
  atencion: ResultadoAtencion;
  cambios: CambioStock[];
}

/**
 * CU-10. Registra la venta en memoria con el siguiente código. La cantidad atendida se calcula con el stock
 * libre actual; la venta queda vinculada a la cotización de origen, que deja de estar disponible para otra venta.
 */
export const registrarVenta = (entrada: EntradaVenta) =>
  mockRequest<ResultadoVenta>(() => {
    const stock = stockLibre();
    const atencion = calcularAtencion(
      entrada.lineas.map((l) => ({ sku: l.sku, cantidad: l.cantidad })),
      stock,
      entrada.modalidad,
    );
    const lineas: LineaVenta[] = entrada.lineas.map((l, i) => ({
      sku: l.sku,
      nombre: l.nombre,
      solicitada: l.cantidad,
      atendida: atencion.lineas[i]!.atendida,
      precio: l.precio,
    }));
    const venta: Venta = {
      id: siguienteCodigo(
        'V',
        VENTAS.map((v) => v.id),
        4,
      ),
      fecha: new Date().toISOString().slice(0, 10),
      clienteId: entrada.clienteId,
      cotizacionId: entrada.cotizacionId,
      estado: atencion.estado,
      entrega: atencion.entrega,
      descuento: entrada.descuento,
      lineas,
    };
    VENTAS.unshift(venta);
    const cambios: CambioStock[] = lineas
      .filter((l) => l.atendida > 0)
      .map((l) => ({
        producto: l.nombre,
        ubicacion: 'Stock libre',
        antes: stock[l.sku] ?? 0,
        despues: (stock[l.sku] ?? 0) - l.atendida,
        unidad: PRODUCTOS.find((p) => p.sku === l.sku)?.unidad ?? '',
      }));
    return { venta, atencion, cambios };
  });
