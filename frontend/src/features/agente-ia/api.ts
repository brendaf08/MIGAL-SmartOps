import { API_REAL, ApiError, api } from '@/api/cliente';
import { mockRequest } from '@/api/mock';
import { recorrerPaginas } from '@/api/paginacion';
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

/** Respuesta del agente tal como la devuelve la API (contrato agente-ia.yaml, esquema AgenteRespuesta). */
interface RespuestaAgenteApi {
  conversacionId: string;
  mensajeId: string;
  texto: string;
  incierta: boolean;
  herramientas: {
    nombre: string;
    parametros: Record<string, unknown>;
    resumen: string;
    codigo: string | null;
  }[];
  referencias: { tipo: string; id: string; codigo: string | null }[];
}

/** Conversación de la sesión: las consultas siguientes se agregan a ella en lugar de abrir una nueva. */
let conversacionActual: string | undefined;

const enviarMensaje = (mensaje: string, conversacionId?: string) =>
  api.post<RespuestaAgenteApi>(
    '/agente/mensajes',
    conversacionId ? { mensaje, conversacionId } : { mensaje },
  );

const consultarAgenteReal = async (pregunta: string): Promise<RespuestaAgente> => {
  let respuesta: RespuestaAgenteApi;
  try {
    respuesta = await enviarMensaje(pregunta, conversacionActual);
  } catch (error) {
    // Una conversación que ya no pertenece al usuario (por ejemplo, tras cambiar de sesión) se sustituye por una nueva.
    if (
      !conversacionActual ||
      !(error instanceof ApiError && error.codigo === 'RECURSO_NO_ENCONTRADO')
    ) {
      throw error;
    }
    conversacionActual = undefined;
    respuesta = await enviarMensaje(pregunta);
  }
  conversacionActual = respuesta.conversacionId;
  return {
    consultas: respuesta.herramientas.map((h) =>
      h.codigo ? `${h.nombre}: ${h.codigo}` : h.resumen,
    ),
    texto: respuesta.texto,
    incierta: respuesta.incierta,
  };
};

/**
 * CU-14. El agente solo consulta. Simula la respuesta con el inventario de prueba; el servicio real
 * resuelve el lenguaje natural y aplica los permisos del rol (RF-IA-09).
 */
export const consultarAgente = (pregunta: string): Promise<RespuestaAgente> =>
  API_REAL
    ? consultarAgenteReal(pregunta)
    : mockRequest<RespuestaAgente>(() => {
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

/** Conversación del usuario en el listado, sin mensajes (esquema AgenteConversacionResumen). */
export interface ConversacionAgente {
  id: string;
  titulo: string;
  fechaCreacion: string;
  fechaUltimoMensaje: string;
  cantidadMensajes: number;
}

/** Herramienta disponible para el rol del usuario (esquema AgenteHerramienta). */
export interface HerramientaAgente {
  nombre: string;
  descripcion: string;
  operacion: string;
}

/*
 * Operaciones de conversaciones y herramientas. Las pantallas actuales no las usan todavía; se exponen con la forma
 * del contrato. Sin API, el listado devuelve una lista vacía y la eliminación no hace nada.
 */

/** Conversaciones propias, de la más reciente a la más antigua. */
export const listarConversacionesAgente = (): Promise<ConversacionAgente[]> =>
  API_REAL ? recorrerPaginas<ConversacionAgente>('/agente/conversaciones') : mockRequest(() => []);

/** Mensaje de una conversación, del usuario o del agente (esquema AgenteMensaje). */
export interface MensajeAgente {
  id: string;
  autor: 'usuario' | 'agente';
  texto: string;
  fechaHora: string;
  incierta: boolean;
  herramientas: RespuestaAgenteApi['herramientas'];
  referencias: RespuestaAgenteApi['referencias'];
}

/** Conversación con sus mensajes en orden cronológico (esquema AgenteConversacion). */
export interface ConversacionDetalle {
  id: string;
  titulo: string;
  fechaCreacion: string;
  fechaUltimoMensaje: string;
  mensajes: MensajeAgente[];
}

/** Conversación propia con sus mensajes. Si no pertenece al usuario, responde RECURSO_NO_ENCONTRADO. */
export const consultarConversacionAgente = (id: string): Promise<ConversacionDetalle> =>
  API_REAL
    ? api.get<ConversacionDetalle>(`/agente/conversaciones/${id}`)
    : mockRequest<ConversacionDetalle>(() => {
        throw new Error('RECURSO_NO_ENCONTRADO');
      });

/** Elimina una conversación propia y olvida la conversación de la sesión si coincide. */
export const eliminarConversacionAgente = (id: string): Promise<void> =>
  API_REAL
    ? api.delete<void>(`/agente/conversaciones/${id}`).then(() => {
        if (conversacionActual === id) conversacionActual = undefined;
      })
    : mockRequest(() => undefined);

/** Herramientas que el rol del usuario puede ejecutar. */
export const listarHerramientasAgente = (): Promise<HerramientaAgente[]> =>
  API_REAL ? api.get<HerramientaAgente[]>('/agente/herramientas') : mockRequest(() => []);
