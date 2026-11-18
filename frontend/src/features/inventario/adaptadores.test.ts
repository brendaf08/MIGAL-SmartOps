import { describe, expect, it } from 'vitest';
import { idDeUbicacion, type AlmacenDto } from './adaptadores';

const ALMACENES: AlmacenDto[] = [
  { id: '1', nombre: 'Tienda', tipoUbicacion: 'tienda' },
  { id: '2', nombre: 'Almacén 1', tipoUbicacion: 'almacen' },
  { id: '3', nombre: 'Almacén 2', tipoUbicacion: 'almacen' },
];

describe('idDeUbicacion', () => {
  it('reconoce la tienda por su tipo aunque la interfaz la llame "Tienda principal"', () => {
    expect(idDeUbicacion(ALMACENES, 'Tienda principal')).toBe('1');
  });

  it('usa el nombre exacto cuando existe', () => {
    expect(idDeUbicacion(ALMACENES, 'Almacén 2')).toBe('3');
    expect(
      idDeUbicacion([{ id: '9', nombre: 'Tienda principal' }, ...ALMACENES], 'Tienda principal'),
    ).toBe('9');
  });

  it('no inventa un almacén que no existe', () => {
    expect(idDeUbicacion(ALMACENES, 'Almacén 3')).toBeUndefined();
  });
});
