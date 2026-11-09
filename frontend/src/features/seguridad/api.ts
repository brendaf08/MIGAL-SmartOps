import { mockRequest } from '@/api/mock';
import { USUARIOS } from './fixtures';
import type { User } from './roles';

export type UsuarioResumen = User & { usuario: string };

/** CU-01. El mensaje no revela cuál de las dos credenciales falló. */
export const iniciarSesion = (usuario: string, clave: string): Promise<User> =>
  mockRequest(() => {
    const found = USUARIOS.find((u) => u.usuario === usuario && u.clave === clave);
    if (!found) throw new Error('Credenciales incorrectas');
    return { name: found.name, role: found.role };
  });

/** CU-02. Nunca expone las contraseñas. */
export const listarUsuarios = () =>
  mockRequest<UsuarioResumen[]>(() =>
    USUARIOS.map(({ usuario, name, role }) => ({ usuario, name, role })),
  );
