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

/** Formatos de SKU de un producto nuevo: letras y número (CEM-049) o el código numérico del catálogo (00123). */
const FORMATO_SKU = /^(?:[A-Z]{3}-\d{3}|\d{5})$/;
export const MENSAJE_FORMATO_SKU =
  'Usar el formato AAA-000 (por ejemplo CEM-049) o un código de 5 dígitos (por ejemplo 00123).';

/** CU-03, registro y modificación de producto. El SKU se normaliza a mayúsculas antes de validar el formato. */
export const productoSchema = z.object({
  sku: z.string().trim().toUpperCase().regex(FORMATO_SKU, MENSAJE_FORMATO_SKU),
  nombre: z.string().trim().min(3, 'Ingresar al menos 3 caracteres.'),
  descripcion: z.string().trim().max(300, 'La descripción no puede superar 300 caracteres.'),
  categoria: z.string().min(1, 'Seleccionar la categoría.'),
  unidad: z.enum(UNIDADES, { error: 'Seleccionar la unidad de medida.' }),
  minimo: minimoValido,
  precioReferencial: z.coerce
    .number({ error: 'Ingresar el precio referencial.' })
    .min(0, 'El precio referencial no puede ser negativo.'),
  activo: z.boolean(),
});

/** Al modificar un producto el SKU está bloqueado: se conserva tal cual, sin validar su formato. */
export const productoSchemaEdicion = productoSchema.extend({ sku: z.string() });
