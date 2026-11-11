import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { guardarToken } from '@/api/cliente';
import { activarApiReal, problema, simularServidor, type PeticionSimulada } from '@/test/apiReal';

const proveedorApi = (id: string, estadoActivo = true) => ({
  id,
  ruc: '20123456789',
  razonSocial: 'Distribuidora Andina S.A.C.',
  contacto: 'Ana Villanueva; 954123456',
  condiciones: 'Pago a 30 días',
  estadoActivo,
  fechaRegistro: '2026-02-10T10:20:00-05:00',
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

describe('proveedores con la API real', () => {
  it('lista los proveedores con los filtros, la paginación y el catálogo de cada uno', async () => {
    const { listarProveedores } = await import('./api');
    const llamadas: PeticionSimulada[] = [];
    const llamada = simularServidor((peticion) => {
      llamadas.push(peticion);
      if (peticion.url.pathname === '/api/v1/proveedores') {
        return { cuerpo: { items: [proveedorApi('12', false)], total: 1, page: 1, pageSize: 100 } };
      }
      return {
        cuerpo: [
          { sku: 'CEM-049', nombre: 'Cemento', precioCompra: 26.5, plazoEntregaReferencialDias: 3 },
        ],
      };
    });

    const proveedores = await listarProveedores({ q: ' Andina ', estado: 'Inactivo' });

    const listado = llamadas[0] as PeticionSimulada;
    expect(listado.metodo).toBe('GET');
    expect(listado.url.searchParams.get('q')).toBe('Andina');
    expect(listado.url.searchParams.get('estadoActivo')).toBe('false');
    expect(listado.url.searchParams.get('page')).toBe('1');
    expect(listado.url.searchParams.get('pageSize')).toBe('100');
    expect(listado.cabeceras.get('Authorization')).toBe('Bearer token-de-prueba');
    expect(llamada).toHaveBeenCalledWith(
      expect.stringContaining('/api/v1/proveedores/12/catalogo'),
      expect.anything(),
    );
    expect(proveedores).toEqual([
      {
        id: '12',
        ruc: '20123456789',
        razonSocial: 'Distribuidora Andina S.A.C.',
        contacto: 'Ana Villanueva; 954123456',
        telefono: '',
        correo: '',
        condiciones: 'Pago a 30 días',
        skus: ['CEM-049'],
        estado: 'Inactivo',
      },
    ]);
  });

  it('recorre todas las páginas hasta alcanzar el total', async () => {
    const { listarProveedores } = await import('./api');
    const paginas: string[] = [];
    simularServidor((peticion) => {
      if (peticion.url.pathname !== '/api/v1/proveedores') return { cuerpo: [] };
      const page = peticion.url.searchParams.get('page') ?? '1';
      paginas.push(page);
      return {
        cuerpo: { items: [proveedorApi(page)], total: 2, page: Number(page), pageSize: 100 },
      };
    });

    const proveedores = await listarProveedores();

    expect(paginas).toEqual(['1', '2']);
    expect(proveedores.map((p) => p.id)).toEqual(['1', '2']);
  });

  it('registra el proveedor, envía el contacto combinado y reemplaza el catálogo con precio cero para los SKU nuevos', async () => {
    const { registrarProveedor } = await import('./api');
    const llamadas: PeticionSimulada[] = [];
    simularServidor((peticion) => {
      llamadas.push(peticion);
      if (peticion.metodo === 'POST') return { estado: 201, cuerpo: proveedorApi('12') };
      if (peticion.metodo === 'GET') return { cuerpo: [] };
      return { cuerpo: [] };
    });

    const creado = await registrarProveedor({
      razonSocial: 'Distribuidora Andina S.A.C.',
      ruc: '20123456789',
      contacto: 'Ana Villanueva',
      telefono: '954123456',
      correo: '',
      condiciones: '',
      skus: ['CEM-049'],
    });

    const alta = llamadas.find((p) => p.metodo === 'POST');
    expect(alta?.url.pathname).toBe('/api/v1/proveedores');
    expect(alta?.cuerpo).toEqual({
      ruc: '20123456789',
      razonSocial: 'Distribuidora Andina S.A.C.',
      contacto: 'Ana Villanueva; 954123456',
      condiciones: null,
    });
    const catalogo = llamadas.find((p) => p.metodo === 'PUT');
    expect(catalogo?.url.pathname).toBe('/api/v1/proveedores/12/catalogo');
    expect(catalogo?.cuerpo).toEqual({
      productos: [{ sku: 'CEM-049', precioCompra: 0, plazoEntregaReferencialDias: null }],
    });
    expect(creado).toMatchObject({ id: '12', estado: 'Activo', skus: ['CEM-049'] });
  });

  it('conserva el precio y el plazo de los productos que ya formaban el catálogo al actualizar', async () => {
    const { actualizarProveedor } = await import('./api');
    const llamadas: PeticionSimulada[] = [];
    simularServidor((peticion) => {
      llamadas.push(peticion);
      if (peticion.metodo === 'PUT' && peticion.url.pathname === '/api/v1/proveedores/12') {
        return { cuerpo: proveedorApi('12') };
      }
      if (peticion.metodo === 'GET') {
        return {
          cuerpo: [
            {
              sku: 'CEM-049',
              nombre: 'Cemento',
              precioCompra: 26.5,
              plazoEntregaReferencialDias: 3,
            },
          ],
        };
      }
      return { cuerpo: [] };
    });

    await actualizarProveedor('12', {
      razonSocial: 'Distribuidora Andina S.A.C.',
      ruc: '20123456789',
      contacto: 'Ana Villanueva',
      telefono: '',
      correo: '',
      condiciones: 'Pago a 30 días',
      skus: ['CEM-049'],
    });

    expect(
      llamadas.find((p) => p.metodo === 'PUT' && p.url.pathname.endsWith('/catalogo'))?.cuerpo,
    ).toEqual({
      productos: [{ sku: 'CEM-049', precioCompra: 26.5, plazoEntregaReferencialDias: 3 }],
    });
  });

  it('convierte RUC_DUPLICADO en RucDuplicadoError con el proveedor que ya lo usa', async () => {
    const { registrarProveedor, RucDuplicadoError } = await import('./api');
    simularServidor(() => problema(409, 'RUC_DUPLICADO', { proveedorId: '7' }));

    const error = await registrarProveedor({
      razonSocial: 'Otra',
      ruc: '20123456789',
      contacto: 'X',
      telefono: '',
      correo: '',
      condiciones: '',
      skus: [],
    }).catch((e: unknown) => e);

    expect(error).toBeInstanceOf(RucDuplicadoError);
    expect(error).toMatchObject({ message: 'RUC_DUPLICADO', proveedorId: '7' });
  });

  it('convierte RECURSO_NO_ENCONTRADO en el error NO_ENCONTRADO que usa la interfaz', async () => {
    const { obtenerProveedor } = await import('./api');
    simularServidor(() => problema(404, 'RECURSO_NO_ENCONTRADO'));

    await expect(obtenerProveedor('99')).rejects.toThrow('NO_ENCONTRADO');
  });

  it('activa o desactiva el proveedor con PUT sobre el estado y devuelve el estado nuevo', async () => {
    const { cambiarEstadoProveedor } = await import('./api');
    const llamadas: PeticionSimulada[] = [];
    simularServidor((peticion) => {
      llamadas.push(peticion);
      if (peticion.url.pathname.endsWith('/estado')) return { cuerpo: proveedorApi('12', false) };
      return { cuerpo: [] };
    });

    const actualizado = await cambiarEstadoProveedor('12', 'Inactivo');

    const cambio = llamadas[0] as PeticionSimulada;
    expect(cambio.metodo).toBe('PUT');
    expect(cambio.url.pathname).toBe('/api/v1/proveedores/12/estado');
    expect(cambio.cuerpo).toEqual({ estadoActivo: false });
    expect(actualizado.estado).toBe('Inactivo');
  });
});
