import { API_REAL, api } from '@/api/cliente';
import { mockRequest } from '@/api/mock';
import { recorrerPaginas } from '@/api/paginacion';

/** Tipos de documento que se cargan desde P23; los dos últimos solo aparecen en documentos ya procesados. */
export type TipoDocumento =
  | 'Guía de proveedor'
  | 'Cotización de cliente'
  | 'Orden de compra'
  | 'Nota de pedido'
  | 'Captura de pedido';

export const TIPOS_CARGA: TipoDocumento[] = [
  'Guía de proveedor',
  'Cotización de cliente',
  'Orden de compra',
];

export type EstadoDocumento = 'Pendiente' | 'Procesado_OCR' | 'Validado';

/** Línea extraída por el OCR y asociada, o no, a un producto del catálogo (RF-FUZ-05). */
export interface LineaOcr {
  /** Identificador técnico de la línea en la API. */
  id: string;
  /** Texto tal como lo leyó el OCR. */
  texto: string;
  /** SKU sugerido o elegido; vacío si no hubo coincidencia. */
  sku: string;
  /** Similitud con el catálogo, de 0 a 1. */
  score: number;
  cantidad: number;
}

export interface Documento {
  id: string;
  archivo: string;
  tipo: TipoDocumento;
  fecha: string;
  usuario: string;
  estado: EstadoDocumento;
  lineas: LineaOcr[];
}

/** Línea de prueba que devuelve el OCR simulado para cualquier guía. */
const LINEAS_GUIA: LineaOcr[] = [
  { id: '1', texto: 'CEMENTO SOL TIPO 1 42.5KG', sku: 'CEM-001', score: 0.94, cantidad: 100 },
  { id: '2', texto: 'FIERRO COR. 1/2 x 9', sku: 'FIE-003', score: 0.72, cantidad: 40 },
  { id: '3', texto: 'ALAMBRE REC. N16', sku: '', score: 0.31, cantidad: 25 },
];

const copiaLineas = (lineas: LineaOcr[]) => lineas.map((l) => ({ ...l }));

/** Documentos de prueba hasta contar con la API. Se modifican en memoria. */
const DOCUMENTOS: Documento[] = [
  {
    id: '5',
    archivo: 'guia-cemento-05.jpg',
    tipo: 'Guía de proveedor',
    fecha: '2026-11-07',
    usuario: 'Lucía Torres',
    estado: 'Pendiente',
    lineas: copiaLineas(LINEAS_GUIA),
  },
  {
    id: '4',
    archivo: 'guia-proveedor.jpg',
    tipo: 'Guía de proveedor',
    fecha: '2026-11-05',
    usuario: 'Rosa Quispe',
    estado: 'Procesado_OCR',
    lineas: copiaLineas(LINEAS_GUIA),
  },
  {
    id: '3',
    archivo: 'pedido-obra-sur.png',
    tipo: 'Captura de pedido',
    fecha: '2026-11-04',
    usuario: 'Lucía Torres',
    estado: 'Validado',
    lineas: [],
  },
  {
    id: '2',
    archivo: 'nota-manuscrita-02.jpg',
    tipo: 'Nota de pedido',
    fecha: '2026-11-03',
    usuario: 'Lucía Torres',
    estado: 'Validado',
    lineas: [],
  },
  {
    id: '1',
    archivo: 'guia-aceros.pdf',
    tipo: 'Guía de proveedor',
    fecha: '2026-11-02',
    usuario: 'Rosa Quispe',
    estado: 'Validado',
    lineas: [],
  },
];

const buscar = (id: string) => {
  const documento = DOCUMENTOS.find((d) => d.id === id);
  if (!documento) throw new Error('El documento no existe o ya fue descartado.');
  return documento;
};

/** Tipo de documento tal como lo recibe la API (contrato documentos-ocr.yaml, DocumentoTipo). */
type TipoApi = 'solicitud_cliente' | 'documento_recepcion';
type EstadoApi = 'pendiente' | 'procesado_ocr' | 'validado';

/** Resumen de documento (esquema DocumentoResumen). */
interface DocumentoResumenApi {
  id: string;
  nombreArchivo: string;
  tipoDocumento: TipoApi;
  estadoProcesamiento: EstadoApi;
  idUsuario: string;
  codigoError: string | null;
  fechaSubida: string;
}

