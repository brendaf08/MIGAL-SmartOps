import { z } from 'zod';
import { ROLES } from '@/features/seguridad/roles';

const rol = z.enum(ROLES, { error: 'Seleccionar un rol.' });
const nombres = z
  .string()
  .trim()
  .min(1, 'Ingresar los nombres.')
  .max(100, 'Ingresar como máximo 100 caracteres.');
const apellidos = z
  .string()
  .trim()
  .min(1, 'Ingresar los apellidos.')
  .max(100, 'Ingresar como máximo 100 caracteres.');

/** Alta de usuario (CU-02, registro). */
export const esquemaRegistro = z.object({
  nombres,
  apellidos,
  usuario: z
    .string()
    .trim()
    .min(4, 'Ingresar un usuario de 4 a 20 caracteres.')
    .max(20, 'Ingresar un usuario de 4 a 20 caracteres.')
    .regex(/^[a-z0-9._-]+$/, 'Usar solo minúsculas, números y los signos . _ -, sin espacios.'),
  clave: z
    .string()
    .min(8, 'La contraseña temporal debe tener al menos 8 caracteres.')
    .regex(/[A-Za-z]/, 'La contraseña temporal debe incluir una letra.')
    .regex(/\d/, 'La contraseña temporal debe incluir un número.'),
  rol,
});

/** Edición: el usuario y la contraseña no se modifican. */
export const esquemaEdicion = z.object({ nombres, apellidos, rol });

export type DatosRegistro = z.infer<typeof esquemaRegistro>;
export type DatosEdicion = z.infer<typeof esquemaEdicion>;
