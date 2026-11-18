import { API_REAL, ApiError, api, borrarToken, guardarToken } from '@/api/cliente';
import { mockRequest } from '@/api/mock';
import { todosLosElementos, type PaginaApi } from '@/api/paginas';
import { USUARIOS } from './fixtures';
import { rolDeApi, type UsuarioRegistradoDto } from './adaptadores';
import type { User } from './roles';

interface RespuestaSesion {
  token: string;
  usuario: { nombres: string; apellidos: string; rol: string };
}

const iniciarSesionReal = async (usuario: string, clave: string): Promise<User> => {
  try {
    const sesion = await api.post<RespuestaSesion>('/sesiones', { username: usuario, clave });
    guardarToken(sesion.token);
    return {
      name: `${sesion.usuario.nombres} ${sesion.usuario.apellidos}`.trim(),
      role: rolDeApi(sesion.usuario.rol),
    };
  } catch (error) {
    // Una cuenta desactivada o el exceso de intentos se informan con el detalle del backend; un servidor caído no se
    // confunde con una clave incorrecta. Una credencial incorrecta no revela cuál de las dos falló.
    if (error instanceof ApiError) {
      if (error.codigo === 'CUENTA_INACTIVA' || error.codigo === 'DEMASIADOS_INTENTOS') {
        throw new Error(error.message);
      }
      if (error.estado === 0 || error.estado >= 500) {
        throw new Error(
          'No fue posible comunicarse con el servidor. Intentar de nuevo en unos minutos.',
        );
      }
    }
    throw new Error('Credenciales incorrectas');
  }
};

/** Cierra la sesión local y descarta el token. */
export const cerrarSesion = (): void => borrarToken();

export type UsuarioResumen = User & { usuario: string };

/** CU-01. El mensaje no revela cuál de las dos credenciales falló. */
export const iniciarSesion = (usuario: string, clave: string): Promise<User> =>
  API_REAL
    ? iniciarSesionReal(usuario, clave)
    : mockRequest(() => {
        const found = USUARIOS.find((u) => u.usuario === usuario && u.clave === clave);
        if (!found) throw new Error('Credenciales incorrectas');
        return { name: found.name, role: found.role };
      });

/** CU-02. Nunca expone las contraseñas. */
export const listarUsuarios = (): Promise<UsuarioResumen[]> =>
  API_REAL
    ? todosLosElementos((page, pageSize) =>
        api.get<PaginaApi<UsuarioRegistradoDto>>('/usuarios', { page, pageSize }),
      ).then((usuarios) =>
        usuarios.map((u) => ({
          usuario: u.username,
          name: `${u.nombres} ${u.apellidos}`.trim(),
          role: rolDeApi(u.rol),
        })),
      )
    : mockRequest<UsuarioResumen[]>(() =>
        USUARIOS.map(({ usuario, name, role }) => ({ usuario, name, role })),
      );

/** Cambia la clave del usuario de la sesión (PUT /sesiones/actual/clave). */
export const cambiarClave = (claveActual: string, claveNueva: string): Promise<void> =>
  API_REAL
    ? api.put<void>('/sesiones/actual/clave', { claveActual, claveNueva })
    : mockRequest(() => {
        if (claveActual === claveNueva) {
          throw new ApiError(
            422,
            'VALIDACION_FALLIDA',
            'La nueva contraseña debe ser distinta de la actual.',
            [
              {
                campo: 'claveNueva',
                codigo: 'VALIDACION_FALLIDA',
                mensaje: 'La nueva contraseña debe ser distinta de la actual.',
              },
            ],
          );
        }
      });
