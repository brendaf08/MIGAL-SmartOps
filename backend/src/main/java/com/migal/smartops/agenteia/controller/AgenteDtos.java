package com.migal.smartops.agenteia.controller;

import com.migal.smartops.agenteia.model.RolMensaje;
import com.migal.smartops.agenteia.service.DefinicionHerramienta;
import com.migal.smartops.agenteia.service.InvocacionHerramienta;
import com.migal.smartops.agenteia.service.ReferenciaAgente;
import com.migal.smartops.agenteia.service.ServicioAgente.ConversacionDetalle;
import com.migal.smartops.agenteia.service.ServicioAgente.MensajeConversacion;
import com.migal.smartops.agenteia.service.ServicioAgente.ResumenConversacion;
import com.migal.smartops.agenteia.service.ServicioAgente.RespuestaAgente;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Respuestas del módulo del agente de IA, según los esquemas del contrato. Los identificadores se expresan como texto.
 */
public final class AgenteDtos {

	private AgenteDtos() {
	}

	/** {@code AgenteHerramientaInvocada}: llamada a una herramienta y su resultado. */
	public record HerramientaDto(String nombre, Map<String, String> parametros, String resumen, String codigo) {

		public static HerramientaDto de(InvocacionHerramienta invocacion) {
			return new HerramientaDto(invocacion.nombre(), invocacion.parametros(), invocacion.resumen(),
					invocacion.codigo() == null ? null : invocacion.codigo().name());
		}
	}

	/** {@code AgenteReferencia}: registro de negocio consultado. */
	public record ReferenciaDto(String tipo, String id, String codigo) {

		public static ReferenciaDto de(ReferenciaAgente referencia) {
			return new ReferenciaDto(referencia.tipo(), referencia.id(), referencia.codigo());
		}
	}

	/** {@code AgenteRespuesta}: respuesta a una consulta. */
	public record RespuestaDto(String conversacionId, String mensajeId, String texto, boolean incierta,
			List<HerramientaDto> herramientas, List<ReferenciaDto> referencias) {

		public static RespuestaDto de(RespuestaAgente respuesta) {
			return new RespuestaDto(String.valueOf(respuesta.conversacionId()), String.valueOf(respuesta.mensajeId()),
					respuesta.texto(), respuesta.incierta(),
					respuesta.herramientas().stream().map(HerramientaDto::de).toList(),
					respuesta.referencias().stream().map(ReferenciaDto::de).toList());
		}
	}

	/** {@code AgenteMensaje}: mensaje de una conversación. */
	public record MensajeDto(String id, String autor, String texto, Instant fechaHora, boolean incierta,
			List<HerramientaDto> herramientas, List<ReferenciaDto> referencias) {

		public static MensajeDto de(MensajeConversacion mensaje) {
			return new MensajeDto(String.valueOf(mensaje.id()), mensaje.autor() == RolMensaje.USUARIO ? "usuario" : "agente",
					mensaje.texto(), mensaje.fecha(), mensaje.incierta(),
					mensaje.herramientas().stream().map(HerramientaDto::de).toList(),
					mensaje.referencias().stream().map(ReferenciaDto::de).toList());
		}
	}

	/** {@code AgenteConversacion}: conversación con sus mensajes. */
	public record ConversacionDto(String id, String titulo, Instant fechaCreacion, Instant fechaUltimoMensaje,
			List<MensajeDto> mensajes) {

		public static ConversacionDto de(ConversacionDetalle conversacion) {
			return new ConversacionDto(String.valueOf(conversacion.id()), conversacion.titulo(),
					conversacion.fechaCreacion(), conversacion.fechaUltimoMensaje(),
					conversacion.mensajes().stream().map(MensajeDto::de).toList());
		}
	}

	/** {@code AgenteConversacionResumen}: conversación en el listado, sin mensajes. */
	public record ResumenDto(String id, String titulo, Instant fechaCreacion, Instant fechaUltimoMensaje,
			long cantidadMensajes) {

		public static ResumenDto de(ResumenConversacion resumen) {
			return new ResumenDto(String.valueOf(resumen.id()), resumen.titulo(), resumen.fechaCreacion(),
					resumen.fechaUltimoMensaje(), resumen.cantidadMensajes());
		}
	}

	/** {@code AgenteHerramienta}: herramienta disponible para el rol. */
	public record HerramientaCatalogoDto(String nombre, String descripcion, String operacion) {

		public static HerramientaCatalogoDto de(DefinicionHerramienta definicion) {
			return new HerramientaCatalogoDto(definicion.nombre(), definicion.descripcion(), definicion.operacion());
		}
	}
}
