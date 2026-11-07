import type { CodigoPantalla, Rol } from '../tipos';

// El administrador ve todo. P01 (login) es pública. P04 es un modal dentro de P02.
export const permisos: Record<Exclude<Rol, 'administrador'>, CodigoPantalla[]> = {
  vendedor:   ['P05', 'P11', 'P17', 'P18', 'P19', 'P20', 'P21', 'P22', 'P23', 'P24'],
  almacenero: ['P05', 'P08', 'P09', 'P10', 'P11', 'P12', 'P13', 'P14', 'P15', 'P16', 'P23', 'P24', 'P27'],
  reponedor:  ['P05', 'P06', 'P07', 'P12', 'P25', 'P26'],
};

export function puedeVer(rol: Rol | null, codigo: CodigoPantalla): boolean {
  if (codigo === 'P01') return true;
  if (!rol) return false;
  if (rol === 'administrador') return true;
  return permisos[rol].includes(codigo);
}
