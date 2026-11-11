import { API_REAL, ApiError, api } from '@/api/cliente';
import { mockRequest } from '@/api/mock';
import { recorrerPaginas } from '@/api/paginacion';
import type { CambioStock } from '@/components/domain/Constancia/Constancia';
import {
  aOrden,
  buscarOrdenPorCodigo,
  type OrdenCompraApi,
} from '@/features/abastecimiento/compras/api';
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

/** Recepción tal como la devuelve la API (contrato recepciones.yaml, esquema RecepcionDetalle). */
interface RecepcionApi {
  id: string;
  codigo: string;
  codigoOrdenCompra: string;
  proveedor: string;
  estado: 'pendiente' | 'validada';
  fechaRecepcion: string;
  observaciones: string | null;
  lineas: {
    productoId: string;
    nombre: string;
    ubicacionId: string;
    nombreUbicacion: string;
    cantidadRecibida: number;
  }[];
  ordenCompra: OrdenCompraReferenciaApi;
}

/** Orden de compra de origen con su estado vigente y la cantidad pendiente por producto. */
interface OrdenCompraReferenciaApi {
  estado: 'pendiente' | 'parcial' | 'completada';
  lineas: { productoId: string; cantidadPendiente: number }[];
}

/** Respuesta de la validación (esquema RecepcionResultadoValidacion). */
interface ResultadoValidacionApi {
  recepcion: RecepcionApi;
  cambios: {
    productoId: string;
    ubicacionId: string;
    unidad: string;
    antes: number;
    despues: number;
  }[];
  ordenCompra: OrdenCompraReferenciaApi;
}

const ESTADO_ORDEN_UI: Record<OrdenCompraReferenciaApi['estado'], OrdenCompra['estado']> = {
  pendiente: 'Pendiente',
  parcial: 'Parcial',
  completada: 'Completada',
};

/*
 * Adaptadores entre el contrato y la interfaz. La interfaz identifica la recepción y la orden por su código
 * (R-0012, OC-0087), igual que en las órdenes de compra. El almacén de la recepción se deduce de las ubicaciones
 * de sus líneas, porque el resumen de la API no lo incluye.
 */
const aRecepcion = (recepcion: RecepcionApi): Recepcion => ({
  id: recepcion.codigo,
  orden: recepcion.codigoOrdenCompra,
  proveedor: recepcion.proveedor,
  almacen: [...new Set(recepcion.lineas.map((l) => l.nombreUbicacion))].join(', '),
  fecha: recepcion.fechaRecepcion,
  estado: recepcion.estado === 'validada' ? 'Validada' : 'Pendiente',
  ...(recepcion.observaciones ? { nota: recepcion.observaciones } : {}),
});

const aResultado = (resultado: ResultadoValidacionApi): ResultadoRecepcion => {
  const { recepcion } = resultado;
  const cambios: CambioStock[] = resultado.cambios.map((c) => {
    const linea = recepcion.lineas.find(
      (l) => l.productoId === c.productoId && l.ubicacionId === c.ubicacionId,
    );
    return {
      producto: linea?.nombre ?? c.productoId,
      ubicacion: linea?.nombreUbicacion ?? ALMACEN,
      antes: c.antes,
      despues: c.despues,
      unidad: c.unidad,
    };
  });
  const pendientes = resultado.ordenCompra.lineas.filter((l) => l.cantidadPendiente > 0);
  return {
    recepcion: aRecepcion(recepcion),
    cambios,
    estadoOrden: ESTADO_ORDEN_UI[resultado.ordenCompra.estado],
    porRecibir: pendientes.reduce((suma, l) => suma + l.cantidadPendiente, 0),
    lineasPendientes: pendientes.length,
  };
};

const listarRecepcionesReal = async (): Promise<RecepcionListada[]> => {
  const resumenes = await recorrerPaginas<{ id: string }>('/recepciones');
  // El resumen no incluye el almacén ni el estado de la orden; ambos se obtienen del detalle de cada recepción.
  const detalles = await Promise.all(
    resumenes.map((r) => api.get<RecepcionApi>(`/recepciones/${r.id}`)),
  );
  return detalles.map((detalle) => ({
    ...aRecepcion(detalle),
    estadoOrden: ESTADO_ORDEN_UI[detalle.ordenCompra.estado],
  }));
};

const listarOrdenesRecepcionablesReal = async (): Promise<OrdenRecepcionable[]> => {
  const [ordenes, proveedores] = await Promise.all([
    recorrerPaginas<OrdenCompraApi>('/ordenes-compra', { estado: 'pendiente,parcial' }),
    recorrerPaginas<{ id: string; razonSocial: string }>('/proveedores'),
  ]);
  const nombres = new Map(proveedores.map((p) => [p.id, p.razonSocial] as const));
  return ordenes.map((o) => {
    const orden = aOrden(o);
    return {
      id: orden.id,
      proveedor: nombres.get(o.proveedorId) ?? '',
      estado: orden.estado as OrdenRecepcionable['estado'],
      lineas: orden.lineas,
    };
  });
};

/** Recepciones registradas en esta sesión y todavía no validadas, por código de orden. */
const pendientesPorOrden = new Map<string, string>();
/** Clave de idempotencia de cada intención del usuario, por huella de sus datos. */
const clavesPorIntencion = new Map<string, string>();

