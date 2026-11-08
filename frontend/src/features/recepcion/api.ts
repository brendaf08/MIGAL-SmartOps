import { mockRequest } from '@/api/mock';
import type { DocumentoProcesado, LineaRecepcion, Recepcion } from './types';

/** Recepciones de prueba hasta contar con la API. Se modifican en memoria. */
export const RECEPCIONES: Recepcion[] = [
  {
    id: 'R-0012',
    orden: 'OC-0086',
    proveedor: 'Aceros del Sur S.A.',
    almacen: 'Almacén 2',
    fecha: '2026-11-02',
    estado: 'Validada',
  },
  {
    id: 'R-0011',
    orden: 'OC-0084',
    proveedor: 'Distribuidora Andina S.A.C.',
    almacen: 'Almacén 1',
    fecha: '2026-10-29',
    estado: 'Validada',
  },
];

export const listarRecepciones = () => mockRequest(() => [...RECEPCIONES]);

/** CU-08. Simula el servicio OCR y Fuzzy Matching: solo precarga datos, no genera movimientos (RF-COM-15). */
export const procesarDocumento = (_archivo: File) =>
  mockRequest<DocumentoProcesado>(
    () => ({
      orden: 'OC-0087',
      proveedor: 'Distribuidora Andina S.A.C.',
      almacen: 'Almacén 1',
      lineas: [
        {
          id: 1,
          texto: 'CEMENTO SOL TIPO 1 42.5KG',
          sku: 'CEM-001',
          score: 0.94,
          pedido: 100,
          recibido: 100,
        },
        {
          id: 2,
          texto: 'FIERRO COR. 1/2 x 9',
          sku: 'FIE-003',
          score: 0.72,
          pedido: 60,
          recibido: 40,
        },
        { id: 3, texto: 'ALAMBRE REC. N16', sku: '', score: 0.31, pedido: 25, recibido: 25 },
      ],
    }),
    1200,
  );

/** CU-13. Los movimientos de entrada al Kardex se generan solo aquí, con las cantidades efectivamente recibidas. */
export const confirmarRecepcion = (
  doc: Pick<DocumentoProcesado, 'orden' | 'proveedor' | 'almacen'>,
  lineas: LineaRecepcion[],
) =>
  mockRequest<Recepcion>(() => {
    if (lineas.some((l) => !l.sku))
      throw new Error('Asociar un producto a cada línea antes de confirmar.');
    const recepcion: Recepcion = {
      id: `R-${String(11 + RECEPCIONES.length + 1).padStart(4, '0')}`,
      ...doc,
      fecha: new Date().toISOString().slice(0, 10),
      estado: 'Validada',
    };
    RECEPCIONES.unshift(recepcion);
    return recepcion;
  });
