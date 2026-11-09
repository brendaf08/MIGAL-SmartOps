import type { User } from './roles';

/** Usuarios de prueba para desarrollo, uno por rol. Se eliminan al integrar la autenticación del backend. */
export const USUARIOS: (User & { usuario: string; clave: string })[] = [
  { usuario: 'admin', clave: 'demo1234', name: 'Carlos Medina', role: 'Administrador' },
  { usuario: 'vendedor', clave: 'demo1234', name: 'Lucía Torres', role: 'Vendedor' },
  { usuario: 'rquispe', clave: 'demo1234', name: 'Rosa Quispe', role: 'Almacenero' },
  { usuario: 'reponedor', clave: 'demo1234', name: 'Miguel Paredes', role: 'Reponedor' },
];
