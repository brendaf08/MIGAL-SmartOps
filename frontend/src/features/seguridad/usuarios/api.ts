import { API_REAL, api } from '@/api/cliente';
import { mockRequest } from '@/api/mock';
import { todosLosElementos, type PaginaApi } from '@/api/paginas';
import { aUsuarioRegistro, type UsuarioRegistradoDto } from '@/features/seguridad/adaptadores';
import { USUARIOS_REGISTRO, type UsuarioRegistro } from '@/features/seguridad/usuarios-tipos';
import type { Role } from '@/features/seguridad/roles';

/** Datos que el formulario envía al registrar o editar un usuario. */
export interface DatosUsuario {
  nombre: string;
  rol: Role;
  usuario?: string;
  clave?: string;
}

/** CU-02. Las operaciones modifican `USUARIOS_REGISTRO` en memoria hasta contar con la API. */
export const listarUsuarios = (): Promise<UsuarioRegistro[]> =>
  API_REAL
    ? todosLosElementos((page, pageSize) =>
        api.get<PaginaApi<UsuarioRegistradoDto>>('/usuarios', { page, pageSize }),
      ).then((usuarios) => usuarios.map(aUsuarioRegistro))
    : mockRequest(() => USUARIOS_REGISTRO.map((u) => ({ ...u })));

export const obtenerUsuario = (id: string) =>
  mockRequest<UsuarioRegistro>(() => {
    const usuario = USUARIOS_REGISTRO.find((u) => u.id === id);
    if (!usuario) throw new Error('USUARIO_NO_ENCONTRADO');
    return { ...usuario };
  });

export const registrarUsuario = (datos: DatosUsuario) =>
  mockRequest<UsuarioRegistro>(() => {
    if (USUARIOS_REGISTRO.some((u) => u.usuario === datos.usuario)) {
      throw new Error('USUARIO_DUPLICADO');
    }
    const nuevo: UsuarioRegistro = {
      id: `U-${String(USUARIOS_REGISTRO.length + 1).padStart(3, '0')}`,
      nombre: datos.nombre,
      usuario: datos.usuario ?? '',
      rol: datos.rol,
      estado: 'Activo',
    };
    USUARIOS_REGISTRO.push(nuevo);
    return { ...nuevo };
  });

export const actualizarUsuario = (id: string, datos: Pick<DatosUsuario, 'nombre' | 'rol'>) =>
  mockRequest<UsuarioRegistro>(() => {
    const usuario = USUARIOS_REGISTRO.find((u) => u.id === id);
    if (!usuario) throw new Error('USUARIO_NO_ENCONTRADO');
    usuario.nombre = datos.nombre;
    usuario.rol = datos.rol;
    return { ...usuario };
  });

/** CU-02, flujo alternativo 3. La revocación es lógica: el usuario conserva su registro. */
export const revocarAcceso = (id: string): Promise<UsuarioRegistro> =>
  API_REAL
    ? api
        .put<UsuarioRegistradoDto>(`/usuarios/${encodeURIComponent(id)}/estado`, { estadoActivo: false })
        .then(aUsuarioRegistro)
    : mockRequest<UsuarioRegistro>(() => {
    const usuario = USUARIOS_REGISTRO.find((u) => u.id === id);
    if (!usuario) throw new Error('USUARIO_NO_ENCONTRADO');
    usuario.estado = 'Inactivo';
    return { ...usuario };
  });
