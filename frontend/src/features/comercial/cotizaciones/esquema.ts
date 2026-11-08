import { z } from 'zod';
import { subtotalLineas } from '@/components/ui/LineasEditor/calculos';

/** Valor del selector de cliente que indica emisión sin cliente (flujo alternativo 5). */
export const SIN_CLIENTE = 'sin-cliente';

/** Validez de la cotización en días naturales (CU-09). */
export const VALIDEZ = { defecto: 7, min: 1, max: 60 } as const;

export const FORMAS_PAGO = [
  'Contado',
  'Transferencia bancaria',
  'Crédito a 15 días',
  'Crédito a 30 días',
] as const;

/** Reglas de validación del formulario de cotización; los mensajes se muestran junto al campo. */
export const esquemaCotizacion = z
  .object({
    clienteId: z.string().min(1, 'Elegir un cliente o continuar sin cliente.'),
    lineas: z
      .array(
        z.object({
          id: z.number(),
          sku: z.string().min(1, 'Elegir un producto para cada línea.'),
          cantidad: z
            .number()
            .int('La cantidad debe ser un número entero.')
            .min(1, 'La cantidad debe ser mayor que 0.'),
          precio: z.number().gt(0, 'El precio debe ser mayor que 0.'),
        }),
      )
      .min(1, 'Agregar al menos un producto.'),
    /** Descuento en soles, como se captura en el formulario. */
    descuento: z.number().min(0, 'El descuento no puede ser negativo.'),
    validezDias: z
      .number()
      .int('La validez debe ser un número entero de días.')
      .min(VALIDEZ.min, `La validez debe estar entre ${VALIDEZ.min} y ${VALIDEZ.max} días.`)
      .max(VALIDEZ.max, `La validez debe estar entre ${VALIDEZ.min} y ${VALIDEZ.max} días.`),
    formaPago: z.string().min(1, 'Elegir la forma de pago.'),
    observaciones: z
      .string()
      .trim()
      .max(500, 'Las observaciones no pueden superar 500 caracteres.'),
  })
  .superRefine((valores, ctx) => {
    if (valores.descuento > subtotalLineas(valores.lineas)) {
      ctx.addIssue({
        code: 'custom',
        path: ['descuento'],
        message: 'El descuento no puede superar el subtotal.',
      });
    }
  });

export type ValoresCotizacion = z.infer<typeof esquemaCotizacion>;
