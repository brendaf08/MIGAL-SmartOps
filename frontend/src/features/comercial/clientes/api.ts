import { API_REAL, ApiError, api } from '@/api/cliente';
import { mockRequest } from '@/api/mock';
import { siguienteCodigo } from '@/lib/codigos';
import { CLIENTES } from './fixtures';
import type { Cliente } from './tipos';

/** Datos que el usuario captura; el código y el estado los asigna el sistema. */
export type ClienteDatos = Pick<
  Cliente,
  'tipoDocumento' | 'documento' | 'nombre' | 'telefono' | 'correo'
>;

/** El documento es único. El error lleva el cliente existente para ofrecer su selección. */
export class DocumentoDuplicadoError extends Error {
  readonly cliente: Cliente;

  constructor(cliente: Cliente) {
    super('Ya existe un cliente con este documento.');
    this.name = 'DocumentoDuplicadoError';
    this.cliente = cliente;
  }
}

/** Forma de un cliente registrado en la API (esquema ClienteRegistrado de comercial.yaml). */
interface ClienteRegistradoApi {
  id: string;
  tipoDocumento: 'dni' | 'ruc' | null;
  numeroDocumento: string | null;
  nombreRazonSocial: string;
  telefonoContacto: string | null;
  direccionReferencia: string | null;
  estadoActivo: boolean;
  fechaRegistro: string;
}

/** Sobre de paginación de GET /clientes (ClientePagina). */
interface ClientePaginaApi {
  items: ClienteRegistradoApi[];
  total: number;
  page: number;
  pageSize: number;
}

/** Tamaño máximo de página admitido por la API (parámetro pageSize). */
const TAMANO_PAGINA = 100;

/**
 * Convierte un cliente de la API al tipo de la interfaz. El contrato no almacena correo electrónico, por lo que
 * el campo se devuelve vacío. Un cliente sin tipo de documento (mostrador) se presenta como DNI sin número.
 */
const clienteDesdeApi = (c: ClienteRegistradoApi): Cliente => ({
  id: c.id,
  tipoDocumento: c.tipoDocumento === 'ruc' ? 'RUC' : 'DNI',
  documento: c.numeroDocumento ?? '',
  nombre: c.nombreRazonSocial,
  telefono: c.telefonoContacto ?? '',
  correo: '',
  estado: c.estadoActivo ? 'Activo' : 'Inactivo',
});

/** Cuerpo de ClienteDatos (comercial.yaml). Los campos vacíos del formulario se envían como nulos. */
const cuerpoDesde = (datos: ClienteDatos) => ({
  tipoDocumento: datos.tipoDocumento === 'RUC' ? 'ruc' : 'dni',
  numeroDocumento: datos.documento,
  nombreRazonSocial: datos.nombre,
  telefonoContacto: datos.telefono || null,
});

/**
 * Traduce el error DOCUMENTO_CLIENTE_DUPLICADO a DocumentoDuplicadoError. La API solo devuelve el identificador del
 * cliente existente, por lo que se consulta su detalle. Cualquier otro error se propaga sin cambios.
 */
async function fallarConError(error: unknown): Promise<never> {
  if (
    error instanceof ApiError &&
    error.codigo === 'DOCUMENTO_CLIENTE_DUPLICADO' &&
    typeof error.extensiones.clienteId === 'string'
  ) {
    const existente = await api.get<ClienteRegistradoApi>(
      `/clientes/${encodeURIComponent(error.extensiones.clienteId)}`,
    );
    throw new DocumentoDuplicadoError(clienteDesdeApi(existente));
  }
  throw error;
}

/** Recorre todas las páginas de GET /clientes. La pantalla filtra por estado en el cliente. */
async function listarClientesReal(): Promise<Cliente[]> {
  const clientes: Cliente[] = [];
  let pagina = 1;
  for (;;) {
    const respuesta = await api.get<ClientePaginaApi>('/clientes', {
      page: pagina,
      pageSize: TAMANO_PAGINA,
    });
    clientes.push(...respuesta.items.map(clienteDesdeApi));
    if (respuesta.items.length === 0 || clientes.length >= respuesta.total) return clientes;
    pagina += 1;
  }
}

async function registrarClienteReal(datos: ClienteDatos): Promise<Cliente> {
  try {
    return clienteDesdeApi(await api.post<ClienteRegistradoApi>('/clientes', cuerpoDesde(datos)));
  } catch (error) {
    return fallarConError(error);
  }
}

async function actualizarClienteReal(id: string, datos: ClienteDatos): Promise<Cliente> {
  try {
    return clienteDesdeApi(
      await api.put<ClienteRegistradoApi>(`/clientes/${encodeURIComponent(id)}`, cuerpoDesde(datos)),
    );
  } catch (error) {
    return fallarConError(error);
  }
}

async function desactivarClienteReal(id: string): Promise<Cliente> {
  return clienteDesdeApi(
    await api.put<ClienteRegistradoApi>(`/clientes/${encodeURIComponent(id)}/estado`, {
      estadoActivo: false,
    }),
  );
}

const documentoOcupado = (documento: string, excluirId?: string) =>
  CLIENTES.find((c) => c.documento === documento && c.id !== excluirId);

/** CU-10. Lista completa, activos e inactivos; la pantalla filtra por estado. */
export const listarClientes = (): Promise<Cliente[]> =>
  API_REAL ? listarClientesReal() : mockRequest(() => [...CLIENTES]);

/** CU-10. Registra un cliente nuevo con estado activo. */
export const registrarCliente = (datos: ClienteDatos): Promise<Cliente> =>
  API_REAL
    ? registrarClienteReal(datos)
    : mockRequest<Cliente>(() => {
        const existente = documentoOcupado(datos.documento);
        if (existente) throw new DocumentoDuplicadoError(existente);
        const cliente: Cliente = {
          ...datos,
          id: siguienteCodigo(
            'CLI',
            CLIENTES.map((c) => c.id),
            3,
          ),
          estado: 'Activo',
        };
        CLIENTES.push(cliente);
        return cliente;
      });

/** Modifica los datos de un cliente existente; conserva su código y su estado. */
export const actualizarCliente = (id: string, datos: ClienteDatos): Promise<Cliente> =>
  API_REAL
    ? actualizarClienteReal(id, datos)
    : mockRequest<Cliente>(() => {
        const cliente = CLIENTES.find((c) => c.id === id);
        if (!cliente) throw new Error('El cliente no existe.');
        const existente = documentoOcupado(datos.documento, id);
        if (existente) throw new DocumentoDuplicadoError(existente);
        Object.assign(cliente, datos);
        return cliente;
      });

/** Desactiva un cliente: deja de ofrecerse en las ventas y conserva su historial. */
export const desactivarCliente = (id: string): Promise<Cliente> =>
  API_REAL
    ? desactivarClienteReal(id)
    : mockRequest<Cliente>(() => {
        const cliente = CLIENTES.find((c) => c.id === id);
        if (!cliente) throw new Error('El cliente no existe.');
        cliente.estado = 'Inactivo';
        return cliente;
      });
