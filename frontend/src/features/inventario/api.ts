import { mockRequest } from '@/api/mock';
import { resumen } from './calculos';
import { PRODUCTOS } from './fixtures';
import type { ConsultaDisponibilidad, RespuestaDisponibilidad } from './types';

const enAlerta = (nivel: string) => nivel !== 'Normal';

/** CU-04. Búsqueda, filtro y paginación se resuelven en el servidor (RNF-06); aquí los simula. */
export const consultarDisponibilidad = ({
  q,
  soloAlerta,
  page,
  pageSize,
}: ConsultaDisponibilidad) =>
  mockRequest<RespuestaDisponibilidad>(() => {
    const term = q.trim().toLowerCase();
    const found = PRODUCTOS.filter(
      (p) =>
        (!soloAlerta || enAlerta(resumen(p).nivel)) &&
        (!term || `${p.sku} ${p.nombre}`.toLowerCase().includes(term)),
    );
    return {
      items: found.slice((page - 1) * pageSize, page * pageSize),
      total: found.length,
      enAlerta: PRODUCTOS.filter((p) => enAlerta(resumen(p).nivel)).length,
      totalGeneral: PRODUCTOS.length,
    };
  });
