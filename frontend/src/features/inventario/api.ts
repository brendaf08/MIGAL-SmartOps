import { API_REAL, api } from '@/api/cliente';
import { mockRequest } from '@/api/mock';
import { todosLosElementos, type PaginaApi } from '@/api/paginas';
import {
  aDisponibilidad,
  aMovimiento,
  type AlmacenDto,
  type MovimientoKardexDto,
  type ProductoExistenciasDto,
} from './adaptadores';
import { resumen } from './calculos';
import { MOVIMIENTOS, PRODUCTOS } from './fixtures';
import type {
  ConsultaDisponibilidad,
  Movimiento,
  ProductoDisponibilidad,
  RespuestaDisponibilidad,
} from './types';

const enAlerta = (nivel: string) => nivel !== 'Normal';

/* Forma de los datos del contrato de inventario que usan estas funciones. */

interface DisponibilidadPaginaDto extends PaginaApi<ProductoExistenciasDto> {
  enAlerta: number;
  totalGeneral: number;
}

interface AlertaStockDto {
  productoId: string;
  sku: string;
  nombre: string;
  unidad: string;
  stockMinimo: number;
  libre: number;
  faltante: number;
  nivelAlerta: 'normal' | 'minimo' | 'critico';
}

/** Ubicaciones (tienda principal y almacenes) del contrato de catálogo. */
export const listarAlmacenes = () => api.get<AlmacenDto[]>('/almacenes');

/** Identificador técnico del producto con el SKU indicado, o `null` si no existe. */
export const buscarIdProducto = async (sku: string): Promise<string | null> => {
  const pagina = await api.get<PaginaApi<{ id: string; sku: string }>>('/productos', { sku, pageSize: 1 });
  return pagina.items.find((p) => p.sku === sku)?.id ?? null;
};

/** Identificador técnico del producto con el SKU indicado. Lanza un error si el producto no existe. */
export const productoIdDeSku = async (sku: string): Promise<string> => {
  const id = await buscarIdProducto(sku);
  if (!id) throw new Error('El producto no existe en el catálogo.');
  return id;
};

const consultarDisponibilidadReal = async ({
  q,
  soloAlerta,
  page,
  pageSize,
}: ConsultaDisponibilidad): Promise<RespuestaDisponibilidad> => {
  const pagina = await api.get<DisponibilidadPaginaDto>('/inventario/disponibilidad', {
    q: q.trim(),
    soloAlerta,
    page,
    pageSize,
  });
  return {
    items: pagina.items.map(aDisponibilidad),
    total: pagina.total,
    enAlerta: pagina.enAlerta,
    totalGeneral: pagina.totalGeneral,
  };
};

/** CU-04. Búsqueda, filtro y paginación se resuelven en el servidor (RNF-06); aquí los simula. */
export const consultarDisponibilidad = (consulta: ConsultaDisponibilidad) =>
  API_REAL
    ? consultarDisponibilidadReal(consulta)
    : mockRequest<RespuestaDisponibilidad>(() => {
        const term = consulta.q.trim().toLowerCase();
        const found = PRODUCTOS.filter(
          (p) =>
            (!consulta.soloAlerta || enAlerta(resumen(p).nivel)) &&
            (!term || `${p.sku} ${p.nombre}`.toLowerCase().includes(term)),
        );
        return {
          items: found.slice((consulta.page - 1) * consulta.pageSize, consulta.page * consulta.pageSize),
          total: found.length,
          enAlerta: PRODUCTOS.filter((p) => enAlerta(resumen(p).nivel)).length,
          totalGeneral: PRODUCTOS.length,
        };
      });

/** CU-05 y CU-07. Movimientos de Kardex, del más reciente al más antiguo. */
export const listarMovimientos = () =>
  API_REAL ? listarMovimientosFiltrados(SIN_FILTROS_KARDEX) : mockRequest(() => [...MOVIMIENTOS]);

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

/**
 * P14. Movimientos que cumplen todos los filtros aplicados, del más reciente al más antiguo. La interfaz pagina en
 * cliente sobre el resultado completo, por lo que la rama real recorre todas las páginas del contrato.
 */