interface CoincidenciaApi {
  idProducto: string;
  sku: string;
  nombreOficial: string;
  confianza: number;
}

/** Línea extraída (esquema DocumentoDetalle). */
interface DetalleApi {
  id: string;
  textoCrudo: string;
  textoCorregido: string | null;
  cantidadLeida: number | null;
  idProducto: string | null;
  sku: string | null;
  coincidencias: CoincidenciaApi[];
  requiereRevision: boolean;
  descartado: boolean;
}

interface DocumentoApi extends DocumentoResumenApi {
  detalles: DetalleApi[];
}

const TIPO_UI: Record<TipoApi, TipoDocumento> = {
  documento_recepcion: 'Guía de proveedor',
  solicitud_cliente: 'Cotización de cliente',
};

/*
 * La interfaz distingue cinco tipos y la API dos. Las órdenes de compra se envían como documento de recepción y
 * los pedidos y notas como solicitud de cliente. Al leer, el tipo recibe la forma más cercana.
 */
const tipoApi = (tipo: TipoDocumento): TipoApi =>
  tipo === 'Cotización de cliente' || tipo === 'Nota de pedido' || tipo === 'Captura de pedido'
    ? 'solicitud_cliente'
    : 'documento_recepcion';

const ESTADO_UI: Record<EstadoApi, EstadoDocumento> = {
  pendiente: 'Pendiente',
  procesado_ocr: 'Procesado_OCR',
  validado: 'Validado',
};

/** Línea de la interfaz: sin producto confirmado, se sugiere la mejor coincidencia del catálogo. */
const aLinea = (linea: DetalleApi): LineaOcr => {
  const mejor = linea.coincidencias[0];
  const confirmada = linea.idProducto
    ? linea.coincidencias.find((c) => c.idProducto === linea.idProducto)
    : undefined;
  return {
    id: linea.id,
    texto: linea.textoCorregido ?? linea.textoCrudo,
    sku: linea.sku ?? mejor?.sku ?? '',
    score: linea.idProducto ? (confirmada?.confianza ?? 1) : (mejor?.confianza ?? 0),
    cantidad: linea.cantidadLeida ?? 0,
  };
};

const aDocumento = (documento: DocumentoResumenApi, detalles: DetalleApi[] = []): Documento => ({
  id: documento.id,
  archivo: documento.nombreArchivo,
  tipo: TIPO_UI[documento.tipoDocumento],
  fecha: documento.fechaSubida.slice(0, 10),
  usuario: documento.idUsuario,
  estado: ESTADO_UI[documento.estadoProcesamiento],
  lineas: detalles.filter((d) => !d.descartado).map(aLinea),
});

const productosPorSku = async (): Promise<Map<string, string>> => {
  const productos = await recorrerPaginas<{ id: string; sku: string }>('/productos');
  return new Map(productos.map((p) => [p.sku, p.id] as const));
};

const INTERVALO_SONDEO_MS = 1500;
const MAXIMO_SONDEOS = 40;

/** Consulta el documento hasta que el OCR termine o registre un error de procesamiento. */
async function esperarProcesamiento(id: string): Promise<DocumentoApi> {
  for (let intento = 0; intento < MAXIMO_SONDEOS; intento += 1) {
    const documento = await api.get<DocumentoApi>(`/documentos/${id}`);
    if (documento.codigoError || documento.estadoProcesamiento !== 'pendiente') return documento;
    await new Promise((resolver) => setTimeout(resolver, INTERVALO_SONDEO_MS));
  }
  throw new Error('El procesamiento del documento sigue en curso. Consultarlo más tarde.');
}

const procesarDocumentoReal = async (archivo: File, tipo: TipoDocumento): Promise<Documento> => {
  const formulario = new FormData();
  formulario.append('archivo', archivo);
  formulario.append('tipoDocumento', tipoApi(tipo));
  const registrado = await api.subir<DocumentoResumenApi>('/documentos', formulario);
  const documento = await esperarProcesamiento(registrado.id);
  if (documento.codigoError === 'DOCUMENTO_ILEGIBLE') throw new Error('Documento ilegible.');
  if (documento.codigoError) throw new Error('No se pudo procesar el documento.');
  return aDocumento(documento, documento.detalles);
};

