import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { guardarToken } from '@/api/cliente';
import { activarApiReal, simularServidor } from '@/test/apiReal';

const existencias = (sku: string, nombre: string) => ({
  productoId: sku,
  sku,
  nombre,
  categoria: 'Fierro y acero de construcción',
  unidad: 'unidad',
  stockMinimo: 100,
  fisico: 300,
  reservado: 20,
  libre: 280,
  nivelAlerta: 'normal',
  ubicaciones: [
    { ubicacionId: '1', nombre: 'Tienda', fisica: 100 },
    { ubicacionId: '2', nombre: 'Almacén 1', fisica: 200 },
  ],
});

beforeEach(() => {
  guardarToken('token-de-prueba');
  activarApiReal();
});

afterEach(() => {
  vi.unstubAllGlobals();
  vi.unstubAllEnvs();
  window.sessionStorage.clear();
});

describe('catálogo vigente con la API real', () => {
  it('reemplaza los productos de prueba por los de la API, recorriendo todas las páginas', async () => {
    const llamada = simularServidor((peticion) => {
      const pagina = Number(peticion.url.searchParams.get('page'));
      const items =
        pagina === 1
          ? [existencias('00754', 'FIERRO AM60 (1/2")')]
          : [existencias('00761', 'FIERRO AM60 (5/8")')];
      return {
        cuerpo: { items, total: 2, page: pagina, pageSize: 1, enAlerta: 0, totalGeneral: 2 },
      };
    });
    const { PRODUCTOS } = await import('@/features/inventario/fixtures');
    const { sincronizarCatalogoVigente } = await import('./catalogoVigente');

    expect(PRODUCTOS).toHaveLength(0);
    const productos = await sincronizarCatalogoVigente();
    expect(productos).toHaveLength(2);
    expect(productos).not.toBe(PRODUCTOS);

    expect(llamada.mock.calls[0]![0]).toContain('/inventario/disponibilidad');
    expect(PRODUCTOS.map((p) => p.sku)).toEqual(['00754', '00761']);
    expect(PRODUCTOS[0]).toMatchObject({
      nombre: 'FIERRO AM60 (1/2")',
      tienda: 100,
      almacen1: 200,
      reservado: 20,
    });
    const { opcionesProducto } = await import('./opciones');
    expect(opcionesProducto()).toEqual([
      { sku: '00754', nombre: 'FIERRO AM60 (1/2")' },
      { sku: '00761', nombre: 'FIERRO AM60 (5/8")' },
    ]);
  });
});
