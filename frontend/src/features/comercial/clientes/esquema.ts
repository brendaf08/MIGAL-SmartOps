import { z } from 'zod';

/**
 * Validación del formulario de cliente (RF de clientes). El DNI tiene 8 dígitos; el RUC, 11 dígitos
 * y empieza en 10 o 20. El teléfono y el correo son opcionales, pero si se escriben deben ser válidos.
 */
export const clienteSchema = z
  .object({
    tipoDocumento: z.enum(['DNI', 'RUC'], { error: 'Seleccionar el tipo de documento.' }),
    documento: z.string().trim(),
    nombre: z.string().trim().min(1, 'Ingresar el nombre o la razón social.'),
    telefono: z
      .string()
      .trim()
      .refine((v) => v === '' || /^\d{9}$/.test(v), 'El teléfono tiene 9 dígitos.'),
    correo: z
      .string()
      .trim()
      .refine(
        (v) => v === '' || /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v),
        'Ingresar un correo válido, por ejemplo nombre@dominio.pe.',
      ),
  })
  .superRefine((cliente, ctx) => {
    const valido =
      cliente.tipoDocumento === 'DNI'
        ? /^\d{8}$/.test(cliente.documento)
        : /^(10|20)\d{9}$/.test(cliente.documento);
    if (!valido) {
      ctx.addIssue({
        code: 'custom',
        path: ['documento'],
        message:
          cliente.tipoDocumento === 'DNI'
            ? 'El DNI tiene 8 dígitos.'
            : 'El RUC tiene 11 dígitos y empieza en 10 o 20.',
      });
    }
  });

export type ClienteFormValues = z.infer<typeof clienteSchema>;
