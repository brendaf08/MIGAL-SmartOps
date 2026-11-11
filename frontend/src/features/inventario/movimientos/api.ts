import { API_REAL, api } from '@/api/cliente';
import { mockRequest } from '@/api/mock';
import { todosLosElementos, type PaginaApi } from '@/api/paginas';
import type { CambioStock } from '@/components/domain/Constancia/Constancia';
import { opcionesProducto } from '@/features/catalogo/opciones';
import type { OpcionProducto } from '@/components/ui/ProductoSelect/ProductoSelect';
import {
  aCambioStock,
  aMovimiento,
  type CambioExistenciaDto,
  type MovimientoKardexDto,
} from '../adaptadores';
import { listarAlmacenes, productoIdDeSku } from '../api';
import { MOVIMIENTOS } from '../fixtures';
import type { Movimiento } from '../types';
import {
  bloqueoPorStock,
  buscarProducto,
  CAMPO_UBICACION,
  existenciaEn,
  TIPOS_MOVIMIENTO,
  type TipoMovimiento,
  type Ubicacion,
} from './esquema';

/** Datos validados del formulario, con el responsable de la sesión y la clave de idempotencia de la intención. */
export interface DatosMovimiento {
  tipo: TipoMovimiento;
  sku: string;
  ubicacion: string;
  origen: string;
  destino: string;
  cantidad: number;
  motivo: string;
  usuario: string;
  /** UUID que identifica la intención del usuario; se reutiliza en los reintentos de la misma acción. */
  claveIdempotencia: string;
}

/** Respuesta de `POST /inventario/movimientos` y `POST /inventario/traslados`, en la forma del contrato. */
interface ResultadoMovimientoDto {
  movimientos: MovimientoKardexDto[];
  cambios: CambioExistenciaDto[];
}

/** Opciones de producto para el selector: solo productos vigentes, que son los que admiten movimientos. */
export const listarProductosMovibles = (): Promise<OpcionProducto[]> =>
  API_REAL
    ? todosLosElementos((page, pageSize) =>
        api.get<PaginaApi<{ sku: string; nombreOficial: string }>>('/productos', {
          estadoActivo: true,
          page,
          pageSize,
        }),
      ).then((productos) => productos.map((p) => ({ sku: p.sku, nombre: p.nombreOficial })))
    : Promise.resolve(opcionesProducto());

/** Registro real de un movimiento: ajuste de entrada o salida, o traslado entre dos ubicaciones. */
const registrarMovimientoReal = async (datos: DatosMovimiento): Promise<ResultadoMovimiento> => {
  const [productoId, almacenes] = await Promise.all([productoIdDeSku(datos.sku), listarAlmacenes()]);
  const idDe = (nombre: string) => {
    const id = almacenes.find((a) => a.nombre === nombre)?.id;
    if (!id) throw new Error('La ubicación seleccionada no existe.');
    return id;
  };
  const nombres = new Map(almacenes.map((a) => [a.id, a.nombre]));

  const resultado =
    datos.tipo === 'traslado'
      ? await api.post<ResultadoMovimientoDto>(
          '/inventario/traslados',
          {
            productoId,
            ubicacionOrigenId: idDe(datos.origen),
            ubicacionDestinoId: idDe(datos.destino),
            cantidad: datos.cantidad,
            motivo: datos.motivo,
          },
          datos.claveIdempotencia,
        )
      : await api.post<ResultadoMovimientoDto>(
          '/inventario/movimientos',
          {
            tipoOperacion: datos.tipo,
            productoId,
            ubicacionId: idDe(datos.ubicacion),
            cantidad: datos.cantidad,
            motivo: datos.motivo,
          },
          datos.claveIdempotencia,
        );

  const nombreProducto = resultado.movimientos[0]?.nombreProducto ?? datos.sku;
  return {
    movimientos: resultado.movimientos.map((m) => aMovimiento(m, nombres)),
    cambios: resultado.cambios.map((c) => aCambioStock(c, nombreProducto, nombres)),
  };
};

/** Movimientos creados (un traslado genera dos) y la existencia antes y después por ubicación. */
export interface ResultadoMovimiento {
  movimientos: Movimiento[];
  cambios: CambioStock[];
}

/** Fecha local en formato `aaaa-mm-dd`, la misma que guarda el Kardex. */
const hoy = () => {
  const d = new Date();
  const mes = String(d.getMonth() + 1).padStart(2, '0');
  const dia = String(d.getDate()).padStart(2, '0');
  return `${d.getFullYear()}-${mes}-${dia}`;
};

/**
 * P13. Registra un movimiento manual. Una salida o un traslado se rechaza si supera la existencia física del origen.
 * Un traslado genera dos movimientos vinculados: salida en el origen y entrada en el destino. Modifica en memoria
 * las existencias y el Kardex de prueba.
 */
export const registrarMovimiento = (datos: DatosMovimiento): Promise<ResultadoMovimiento> =>
  API_REAL ? registrarMovimientoReal(datos) : mockRequest<ResultadoMovimiento>(() => {
    const producto = buscarProducto(datos.sku);
    if (!producto) throw new Error('El producto no existe en el catálogo.');

    const tipo = TIPOS_MOVIMIENTO.find((t) => t.value === datos.tipo);
    if (!tipo) throw new Error('El tipo de movimiento no es válido.');

    const origen = (datos.tipo === 'traslado' ? datos.origen : datos.ubicacion) as Ubicacion;
    if (datos.tipo !== 'entrada') {
      const bloqueo = bloqueoPorStock(datos.sku, origen, datos.cantidad);
      if (bloqueo) throw new Error(bloqueo);
    }

    const fecha = hoy();
    const id = Math.max(0, ...MOVIMIENTOS.map((m) => Number(m.id)));
    const nuevos: Movimiento[] = [];
    const cambios: CambioStock[] = [];

    /** Aplica un delta a la existencia física de una ubicación y anota el cambio para la constancia. */
    const aplicar = (ubicacion: Ubicacion, delta: number) => {
      const antes = existenciaEn(producto, ubicacion);
      const despues = antes + delta;
      producto[CAMPO_UBICACION[ubicacion]] = despues;
      cambios.push({
        producto: producto.nombre,
        ubicacion,
        antes,
        despues,
        unidad: producto.unidad,
      });
      nuevos.push({
        id: String(id + nuevos.length + 1),
        fecha,
        sku: producto.sku,
        producto: producto.nombre,
        tipo: delta > 0 ? 'Entrada' : 'Salida',
        cantidad: Math.abs(delta),
        origen: tipo.origen,
        ubicacion,
        usuario: datos.usuario,
      });
    };

    if (datos.tipo === 'traslado') {
      aplicar(origen, -datos.cantidad);
      aplicar(datos.destino as Ubicacion, datos.cantidad);
    } else {
      aplicar(origen, datos.tipo === 'entrada' ? datos.cantidad : -datos.cantidad);
    }

    MOVIMIENTOS.unshift(...nuevos);
    return { movimientos: nuevos, cambios };
  });
