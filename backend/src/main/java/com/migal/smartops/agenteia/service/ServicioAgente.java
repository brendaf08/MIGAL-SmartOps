package com.migal.smartops.agenteia.service;

import com.migal.smartops.agenteia.model.ConversacionAgente;
import com.migal.smartops.agenteia.model.MensajeAgente;
import com.migal.smartops.agenteia.model.RolMensaje;
import com.migal.smartops.agenteia.repository.ConversacionAgenteRepository;
import com.migal.smartops.agenteia.repository.MensajeAgenteRepository;
import com.migal.smartops.comun.errores.CodigoError;
import com.migal.smartops.comun.errores.ErrorDeNegocio;
import com.migal.smartops.comun.web.Ordenamiento;
import com.migal.smartops.comun.web.PaginaDto;
import com.migal.smartops.seguridad.config.UsuarioAutenticado;
import com.migal.smartops.seguridad.repository.UsuarioRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * Conversaciones del agente de IA (CU-14). Envía cada consulta al proveedor del modelo, ejecuta las herramientas que este
 * elige con los permisos del usuario y conserva la consulta y la respuesta en la conversación. Toda operación sobre una
 * conversación se restringe a su propietario: una conversación ajena responde RECURSO_NO_ENCONTRADO. El agente no modifica
 * datos transaccionales.
 */
@Service
public class ServicioAgente {

	/** Longitud máxima del título, según el esquema {@code AgenteConversacion} del contrato. */
	static final int LONGITUD_TITULO = 120;

	private static final String SIN_HERRAMIENTAS = "[]";
	private static final Map<String, String> CAMPOS_ORDENABLES = Map.of("fechaUltimoMensaje", "fechaUltimoMensaje",
			"fechaCreacion", "fechaCreacion");

	/** Resultado de una consulta: la respuesta, las herramientas invocadas y los registros consultados. */
	public record RespuestaAgente(long conversacionId, long mensajeId, String texto, boolean incierta,
			List<InvocacionHerramienta> herramientas, List<ReferenciaAgente> referencias) {
	}

	/** Mensaje de una conversación con sus consultas. */
	public record MensajeConversacion(long id, RolMensaje autor, String texto, Instant fecha, boolean incierta,
			List<InvocacionHerramienta> herramientas, List<ReferenciaAgente> referencias) {
	}

	/** Conversación con todos sus mensajes en orden cronológico. */
	public record ConversacionDetalle(long id, String titulo, Instant fechaCreacion, Instant fechaUltimoMensaje,
			List<MensajeConversacion> mensajes) {
	}

	/** Datos de una conversación en el listado, sin sus mensajes. */
	public record ResumenConversacion(long id, String titulo, Instant fechaCreacion, Instant fechaUltimoMensaje,
			long cantidadMensajes) {
	}

	private static final TypeReference<List<InvocacionHerramienta>> TIPO_INVOCACIONES = new TypeReference<>() {
	};
	private static final TypeReference<List<ReferenciaAgente>> TIPO_REFERENCIAS = new TypeReference<>() {
	};

	private final ProveedorLlm proveedor;
	private final CatalogoHerramientas catalogo;
	private final ConversacionAgenteRepository conversaciones;
	private final MensajeAgenteRepository mensajes;
	private final UsuarioRepository usuarios;
	private final ObjectMapper mapeador;
	private final Clock reloj;

	public ServicioAgente(ProveedorLlm proveedor, CatalogoHerramientas catalogo,
			ConversacionAgenteRepository conversaciones, MensajeAgenteRepository mensajes, UsuarioRepository usuarios,
			ObjectMapper mapeador, Clock reloj) {
		this.proveedor = proveedor;
		this.catalogo = catalogo;
		this.conversaciones = conversaciones;
		this.mensajes = mensajes;
		this.usuarios = usuarios;
		this.mapeador = mapeador;
		this.reloj = reloj;
	}

