import { DocumentosPage } from '@/features/documentos-ocr/DocumentosPage';
import { ProcesarDocumentoPage } from '@/features/documentos-ocr/ProcesarDocumentoPage';
import type { RutaModulo } from './tipos';

/** Rutas del módulo `documentos`; la rama dueña del módulo agrega aquí sus pantallas. */
export const RUTAS: RutaModulo[] = [
  { path: '', element: <DocumentosPage /> },
  { path: 'procesar', element: <ProcesarDocumentoPage /> },
];
