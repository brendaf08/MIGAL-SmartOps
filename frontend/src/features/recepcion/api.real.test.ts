import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { guardarToken } from '@/api/cliente';
import { activarApiReal, problema, simularServidor, type PeticionSimulada } from '@/test/apiReal';

const ordenApi = {
  id: '87',
  codigo: 'OC-0087',
  proveedorId: '12',
  fechaEmision: '2026-11-03T10:15:00-05:00',
  estado: 'parcial',
  observaciones: null,
  fechaEntregaEstimada: null,
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
};

const recepcionApi = (estado: 'pendiente' | 'validada', codigo = 'R-0012') => ({
  id: '12',
  codigo,
  ordenCompraId: '87',
  codigoOrdenCompra: 'OC-0087',
  proveedor: 'Distribuidora Andina S.A.C.',
  estado,
  fechaRecepcion: '2026-11-10',
  fechaValidacion: estado === 'validada' ? '2026-11-10T15:40:12-05:00' : null,
  usuarioResponsableId: '7',
  observaciones: null,
  documentoId: null,
  lineas: [
    {
      productoId: '42',
      sku: 'CEM-049',
      nombre: 'Cemento Sol tipo I 42.5 kg',
      unidad: 'bolsa',
      ubicacionId: '102',
      nombreUbicacion: 'Almacén 1',
      cantidadRecibida: 40,
    },
  ],
  ordenCompra: {
    id: '87',
    codigo: 'OC-0087',
    proveedor: 'Distribuidora Andina S.A.C.',
    estado: 'parcial',
    lineas: [
      {
        productoId: '42',
        sku: 'CEM-049',
        nombre: 'Cemento Sol tipo I 42.5 kg',
        unidad: 'bolsa',
        cantidadComprada: 100,
        cantidadPendiente: 60,
      },
    ],
  },
});

const resultadoApi = {
  recepcion: recepcionApi('validada'),
  movimientos: [],
  cambios: [
    {
      productoId: '42',
      sku: 'CEM-049',
      ubicacionId: '102',
      unidad: 'bolsa',
      antes: 120,
      despues: 160,
    },
  ],
  ordenCompra: recepcionApi('validada').ordenCompra,
};

/** Respuestas comunes de las consultas auxiliares de la confirmación. */
function respuestaAuxiliar(peticion: PeticionSimulada) {
  if (peticion.url.pathname === '/api/v1/ordenes-compra') {
    return { cuerpo: { items: [ordenApi], total: 1, page: 1, pageSize: 100 } };
  }
  if (peticion.url.pathname === '/api/v1/productos') {
    return { cuerpo: { items: [{ id: '42', sku: 'CEM-049' }], total: 1, page: 1, pageSize: 100 } };
  }
  if (peticion.url.pathname === '/api/v1/almacenes') {
    return { cuerpo: [{ id: '102', nombre: 'Almacén 1' }] };
  }
  return undefined;
}

