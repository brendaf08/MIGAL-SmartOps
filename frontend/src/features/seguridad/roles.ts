export const ROLES = ['Administrador', 'Vendedor', 'Almacenero', 'Reponedor'] as const;
export type Role = (typeof ROLES)[number];

export interface User {
  name: string;
  role: Role;
}
