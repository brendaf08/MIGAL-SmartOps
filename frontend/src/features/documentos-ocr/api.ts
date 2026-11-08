import { mockRequest } from '@/api/mock';

export interface Documento {
  id: number;
  archivo: string;
  tipo: 'Guía de proveedor' | 'Nota de pedido' | 'Captura de pedido';
  fecha: string;
  usuario: string;
  estado: 'Pendiente' | 'Procesado_OCR' | 'Validado';
}

/** Documentos de prueba hasta contar con la API. */
const DOCUMENTOS: Documento[] = [
  {
    id: 4,
    archivo: 'guia-proveedor.jpg',
    tipo: 'Guía de proveedor',
    fecha: '2026-11-05',
    usuario: 'Rosa Quispe',
    estado: 'Procesado_OCR',
  },
  {
    id: 3,
    archivo: 'pedido-obra-sur.png',
    tipo: 'Captura de pedido',
    fecha: '2026-11-04',
    usuario: 'Lucía Torres',
    estado: 'Validado',
  },
  {
    id: 2,
    archivo: 'nota-manuscrita-02.jpg',
    tipo: 'Nota de pedido',
    fecha: '2026-11-03',
    usuario: 'Lucía Torres',
    estado: 'Validado',
  },
  {
    id: 1,
    archivo: 'guia-aceros.pdf',
    tipo: 'Guía de proveedor',
    fecha: '2026-11-02',
    usuario: 'Rosa Quispe',
    estado: 'Validado',
  },
];

export const listarDocumentos = () => mockRequest(() => [...DOCUMENTOS]);
