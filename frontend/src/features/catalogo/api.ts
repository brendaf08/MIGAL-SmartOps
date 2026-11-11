import { API_REAL, api, ApiError } from '@/api/cliente';
import { mockRequest, type Page } from '@/api/mock';
import { aDisponibilidad, type ProductoExistenciasDto } from '@/features/inventario/adaptadores';
import { PRODUCTOS } from '@/features/inventario/fixtures';
import type { ProductoDisponibilidad } from '@/features/inventario/types';

export type EstadoProducto = 'Activo' | 'Inactivo';

/** Producto del catálogo maestro: datos de registro, sin existencias. */
export interface ProductoCatalogo {
  /** Identificador técnico del producto. La interfaz navega por SKU y lo usa solo en las operaciones de escritura. */
  id: string;
  sku: string;
  nombre: string;
  descripcion: string;
  categoria: string;
  unidad: string;
  minimo: number;
  precioReferencial: number;
  estado: EstadoProducto;
}

/** Datos que el formulario envía al registrar o modificar un producto. */
export type DatosProducto = Omit<ProductoCatalogo, 'id' | 'estado'> & { estado: EstadoProducto };

export interface ConsultaCatalogo {
  q: string;
  categoria: string;
  estado: EstadoProducto | '';
  page: number;
  pageSize: number;
}

/** Error de negocio: el SKU ya existe en el catálogo (flujo alternativo 1.1 de CU-03). */
export class SkuDuplicadoError extends Error {
  constructor() {
    super('El SKU ya existe. Ingresar un código distinto.');
    this.name = 'SkuDuplicadoError';
  }
}

/** Datos que el catálogo conserva fuera de `PRODUCTOS` (descripción y estado), por SKU. */
const DATOS_EXTRA = new Map<string, { descripcion: string; estado: EstadoProducto }>();

const extraDe = (sku: string) => DATOS_EXTRA.get(sku) ?? { descripcion: '', estado: 'Activo' as const };

const aCatalogo = (p: ProductoDisponibilidad): ProductoCatalogo => {
  const extra = extraDe(p.sku);
  return {
    id: p.sku,
    sku: p.sku,
    nombre: p.nombre,
    descripcion: extra.descripcion,
    categoria: p.categoria,
    unidad: p.unidad,
    minimo: p.minimo,
    precioReferencial: 0,
    estado: extra.estado,
  };
};

/* Adaptadores de la API real. Traducen el contrato de catálogo (idCategoria, nombreOficial, estadoActivo, etc.) al
 * formato de la interfaz (categoría por nombre, nombre, estado como texto). */

/** Producto según `Producto` del contrato de catálogo. */
interface ProductoDto {
  id: string;
  sku: string;
  nombreOficial: string;
  descripcion: string | null;
  unidadMedida: string;
  precioReferencial: number;
  stockMinimo: number;
  estadoActivo: boolean;
  idCategoria: string;
}

/** Categoría según `Categoria` del contrato de catálogo. */
interface CategoriaDto {
  id: string;
  nombre: string;
}

/** Página de la API con el sobre de paginación común. */
interface PaginaDto<T> {
  items: T[];
  total: number;
  page: number;
  pageSize: number;
}

const estadoDe = (activo: boolean): EstadoProducto => (activo ? 'Activo' : 'Inactivo');

/** Traduce la lista de categorías en un mapa de identificador a nombre. */
const nombresPorId = (categorias: CategoriaDto[]) => new Map(categorias.map((c) => [c.id, c.nombre]));

/** Identificador de la categoría con el nombre indicado, o `undefined` si no existe. */
const idDeCategoria = (categorias: CategoriaDto[], nombre: string) =>
  categorias.find((c) => c.nombre === nombre)?.id;

