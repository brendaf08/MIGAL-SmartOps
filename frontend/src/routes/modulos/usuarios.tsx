import { UsuariosPage } from '@/features/seguridad/UsuariosPage';
import { UsuarioFormPage } from '@/features/seguridad/usuarios/UsuarioFormPage';
import type { RutaModulo } from './tipos';

/** Rutas del módulo `usuarios`: lista, registro y edición de rol. */
export const RUTAS: RutaModulo[] = [
  { path: '', element: <UsuariosPage /> },
  { path: 'nuevo', element: <UsuarioFormPage /> },
  { path: ':id', element: <UsuarioFormPage /> },
];
