import { API_REAL, ApiError, api } from '@/api/cliente';
import { mockRequest } from '@/api/mock';
import { siguienteCodigo } from '@/lib/codigos';
import { PROVEEDORES } from '../fixtures';
import type { Proveedor } from '../tipos';

/** Datos que se registran o modifican; el identificador y el estado los asigna el servicio. */
export type DatosProveedor = Omit<Proveedor, 'id' | 'estado'>;

export interface FiltroProveedores {
  /** Razón social o RUC, sin distinguir mayúsculas. */
  q?: string;
  estado?: Proveedor['estado'];
}

/** Error de RUC duplicado (`Error('RUC_DUPLICADO')`) con el identificador del proveedor que ya lo usa. */
export class RucDuplicadoError extends Error {
  readonly proveedorId: string;

  constructor(proveedorId: string) {
    super('RUC_DUPLICADO');
    this.name = 'RucDuplicadoError';
    this.proveedorId = proveedorId;
  }
}

/** Proveedor tal como lo devuelve la API (contrato abastecimiento.yaml, esquema Proveedor). */
interface ProveedorApi {
  id: string;
  ruc: string;
  razonSocial: string;
  contacto: string;
  condiciones: string | null;
  estadoActivo: boolean;
  fechaRegistro: string;
}

/** Producto del catálogo del proveedor (esquema ProveedorCatalogoItem). */
interface CatalogoItemApi {
  sku: string;
  nombre: string;
  precioCompra: number;
  plazoEntregaReferencialDias: number | null;
}

interface PaginaApi<T> {
  items: T[];
  total: number;
  page: number;
  pageSize: number;
}

const TAMANO_PAGINA = 100;

/*
 * Adaptadores entre el contrato y la interfaz. La interfaz distingue telefono y correo, que el contrato no
 * modela por separado: se guardan junto con el contacto, separados por punto y coma. Al leer, el contacto
 * recibe el texto completo y los campos telefono y correo quedan vacíos.
 */
const aContacto = (datos: DatosProveedor): string =>
  [datos.contacto, datos.telefono, datos.correo]
    .map((valor) => valor.trim())
    .filter(Boolean)
    .join('; ');

const aRegistro = (datos: DatosProveedor) => ({
  ruc: datos.ruc,
  razonSocial: datos.razonSocial,
  contacto: aContacto(datos),
  condiciones: datos.condiciones.trim() || null,
});

const aProveedor = (proveedor: ProveedorApi, skus: string[]): Proveedor => ({
  id: proveedor.id,
  ruc: proveedor.ruc,
  razonSocial: proveedor.razonSocial,
  contacto: proveedor.contacto,
  telefono: '',
  correo: '',
  condiciones: proveedor.condiciones ?? '',
  skus,
  estado: proveedor.estadoActivo ? 'Activo' : 'Inactivo',
});

/** Traduce los códigos de error de la API a los errores que consume la interfaz. */
const traducirError = (error: unknown): never => {
  if (error instanceof ApiError) {
    if (error.codigo === 'RUC_DUPLICADO') {
      throw new RucDuplicadoError(String(error.extensiones.proveedorId ?? ''));
    }
    if (error.codigo === 'RECURSO_NO_ENCONTRADO') throw new Error('NO_ENCONTRADO');
  }
  throw error;
};

const consultarCatalogo = (id: string) => api.get<CatalogoItemApi[]>(`/proveedores/${id}/catalogo`);

/*
 * La interfaz solo gestiona los SKU del proveedor, pero el contrato exige el precio de compra en cada producto.
 * Los productos que ya existían conservan su precio y plazo; los nuevos se envían con precio 0 hasta que se
 * registre su precio en el catálogo del proveedor.
 */
const reemplazarCatalogo = async (id: string, skus: string[]): Promise<void> => {
  const vigente = new Map((await consultarCatalogo(id)).map((item) => [item.sku, item] as const));
  const productos = skus.map((sku) => {
    const previo = vigente.get(sku);
    return {
      sku,
      precioCompra: previo?.precioCompra ?? 0,
      plazoEntregaReferencialDias: previo?.plazoEntregaReferencialDias ?? null,
    };
  });
  await api.put(`/proveedores/${id}/catalogo`, { productos });
};

/** Recorre todas las páginas del listado de proveedores. */
const listarTodos = async (consulta: Record<string, string | boolean>): Promise<ProveedorApi[]> => {
  const proveedores: ProveedorApi[] = [];
  for (let page = 1; ; page += 1) {
    const pagina = await api.get<PaginaApi<ProveedorApi>>('/proveedores', {
      ...consulta,
      page,
      pageSize: TAMANO_PAGINA,
    });
    proveedores.push(...pagina.items);
    if (pagina.items.length === 0 || proveedores.length >= pagina.total) return proveedores;
  }
};

