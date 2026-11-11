import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import {
  activarApiReal,
  cargarModulo,
  json,
  problema,
  restablecerApiReal,
  simularServidor,
} from '@/test/servidorApi';
import { SIN_FILTROS_KARDEX } from './api';

const EXISTENCIAS_CEM = {
  productoId: '42',
  sku: 'CEM-001',
  nombre: 'Cemento Sol tipo I, bolsa 42.5 kg',
  categoria: 'Cemento',
  unidad: 'bolsa',
  stockMinimo: 200,
  fisico: 160,
  reservado: 40,
  libre: 120,
  nivelAlerta: 'minimo',
  ubicaciones: [
    { ubicacionId: '101', nombre: 'Tienda principal', fisica: 40, reservada: 0, libre: 40 },
    { ubicacionId: '102', nombre: 'Almacén 1', fisica: 120, reservada: 40, libre: 80 },
  ],
};

const ALMACENES = [
  { id: '101', nombre: 'Tienda principal', tipoUbicacion: 'tienda', direccionReferencia: null, estadoActivo: true },
  { id: '102', nombre: 'Almacén 1', tipoUbicacion: 'almacen', direccionReferencia: null, estadoActivo: true },
];

const MOVIMIENTO_AJUSTE = {
  id: '1208',
  fechaHora: '2026-11-07T14:32:10-05:00',
  productoId: '42',
  sku: 'CEM-001',
  nombreProducto: 'Cemento Sol tipo I, bolsa 42.5 kg',
  tipoOperacion: 'salida',
  cantidad: 5,
  origenMovimiento: 'ajuste',
  ubicacionId: '102',
  usuarioId: '7',
  motivo: 'Bolsas dañadas por humedad',
  idGuia: null,
  idRecepcion: null,
  referenciaTraslado: null,
  idMovimientoCorregido: null,
};

beforeEach(activarApiReal);
afterEach(restablecerApiReal);

describe('inventario, disponibilidad (rama de la API real)', () => {
  it('envía los filtros con el token y convierte las existencias y los contadores', async () => {
    const llamadas = simularServidor({
      'GET /api/v1/inventario/disponibilidad': () =>
        json({
          items: [EXISTENCIAS_CEM],
          total: 1,
          page: 1,
          pageSize: 20,
          enAlerta: 5,
          totalGeneral: 48,
        }),
    });
    const { consultarDisponibilidad } = await cargarModulo(() => import('./api'));

    const respuesta = await consultarDisponibilidad({ q: ' cem ', soloAlerta: true, page: 1, pageSize: 20 });

    const llamada = llamadas[0]!;
    expect(llamada.metodo).toBe('GET');
    expect(llamada.consulta.get('q')).toBe('cem');
    expect(llamada.consulta.get('soloAlerta')).toBe('true');
    expect(llamada.consulta.get('page')).toBe('1');
    expect(llamada.consulta.get('pageSize')).toBe('20');
    expect(llamada.cabeceras.get('Authorization')).toBe('Bearer TOKEN-PRUEBA');
    expect(llamada.cabeceras.has('Idempotency-Key')).toBe(false);
    expect(respuesta).toEqual({
      items: [
        {
          sku: 'CEM-001',
          nombre: 'Cemento Sol tipo I, bolsa 42.5 kg',
          categoria: 'Cemento',
          unidad: 'bolsa',
          tienda: 40,
          almacen1: 120,
          almacen2: 0,
          reservado: 40,
          minimo: 200,
        },
      ],
      total: 1,
      enAlerta: 5,
      totalGeneral: 48,
    });
  });
});

