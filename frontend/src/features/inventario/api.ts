import { mockRequest } from '@/api/mock';
import { resumen } from './calculos';
import { MOVIMIENTOS, PRODUCTOS } from './fixtures';
import type {
  ConsultaDisponibilidad,
  Movimiento,
  ProductoDisponibilidad,
  RespuestaDisponibilidad,
} from './types';

const enAlerta = (nivel: string) => nivel !== 'Normal';

/** CU-04. Búsqueda, filtro y paginación se resuelven en el servidor (RNF-06); aquí los simula. */
export const consultarDisponibilidad = ({
  q,
  soloAlerta,
  page,
  pageSize,
}: ConsultaDisponibilidad) =>
  mockRequest<RespuestaDisponibilidad>(() => {
    const term = q.trim().toLowerCase();
    const found = PRODUCTOS.filter(
      (p) =>
        (!soloAlerta || enAlerta(resumen(p).nivel)) &&
        (!term || `${p.sku} ${p.nombre}`.toLowerCase().includes(term)),
    );
    return {
      items: found.slice((page - 1) * pageSize, page * pageSize),
      total: found.length,
      enAlerta: PRODUCTOS.filter((p) => enAlerta(resumen(p).nivel)).length,
      totalGeneral: PRODUCTOS.length,
    };
  });

/** CU-05 y CU-07. Movimientos de Kardex, del más reciente al más antiguo. */
export const listarMovimientos = () => mockRequest(() => [...MOVIMIENTOS]);

/** Filtros combinables del Kardex (P14). Un valor vacío no restringe la consulta. */
export interface FiltrosKardex {
  q: string;
  tipo: '' | Movimiento['tipo'];
  origen: '' | Movimiento['origen'];
  ubicacion: '' | Movimiento['ubicacion'];
  /** Fecha `aaaa-mm-dd`, inclusiva. */
  desde: string;
  /** Fecha `aaaa-mm-dd`, inclusiva. */
  hasta: string;
}

export const SIN_FILTROS_KARDEX: FiltrosKardex = {
  q: '',
  tipo: '',
  origen: '',
  ubicacion: '',
  desde: '',
  hasta: '',
};

/** P14. Movimientos que cumplen todos los filtros aplicados, del más reciente al más antiguo. */
export const listarMovimientosFiltrados = (filtros: FiltrosKardex) =>
  mockRequest<Movimiento[]>(() => {
    const term = filtros.q.trim().toLowerCase();
    return MOVIMIENTOS.filter(
      (m) =>
        (!term || `${m.sku} ${m.producto}`.toLowerCase().includes(term)) &&
        (!filtros.tipo || m.tipo === filtros.tipo) &&
        (!filtros.origen || m.origen === filtros.origen) &&
        (!filtros.ubicacion || m.ubicacion === filtros.ubicacion) &&
        (!filtros.desde || m.fecha >= filtros.desde) &&
        (!filtros.hasta || m.fecha <= filtros.hasta),
    );
  });

/** Producto en alerta: nivel según el stock libre y cantidad que falta para llegar al mínimo (P12). */
export interface Alerta extends ProductoDisponibilidad {
  libre: number;
  nivel: 'Critico' | 'Minimo';
  faltante: number;
}

/** P12. Productos en nivel crítico o en el mínimo, del más grave al menos grave y, dentro de cada nivel, del mayor faltante. */
export const listarAlertas = () =>
  mockRequest<Alerta[]>(() =>
    PRODUCTOS.flatMap((p) => {
      const { libre, nivel } = resumen(p);
      if (nivel === 'Normal') return [];
      return [{ ...p, libre, nivel, faltante: p.minimo - libre }];
    }).sort((a, b) => {
      if (a.nivel !== b.nivel) return a.nivel === 'Critico' ? -1 : 1;
      return b.faltante - a.faltante || a.sku.localeCompare(b.sku);
    }),
  );
