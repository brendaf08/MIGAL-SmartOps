package com.migal.smartops.agenteia.controller;

import com.migal.smartops.agenteia.controller.AgenteDtos.ConversacionDto;
import com.migal.smartops.agenteia.controller.AgenteDtos.HerramientaCatalogoDto;
import com.migal.smartops.agenteia.controller.AgenteDtos.RespuestaDto;
import com.migal.smartops.agenteia.controller.AgenteDtos.ResumenDto;
import com.migal.smartops.agenteia.controller.SolicitudesAgente.MensajeSolicitud;
import com.migal.smartops.agenteia.service.ServicioAgente;
import com.migal.smartops.catalogo.controller.Identificadores;
import com.migal.smartops.comun.web.PaginaDto;
import com.migal.smartops.seguridad.config.UsuarioAutenticado;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Agente de IA (CU-14): consultas en lenguaje natural, conversaciones propias y catálogo de herramientas del rol. Todos los
 * roles pueden usar el agente; cada herramienta se restringe por rol en el servicio. Las conversaciones se restringen a su
 * propietario.
 */
@RestController
@RequestMapping("/api/v1/agente")
public class AgenteController {

	private static final String ROLES_AGENTE = "hasAnyRole('VENDEDOR','ALMACENERO','ADMINISTRADOR','REPONEDOR')";

	private final ServicioAgente agente;

	public AgenteController(ServicioAgente agente) {
		this.agente = agente;
	}

	@PostMapping("/mensajes")
	@PreAuthorize(ROLES_AGENTE)
	public ResponseEntity<RespuestaDto> enviarMensaje(@AuthenticationPrincipal UsuarioAutenticado actor,
			@Valid @RequestBody MensajeSolicitud solicitud) {
		Long idConversacion = solicitud.conversacionId() == null ? null
				: Identificadores.numero(solicitud.conversacionId());
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(RespuestaDto.de(agente.enviar(actor, solicitud.mensaje(), idConversacion)));
	}

	@GetMapping("/conversaciones")
	@PreAuthorize(ROLES_AGENTE)
	public PaginaDto<ResumenDto> listarConversaciones(@AuthenticationPrincipal UsuarioAutenticado actor,
			@RequestParam(defaultValue = "1") @Min(1) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize,
			@RequestParam(required = false) String orden) {
		PaginaDto<ServicioAgente.ResumenConversacion> resultado = agente.listar(actor, page, pageSize, orden);
		return new PaginaDto<>(resultado.items().stream().map(ResumenDto::de).toList(), resultado.total(),
				resultado.page(), resultado.pageSize());
	}

	@GetMapping("/conversaciones/{id}")
	@PreAuthorize(ROLES_AGENTE)
	public ConversacionDto consultarConversacion(@AuthenticationPrincipal UsuarioAutenticado actor,
			@PathVariable String id) {
		return ConversacionDto.de(agente.consultar(actor, Identificadores.numero(id)));
	}

	@DeleteMapping("/conversaciones/{id}")
	@PreAuthorize(ROLES_AGENTE)
	public ResponseEntity<Void> eliminarConversacion(@AuthenticationPrincipal UsuarioAutenticado actor,
			@PathVariable String id) {
		agente.eliminar(actor, Identificadores.numero(id));
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/herramientas")
	@PreAuthorize(ROLES_AGENTE)
	public List<HerramientaCatalogoDto> listarHerramientas(@AuthenticationPrincipal UsuarioAutenticado actor) {
		return agente.herramientasDisponibles(actor).stream().map(HerramientaCatalogoDto::de).toList();
	}
}
