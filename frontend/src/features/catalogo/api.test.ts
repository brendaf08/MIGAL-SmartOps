import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

/** Respuesta JSON simulada del servidor. */
const json = (cuerpo: unknown, estado = 200) =>
  new Response(JSON.stringify(cuerpo), { status: estado, headers: { 'Content-Type': 'application/json' } });

const CATEGORIAS = [
  { id: '5', nombre: 'Ladrillos', idCategoriaPadre: null, aprobacionAutomatica: true },
  { id: '3', nombre: 'Cemento', idCategoriaPadre: null, aprobacionAutomatica: true },
];

const PRODUCTO = {
  id: '101',
  sku: 'CEM-049',
  nombreOficial: 'Cemento Sol tipo I 42.5 kg',
  descripcion: null,
  unidadMedida: 'bolsa',
  precioReferencial: 28.5,
  stockMinimo: 20,
  estadoActivo: true,
  idCategoria: '3',
  marca: 'Sol',
  presentacion: 'Bolsa 42.5 kg',
  fechaRegistro: '2026-01-15T09:30:00-05:00',
  fechaActualizacion: null,
};

const pagina = <T,>(items: T[]) => ({ items, total: items.length, page: 1, pageSize: 20 });

/** Llamada registrada por el `fetch` simulado. */
interface Llamada {
  metodo: string;
  ruta: string;
  consulta: URLSearchParams;
  cabeceras: Headers;
  cuerpo: unknown;
}

const llamadas: Llamada[] = [];

/**
 * Sustituye `fetch` por un servidor simulado: cada clave "METODO /ruta" devuelve la respuesta indicada y se registra la
 * llamada para comprobar ruta, cabeceras y cuerpo.
 */
function simularServidor(respuestas: Record<string, () => Response>) {
  const fetchSimulado = vi.fn(async (url: string, init: RequestInit = {}) => {
    const destino = new URL(url, 'http://localhost');
    const metodo = init.method ?? 'GET';
    llamadas.push({
      metodo,
      ruta: destino.pathname,
      consulta: destino.searchParams,
      cabeceras: new Headers(init.headers),
      cuerpo: init.body === undefined ? undefined : JSON.parse(String(init.body)),
    });
    const respuesta = respuestas[`${metodo} ${destino.pathname}`];
    if (!respuesta) throw new Error(`Ruta no simulada: ${metodo} ${destino.pathname}`);
    return respuesta();
  });
  vi.stubGlobal('fetch', fetchSimulado);
  return fetchSimulado;
}

/** Carga el módulo con la API real activa; `resetModules` vuelve a evaluar `API_REAL` con el entorno indicado. */
async function cargarApi() {
  vi.resetModules();
  return import('./api');
}

beforeEach(() => {
  vi.stubEnv('VITE_API_REAL', 'true');
  window.sessionStorage.setItem('migal-token', 'TOKEN-PRUEBA');
  llamadas.length = 0;
});

afterEach(() => {
  vi.unstubAllEnvs();
  vi.unstubAllGlobals();
  window.sessionStorage.clear();
});

