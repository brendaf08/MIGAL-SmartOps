import { mockRequest } from '@/api/mock';

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
  id: number;
  /** Texto tal como lo leyó el OCR. */
  texto: string;
  /** SKU sugerido o elegido; vacío si no hubo coincidencia. */
  sku: string;
  /** Similitud con el catálogo, de 0 a 1. */
  score: number;
  cantidad: number;
}

export interface Documento {
  id: number;
  archivo: string;
  tipo: TipoDocumento;
  fecha: string;
  usuario: string;
  estado: EstadoDocumento;
  lineas: LineaOcr[];
}

/** Líneas que el OCR devuelve para cualquier guía de prueba. */
const LINEAS_GUIA: LineaOcr[] = [
  { id: 1, texto: 'CEMENTO SOL TIPO 1 42.5KG', sku: 'CEM-001', score: 0.94, cantidad: 100 },
  { id: 2, texto: 'FIERRO COR. 1/2 x 9', sku: 'FIE-003', score: 0.72, cantidad: 40 },
  { id: 3, texto: 'ALAMBRE REC. N16', sku: '', score: 0.31, cantidad: 25 },
];

const copiaLineas = (lineas: LineaOcr[]) => lineas.map((l) => ({ ...l }));

/** Documentos de prueba hasta contar con la API. Se modifican en memoria. */
const DOCUMENTOS: Documento[] = [
  {
    id: 5,
    archivo: 'guia-cemento-05.jpg',
    tipo: 'Guía de proveedor',
    fecha: '2026-11-07',
    usuario: 'Lucía Torres',
    estado: 'Pendiente',
    lineas: copiaLineas(LINEAS_GUIA),
  },
  {
    id: 4,
    archivo: 'guia-proveedor.jpg',
    tipo: 'Guía de proveedor',
    fecha: '2026-11-05',
    usuario: 'Rosa Quispe',
    estado: 'Procesado_OCR',
    lineas: copiaLineas(LINEAS_GUIA),
  },
  {
    id: 3,
    archivo: 'pedido-obra-sur.png',
    tipo: 'Captura de pedido',
    fecha: '2026-11-04',
    usuario: 'Lucía Torres',
    estado: 'Validado',
    lineas: [],
  },
  {
    id: 2,
    archivo: 'nota-manuscrita-02.jpg',
    tipo: 'Nota de pedido',
    fecha: '2026-11-03',
    usuario: 'Lucía Torres',
    estado: 'Validado',
    lineas: [],
  },
  {
    id: 1,
    archivo: 'guia-aceros.pdf',
    tipo: 'Guía de proveedor',
    fecha: '2026-11-02',
    usuario: 'Rosa Quispe',
    estado: 'Validado',
    lineas: [],
  },
];

const buscar = (id: number) => {
  const documento = DOCUMENTOS.find((d) => d.id === id);
  if (!documento) throw new Error('El documento no existe o ya fue descartado.');
  return documento;
};

export const listarDocumentos = () => mockRequest(() => DOCUMENTOS.map((d) => ({ ...d })));

export const obtenerDocumento = (id: number) =>
  mockRequest(() => {
    const documento = buscar(id);
    return { ...documento, lineas: copiaLineas(documento.lineas) };
  });

/**
 * CU-08. Simula el servicio OCR: procesa el archivo y registra el documento. No genera ventas, reservas
 * ni movimientos de Kardex. Un archivo cuyo nombre contiene "ilegible" simula una foto que no se puede leer.
 */
export const procesarDocumento = (archivo: File, tipo: TipoDocumento) =>
  mockRequest<Documento>(() => {
    if (archivo.name.toLowerCase().includes('ilegible')) {
      throw new Error('Documento ilegible.');
    }
    const id = Math.max(...DOCUMENTOS.map((d) => d.id)) + 1;
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
export const validarDocumento = (id: number, lineas: LineaOcr[]) =>
  mockRequest(() => {
    const documento = buscar(id);
    documento.lineas = copiaLineas(lineas);
    documento.estado = 'Validado';
  });

/** Quita el documento de la lista; la acción pide confirmación en la pantalla de revisión. */
export const descartarDocumento = (id: number) =>
  mockRequest(() => {
    const indice = DOCUMENTOS.findIndex((d) => d.id === id);
    if (indice === -1) throw new Error('El documento no existe o ya fue descartado.');
    DOCUMENTOS.splice(indice, 1);
  });
