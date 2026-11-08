import { z } from 'zod';

/** Unidades de medida admitidas por el catálogo. */
export const UNIDADES = ['bolsa', 'unidad', 'varilla', 'rollo', 'balde', 'm³', 'kg'] as const;

/** Un stock mínimo vacío se trata como ausente para que el mensaje sea "Ingresar el stock mínimo.". */
const minimoValido = z.preprocess(
  (valor) => (valor === '' || valor === null || valor === undefined ? undefined : Number(valor)),
  z
    .number({ error: 'Ingresar el stock mínimo.' })
    .int('El stock mínimo debe ser un número entero.')
    .min(0, 'El stock mínimo no puede ser negativo.'),
);

/** CU-03, registro y modificación de producto. El SKU se normaliza a mayúsculas antes de validar el formato. */
export const productoSchema = z.object({
  sku: z
    .string()
    .trim()
    .toUpperCase()
    .regex(/^[A-Z]{3}-\d{3}$/, 'Usar el formato AAA-000, por ejemplo CEM-049.'),
  nombre: z.string().trim().min(3, 'Ingresar al menos 3 caracteres.'),
  descripcion: z.string().trim().max(300, 'La descripción no puede superar 300 caracteres.'),
  categoria: z.string().min(1, 'Seleccionar la categoría.'),
  unidad: z.enum(UNIDADES, { error: 'Seleccionar la unidad de medida.' }),
  minimo: minimoValido,
  activo: z.boolean(),
});
