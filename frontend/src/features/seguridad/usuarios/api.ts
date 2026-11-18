import { API_REAL, ApiError, api } from '@/api/cliente';
import { mockRequest } from '@/api/mock';
import { todosLosElementos, type PaginaApi } from '@/api/paginas';
import { aUsuarioRegistro, type UsuarioRegistradoDto } from '@/features/seguridad/adaptadores';
import { USUARIOS_REGISTRO, type UsuarioRegistro } from '@/features/seguridad/usuarios-tipos';
import type { Role } from '@/features/seguridad/roles';

/** Datos que el formulario envía al registrar o editar un usuario (UsuarioNuevo y UsuarioActualizacion). */
export interface DatosUsuario {
  nombres: string;
  apellidos: string;
  rol: Role;
  usuario?: string;
  clave?: string;
}

const ruta = (id: string) => `/usuarios/${encodeURIComponent(id)}`;

/** El backend espera el rol en minúsculas (ADR-008). */
const rolApi = (rol: Role) => rol.toLowerCase();

/** Un nombre de usuario repetido llega como USUARIO_DUPLICADO (409); el formulario lo muestra en su campo. */
const traducirDuplicado = (error: unknown): never => {
  if (error instanceof ApiError && error.codigo === 'USUARIO_DUPLICADO') {
    throw new Error('USUARIO_DUPLICADO');
  }
  throw error;
};

const buscarSimulado = (id: string) => {
  const usuario = USUARIOS_REGISTRO.find((u) => u.id === id);
  if (!usuario) throw new Error('USUARIO_NO_ENCONTRADO');
  return usuario;
};

/** CU-02. Con los datos simulados, las operaciones modifican `USUARIOS_REGISTRO` en memoria. */
export const listarUsuarios = (): Promise<UsuarioRegistro[]> =>
  API_REAL
    ? todosLosElementos((page, pageSize) =>
        api.get<PaginaApi<UsuarioRegistradoDto>>('/usuarios', { page, pageSize }),
      ).then((usuarios) => usuarios.map(aUsuarioRegistro))
    : mockRequest(() => USUARIOS_REGISTRO.map((u) => ({ ...u })));

export const obtenerUsuario = (id: string): Promise<UsuarioRegistro> =>
  API_REAL
    ? api.get<UsuarioRegistradoDto>(ruta(id)).then(aUsuarioRegistro)
    : mockRequest(() => ({ ...buscarSimulado(id) }));

export const registrarUsuario = (datos: DatosUsuario): Promise<UsuarioRegistro> =>
  API_REAL
    ? api
        .post<UsuarioRegistradoDto>('/usuarios', {
          nombres: datos.nombres.trim(),
          apellidos: datos.apellidos.trim(),
          username: datos.usuario ?? '',
          claveTemporal: datos.clave ?? '',
          rol: rolApi(datos.rol),
        })
        .then(aUsuarioRegistro, traducirDuplicado)
    : mockRequest(() => {
        if (USUARIOS_REGISTRO.some((u) => u.usuario === datos.usuario)) {
          throw new Error('USUARIO_DUPLICADO');
        }
        const nuevo: UsuarioRegistro = {
          id: `U-${String(USUARIOS_REGISTRO.length + 1).padStart(3, '0')}`,
          nombre: `${datos.nombres.trim()} ${datos.apellidos.trim()}`,
          nombres: datos.nombres.trim(),
          apellidos: datos.apellidos.trim(),
          usuario: datos.usuario ?? '',
          rol: datos.rol,
          estado: 'Activo',
        };
        USUARIOS_REGISTRO.push(nuevo);
        return { ...nuevo };
      });

export const actualizarUsuario = (
  id: string,
  datos: Pick<DatosUsuario, 'nombres' | 'apellidos' | 'rol'>,
): Promise<UsuarioRegistro> =>
  API_REAL
    ? api
        .put<UsuarioRegistradoDto>(ruta(id), {
          nombres: datos.nombres.trim(),
          apellidos: datos.apellidos.trim(),
          rol: rolApi(datos.rol),
        })
        .then(aUsuarioRegistro)
    : mockRequest(() => {
        const usuario = buscarSimulado(id);
        usuario.nombres = datos.nombres.trim();
        usuario.apellidos = datos.apellidos.trim();
        usuario.nombre = `${usuario.nombres} ${usuario.apellidos}`;
        usuario.rol = datos.rol;
        return { ...usuario };
      });

const cambiarEstado = (id: string, estadoActivo: boolean): Promise<UsuarioRegistro> =>
  API_REAL
    ? api.put<UsuarioRegistradoDto>(`${ruta(id)}/estado`, { estadoActivo }).then(aUsuarioRegistro)
    : mockRequest(() => {
        const usuario = buscarSimulado(id);
        usuario.estado = estadoActivo ? 'Activo' : 'Inactivo';
        return { ...usuario };
      });

/** CU-02, flujo alternativo 3. La revocación es lógica: el usuario conserva su registro. */
export const revocarAcceso = (id: string) => cambiarEstado(id, false);

/** Devuelve el acceso a un usuario revocado (PUT /usuarios/{id}/estado con estadoActivo verdadero). */
export const reactivarAcceso = (id: string) => cambiarEstado(id, true);
