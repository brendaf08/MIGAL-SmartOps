import { mockRequest } from '@/api/mock';
import type { CambioStock } from '@/components/domain/Constancia/Constancia';
import { ORDENES_COMPRA, PROVEEDORES } from '@/features/abastecimiento/fixtures';
import type { LineaOrdenCompra, OrdenCompra } from '@/features/abastecimiento/tipos';
import { PRODUCTOS } from '@/features/inventario/fixtures';
import type { LineaRecepcion, Recepcion, RecepcionListada } from './types';

/** Almacén que recibe las entradas; la API indicará el almacén de cada orden. */
export const ALMACEN = 'Almacén 1';

/** Recepciones de prueba hasta contar con la API. Se modifican en memoria. */
export const RECEPCIONES: Recepcion[] = [
  {
    id: 'R-0012',
    orden: 'OC-0086',
    proveedor: 'Aceros del Sur S.A.',
    almacen: 'Almacén 2',
    fecha: '2026-11-02',
    estado: 'Validada',
  },
  {
    id: 'R-0011',
    orden: 'OC-0084',
    proveedor: 'Distribuidora Andina S.A.C.',
    almacen: 'Almacén 1',
    fecha: '2026-10-29',
    estado: 'Validada',
  },
];

const nombreProveedor = (orden: OrdenCompra) =>
  PROVEEDORES.find((p) => p.id === orden.proveedorId)?.razonSocial ?? '';

/** Orden que admite recepciones: pendiente o con recepción parcial. */
export interface OrdenRecepcionable {
  id: string;
  proveedor: string;
  estado: 'Pendiente' | 'Parcial';
  lineas: LineaOrdenCompra[];
}

export const listarRecepciones = () =>
  mockRequest<RecepcionListada[]>(() =>
    RECEPCIONES.map((r) => ({
      ...r,
      estadoOrden: ORDENES_COMPRA.find((o) => o.id === r.orden)?.estado ?? 'Pendiente',
    })),
  );

export const listarOrdenesRecepcionables = () =>
  mockRequest<OrdenRecepcionable[]>(() =>
    ORDENES_COMPRA.filter((o) => o.estado !== 'Completada').map((o) => ({
      id: o.id,
      proveedor: nombreProveedor(o),
      estado: o.estado as OrdenRecepcionable['estado'],
      lineas: o.lineas.map((l) => ({ ...l })),
    })),
  );

/** Resultado de confirmar: la recepción, la existencia antes y después y lo que queda pendiente en la orden. */
export interface ResultadoRecepcion {
  recepcion: Recepcion;
  cambios: CambioStock[];
  /** `Parcial` admite otra recepción; `Completada` cierra la orden. */
  estadoOrden: OrdenCompra['estado'];
  /** Unidades que faltan por recibir en la orden. */
  porRecibir: number;
  /** Líneas de la orden con cantidad pendiente. */
  lineasPendientes: number;
}

/** Calcula el efecto de las entradas sobre la existencia física del almacén que recibe. */
function cambiosDeEntrada(lineas: LineaRecepcion[]): CambioStock[] {
  return lineas.flatMap((l) => {
    const producto = PRODUCTOS.find((p) => p.sku === l.sku);
    if (!producto || l.recibido === 0) return [];
    return [
      {
        producto: producto.nombre,
        ubicacion: ALMACEN,
        antes: producto.almacen1,
        despues: producto.almacen1 + l.recibido,
        unidad: producto.unidad,
      },
    ];
  });
}

const siguienteId = () =>
  `R-${String(Math.max(...RECEPCIONES.map((r) => Number(r.id.slice(2)))) + 1).padStart(4, '0')}`;

/**
 * CU-13. Los movimientos de entrada al Kardex se generan solo aquí, con las cantidades efectivamente recibidas.
 * Las cantidades se acumulan en la orden de compra: la orden queda `Parcial` o `Completada`.
 */
export const confirmarRecepcion = (ordenId: string, lineas: LineaRecepcion[], nota: string) =>
  mockRequest<ResultadoRecepcion>(() => {
    const orden = ORDENES_COMPRA.find((o) => o.id === ordenId);
    if (!orden || orden.estado === 'Completada') {
      throw new Error('La orden ya no admite recepciones.');
    }
    if (lineas.some((l) => !l.sku)) {
      throw new Error('Asociar un producto a cada línea antes de confirmar.');
    }
    if (lineas.some((l) => l.recibido > l.pedido) && !nota.trim()) {
      throw new Error('Registrar una nota para explicar el sobrante.');
    }

    for (const linea of lineas) {
      const enOrden = orden.lineas.find((l) => l.sku === linea.sku);
      if (enOrden) enOrden.recibido += linea.recibido;
    }
    const pendientes = orden.lineas.filter((l) => l.recibido < l.cantidad);
    orden.estado = pendientes.length === 0 ? 'Completada' : 'Parcial';

    const recepcion: Recepcion = {
      id: siguienteId(),
      orden: orden.id,
      proveedor: nombreProveedor(orden),
      almacen: ALMACEN,
      fecha: new Date().toISOString().slice(0, 10),
      estado: 'Validada',
      ...(nota.trim() ? { nota: nota.trim() } : {}),
    };
    RECEPCIONES.unshift(recepcion);

    return {
      recepcion,
      cambios: cambiosDeEntrada(lineas),
      estadoOrden: orden.estado,
      porRecibir: pendientes.reduce((suma, l) => suma + (l.cantidad - l.recibido), 0),
      lineasPendientes: pendientes.length,
    };
  });
