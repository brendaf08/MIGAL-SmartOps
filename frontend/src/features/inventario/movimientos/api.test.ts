import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { activarApiReal, cargarModulo, json, problema, restablecerApiReal, simularServidor } from '@/test/servidorApi';

const CLAVE = '6f1d2c3a-4b5e-4f70-8a9b-0c1d2e3f4a5b';

const ALMACENES = [
  { id: '101', nombre: 'Tienda principal', tipoUbicacion: 'tienda', direccionReferencia: null, estadoActivo: true },
  { id: '102', nombre: 'Almacén 1', tipoUbicacion: 'almacen', direccionReferencia: null, estadoActivo: true },
  { id: '103', nombre: 'Almacén 2', tipoUbicacion: 'almacen', direccionReferencia: null, estadoActivo: true },
];

const MOVIMIENTO = (id: string, ubicacionId: string, tipo: 'entrada' | 'salida', origen: string) => ({
  id,
  fechaHora: '2026-11-07T15:10:02-05:00',
  productoId: '42',
  sku: 'CEM-001',
  nombreProducto: 'Cemento Sol tipo I, bolsa 42.5 kg',
  tipoOperacion: tipo,
  cantidad: 5,
  origenMovimiento: origen,
  ubicacionId,
  usuarioId: '7',
  motivo: 'Bolsas dañadas por humedad',
  idGuia: null,
  idRecepcion: null,
  referenciaTraslado: null,
  idMovimientoCorregido: null,
});

const SOLICITUD = {
  tipo: 'salida' as const,
  sku: 'CEM-001',
  ubicacion: 'Almacén 1',
  origen: '',
  destino: '',
  cantidad: 5,
  motivo: 'Bolsas dañadas por humedad',
  usuario: 'Rosa Quispe',
  claveIdempotencia: CLAVE,
};

beforeEach(activarApiReal);
afterEach(restablecerApiReal);

