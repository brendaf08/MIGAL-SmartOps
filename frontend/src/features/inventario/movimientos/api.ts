import { mockRequest } from '@/api/mock';
import type { CambioStock } from '@/components/domain/Constancia/Constancia';
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

/** Datos validados del formulario, con el responsable de la sesión. */
export interface DatosMovimiento {
  tipo: TipoMovimiento;
  sku: string;
  ubicacion: string;
  origen: string;
  destino: string;
  cantidad: number;
  motivo: string;
  usuario: string;
}

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
export const registrarMovimiento = (datos: DatosMovimiento) =>
  mockRequest<ResultadoMovimiento>(() => {
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