const aProductoCatalogo = (dto: ProductoDto, categorias: CategoriaDto[]): ProductoCatalogo => ({
  id: dto.id,
  sku: dto.sku,
  nombre: dto.nombreOficial,
  descripcion: dto.descripcion ?? '',
  categoria: nombresPorId(categorias).get(dto.idCategoria) ?? '',
  unidad: dto.unidadMedida,
  minimo: dto.stockMinimo,
  precioReferencial: dto.precioReferencial,
  estado: estadoDe(dto.estadoActivo),
});

/** Busca el producto por SKU exacto. Devuelve `null` si no existe. */
const productoDtoPorSku = async (sku: string): Promise<ProductoDto | null> => {
  const pagina = await api.get<PaginaDto<ProductoDto>>('/productos', { sku, pageSize: 1 });
  return pagina.items.find((p) => p.sku === sku) ?? null;
};

const listarCategoriasApi = () => api.get<CategoriaDto[]>('/categorias');

const buscarCatalogoReal = async (consulta: ConsultaCatalogo): Promise<Page<ProductoCatalogo>> => {
  const categorias = await listarCategoriasApi();
  const idCategoria = consulta.categoria ? idDeCategoria(categorias, consulta.categoria) : undefined;
  if (consulta.categoria && !idCategoria) return { items: [], total: 0 };
  const pagina = await api.get<PaginaDto<ProductoDto>>('/productos', {
    q: consulta.q.trim(),
    idCategoria,
    estadoActivo: consulta.estado === '' ? undefined : consulta.estado === 'Activo',
    page: consulta.page,
    pageSize: consulta.pageSize,
  });
  return {
    items: pagina.items.map((p) => aProductoCatalogo(p, categorias)),
    total: pagina.total,
  };
};

/** Cuerpo de `ProductoRegistro` y `ProductoActualizacion`. La marca y la presentación no se editan en la interfaz y se omiten. */
const cuerpoProducto = (datos: DatosProducto, categorias: CategoriaDto[]) => {
  const idCategoria = idDeCategoria(categorias, datos.categoria);
  if (!idCategoria) throw new Error('La categoría seleccionada no existe.');
  return {
    nombreOficial: datos.nombre,
    descripcion: datos.descripcion || null,
    unidadMedida: datos.unidad,
    precioReferencial: datos.precioReferencial,
    stockMinimo: datos.minimo,
    idCategoria,
  };
};

const registrarProductoReal = async (datos: DatosProducto): Promise<ProductoCatalogo> => {
  const categorias = await listarCategoriasApi();
  try {
    const creado = await api.post<ProductoDto>('/productos', {
      sku: datos.sku,
      ...cuerpoProducto(datos, categorias),
      estadoActivo: datos.estado === 'Activo',
    });
    return aProductoCatalogo(creado, categorias);
  } catch (error) {
    if (error instanceof ApiError && error.codigo === 'SKU_DUPLICADO') throw new SkuDuplicadoError();
    throw error;
  }
};

const actualizarProductoReal = async (sku: string, datos: DatosProducto): Promise<ProductoCatalogo> => {
  const actual = await productoDtoPorSku(sku);
  if (!actual) throw new Error('El producto no existe.');
  const categorias = await listarCategoriasApi();
  const ruta = `/productos/${encodeURIComponent(actual.id)}`;
  let producto = await api.put<ProductoDto>(ruta, cuerpoProducto(datos, categorias));
  // El estado de vigencia no forma parte de PUT /productos/{id}: se cambia con su operación propia cuando difiere.
  if (estadoDe(actual.estadoActivo) !== datos.estado) {
    producto = await api.put<ProductoDto>(`${ruta}/estado`, { estadoActivo: datos.estado === 'Activo' });
  }
  return aProductoCatalogo(producto, categorias);
};

