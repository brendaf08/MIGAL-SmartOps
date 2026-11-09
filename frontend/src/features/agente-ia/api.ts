import { mockRequest } from '@/api/mock';
import { formatNumber } from '@/lib/format';
import { resumen } from '@/features/inventario/calculos';
import { PRODUCTOS } from '@/features/inventario/fixtures';

export interface RespuestaAgente {
  /** Consultas al backend que respaldan la respuesta. */
  consultas: string[];
  texto: string;
  /** Sin información suficiente: no se inventan cantidades ni ubicaciones (RF-IA-08). */
  incierta?: boolean;
}

const normalizar = (s: string) => s.normalize('NFD').replace(/\p{M}/gu, '').toLowerCase();
const palabras = (s: string) =>
  normalizar(s)
    .split(/\W+/)
    .filter((w) => w.length > 3);

/**
 * CU-14. El agente solo consulta. Simula la respuesta con el inventario de prueba; el servicio real
 * resuelve el lenguaje natural y aplica los permisos del rol (RF-IA-09).
 */
export const consultarAgente = (pregunta: string) =>
  mockRequest<RespuestaAgente>(() => {
    const q = normalizar(pregunta);

    if (/critic|alerta|minimo/.test(q)) {
      const alerta = PRODUCTOS.filter((p) => resumen(p).nivel !== 'Normal').slice(0, 3);
      return {
        consultas: ['Consultó productos en alerta de stock'],
        texto: `Hay ${alerta.length} productos para revisar, entre ellos: ${alerta
          .map((p) => `${p.nombre} (libre ${formatNumber(resumen(p).libre)})`)
          .join('; ')}.`,
      };
    }

    const terms = palabras(pregunta);
    const hit = PRODUCTOS.map((p) => ({
      p,
      score: terms.filter((t) => normalizar(p.nombre).includes(t)).length,
    }))
      .filter((m) => m.score > 0)
      .sort((a, b) => b.score - a.score)[0]?.p;

    if (!hit) {
      return {
        consultas: ['Consultó el catálogo de productos'],
        texto:
          'No encontré información sobre ese producto. Reformular la consulta con el nombre o el SKU.',
        incierta: true,
      };
    }
    const { libre } = resumen(hit);
    return {
      consultas: [`Consultó disponibilidad de ${hit.sku}`, 'Consultó ubicaciones'],
      texto: `Hay ${formatNumber(libre)} ${hit.unidad} libres de ${hit.nombre}. Están en la tienda principal (${formatNumber(hit.tienda)}), Almacén 1 (${formatNumber(hit.almacen1)}) y Almacén 2 (${formatNumber(hit.almacen2)}).`,
    };
  }, 700);
