import { API_REAL, api, borrarToken, guardarToken } from '@/api/cliente';
import { mockRequest } from '@/api/mock';
import { USUARIOS } from './fixtures';
import { ROLES, type Role, type User } from './roles';

interface RespuestaSesion {
  token: string;
  usuario: { nombres: string; apellidos: string; rol: string };
}

/** El backend envía el rol en minúsculas (ADR-008); la interfaz lo muestra con inicial mayúscula. */
const rolDeApi = (rol: string): Role => {
  const encontrado = ROLES.find((r) => r.toLowerCase() === rol);
  if (!encontrado) throw new Error('Rol desconocido.');
  return encontrado;
};

const iniciarSesionReal = async (usuario: string, clave: string): Promise<User> => {
  try {
    const sesion = await api.post<RespuestaSesion>('/sesiones', { username: usuario, clave });
    guardarToken(sesion.token);
    return {
      name: `${sesion.usuario.nombres} ${sesion.usuario.apellidos}`.trim(),
      role: rolDeApi(sesion.usuario.rol),
    };
  } catch {
    // El mensaje no revela cuál de las dos credenciales falló, ni el motivo del rechazo.
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
export const listarUsuarios = () =>
  mockRequest<UsuarioResumen[]>(() =>
    USUARIOS.map(({ usuario, name, role }) => ({ usuario, name, role })),
  );
