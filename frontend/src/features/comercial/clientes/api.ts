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

const documentoOcupado = (documento: string, excluirId?: string) =>
  CLIENTES.find((c) => c.documento === documento && c.id !== excluirId);

/** CU-10. Lista completa, activos e inactivos; la pantalla filtra por estado. */
export const listarClientes = () => mockRequest(() => [...CLIENTES]);

/** CU-10. Registra un cliente nuevo con estado activo. */
export const registrarCliente = (datos: ClienteDatos) =>
  mockRequest<Cliente>(() => {
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
export const actualizarCliente = (id: string, datos: ClienteDatos) =>
  mockRequest<Cliente>(() => {
    const cliente = CLIENTES.find((c) => c.id === id);
    if (!cliente) throw new Error('El cliente no existe.');
    const existente = documentoOcupado(datos.documento, id);
    if (existente) throw new DocumentoDuplicadoError(existente);
    Object.assign(cliente, datos);
    return cliente;
  });

/** Desactiva un cliente: deja de ofrecerse en las ventas y conserva su historial. */
export const desactivarCliente = (id: string) =>
  mockRequest<Cliente>(() => {
    const cliente = CLIENTES.find((c) => c.id === id);
    if (!cliente) throw new Error('El cliente no existe.');
    cliente.estado = 'Inactivo';
    return cliente;
  });