	/**
	 * Procesa una consulta. Si se indica una conversación, la consulta se agrega a ella; si no, se crea una nueva. Si el
	 * proveedor no responde, no se registra nada.
	 */
	@Transactional
	public RespuestaAgente enviar(UsuarioAutenticado actor, String consulta, Long idConversacion) {
		String texto = consulta == null ? "" : consulta.trim();
		if (texto.isEmpty()) {
			throw new ErrorDeNegocio(CodigoError.VALIDACION_FALLIDA, "El mensaje no puede estar vacío.");
		}
		ConversacionAgente conversacion = idConversacion == null ? null : buscar(actor, idConversacion);

		List<LlamadaHerramienta> llamadas = proveedor.planificar(texto, catalogo.disponiblesPara(actor.rol()));
		List<InvocacionHerramienta> invocaciones = llamadas.stream()
				.map(llamada -> catalogo.ejecutar(actor.rol(), llamada.nombre(), llamada.parametros())).toList();
		RespuestaRedactada redactada = proveedor.redactar(texto, invocaciones);
		List<ReferenciaAgente> referencias = invocaciones.stream().filter(invocacion -> invocacion.codigo() == null)
				.flatMap(invocacion -> invocacion.referencias().stream()).toList();

		Instant ahora = reloj.instant();
		if (conversacion == null) {
			conversacion = conversaciones.saveAndFlush(new ConversacionAgente(
					usuarios.findById(actor.id()).orElseThrow(), tituloDe(texto), ahora));
		}
		conversacion.agregarMensaje(new MensajeAgente(RolMensaje.USUARIO, texto, false, SIN_HERRAMIENTAS,
				SIN_HERRAMIENTAS, ahora));
		MensajeAgente respuesta = new MensajeAgente(RolMensaje.AGENTE, redactada.texto(), redactada.incierta(),
				json(invocaciones), json(referencias), ahora.plusMillis(1));
		conversacion.agregarMensaje(respuesta);
		// La conversación ya está gestionada: el vaciado persiste los mensajes nuevos en cascada y les asigna identificador.
		conversaciones.flush();

		return new RespuestaAgente(conversacion.getId(), respuesta.getId(), redactada.texto(), redactada.incierta(),
				invocaciones, referencias);
	}

	/** Conversación del usuario con todos sus mensajes. */
	@Transactional(readOnly = true)
	public ConversacionDetalle consultar(UsuarioAutenticado actor, long id) {
		ConversacionAgente conversacion = buscar(actor, id);
		return new ConversacionDetalle(conversacion.getId(), conversacion.getTitulo(), conversacion.getFechaCreacion(),
				conversacion.getFechaUltimoMensaje(), conversacion.getMensajes().stream().map(this::mensaje).toList());
	}

	/** Conversaciones del usuario, paginadas. */
	@Transactional(readOnly = true)
	public PaginaDto<ResumenConversacion> listar(UsuarioAutenticado actor, int page, int pageSize, String orden) {
		Pageable paginacion = Ordenamiento.paginacion(page, pageSize, orden, CAMPOS_ORDENABLES,
				Sort.by(Sort.Direction.DESC, "fechaUltimoMensaje"));
		Page<ConversacionAgente> pagina = conversaciones.findByUsuarioId(actor.id(), paginacion);
		return PaginaDto.de(pagina, conversacion -> new ResumenConversacion(conversacion.getId(),
				conversacion.getTitulo(), conversacion.getFechaCreacion(), conversacion.getFechaUltimoMensaje(),
				mensajes.countByConversacionId(conversacion.getId())));
	}

	/** Elimina una conversación del usuario junto con sus mensajes. */
	@Transactional
	public void eliminar(UsuarioAutenticado actor, long id) {
		conversaciones.delete(buscar(actor, id));
	}

	/** Herramientas que el rol del usuario puede usar. */
	public List<DefinicionHerramienta> herramientasDisponibles(UsuarioAutenticado actor) {
		return catalogo.disponiblesPara(actor.rol());
	}

	private ConversacionAgente buscar(UsuarioAutenticado actor, long id) {
		return conversaciones.findByIdAndUsuarioId(id, actor.id())
				.orElseThrow(() -> new ErrorDeNegocio(CodigoError.RECURSO_NO_ENCONTRADO,
						"La conversación indicada no existe."));
	}

	private MensajeConversacion mensaje(MensajeAgente mensaje) {
		return new MensajeConversacion(mensaje.getId(), mensaje.getRol(), mensaje.getTexto(), mensaje.getFecha(),
				mensaje.isIncierta(), leer(mensaje.getHerramientas(), TIPO_INVOCACIONES),
				leer(mensaje.getReferencias(), TIPO_REFERENCIAS));
	}

	private String json(Object valor) {
		return mapeador.writeValueAsString(valor);
	}

	private <T> List<T> leer(String json, TypeReference<List<T>> tipo) {
		return mapeador.readValue(json, tipo);
	}

	/** Título a partir del primer mensaje, truncado a la longitud del contrato. */
	static String tituloDe(String texto) {
		return texto.length() <= LONGITUD_TITULO ? texto : texto.substring(0, LONGITUD_TITULO).trim();
	}
}