export const listarMovimientosFiltrados = (filtros: FiltrosKardex): Promise<Movimiento[]> =>
  API_REAL
    ? listarMovimientosFiltradosReal(filtros)
    : mockRequest<Movimiento[]>(() => {
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

const listarMovimientosFiltradosReal = async (filtros: FiltrosKardex): Promise<Movimiento[]> => {
  const almacenes = await listarAlmacenes();
  // La interfaz filtra por el nombre de la ubicación; el contrato filtra por identificador.
  const ubicacionId = filtros.ubicacion ? almacenes.find((a) => a.nombre === filtros.ubicacion)?.id : undefined;
  if (filtros.ubicacion && !ubicacionId) return [];
  const nombres = new Map(almacenes.map((a) => [a.id, a.nombre]));
  const movimientos = await todosLosElementos((page, pageSize) =>
    api.get<PaginaApi<MovimientoKardexDto>>('/inventario/movimientos', {
      q: filtros.q.trim(),
      ubicacionId,
      tipoOperacion: filtros.tipo ? filtros.tipo.toLowerCase() : undefined,
      origen: filtros.origen ? filtros.origen.toLowerCase() : undefined,
      desde: filtros.desde,
      hasta: filtros.hasta,
      page,
      pageSize,
    }),
  );
  return movimientos.map((m) => aMovimiento(m, nombres));
};

/** Existencias de un producto por SKU para el formulario de movimientos, o `null` si no existe. */
export const obtenerDisponibilidadProducto = (sku: string): Promise<ProductoDisponibilidad | null> =>
  API_REAL
    ? buscarIdProducto(sku).then(async (id) => {
        if (!id) return null;
        return aDisponibilidad(await api.get<ProductoExistenciasDto>(`/productos/${encodeURIComponent(id)}/existencias`));
      })
    : mockRequest<ProductoDisponibilidad | null>(() => PRODUCTOS.find((p) => p.sku === sku) ?? null);

/** Producto en alerta: nivel según el stock libre y cantidad que falta para llegar al mínimo (P12). */
export interface Alerta extends ProductoDisponibilidad {
  libre: number;
  nivel: 'Critico' | 'Minimo';
  faltante: number;
}

/**
 * Alertas de la API: la lista y el orden provienen de `/inventario/alertas`. Las existencias por producto (reservado,
 * ubicaciones) no figuran en ese contrato, por lo que se obtienen de la disponibilidad filtrada por alerta y se cruzan
 * por SKU.
 */
const listarAlertasReal = async (): Promise<Alerta[]> => {
  const [alertas, disponibles] = await Promise.all([
    todosLosElementos((page, pageSize) =>
      api.get<PaginaApi<AlertaStockDto>>('/inventario/alertas', { page, pageSize }),
    ),
    todosLosElementos((page, pageSize) =>
      api.get<DisponibilidadPaginaDto>('/inventario/disponibilidad', { soloAlerta: true, page, pageSize }),
    ),
  ]);
  const porSku = new Map(disponibles.map((d) => [d.sku, aDisponibilidad(d)]));
  return alertas.flatMap((a) => {
    const nivel: Alerta['nivel'] | null =
      a.nivelAlerta === 'critico' ? 'Critico' : a.nivelAlerta === 'minimo' ? 'Minimo' : null;
    if (!nivel) return [];
    const base: ProductoDisponibilidad = porSku.get(a.sku) ?? {
      sku: a.sku,
      nombre: a.nombre,
      categoria: '',
      unidad: a.unidad,
      tienda: 0,
      almacen1: 0,
      almacen2: 0,
      reservado: 0,
      minimo: a.stockMinimo,
    };
    return [{ ...base, libre: a.libre, nivel, faltante: a.faltante }];
  });
};

/** P12. Productos en nivel crítico o en el mínimo, del más grave al menos grave y, dentro de cada nivel, del mayor faltante. */
export const listarAlertas = (): Promise<Alerta[]> =>
  API_REAL
    ? listarAlertasReal()
    : mockRequest<Alerta[]>(() =>
        PRODUCTOS.flatMap((p) => {
          const { libre, nivel } = resumen(p);
          if (nivel === 'Normal') return [];
          return [{ ...p, libre, nivel, faltante: p.minimo - libre }];
        }).sort((a, b) => {
          if (a.nivel !== b.nivel) return a.nivel === 'Critico' ? -1 : 1;
          return b.faltante - a.faltante || a.sku.localeCompare(b.sku);
        }),
      );
