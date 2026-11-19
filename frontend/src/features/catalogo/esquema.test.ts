import { describe, expect, it } from 'vitest';
import { productoSchema, productoSchemaEdicion } from './esquema';

const base = {
  nombre: 'Malla galvanizada',
  descripcion: '',
  categoria: 'Fierro',
  unidad: 'unidad',
  minimo: 5,
  precioReferencial: 10,
  activo: true,
};

describe('SKU de un producto', () => {
  it.each(['CEM-049', 'cem-049', '00123', ' 00001 '])('acepta «%s» al registrar', (sku) => {
    expect(productoSchema.safeParse({ ...base, sku }).success).toBe(true);
  });

  it.each(['CEM049', '123', '001234', 'AB-123', 'CEM 049', ''])(
    'rechaza «%s» al registrar',
    (sku) => {
      expect(productoSchema.safeParse({ ...base, sku }).success).toBe(false);
    },
  );

  it('al modificar conserva el SKU del catálogo sin validar su formato', () => {
    const resultado = productoSchemaEdicion.safeParse({ ...base, sku: '00001' });

    expect(resultado.success).toBe(true);
    expect(productoSchemaEdicion.safeParse({ ...base, sku: 'SKU-ANTIGUO-X' }).success).toBe(true);
  });
});