describe('movimientos manuales (rama de la API real)', () => {
  it('registra una salida con Idempotency-Key, traduce la ubicación y mapea la constancia', async () => {
    const llamadas = simularServidor({
      'GET /api/v1/productos': () =>
        json({ items: [{ id: '42', sku: 'CEM-001' }], total: 1, page: 1, pageSize: 1 }),
      'GET /api/v1/almacenes': () => json(ALMACENES),
      'POST /api/v1/inventario/movimientos': () =>
        json(
          {
            movimientos: [MOVIMIENTO('1208', '102', 'salida', 'ajuste')],
            cambios: [{ productoId: '42', sku: 'CEM-001', ubicacionId: '102', unidad: 'bolsa', antes: 120, despues: 115 }],
          },
          201,
        ),
    });
    const { registrarMovimiento } = await cargarModulo(() => import('./api'));

    const resultado = await registrarMovimiento(SOLICITUD);

    const alta = llamadas.find((l) => l.metodo === 'POST')!;
    expect(alta.ruta).toBe('/api/v1/inventario/movimientos');
    expect(alta.cabeceras.get('Idempotency-Key')).toBe(CLAVE);
    expect(alta.cabeceras.get('Authorization')).toBe('Bearer TOKEN-PRUEBA');
    expect(alta.cuerpo).toEqual({
      tipoOperacion: 'salida',
      productoId: '42',
      ubicacionId: '102',
      cantidad: 5,
      motivo: 'Bolsas dañadas por humedad',
    });
    expect(resultado.movimientos).toEqual([
      {
        id: '1208',
        fecha: '2026-11-07',
        sku: 'CEM-001',
        producto: 'Cemento Sol tipo I, bolsa 42.5 kg',
        tipo: 'Salida',
        cantidad: 5,
        origen: 'Ajuste',
        ubicacion: 'Almacén 1',
        usuario: '7',
      },
    ]);
    expect(resultado.cambios).toEqual([
      { producto: 'Cemento Sol tipo I, bolsa 42.5 kg', ubicacion: 'Almacén 1', antes: 120, despues: 115, unidad: 'bolsa' },
    ]);
  });

  it('registra un traslado en /inventario/traslados con origen y destino por identificador', async () => {
    const llamadas = simularServidor({
      'GET /api/v1/productos': () =>
        json({ items: [{ id: '42', sku: 'CEM-001' }], total: 1, page: 1, pageSize: 1 }),
      'GET /api/v1/almacenes': () => json(ALMACENES),
      'POST /api/v1/inventario/traslados': () =>
        json(
          {
            referenciaTraslado: 'TRS-7f3a',
            movimientos: [
              MOVIMIENTO('1210', '102', 'salida', 'traslado'),
              MOVIMIENTO('1211', '103', 'entrada', 'traslado'),
            ],
            cambios: [
              { productoId: '42', sku: 'CEM-001', ubicacionId: '102', unidad: 'bolsa', antes: 40, despues: 20 },
              { productoId: '42', sku: 'CEM-001', ubicacionId: '103', unidad: 'bolsa', antes: 120, despues: 140 },
            ],
          },
          201,
        ),
    });
    const { registrarMovimiento } = await cargarModulo(() => import('./api'));

    const resultado = await registrarMovimiento({
      ...SOLICITUD,
      tipo: 'traslado',
      ubicacion: '',
      origen: 'Almacén 1',
      destino: 'Almacén 2',
    });

    const traslado = llamadas.find((l) => l.metodo === 'POST')!;
    expect(traslado.ruta).toBe('/api/v1/inventario/traslados');
    expect(traslado.cabeceras.get('Idempotency-Key')).toBe(CLAVE);
    expect(traslado.cuerpo).toEqual({
      productoId: '42',
      ubicacionOrigenId: '102',
      ubicacionDestinoId: '103',
      cantidad: 5,
      motivo: 'Bolsas dañadas por humedad',
    });
    expect(resultado.movimientos.map((m) => [m.tipo, m.ubicacion, m.origen])).toEqual([
      ['Salida', 'Almacén 1', 'Traslado'],
      ['Entrada', 'Almacén 2', 'Traslado'],
    ]);
    expect(resultado.cambios.map((c) => [c.ubicacion, c.antes, c.despues])).toEqual([
      ['Almacén 1', 40, 20],
      ['Almacén 2', 120, 140],
    ]);
  });

  it('reutiliza la misma clave en un reintento de la misma intención', async () => {
    const llamadas = simularServidor({
      'GET /api/v1/productos': () =>
        json({ items: [{ id: '42', sku: 'CEM-001' }], total: 1, page: 1, pageSize: 1 }),
      'GET /api/v1/almacenes': () => json(ALMACENES),
      'POST /api/v1/inventario/movimientos': () =>
        json({ movimientos: [MOVIMIENTO('1208', '102', 'salida', 'ajuste')], cambios: [] }, 201),
    });
    const { registrarMovimiento } = await cargarModulo(() => import('./api'));

    await registrarMovimiento(SOLICITUD);
    await registrarMovimiento(SOLICITUD);

    const envios = llamadas.filter((l) => l.metodo === 'POST');
    expect(envios).toHaveLength(2);
    expect(envios.every((e) => e.cabeceras.get('Idempotency-Key') === CLAVE)).toBe(true);
  });

  it('propaga STOCK_INSUFICIENTE con su código para que la interfaz lo muestre', async () => {
    simularServidor({
      'GET /api/v1/productos': () =>
        json({ items: [{ id: '42', sku: 'CEM-001' }], total: 1, page: 1, pageSize: 1 }),
      'GET /api/v1/almacenes': () => json(ALMACENES),
      'POST /api/v1/inventario/movimientos': () =>
        problema(422, 'STOCK_INSUFICIENTE', 'Stock insuficiente: hay 3 y se solicitan 5'),
    });
    const { registrarMovimiento } = await cargarModulo(() => import('./api'));

    const error = await registrarMovimiento(SOLICITUD).catch((e: unknown) => e);

    expect(error).toMatchObject({ codigo: 'STOCK_INSUFICIENTE', estado: 422 });
  });

  it('rechaza sin enviar nada cuando el producto no existe', async () => {
    const llamadas = simularServidor({
      'GET /api/v1/productos': () => json({ items: [], total: 0, page: 1, pageSize: 1 }),
      'GET /api/v1/almacenes': () => json(ALMACENES),
    });
    const { registrarMovimiento } = await cargarModulo(() => import('./api'));

    await expect(registrarMovimiento(SOLICITUD)).rejects.toThrow('El producto no existe en el catálogo.');
    expect(llamadas.some((l) => l.metodo === 'POST')).toBe(false);
  });
});

describe('selector de productos (rama de la API real)', () => {
  it('lista los productos vigentes en todas las páginas con nombre y SKU', async () => {
    const primera = Array.from({ length: 100 }, (_, i) => ({ sku: `P-${i}`, nombreOficial: `Producto ${i}` }));
    const llamadas = simularServidor({
      'GET /api/v1/productos': (llamada) =>
        llamada.consulta.get('page') === '1'
          ? json({ items: primera, total: 101, page: 1, pageSize: 100 })
          : json({ items: [{ sku: 'ULT-1', nombreOficial: 'Último' }], total: 101, page: 2, pageSize: 100 }),
    });
    const { listarProductosMovibles } = await cargarModulo(() => import('./api'));

    const opciones = await listarProductosMovibles();

    expect(opciones).toHaveLength(101);
    expect(opciones.at(-1)).toEqual({ sku: 'ULT-1', nombre: 'Último' });
    expect(llamadas.every((l) => l.consulta.get('estadoActivo') === 'true')).toBe(true);
  });
});
