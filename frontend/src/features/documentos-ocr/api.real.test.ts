import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { guardarToken } from '@/api/cliente';
import { activarApiReal, problema, simularServidor, type PeticionSimulada } from '@/test/apiReal';

const documentoApi = (estado: string, extra: Record<string, unknown> = {}) => ({
  id: '31',
  nombreArchivo: 'guia-cemento.jpg',
  tipoDocumento: 'documento_recepcion',
  estadoProcesamiento: estado,
  idUsuario: '7',
  idCotizacion: null,
  idRecepcion: null,
  codigoError: null,
  fechaSubida: '2026-11-08T10:15:00-05:00',
  ...extra,
});

const detalleApi = (extra: Record<string, unknown> = {}) => ({
  id: '48',
  textoCrudo: 'CEMENTO SOL TIPO 1 42.5KG',
  textoCorregido: null,
  cantidadLeida: 100,
  idProducto: null,
  sku: null,
  coincidencias: [
    { idProducto: '101', sku: 'CEM-049', nombreOficial: 'Cemento Sol tipo I', confianza: 0.94 },
  ],
  requiereRevision: true,
  descartado: false,
  ...extra,
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

describe('documentos OCR con la API real', () => {
  it('lista los documentos con su tipo y estado en la forma de la interfaz', async () => {
    const { listarDocumentos } = await import('./api');
    simularServidor(() => ({
      cuerpo: {
        items: [
          documentoApi('procesado_ocr'),
          documentoApi('validado', { tipoDocumento: 'solicitud_cliente', id: '30' }),
        ],
        total: 2,
        page: 1,
        pageSize: 100,
      },
    }));

    const documentos = await listarDocumentos();

    expect(documentos).toEqual([
      expect.objectContaining({
        id: '31',
        archivo: 'guia-cemento.jpg',
        tipo: 'Guía de proveedor',
        fecha: '2026-11-08',
        estado: 'Procesado_OCR',
        lineas: [],
      }),
      expect.objectContaining({ id: '30', tipo: 'Cotización de cliente', estado: 'Validado' }),
    ]);
  });

  it('consulta el documento con sus líneas, sin las descartadas, y sugiere la mejor coincidencia', async () => {
    const { obtenerDocumento } = await import('./api');
    const llamadas: PeticionSimulada[] = [];
    simularServidor((peticion) => {
      llamadas.push(peticion);
      return {
        cuerpo: {
          ...documentoApi('procesado_ocr'),
          detalles: [detalleApi(), detalleApi({ id: '49', descartado: true })],
        },
      };
    });

    const documento = await obtenerDocumento('31');

    expect((llamadas[0] as PeticionSimulada).url.pathname).toBe('/api/v1/documentos/31');
    expect(documento.lineas).toEqual([
      { id: '48', texto: 'CEMENTO SOL TIPO 1 42.5KG', sku: 'CEM-049', score: 0.94, cantidad: 100 },
    ]);
  });

  it('sube el archivo como multipart sin fijar Content-Type y espera el resultado del OCR', async () => {
    const { procesarDocumento } = await import('./api');
    const llamadas: PeticionSimulada[] = [];
    simularServidor((peticion) => {
      llamadas.push(peticion);
      if (peticion.metodo === 'POST') return { estado: 202, cuerpo: documentoApi('pendiente') };
      return { cuerpo: { ...documentoApi('procesado_ocr'), detalles: [detalleApi()] } };
    });
    const archivo = new File(['imagen'], 'guia-cemento.jpg', { type: 'image/jpeg' });

    const documento = await procesarDocumento(archivo, 'Cotización de cliente');

    const subida = llamadas[0] as PeticionSimulada;
    expect(subida.metodo).toBe('POST');
    expect(subida.url.pathname).toBe('/api/v1/documentos');
    expect(subida.cabeceras.get('Authorization')).toBe('Bearer token-de-prueba');
    expect(subida.cabeceras.has('Content-Type')).toBe(false);
    const formulario = subida.cuerpo as FormData;
    expect(formulario).toBeInstanceOf(FormData);
    expect(formulario.get('tipoDocumento')).toBe('solicitud_cliente');
    expect((formulario.get('archivo') as File).name).toBe('guia-cemento.jpg');
    expect(documento).toMatchObject({
      id: '31',
      tipo: 'Guía de proveedor',
      estado: 'Procesado_OCR',
    });
    expect(documento.lineas).toHaveLength(1);
  });

  it('informa "Documento ilegible." cuando el OCR no puede leer el archivo', async () => {
    const { procesarDocumento } = await import('./api');
    simularServidor((peticion) => {
      if (peticion.metodo === 'POST') return { estado: 202, cuerpo: documentoApi('pendiente') };
      return { cuerpo: documentoApi('pendiente', { codigoError: 'DOCUMENTO_ILEGIBLE' }) };
    });

    await expect(
      procesarDocumento(new File(['x'], 'foto.jpg', { type: 'image/jpeg' }), 'Guía de proveedor'),
    ).rejects.toThrow('Documento ilegible.');
  });

  it('corrige en la API solo las líneas que cambiaron y después valida el documento', async () => {
    const { validarDocumento } = await import('./api');
    const llamadas: PeticionSimulada[] = [];
    simularServidor((peticion) => {
      llamadas.push(peticion);
      if (peticion.url.pathname === '/api/v1/productos') {
        return {
          cuerpo: { items: [{ id: '101', sku: 'CEM-049' }], total: 1, page: 1, pageSize: 100 },
        };
      }
      if (peticion.metodo === 'GET') {
        return { cuerpo: { ...documentoApi('procesado_ocr'), detalles: [detalleApi()] } };
      }
      return { cuerpo: documentoApi('validado') };
    });

    await validarDocumento('31', [
      { id: '48', texto: 'CEMENTO', sku: 'CEM-049', score: 0.94, cantidad: 120 },
    ]);

    const correccion = llamadas.find((p) => p.metodo === 'PUT');
    expect(correccion?.url.pathname).toBe('/api/v1/documentos/31/detalles/48');
    expect(correccion?.cuerpo).toEqual({ idProducto: '101', cantidad: 120 });
    const validacion = llamadas.find((p) => p.url.pathname === '/api/v1/documentos/31/validacion');
    expect(validacion?.metodo).toBe('POST');
  });

  it('no envía correcciones cuando la línea no cambió y propaga VALIDACION_FALLIDA con sus errores', async () => {
    const { validarDocumento } = await import('./api');
    const llamadas: PeticionSimulada[] = [];
    simularServidor((peticion) => {
      llamadas.push(peticion);
      if (peticion.url.pathname === '/api/v1/productos') {
        return {
          cuerpo: { items: [{ id: '101', sku: 'CEM-049' }], total: 1, page: 1, pageSize: 100 },
        };
      }
      if (peticion.metodo === 'GET') {
        return {
          cuerpo: {
            ...documentoApi('procesado_ocr'),
            detalles: [detalleApi({ idProducto: '101', sku: 'CEM-049' })],
          },
        };
      }
      return problema(422, 'VALIDACION_FALLIDA', {
        detail: 'Hay líneas sin producto.',
        errores: [
          { campo: 'lineas', codigo: 'PRODUCTO_REQUERIDO', mensaje: 'Asociar un producto.' },
        ],
      });
    });

    const error = await validarDocumento('31', [
      { id: '48', texto: 'CEMENTO', sku: 'CEM-049', score: 0.94, cantidad: 100 },
    ]).catch((e: unknown) => e);

    expect(llamadas.some((p) => p.metodo === 'PUT')).toBe(false);
    expect(error).toMatchObject({
      codigo: 'VALIDACION_FALLIDA',
      message: 'Hay líneas sin producto.',
      errores: [{ campo: 'lineas', codigo: 'PRODUCTO_REQUERIDO', mensaje: 'Asociar un producto.' }],
    });
  });

  it('descarta el documento con DELETE sobre su ruta', async () => {
    const { descartarDocumento } = await import('./api');
    const llamadas: PeticionSimulada[] = [];
    simularServidor((peticion) => {
      llamadas.push(peticion);
      return { estado: 204 };
    });

    await descartarDocumento('31');

    expect(llamadas[0] as PeticionSimulada).toMatchObject({ metodo: 'DELETE' });
    expect((llamadas[0] as PeticionSimulada).url.pathname).toBe('/api/v1/documentos/31');
  });
});