/*
 * Confirma las líneas: corrige en la API las que cambiaron (producto o cantidad) y después valida el documento.
 * La cantidad cero se envía como nula, porque la API exige una cantidad positiva para una corrección.
 */
const validarDocumentoReal = async (id: string, lineas: LineaOcr[]): Promise<void> => {
  const documento = await api.get<DocumentoApi>(`/documentos/${id}`);
  const previas = new Map(documento.detalles.map((d) => [d.id, d] as const));
  const productos = await productosPorSku();
  for (const linea of lineas) {
    const previa = previas.get(linea.id);
    if (!previa) continue;
    if (linea.sku && !productos.has(linea.sku))
      throw new Error('El producto no existe en el catálogo.');
    const idProducto = linea.sku ? (productos.get(linea.sku) ?? null) : null;
    const cantidad = linea.cantidad > 0 ? linea.cantidad : null;
    const correccion: { idProducto?: string | null; cantidad?: number | null } = {};
    if (idProducto !== previa.idProducto) correccion.idProducto = idProducto;
    if (cantidad !== (previa.cantidadLeida ?? null)) correccion.cantidad = cantidad;
    if (Object.keys(correccion).length > 0) {
      await api.put(`/documentos/${id}/detalles/${linea.id}`, correccion);
    }
  }
  await api.post(`/documentos/${id}/validacion`);
};

/** Documentos de prueba para el listado. */
export const listarDocumentos = (): Promise<Documento[]> =>
  API_REAL
    ? recorrerPaginas<DocumentoResumenApi>('/documentos').then((items) =>
        items.map((d) => aDocumento(d)),
      )
    : mockRequest(() => DOCUMENTOS.map((d) => ({ ...d })));

export const obtenerDocumento = (id: string): Promise<Documento> =>
  API_REAL
    ? api.get<DocumentoApi>(`/documentos/${id}`).then((d) => aDocumento(d, d.detalles))
    : mockRequest(() => {
        const documento = buscar(id);
        return { ...documento, lineas: copiaLineas(documento.lineas) };
      });

/**
 * CU-08. Simula el servicio OCR: procesa el archivo y registra el documento. No genera ventas, reservas
 * ni movimientos de Kardex. Un archivo cuyo nombre contiene "ilegible" simula una foto que no se puede leer.
 */
export const procesarDocumento = (archivo: File, tipo: TipoDocumento): Promise<Documento> =>
  API_REAL
    ? procesarDocumentoReal(archivo, tipo)
    : mockRequest<Documento>(() => {
        if (archivo.name.toLowerCase().includes('ilegible')) {
          throw new Error('Documento ilegible.');
        }
        const id = String(Math.max(...DOCUMENTOS.map((d) => Number(d.id))) + 1);
        const documento: Documento = {
          id,
          archivo: archivo.name,
          tipo,
          fecha: new Date().toISOString().slice(0, 10),
          usuario: 'Sesión actual',
          estado: 'Procesado_OCR',
          lineas: copiaLineas(LINEAS_GUIA),
        };
        DOCUMENTOS.unshift(documento);
        return { ...documento, lineas: copiaLineas(documento.lineas) };
      }, 1000);

/** Marca el documento como validado con las líneas revisadas por el usuario. */
export const validarDocumento = (id: string, lineas: LineaOcr[]): Promise<void> =>
  API_REAL
    ? validarDocumentoReal(id, lineas)
    : mockRequest(() => {
        const documento = buscar(id);
        documento.lineas = copiaLineas(lineas);
        documento.estado = 'Validado';
      });

/** Quita el documento de la lista; la acción pide confirmación en la pantalla de revisión. */
export const descartarDocumento = (id: string): Promise<void> =>
  API_REAL
    ? api.delete<void>(`/documentos/${id}`)
    : mockRequest(() => {
        const indice = DOCUMENTOS.findIndex((d) => d.id === id);
        if (indice === -1) throw new Error('El documento no existe o ya fue descartado.');
        DOCUMENTOS.splice(indice, 1);
      });
