/** Roles del negocio: los únicos que el Administrador asigna desde el formulario de usuarios (CU-02). */
export const ROLES = ['Administrador', 'Vendedor', 'Almacenero', 'Reponedor'] as const;
export type RolNegocio = (typeof ROLES)[number];

/**
 * Rol de demostración (ADR-013): reúne los permisos de los cuatro roles para presentar el sistema con una sola cuenta.
 * Solo existe si el despliegue lo habilita y no se asigna desde la interfaz.
 */
export const ROL_DEMOSTRACION = 'Demostración';
export type Role = RolNegocio | typeof ROL_DEMOSTRACION;

/** Indica si `rol` tiene los permisos de alguno de `roles`. El de demostración los tiene todos. */
export const tieneRol = (rol: string | null | undefined, roles: readonly string[]): boolean =>
  rol === ROL_DEMOSTRACION || (rol !== null && rol !== undefined && roles.includes(rol));

export interface User {
  name: string;
  role: Role;
}