const lineaEnPantalla = {
  id: 1,
  texto: 'Cemento Sol tipo I 42.5 kg',
  sku: 'CEM-049',
  score: 1,
  pedido: 60,
  recibido: 40,
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

describe('recepciones con la API real', () => {
  it('lista las recepciones con su almacén y el estado de la orden del detalle', async () => {
    const { listarRecepciones } = await import('./api');
    simularServidor((peticion) => {
      if (peticion.url.pathname === '/api/v1/recepciones') {
        return {
          cuerpo: { items: [{ id: '12', codigo: 'R-0012' }], total: 1, page: 1, pageSize: 100 },
        };
      }
      return { cuerpo: recepcionApi('validada') };
    });

    const filas = await listarRecepciones();

    expect(filas).toEqual([
      {
        id: 'R-0012',
        orden: 'OC-0087',
        proveedor: 'Distribuidora Andina S.A.C.',
        almacen: 'Almacén 1',
        fecha: '2026-11-10',
        estado: 'Validada',
        estadoOrden: 'Parcial',
      },
    ]);
  });

  it('lista solo las órdenes pendientes o parciales con su proveedor y sus líneas', async () => {
    const { listarOrdenesRecepcionables } = await import('./api');
    const llamadas: PeticionSimulada[] = [];
    simularServidor((peticion) => {
      llamadas.push(peticion);
      if (peticion.url.pathname === '/api/v1/ordenes-compra') {
        return { cuerpo: { items: [ordenApi], total: 1, page: 1, pageSize: 100 } };
      }
      return {
        cuerpo: {
          items: [{ id: '12', razonSocial: 'Distribuidora Andina S.A.C.' }],
          total: 1,
          page: 1,
          pageSize: 100,
        },
      };
    });

    const ordenes = await listarOrdenesRecepcionables();

    expect((llamadas[0] as PeticionSimulada).url.searchParams.get('estado')).toBe('pendiente,parcial');
    expect(ordenes).toEqual([
      {
        id: 'OC-0087',
        proveedor: 'Distribuidora Andina S.A.C.',
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
      },
    ]);
  });

  it('registra la recepción con productos y ubicación por identificador, y la valida con Idempotency-Key', async () => {
    const { confirmarRecepcion } = await import('./api');
    const llamadas: PeticionSimulada[] = [];
    simularServidor((peticion) => {
      llamadas.push(peticion);
      const auxiliar = respuestaAuxiliar(peticion);
      if (auxiliar) return auxiliar;
      if (peticion.metodo === 'POST' && peticion.url.pathname === '/api/v1/recepciones') {
        return { estado: 201, cuerpo: recepcionApi('pendiente') };
      }
      return { cuerpo: resultadoApi };
    });

    const resultado = await confirmarRecepcion('OC-0087', [lineaEnPantalla], '');

    const alta = llamadas.find(
      (p) => p.metodo === 'POST' && p.url.pathname === '/api/v1/recepciones',
    );
    expect(alta?.cuerpo).toEqual({
      ordenCompraId: '87',
      fechaRecepcion: expect.stringMatching(/^\d{4}-\d{2}-\d{2}$/),
      observaciones: null,
      lineas: [{ productoId: '42', ubicacionId: '102', cantidadRecibida: 40 }],
    });
    const validacion = llamadas.find((p) => p.url.pathname === '/api/v1/recepciones/12/validacion');
    expect(validacion?.metodo).toBe('POST');
    expect(validacion?.cabeceras.get('Idempotency-Key')).toMatch(
      /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/,
    );
    expect(resultado).toMatchObject({
      recepcion: { id: 'R-0012', orden: 'OC-0087', estado: 'Validada', almacen: 'Almacén 1' },
      cambios: [
        {
          producto: 'Cemento Sol tipo I 42.5 kg',
          ubicacion: 'Almacén 1',
          antes: 120,
          despues: 160,
          unidad: 'bolsa',
        },
      ],
      estadoOrden: 'Parcial',
      porRecibir: 60,
      lineasPendientes: 1,
    });
  });

  it('reintenta sin registrar otra recepción y con la misma clave tras un fallo de la validación', async () => {
    const { confirmarRecepcion } = await import('./api');
    const llamadas: PeticionSimulada[] = [];
    let intentosValidacion = 0;
    simularServidor((peticion) => {
      llamadas.push(peticion);
      const auxiliar = respuestaAuxiliar(peticion);
      if (auxiliar) return auxiliar;
      if (peticion.url.pathname === '/api/v1/recepciones' && peticion.metodo === 'POST') {
        return { estado: 201, cuerpo: recepcionApi('pendiente') };
      }
      if (peticion.url.pathname === '/api/v1/recepciones/12' && peticion.metodo === 'PUT') {
        return { cuerpo: recepcionApi('pendiente') };
      }
      intentosValidacion += 1;
      if (intentosValidacion === 1) return problema(503, 'SERVICIO_NO_DISPONIBLE');
      return { cuerpo: resultadoApi };
    });

    await expect(confirmarRecepcion('OC-0087', [lineaEnPantalla], '')).rejects.toMatchObject({
      codigo: 'SERVICIO_NO_DISPONIBLE',
    });
    await confirmarRecepcion('OC-0087', [lineaEnPantalla], '');

    const registros = llamadas.filter(
      (p) => p.metodo === 'POST' && p.url.pathname === '/api/v1/recepciones',
    );
    expect(registros).toHaveLength(1);
    expect(
      llamadas.some((p) => p.metodo === 'PUT' && p.url.pathname === '/api/v1/recepciones/12'),
    ).toBe(true);
    const claves = llamadas
      .filter((p) => p.url.pathname === '/api/v1/recepciones/12/validacion')
      .map((p) => p.cabeceras.get('Idempotency-Key'));
    expect(claves).toHaveLength(2);
    expect(claves[0]).toBe(claves[1]);
  });

  it('informa SOBRANTE_SIN_NOTA cuando la validación rechaza un sobrante sin observaciones', async () => {
    const { confirmarRecepcion } = await import('./api');
    simularServidor((peticion) => {
      const auxiliar = respuestaAuxiliar(peticion);
      if (auxiliar) return auxiliar;
      if (peticion.url.pathname === '/api/v1/recepciones') {
        return { estado: 201, cuerpo: recepcionApi('pendiente') };
      }
      return problema(422, 'SOBRANTE_SIN_NOTA', {
        detail: 'Explicar el sobrante en las observaciones.',
      });
    });

    await expect(confirmarRecepcion('OC-0087', [lineaEnPantalla], '')).rejects.toMatchObject({
      codigo: 'SOBRANTE_SIN_NOTA',
      estado: 422,
      message: 'Explicar el sobrante en las observaciones.',
    });
  });
});
