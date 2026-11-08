import type { Role } from './roles';

/** Usuario del directorio administrado en CU-02. Las credenciales de prueba viven en `fixtures.ts`. */
export interface UsuarioRegistro {
  id: string;
  nombre: string;
  usuario: string;
  rol: Role;
  estado: 'Activo' | 'Inactivo';
}

/** Directorio de usuarios de prueba. Se modifica en memoria. */
export const USUARIOS_REGISTRO: UsuarioRegistro[] = [
  {
    id: 'U-001',
    nombre: 'Carlos Medina',
    usuario: 'admin',
    rol: 'Administrador',
    estado: 'Activo',
  },
  { id: 'U-002', nombre: 'Lucía Torres', usuario: 'vendedor', rol: 'Vendedor', estado: 'Activo' },
  { id: 'U-003', nombre: 'Rosa Quispe', usuario: 'rquispe', rol: 'Almacenero', estado: 'Activo' },
  {
    id: 'U-004',
    nombre: 'Miguel Paredes',
    usuario: 'reponedor',
    rol: 'Reponedor',
    estado: 'Activo',
  },
  {
    id: 'U-005',
    nombre: 'Julio Camacho',
    usuario: 'jcamacho',
    rol: 'Almacenero',
    estado: 'Activo',
  },
  { id: 'U-006', nombre: 'Elena Soto', usuario: 'esoto', rol: 'Vendedor', estado: 'Activo' },
  {
    id: 'U-007',
    nombre: 'Pedro Huamán',
    usuario: 'phuaman',
    rol: 'Almacenero',
    estado: 'Inactivo',
  },
  { id: 'U-008', nombre: 'Marta Ríos', usuario: 'mrios', rol: 'Reponedor', estado: 'Inactivo' },
];
