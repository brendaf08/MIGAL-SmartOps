import type { UsuarioRegistro } from './usuarios-tipos';
import { ROLES, type Role } from './roles';

/** Usuario registrado según `UsuarioRegistrado` del contrato de autenticación y usuarios. Nunca incluye la clave. */
export interface UsuarioRegistradoDto {
  id: string;
  nombres: string;
  apellidos: string;
  username: string;
  rol: string;
  estadoActivo: boolean;
}

/** El backend envía el rol en minúsculas (ADR-008); la interfaz lo muestra con inicial mayúscula. */
export const rolDeApi = (rol: string): Role => {
  const encontrado = ROLES.find((r) => r.toLowerCase() === rol);
  if (!encontrado) throw new Error('Rol desconocido.');
  return encontrado;
};

/** Usuario de la API a la forma del directorio de la interfaz. */
export const aUsuarioRegistro = (dto: UsuarioRegistradoDto): UsuarioRegistro => ({
  id: dto.id,
  nombre: `${dto.nombres} ${dto.apellidos}`.trim(),
  usuario: dto.username,
  rol: rolDeApi(dto.rol),
  estado: dto.estadoActivo ? 'Activo' : 'Inactivo',
});
