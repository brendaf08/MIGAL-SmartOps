import { z } from 'zod';
import { descuentoValido, type LineaEditable } from '@/components/ui/LineasEditor/calculos';
import { tieneLineasSinCobertura, type ModalidadAtencion } from './atencion';

/** Valores del formulario de venta. `cliente` vacío significa que aún no se elige; `SIN_CLIENTE` es una venta sin cliente. */
export interface VentaFormValues {
  cliente: string | null;
  cotizacionId: string | null;
  lineas: LineaEditable[];
  descuento: number;
  modalidad: ModalidadAtencion | null;
}

/**
 * Validación del registro de venta (CU-10). El esquema depende del stock libre por SKU: si alguna línea no tiene
 * cobertura, la modalidad de atención es obligatoria. El descuento no puede superar el subtotal (flujo alternativo 1).
 */
export const crearEsquemaVenta = (stock: Readonly<Record<string, number>>) =>
  z
    .object({
      cliente: z.string().nullable(),
      cotizacionId: z.string().nullable(),
      lineas: z.array(
        z.object({
          id: z.number(),
          sku: z.string(),
          cantidad: z.number(),
          precio: z.number(),
        }),
      ),
      descuento: z.number(),
      modalidad: z.enum(['parcial', 'pendienteAbastecimiento', 'pendienteCliente']).nullable(),
    })
    .superRefine((v, ctx) => {
      if (v.cliente === null)
        ctx.addIssue({
          code: 'custom',
          path: ['cliente'],
          message: 'Elegir el cliente o continuar sin cliente.',
        });
      if (v.lineas.length === 0)
        ctx.addIssue({
          code: 'custom',
          path: ['lineas'],
          message: 'Agregar al menos un producto.',
        });
      if (v.lineas.some((l) => !l.sku))
        ctx.addIssue({
          code: 'custom',
          path: ['lineas'],
          message: 'Seleccionar un producto en cada línea.',
        });
      if (v.lineas.some((l) => !(l.cantidad >= 1)))
        ctx.addIssue({
          code: 'custom',
          path: ['lineas'],
          message: 'La cantidad de cada línea es de al menos 1.',
        });
      if (!descuentoValido(v.lineas, v.descuento))
        ctx.addIssue({
          code: 'custom',
          path: ['descuento'],
          message: 'El descuento no puede ser negativo ni mayor que el subtotal.',
        });
      if (
        v.modalidad === null &&
        tieneLineasSinCobertura(
          v.lineas.map((l) => ({ sku: l.sku, cantidad: l.cantidad })),
          stock,
        )
      )
        ctx.addIssue({
          code: 'custom',
          path: ['modalidad'],
          message: 'Elegir cómo atender las líneas sin existencias.',
        });
    });