describe('inventario, alertas (rama de la API real)', () => {
  it('une la lista de alertas con las existencias, conserva el orden y excluye el nivel normal', async () => {
    simularServidor({
      'GET /api/v1/inventario/alertas': () =>
        json({
          items: [
            {
              productoId: '44',
              sku: 'AGR-004',
              nombre: 'Arena gruesa, metro cúbico',
              unidad: 'm³',
              stockMinimo: 10,
              libre: 0,
              faltante: 10,
              nivelAlerta: 'critico',
            },
            {
              productoId: '42',
              sku: 'CEM-001',
              nombre: 'Cemento Sol tipo I, bolsa 42.5 kg',
              unidad: 'bolsa',
              stockMinimo: 200,
              libre: 120,
              faltante: 80,
              nivelAlerta: 'minimo',
            },
            { productoId: '50', sku: 'LAD-002', nombre: 'Ladrillo', unidad: 'unidad', stockMinimo: 800, libre: 900, faltante: -100, nivelAlerta: 'normal' },
          ],
          total: 3,
          page: 1,
          pageSize: 100,
        }),
      'GET /api/v1/inventario/disponibilidad': () =>
        json({
          items: [
            EXISTENCIAS_CEM,
            { ...EXISTENCIAS_CEM, productoId: '44', sku: 'AGR-004', nombre: 'Arena gruesa, metro cúbico', unidad: 'm³', stockMinimo: 10, fisico: 0, reservado: 0, libre: 0, nivelAlerta: 'critico', ubicaciones: [] },
          ],
          total: 2,
          page: 1,
          pageSize: 100,
          enAlerta: 2,
          totalGeneral: 48,
        }),
    });
    const { listarAlertas } = await cargarModulo(() => import('./api'));

    const alertas = await listarAlertas();

    expect(alertas.map((a) => [a.sku, a.nivel, a.libre, a.faltante, a.reservado])).toEqual([
      ['AGR-004', 'Critico', 0, 10, 0],
      ['CEM-001', 'Minimo', 120, 80, 40],
    ]);
  });

  it('recorre todas las páginas del listado de alertas', async () => {
    const primera = Array.from({ length: 100 }, (_, i) => ({
      productoId: String(i),
      sku: `ALR-${String(i).padStart(3, '0')}`,
      nombre: `Producto ${i}`,
      unidad: 'unidad',
      stockMinimo: 10,
      libre: 0,
      faltante: 10,
      nivelAlerta: 'critico',
    }));
    const llamadas = simularServidor({
      'GET /api/v1/inventario/alertas': (llamada) =>
        llamada.consulta.get('page') === '1'
          ? json({ items: primera, total: 101, page: 1, pageSize: 100 })
          : json({ items: [{ ...primera[0], sku: 'ULT-999' }], total: 101, page: 2, pageSize: 100 }),
      'GET /api/v1/inventario/disponibilidad': () =>
        json({ items: [], total: 0, page: 1, pageSize: 100, enAlerta: 0, totalGeneral: 0 }),
    });
    const { listarAlertas } = await cargarModulo(() => import('./api'));

    const alertas = await listarAlertas();

    expect(alertas).toHaveLength(101);
    expect(alertas.at(-1)!.sku).toBe('ULT-999');
    expect(llamadas.filter((l) => l.ruta === '/api/v1/inventario/alertas')).toHaveLength(2);
  });

  it('propaga el error de permiso de la API', async () => {
    simularServidor({
      'GET /api/v1/inventario/alertas': () => problema(403, 'PERMISO_INSUFICIENTE', 'Sin permiso.'),
      'GET /api/v1/inventario/disponibilidad': () =>
        json({ items: [], total: 0, page: 1, pageSize: 100, enAlerta: 0, totalGeneral: 0 }),
    });
    const { listarAlertas } = await cargarModulo(() => import('./api'));

    await expect(listarAlertas()).rejects.toMatchObject({ codigo: 'PERMISO_INSUFICIENTE', estado: 403 });
  });
});

describe('inventario, Kardex (rama de la API real)', () => {
  it('traduce los filtros de la interfaz a los parámetros del contrato y mapea los movimientos', async () => {
    const llamadas = simularServidor({
      'GET /api/v1/almacenes': () => json(ALMACENES),
      'GET /api/v1/inventario/movimientos': () =>
        json({ items: [MOVIMIENTO_AJUSTE], total: 1, page: 1, pageSize: 100 }),
    });
    const { listarMovimientosFiltrados } = await cargarModulo(() => import('./api'));

    const movimientos = await listarMovimientosFiltrados({
      q: 'cem',
      tipo: 'Salida',
      origen: 'Ajuste',
      ubicacion: 'Almacén 1',
      desde: '2026-11-01',
      hasta: '2026-11-07',
    });

    const consulta = llamadas.find((l) => l.ruta === '/api/v1/inventario/movimientos')!.consulta;
    expect(consulta.get('q')).toBe('cem');
    expect(consulta.get('tipoOperacion')).toBe('salida');
    expect(consulta.get('origen')).toBe('ajuste');
    expect(consulta.get('ubicacionId')).toBe('102');
    expect(consulta.get('desde')).toBe('2026-11-01');
    expect(consulta.get('hasta')).toBe('2026-11-07');
    expect(movimientos).toEqual([
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
  });

  it('no consulta movimientos y devuelve una lista vacía cuando la ubicación no existe', async () => {
    const llamadas = simularServidor({ 'GET /api/v1/almacenes': () => json(ALMACENES) });
    const { listarMovimientosFiltrados } = await cargarModulo(() => import('./api'));

    const movimientos = await listarMovimientosFiltrados({ ...SIN_FILTROS_KARDEX, ubicacion: 'Almacén 9' });

    expect(movimientos).toEqual([]);
    expect(llamadas.some((l) => l.ruta === '/api/v1/inventario/movimientos')).toBe(false);
  });

  it('omite los filtros vacíos y recorre todas las páginas del Kardex', async () => {
    const lote = (inicio: number, cantidad: number) =>
      Array.from({ length: cantidad }, (_, i) => ({ ...MOVIMIENTO_AJUSTE, id: String(inicio + i) }));
    const llamadas = simularServidor({
      'GET /api/v1/almacenes': () => json(ALMACENES),
      'GET /api/v1/inventario/movimientos': (llamada) =>
        llamada.consulta.get('page') === '1'
          ? json({ items: lote(1, 100), total: 102, page: 1, pageSize: 100 })
          : json({ items: lote(101, 2), total: 102, page: 2, pageSize: 100 }),
    });
    const { listarMovimientosFiltrados, SIN_FILTROS_KARDEX: sinFiltros } = await cargarModulo(() => import('./api'));

    const movimientos = await listarMovimientosFiltrados(sinFiltros);

    const consultas = llamadas.filter((l) => l.ruta === '/api/v1/inventario/movimientos');
    expect(consultas).toHaveLength(2);
    expect([...consultas[0]!.consulta.keys()].sort()).toEqual(['page', 'pageSize']);
    expect(movimientos).toHaveLength(102);
    expect(movimientos.at(-1)!.id).toBe('102');
  });
});