const hoy = () => new Date().toLocaleDateString('en-CA');

const productosPorSku = async (): Promise<Map<string, string>> => {
  const productos = await recorrerPaginas<{ id: string; sku: string }>('/productos');
  return new Map(productos.map((p) => [p.sku, p.id] as const));
};

const ubicacionDeRecepcion = async (): Promise<string> => {
  const almacenes = await api.get<{ id: string; nombre: string }[]>('/almacenes');
  const almacen = almacenes.find((a) => a.nombre === ALMACEN);
  if (!almacen) throw new Error('ALMACEN_NO_ENCONTRADO');
  return almacen.id;
};

/** Líneas de la solicitud: una por producto en la ubicación que recibe. Las líneas de un mismo SKU se suman. */
const lineasSolicitud = (
  lineas: LineaRecepcion[],
  productos: Map<string, string>,
  ubicacionId: string,
) => {
  const cantidades = new Map<string, number>();
  lineas.forEach((l) => cantidades.set(l.sku, (cantidades.get(l.sku) ?? 0) + l.recibido));
  return [...cantidades].map(([sku, cantidadRecibida]) => {
    const productoId = productos.get(sku);
    if (!productoId) throw new Error('PRODUCTO_NO_ENCONTRADO');
    return { productoId, ubicacionId, cantidadRecibida };
  });
};

/**
 * Registra la recepción pendiente y la valida. Si una operación falla, la recepción pendiente se conserva y el
 * siguiente intento la actualiza en lugar de registrar otra. La clave de idempotencia se genera una vez por
 * intención (mismos datos) y se reutiliza en los reintentos.
 */
const confirmarRecepcionReal = async (
  ordenCodigo: string,
  lineas: LineaRecepcion[],
  nota: string,
): Promise<ResultadoRecepcion> => {
  const observaciones = nota.trim();
  const huella = JSON.stringify([
    ordenCodigo,
    lineas.map((l) => [l.sku, l.recibido]),
    observaciones,
  ]);
  const clave = clavesPorIntencion.get(huella) ?? crypto.randomUUID();
  clavesPorIntencion.set(huella, clave);

  const orden = await buscarOrdenPorCodigo(ordenCodigo);
  if (!orden) throw new Error('ORDEN_NO_ENCONTRADA');
  const [productos, ubicacionId] = await Promise.all([productosPorSku(), ubicacionDeRecepcion()]);
  const solicitud = {
    fechaRecepcion: hoy(),
    observaciones: observaciones || null,
    lineas: lineasSolicitud(lineas, productos, ubicacionId),
  };

  let recepcionId = pendientesPorOrden.get(ordenCodigo);
  if (recepcionId) {
    try {
      await api.put(`/recepciones/${recepcionId}`, solicitud);
    } catch (error) {
      // Una recepción ya validada en un intento anterior se confirma con la misma clave, que devuelve su respuesta.
      if (!(error instanceof ApiError && error.codigo === 'TRANSICION_NO_PERMITIDA')) {
        if (error instanceof ApiError && error.codigo === 'RECURSO_NO_ENCONTRADO') {
          pendientesPorOrden.delete(ordenCodigo);
        }
        throw error;
      }
    }
  } else {
    const registrada = await api.post<RecepcionApi>('/recepciones', {
      ordenCompraId: orden.id,
      ...solicitud,
    });
    recepcionId = registrada.id;
    pendientesPorOrden.set(ordenCodigo, recepcionId);
  }

  const resultado = await api.post<ResultadoValidacionApi>(
    `/recepciones/${recepcionId}/validacion`,
    observaciones ? { observaciones } : undefined,
    clave,
  );
  pendientesPorOrden.delete(ordenCodigo);
  clavesPorIntencion.delete(huella);
  return aResultado(resultado);
};

export const listarRecepciones = (): Promise<RecepcionListada[]> =>
  API_REAL
    ? listarRecepcionesReal()
    : mockRequest<RecepcionListada[]>(() =>
        RECEPCIONES.map((r) => ({
          ...r,
          estadoOrden: ORDENES_COMPRA.find((o) => o.id === r.orden)?.estado ?? 'Pendiente',
        })),
      );

export const listarOrdenesRecepcionables = (): Promise<OrdenRecepcionable[]> =>
  API_REAL
    ? listarOrdenesRecepcionablesReal()
    : mockRequest<OrdenRecepcionable[]>(() =>
        ORDENES_COMPRA.filter((o) => o.estado !== 'Completada').map((o) => ({
          id: o.id,
          proveedor: nombreProveedor(o),
          estado: o.estado as OrdenRecepcionable['estado'],
          lineas: o.lineas.map((l) => ({ ...l })),
        })),
      );

/** CU-13. Los movimientos de entrada al Kardex se generan solo aquí, con las cantidades efectivamente recibidas. */
export const confirmarRecepcion = (
  ordenId: string,
  lineas: LineaRecepcion[],
  nota: string,
): Promise<ResultadoRecepcion> =>
  API_REAL
    ? confirmarRecepcionReal(ordenId, lineas, nota)
    : mockRequest<ResultadoRecepcion>(() => {
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
