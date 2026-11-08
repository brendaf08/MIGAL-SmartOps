import { mockRequest } from '@/api/mock';
import { COTIZACIONES } from './fixtures';
import type { Cotizacion } from './types';

export const listarCotizaciones = () => mockRequest(() => [...COTIZACIONES]);

export const obtenerCotizacion = (id: string) =>
  mockRequest(() => {
    const found = COTIZACIONES.find((c) => c.id === id);
    if (!found) throw new Error('Cotización no encontrada');
    return found;
  });

/** CU-09, flujo alternativo 1. El stock no se consulta ni se reserva hasta convertirla en venta (RF-COT-08). */
export const registrarAceptacion = (id: string) =>
  mockRequest<Cotizacion>(() => {
    const found = COTIZACIONES.find((c) => c.id === id);
    if (!found || found.estado !== 'Emitida')
      throw new Error('Solo se puede aceptar una cotización emitida.');
    found.estado = 'AceptadaCliente';
    return found;
  });
