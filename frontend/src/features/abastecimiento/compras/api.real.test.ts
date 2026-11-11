import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { guardarToken } from '@/api/cliente';
import { activarApiReal, problema, simularServidor, type PeticionSimulada } from '@/test/apiReal';

const ordenApi = (codigo: string, id: string, estado: string) => ({
  id,
  codigo,
  proveedorId: '12',
  usuarioResponsableId: '4',
  fechaEmision: '2026-11-03T10:15:00-05:00',
  estado,
  fechaEntregaEstimada: null,
  observaciones: null,
  modalidadPago: null,
  total: 2650,
  lineas: [
    {
      sku: 'CEM-049',
      nombre: 'Cemento Sol tipo I 42.5 kg',
      cantidadComprada: 100,
      costoUnitario: 26.5,
      subtotal: 2650,
      cantidadRecibida: 40,
      cantidadPendienteRecepcion: 60,
    },
  ],
});

const proveedorApi = {
  id: '12',
  ruc: '20123456789',
  razonSocial: 'Distribuidora Andina S.A.C.',
  contacto: 'Ana Villanueva',
  condiciones: null,
  estadoActivo: true,
  fechaRegistro: '2026-02-10T10:20:00-05:00',
};

beforeEach(() => {
  guardarToken('token-de-prueba');
  activarApiReal();
});

afterEach(() => {
  vi.unstubAllGlobals();
  vi.unstubAllEnvs();
  window.sessionStorage.clear();
});

describe('órdenes de compra con la API real', () => {
  it('lista las órdenes con el nombre del proveedor y el avance de recepción', async () => {
    const { listarOrdenesCompra } = await import('./api');
    const llamadas: PeticionSimulada[] = [];
    simularServidor((peticion) => {
      llamadas.push(peticion);
      if (peticion.url.pathname === '/api/v1/ordenes-compra') {
        return {
          cuerpo: {
            items: [ordenApi('OC-0087', '87', 'parcial')],
            total: 1,
            page: 1,
            pageSize: 100,
          },
        };
      }
      return { cuerpo: { items: [proveedorApi], total: 1, page: 1, pageSize: 100 } };
    });

    const filas = await listarOrdenesCompra();

    expect(llamadas.map((p) => p.url.pathname)).toEqual(
      expect.arrayContaining(['/api/v1/ordenes-compra', '/api/v1/proveedores']),
    );
    expect(
      llamadas.every((p) => p.cabeceras.get('Authorization') === 'Bearer token-de-prueba'),
    ).toBe(true);
    expect(filas).toEqual([
      {
        id: 'OC-0087',
        proveedor: 'Distribuidora Andina S.A.C.',
        fecha: '2026-11-03',
        estado: 'Parcial',
        productos: 1,
        pedidas: 100,
        recibidas: 40,
        total: 2650,
      },
    ]);
  });

  it('localiza la orden por su código y devuelve sus líneas y su proveedor', async () => {
    const { obtenerOrdenCompra } = await import('./api');
    const llamadas: PeticionSimulada[] = [];
    simularServidor((peticion) => {
      llamadas.push(peticion);
      if (peticion.url.pathname === '/api/v1/ordenes-compra') {
        return {
          cuerpo: {
            items: [ordenApi('OC-0086', '86', 'pendiente'), ordenApi('OC-0087', '87', 'parcial')],
            total: 2,
            page: 1,
            pageSize: 100,
          },
        };
      }
      if (peticion.url.pathname === '/api/v1/proveedores/12/catalogo') return { cuerpo: [] };
      return { cuerpo: proveedorApi };
    });

    const { orden, proveedor } = await obtenerOrdenCompra('OC-0087');

    expect((llamadas[0] as PeticionSimulada).url.searchParams.get('q')).toBe('OC-0087');
    expect(orden).toEqual({
      id: 'OC-0087',
      proveedorId: '12',
      fecha: '2026-11-03',
      estado: 'Parcial',
      lineas: [
        {
          sku: 'CEM-049',
          nombre: 'Cemento Sol tipo I 42.5 kg',
          cantidad: 100,
          recibido: 40,
          precio: 26.5,
        },
      ],
    });
    expect(proveedor).toMatchObject({
      id: '12',
      razonSocial: 'Distribuidora Andina S.A.C.',
      estado: 'Activo',
    });
  });

  it('informa ORDEN_NO_ENCONTRADA cuando ningún resultado coincide con el código', async () => {
    const { obtenerOrdenCompra } = await import('./api');
    simularServidor(() => ({ cuerpo: { items: [], total: 0, page: 1, pageSize: 100 } }));

    await expect(obtenerOrdenCompra('OC-9999')).rejects.toThrow('ORDEN_NO_ENCONTRADA');
  });

  it('registra la orden con las líneas del formulario y devuelve la orden creada', async () => {
    const { crearOrdenCompra } = await import('./api');
    const llamadas: PeticionSimulada[] = [];
    simularServidor((peticion) => {
      llamadas.push(peticion);
      return { estado: 201, cuerpo: ordenApi('OC-0088', '88', 'pendiente') };
    });

    const orden = await crearOrdenCompra({
      proveedorId: '12',
      fecha: '2026-11-10',
      lineas: [{ sku: 'CEM-049', cantidad: 100, precio: 26.5 }],
    });

    const alta = llamadas[0] as PeticionSimulada;
    expect(alta.metodo).toBe('POST');
    expect(alta.url.pathname).toBe('/api/v1/ordenes-compra');
    expect(alta.cuerpo).toEqual({
      proveedorId: '12',
      lineas: [{ sku: 'CEM-049', cantidadComprada: 100, costoUnitario: 26.5 }],
    });
    expect(orden).toMatchObject({ id: 'OC-0088', estado: 'Pendiente' });
  });

  it('convierte PROVEEDOR_INACTIVO en el error que consume el formulario', async () => {
    const { crearOrdenCompra } = await import('./api');
    simularServidor(() => problema(422, 'PROVEEDOR_INACTIVO'));

    await expect(
      crearOrdenCompra({ proveedorId: '12', fecha: '2026-11-10', lineas: [] }),
    ).rejects.toThrow('PROVEEDOR_INACTIVO');
  });
});
