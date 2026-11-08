import { z } from 'zod';

/** Fecha de hoy en formato `aaaa-mm-dd`, en la hora local. */
export const hoyISO = () => new Date().toLocaleDateString('en-CA');

const lineaEsValida = (l: { sku: string; cantidad: number; precio: number }) =>
  l.sku !== '' && Number.isInteger(l.cantidad) && l.cantidad > 0 && l.precio > 0;

/** Esquema del formulario de orden de compra (CU-12). */
export const esquemaOrden = z.object({
  proveedorId: z.string().min(1, 'Seleccionar el proveedor.'),
  fecha: z
    .string()
    .min(1, 'Ingresar la fecha de la orden.')
    .refine((f) => f >= hoyISO(), 'La fecha no puede ser anterior a hoy.'),
  lineas: z
    .array(
      z.object({
        id: z.number(),
        sku: z.string(),
        cantidad: z.number(),
        precio: z.number(),
      }),
    )
    .min(1, 'Agregar al menos un producto.')
    .refine((ls) => ls.every(lineaEsValida), {
      message:
        'Revisar cada línea: el producto es obligatorio, la cantidad debe ser un entero mayor que 0 y el precio mayor que 0.',
    }),
});

export type ValoresOrden = z.infer<typeof esquemaOrden>;