const listarProveedoresReal = async (filtro: FiltroProveedores): Promise<Proveedor[]> => {
  const consulta: Record<string, string | boolean> = {};
  const q = filtro.q?.trim();
  if (q) consulta.q = q;
  if (filtro.estado) consulta.estadoActivo = filtro.estado === 'Activo';

  const proveedores = await listarTodos(consulta).catch(traducirError);
  // El listado de la API no incluye los SKU; la columna de productos necesita el catálogo de cada proveedor.
  return Promise.all(
    proveedores.map(async (proveedor) => {
      const catalogo = await consultarCatalogo(proveedor.id).catch(traducirError);
      return aProveedor(
        proveedor,
        catalogo.map((item) => item.sku),
      );
    }),
  );
};

const obtenerProveedorReal = async (id: string): Promise<Proveedor> => {
  const proveedor = await api.get<ProveedorApi>(`/proveedores/${id}`).catch(traducirError);
  const catalogo = await consultarCatalogo(id).catch(traducirError);
  return aProveedor(
    proveedor,
    catalogo.map((item) => item.sku),
  );
};

const registrarProveedorReal = async (datos: DatosProveedor): Promise<Proveedor> => {
  try {
    const creado = await api.post<ProveedorApi>('/proveedores', aRegistro(datos));
    if (datos.skus.length > 0) await reemplazarCatalogo(creado.id, datos.skus);
    return aProveedor(creado, datos.skus);
  } catch (error) {
    return traducirError(error);
  }
};

const actualizarProveedorReal = async (id: string, datos: DatosProveedor): Promise<Proveedor> => {
  try {
    const actualizado = await api.put<ProveedorApi>(`/proveedores/${id}`, aRegistro(datos));
    await reemplazarCatalogo(id, datos.skus);
    return aProveedor(actualizado, datos.skus);
  } catch (error) {
    return traducirError(error);
  }
};

const cambiarEstadoProveedorReal = async (
  id: string,
  estado: Proveedor['estado'],
): Promise<Proveedor> => {
  try {
    const actualizado = await api.put<ProveedorApi>(`/proveedores/${id}/estado`, {
      estadoActivo: estado === 'Activo',
    });
    const catalogo = await consultarCatalogo(id);
    return aProveedor(
      actualizado,
      catalogo.map((item) => item.sku),
    );
  } catch (error) {
    return traducirError(error);
  }
};

/** Proveedores de prueba. Se modifican en memoria. */
export const listarProveedores = (filtro: FiltroProveedores = {}): Promise<Proveedor[]> =>
  API_REAL
    ? listarProveedoresReal(filtro)
    : mockRequest(() => {
        const term = (filtro.q ?? '').trim().toLowerCase();
        return PROVEEDORES.filter(
          (p) =>
            (!filtro.estado || p.estado === filtro.estado) &&
            (!term || `${p.razonSocial} ${p.ruc}`.toLowerCase().includes(term)),
        ).map((p) => ({ ...p, skus: [...p.skus] }));
      });

export const obtenerProveedor = (id: string): Promise<Proveedor> =>
  API_REAL
    ? obtenerProveedorReal(id)
    : mockRequest(() => {
        const proveedor = PROVEEDORES.find((p) => p.id === id);
        if (!proveedor) throw new Error('NO_ENCONTRADO');
        return { ...proveedor, skus: [...proveedor.skus] };
      });

/** CU-11. Falla con `RucDuplicadoError` si el RUC ya está registrado. */
export const registrarProveedor = (datos: DatosProveedor): Promise<Proveedor> =>
  API_REAL
    ? registrarProveedorReal(datos)
    : mockRequest(() => {
        const existente = PROVEEDORES.find((p) => p.ruc === datos.ruc);
        if (existente) throw new RucDuplicadoError(existente.id);
        const nuevo: Proveedor = {
          ...datos,
          skus: [...datos.skus],
          id: siguienteCodigo(
            'P',
            PROVEEDORES.map((p) => p.id),
            3,
          ),
          estado: 'Activo',
        };
        PROVEEDORES.push(nuevo);
        return { ...nuevo };
      });

export const actualizarProveedor = (id: string, datos: DatosProveedor): Promise<Proveedor> =>
  API_REAL
    ? actualizarProveedorReal(id, datos)
    : mockRequest(() => {
        const proveedor = PROVEEDORES.find((p) => p.id === id);
        if (!proveedor) throw new Error('NO_ENCONTRADO');
        const duplicado = PROVEEDORES.find((p) => p.ruc === datos.ruc && p.id !== id);
        if (duplicado) throw new RucDuplicadoError(duplicado.id);
        Object.assign(proveedor, { ...datos, skus: [...datos.skus] });
        return { ...proveedor, skus: [...proveedor.skus] };
      });

export const cambiarEstadoProveedor = (
  id: string,
  estado: Proveedor['estado'],
): Promise<Proveedor> =>
  API_REAL
    ? cambiarEstadoProveedorReal(id, estado)
    : mockRequest(() => {
        const proveedor = PROVEEDORES.find((p) => p.id === id);
        if (!proveedor) throw new Error('NO_ENCONTRADO');
        proveedor.estado = estado;
        return { ...proveedor, skus: [...proveedor.skus] };
      });