/** CU-03. Búsqueda por nombre o SKU, filtros por categoría y estado, y paginación en memoria. */
export const buscarCatalogo = (consulta: ConsultaCatalogo): Promise<Page<ProductoCatalogo>> =>
  API_REAL
    ? buscarCatalogoReal(consulta)
    : mockRequest<Page<ProductoCatalogo>>(() => {
        const term = consulta.q.trim().toLowerCase();
        const filtrados = PRODUCTOS.map(aCatalogo).filter(
          (p) =>
            (!term || `${p.sku} ${p.nombre}`.toLowerCase().includes(term)) &&
            (!consulta.categoria || p.categoria === consulta.categoria) &&
            (!consulta.estado || p.estado === consulta.estado),
        );
        const inicio = (consulta.page - 1) * consulta.pageSize;
        return {
          items: filtrados.slice(inicio, inicio + consulta.pageSize),
          total: filtrados.length,
        };
      });

/** Producto por SKU, o `null` si no existe. */
export const obtenerProducto = (sku: string): Promise<ProductoCatalogo | null> =>
  API_REAL
    ? productoDtoPorSku(sku).then(async (dto) =>
        dto ? aProductoCatalogo(dto, await listarCategoriasApi()) : null,
      )
    : mockRequest<ProductoCatalogo | null>(() => {
        const producto = PRODUCTOS.find((p) => p.sku === sku);
        return producto ? aCatalogo(producto) : null;
      });

/** Existencias por ubicación de un producto (CU-04), o `null` si no existe. */
export const obtenerExistencias = (sku: string): Promise<ProductoDisponibilidad | null> =>
  API_REAL
    ? productoDtoPorSku(sku).then(async (dto) => {
        if (!dto) return null;
        const existencias = await api.get<ProductoExistenciasDto>(
          `/productos/${encodeURIComponent(dto.id)}/existencias`,
        );
        return aDisponibilidad(existencias);
      })
    : mockRequest<ProductoDisponibilidad | null>(() => PRODUCTOS.find((p) => p.sku === sku) ?? null);

/** Categorías presentes en el catálogo, en orden alfabético. */
export const listarCategorias = (): Promise<string[]> =>
  API_REAL
    ? listarCategoriasApi().then((categorias) =>
        categorias.map((c) => c.nombre).sort((a, b) => a.localeCompare(b, 'es')),
      )
    : mockRequest(() => [...new Set(PRODUCTOS.map((p) => p.categoria))].sort((a, b) => a.localeCompare(b, 'es')));

/** Registra un producto nuevo. Sin existencias iniciales: las cantidades se registran por recepción. */
export const registrarProducto = (datos: DatosProducto): Promise<ProductoCatalogo> =>
  API_REAL
    ? registrarProductoReal(datos)
    : mockRequest(() => {
        if (PRODUCTOS.some((p) => p.sku === datos.sku)) throw new SkuDuplicadoError();
        PRODUCTOS.push({
          sku: datos.sku,
          nombre: datos.nombre,
          categoria: datos.categoria,
          unidad: datos.unidad,
          tienda: 0,
          almacen1: 0,
          almacen2: 0,
          reservado: 0,
          minimo: datos.minimo,
        });
        DATOS_EXTRA.set(datos.sku, { descripcion: datos.descripcion, estado: datos.estado });
        return aCatalogo(PRODUCTOS[PRODUCTOS.length - 1]!);
      });

/** Modifica un producto existente. El SKU no cambia. */
export const actualizarProducto = (sku: string, datos: DatosProducto): Promise<ProductoCatalogo> =>
  API_REAL
    ? actualizarProductoReal(sku, datos)
    : mockRequest(() => {
        const producto = PRODUCTOS.find((p) => p.sku === sku);
        if (!producto) throw new Error('El producto no existe.');
        Object.assign(producto, {
          nombre: datos.nombre,
          categoria: datos.categoria,
          unidad: datos.unidad,
          minimo: datos.minimo,
        });
        DATOS_EXTRA.set(sku, { descripcion: datos.descripcion, estado: datos.estado });
        return aCatalogo(producto);
      });