describe('catálogo, rama de la API real', () => {
  it('busca productos con paginación, filtros y el token de sesión, y convierte la respuesta', async () => {
    simularServidor({
      'GET /api/v1/categorias': () => json(CATEGORIAS),
      'GET /api/v1/productos': () => json({ items: [PRODUCTO], total: 41, page: 2, pageSize: 20 }),
    });
    const { buscarCatalogo } = await cargarApi();

    const resultado = await buscarCatalogo({ q: ' cem ', categoria: 'Cemento', estado: 'Activo', page: 2, pageSize: 20 });

    const busqueda = llamadas.find((l) => l.ruta === '/api/v1/productos')!;
    expect(busqueda.metodo).toBe('GET');
    expect(busqueda.consulta.get('q')).toBe('cem');
    expect(busqueda.consulta.get('idCategoria')).toBe('3');
    expect(busqueda.consulta.get('estadoActivo')).toBe('true');
    expect(busqueda.consulta.get('page')).toBe('2');
    expect(busqueda.consulta.get('pageSize')).toBe('20');
    expect(busqueda.cabeceras.get('Authorization')).toBe('Bearer TOKEN-PRUEBA');
    expect(busqueda.cabeceras.has('Idempotency-Key')).toBe(false);
    expect(resultado.total).toBe(41);
    expect(resultado.items[0]).toEqual({
      id: '101',
      sku: 'CEM-049',
      nombre: 'Cemento Sol tipo I 42.5 kg',
      descripcion: '',
      categoria: 'Cemento',
      unidad: 'bolsa',
      minimo: 20,
      precioReferencial: 28.5,
      estado: 'Activo',
    });
  });

  it('omite los filtros vacíos y consulta los inactivos con estadoActivo=false', async () => {
    simularServidor({
      'GET /api/v1/categorias': () => json(CATEGORIAS),
      'GET /api/v1/productos': () => json({ ...pagina([{ ...PRODUCTO, estadoActivo: false }]), page: 1, pageSize: 20 }),
    });
    const { buscarCatalogo } = await cargarApi();

    const resultado = await buscarCatalogo({ q: '', categoria: '', estado: 'Inactivo', page: 1, pageSize: 20 });

    const busqueda = llamadas.find((l) => l.ruta === '/api/v1/productos')!;
    expect([...busqueda.consulta.keys()].sort()).toEqual(['estadoActivo', 'page', 'pageSize']);
    expect(busqueda.consulta.get('estadoActivo')).toBe('false');
    expect(resultado.items[0]!.estado).toBe('Inactivo');
  });

  it('devuelve una página vacía sin consultar productos cuando la categoría no existe', async () => {
    simularServidor({ 'GET /api/v1/categorias': () => json(CATEGORIAS) });
    const { buscarCatalogo } = await cargarApi();

    const resultado = await buscarCatalogo({ q: '', categoria: 'Inexistente', estado: '', page: 1, pageSize: 20 });

    expect(resultado).toEqual({ items: [], total: 0 });
    expect(llamadas.some((l) => l.ruta === '/api/v1/productos')).toBe(false);
  });

  it('obtiene un producto por SKU y devuelve null cuando no existe', async () => {
    simularServidor({
      'GET /api/v1/categorias': () => json(CATEGORIAS),
      'GET /api/v1/productos': () => json(pagina([])),
    });
    const { obtenerProducto } = await cargarApi();

    expect(await obtenerProducto('XXX-999')).toBeNull();
    expect(llamadas.find((l) => l.ruta === '/api/v1/productos')!.consulta.get('sku')).toBe('XXX-999');
  });

  it('lista las categorías por nombre en orden alfabético', async () => {
    simularServidor({ 'GET /api/v1/categorias': () => json(CATEGORIAS) });
    const { listarCategorias } = await cargarApi();

    expect(await listarCategorias()).toEqual(['Cemento', 'Ladrillos']);
  });

  it('registra un producto con el cuerpo del contrato y sin Idempotency-Key', async () => {
    simularServidor({
      'GET /api/v1/categorias': () => json(CATEGORIAS),
      'POST /api/v1/productos': () => json(PRODUCTO, 201),
    });
    const { registrarProducto } = await cargarApi();

    const creado = await registrarProducto({
      sku: 'CEM-049',
      nombre: 'Cemento Sol tipo I 42.5 kg',
      descripcion: '',
      categoria: 'Cemento',
      unidad: 'bolsa',
      minimo: 20,
      precioReferencial: 28.5,
      estado: 'Activo',
    });

    const alta = llamadas.find((l) => l.metodo === 'POST')!;
    expect(alta.ruta).toBe('/api/v1/productos');
    expect(alta.cabeceras.get('Authorization')).toBe('Bearer TOKEN-PRUEBA');
    expect(alta.cabeceras.has('Idempotency-Key')).toBe(false);
    expect(alta.cuerpo).toEqual({
      sku: 'CEM-049',
      nombreOficial: 'Cemento Sol tipo I 42.5 kg',
      descripcion: null,
      unidadMedida: 'bolsa',
      precioReferencial: 28.5,
      stockMinimo: 20,
      idCategoria: '3',
      estadoActivo: true,
    });
    expect(creado.id).toBe('101');
    expect(creado.categoria).toBe('Cemento');
  });

  it('traduce SKU_DUPLICADO al error tipado de la interfaz', async () => {
    simularServidor({
      'GET /api/v1/categorias': () => json(CATEGORIAS),
      'POST /api/v1/productos': () =>
        json({ type: 'about:blank', title: 'Conflicto', status: 409, detail: 'Duplicado', instance: '/api/v1/productos', code: 'SKU_DUPLICADO' }, 409),
    });
    const { registrarProducto, SkuDuplicadoError } = await cargarApi();

    await expect(
      registrarProducto({
        sku: 'CEM-001',
        nombre: 'Cemento duplicado',
        descripcion: '',
        categoria: 'Cemento',
        unidad: 'bolsa',
        minimo: 5,
        precioReferencial: 0,
        estado: 'Activo',
      }),
    ).rejects.toBeInstanceOf(SkuDuplicadoError);
  });

  it('modifica los datos sin cambiar el estado cuando no difiere', async () => {
    simularServidor({
      'GET /api/v1/categorias': () => json(CATEGORIAS),
      'GET /api/v1/productos': () => json(pagina([PRODUCTO])),
      'PUT /api/v1/productos/101': () => json({ ...PRODUCTO, stockMinimo: 25 }),
    });
    const { actualizarProducto } = await cargarApi();

    const actualizado = await actualizarProducto('CEM-049', {
      sku: 'CEM-049',
      nombre: 'Cemento Sol tipo I 42.5 kg',
      descripcion: 'Uso general',
      categoria: 'Cemento',
      unidad: 'bolsa',
      minimo: 25,
      precioReferencial: 29,
      estado: 'Activo',
    });

    const modificacion = llamadas.find((l) => l.metodo === 'PUT' && l.ruta === '/api/v1/productos/101')!;
    expect(modificacion.cuerpo).toEqual({
      nombreOficial: 'Cemento Sol tipo I 42.5 kg',
      descripcion: 'Uso general',
      unidadMedida: 'bolsa',
      precioReferencial: 29,
      stockMinimo: 25,
      idCategoria: '3',
    });
    expect(llamadas.some((l) => l.ruta.endsWith('/estado'))).toBe(false);
    expect(actualizado.minimo).toBe(25);
  });

  it('cambia el estado con PUT /productos/{id}/estado cuando difiere del registrado', async () => {
    simularServidor({
      'GET /api/v1/categorias': () => json(CATEGORIAS),
      'GET /api/v1/productos': () => json(pagina([PRODUCTO])),
      'PUT /api/v1/productos/101': () => json(PRODUCTO),
      'PUT /api/v1/productos/101/estado': () => json({ ...PRODUCTO, estadoActivo: false }),
    });
    const { actualizarProducto } = await cargarApi();

    const actualizado = await actualizarProducto('CEM-049', {
      sku: 'CEM-049',
      nombre: 'Cemento Sol tipo I 42.5 kg',
      descripcion: '',
      categoria: 'Cemento',
      unidad: 'bolsa',
      minimo: 20,
      precioReferencial: 28.5,
      estado: 'Inactivo',
    });

    const cambioEstado = llamadas.find((l) => l.ruta === '/api/v1/productos/101/estado')!;
    expect(cambioEstado.metodo).toBe('PUT');
    expect(cambioEstado.cuerpo).toEqual({ estadoActivo: false });
    expect(actualizado.estado).toBe('Inactivo');
  });

  it('convierte las existencias del contrato en disponibilidad por tienda y almacenes', async () => {
    simularServidor({
      'GET /api/v1/productos': () => json(pagina([PRODUCTO])),
      'GET /api/v1/productos/101/existencias': () =>
        json({
          productoId: '101',
          sku: 'CEM-049',
          nombre: 'Cemento Sol tipo I 42.5 kg',
          categoria: 'Cemento',
          unidad: 'bolsa',
          stockMinimo: 20,
          fisico: 160,
          reservado: 40,
          libre: 120,
          nivelAlerta: 'normal',
          ubicaciones: [
            { ubicacionId: '1', nombre: 'Tienda principal', fisica: 40, reservada: 0, libre: 40 },
            { ubicacionId: '2', nombre: 'Almacén 1', fisica: 120, reservada: 40, libre: 80 },
          ],
        }),
    });
    const { obtenerExistencias } = await cargarApi();

    const disponibilidad = await obtenerExistencias('CEM-049');

    expect(disponibilidad).toEqual({
      sku: 'CEM-049',
      nombre: 'Cemento Sol tipo I 42.5 kg',
      categoria: 'Cemento',
      unidad: 'bolsa',
      tienda: 40,
      almacen1: 120,
      almacen2: 0,
      reservado: 40,
      minimo: 20,
    });
  });

  it('propaga los errores de la API que la interfaz no traduce', async () => {
    simularServidor({
      'GET /api/v1/categorias': () => json(CATEGORIAS),
      'GET /api/v1/productos': () => json(pagina([PRODUCTO])),
      'PUT /api/v1/productos/101': () =>
        json({ type: 'about:blank', title: 'Permiso', status: 403, detail: 'Sin permiso.', instance: '/api/v1/productos/101', code: 'PERMISO_INSUFICIENTE' }, 403),
    });
    const { actualizarProducto } = await cargarApi();

    await expect(
      actualizarProducto('CEM-049', {
        sku: 'CEM-049',
        nombre: 'Cemento',
        descripcion: '',
        categoria: 'Cemento',
        unidad: 'bolsa',
        minimo: 20,
        precioReferencial: 28.5,
        estado: 'Activo',
      }),
    ).rejects.toMatchObject({ codigo: 'PERMISO_INSUFICIENTE', estado: 403 });
  });
});
