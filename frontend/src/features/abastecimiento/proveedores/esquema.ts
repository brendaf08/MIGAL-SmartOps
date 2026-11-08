import { z } from 'zod';

/** Validación del formulario de proveedor (CU-11). Los mensajes indican qué corregir. */
export const esquemaProveedor = z.object({
  razonSocial: z.string().trim().min(1, 'Ingresar la razón social.'),
  ruc: z
    .string()
    .trim()
    .min(1, 'Ingresar el RUC.')
    .regex(/^\d{11}$/, 'El RUC debe tener 11 dígitos.')
    .regex(/^(10|20)/, 'El RUC debe empezar con 10 o 20.'),
  contacto: z.string().trim().min(1, 'Ingresar la persona de contacto.'),
  telefono: z
    .string()
    .trim()
    .refine((v) => v === '' || /^\d{9}$/.test(v), 'El teléfono debe tener 9 dígitos.'),
  correo: z
    .string()
    .trim()
    .refine((v) => v === '' || /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v), 'Ingresar un correo válido.'),
  condiciones: z.string().trim().min(1, 'Ingresar las condiciones comerciales.'),
  skus: z.array(z.string()),
});

export type ValoresProveedor = z.infer<typeof esquemaProveedor>;
