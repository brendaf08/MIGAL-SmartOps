// Módulo cotizaciones · Alexander · P17–P18, P23–P24, P27
// Reemplaza cada <EnConstruccion /> por tu página de ./pages/. Mantén codigo y path.
import type { RutaModulo } from '../../shared/tipos';
import EnConstruccion from '../../shared/components/EnConstruccion';

export const rutas: RutaModulo[] = [
  { codigo: 'P17', path: '/cotizaciones', titulo: 'Cotizaciones', elemento: <EnConstruccion codigo="P17" titulo="Cotizaciones" />, enMenu: true },
  { codigo: 'P18', path: '/cotizaciones/nueva', titulo: 'Nueva cotización', elemento: <EnConstruccion codigo="P18" titulo="Nueva cotización" />, enMenu: false },
  { codigo: 'P23', path: '/documentos/procesar', titulo: 'Procesar documento', elemento: <EnConstruccion codigo="P23" titulo="Procesar documento" />, enMenu: true },
  { codigo: 'P24', path: '/documentos/:id/revision', titulo: 'Revisión del OCR', elemento: <EnConstruccion codigo="P24" titulo="Revisión del OCR" />, enMenu: false },
  { codigo: 'P27', path: '/recepciones/nueva', titulo: 'Orden de recepción', elemento: <EnConstruccion codigo="P27" titulo="Orden de recepción" />, enMenu: true },
];
