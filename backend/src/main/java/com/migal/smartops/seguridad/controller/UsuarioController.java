package com.migal.smartops.seguridad.controller;

import com.migal.smartops.comun.errores.CodigoError;
import com.migal.smartops.comun.errores.ErrorDeNegocio;
import com.migal.smartops.comun.web.Ordenamiento;
import com.migal.smartops.comun.web.PaginaDto;
import com.migal.smartops.seguridad.config.UsuarioAutenticado;
import com.migal.smartops.seguridad.controller.SolicitudesUsuario.Actualizacion;
import com.migal.smartops.seguridad.controller.SolicitudesUsuario.EstadoAcceso;
import com.migal.smartops.seguridad.controller.SolicitudesUsuario.Nuevo;
import com.migal.smartops.seguridad.model.RolUsuario;
import com.migal.smartops.seguridad.service.UsuarioService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.util.Map;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Administración de usuarios, roles y estado de acceso (CU-02). Exclusiva del Administrador. */
@RestController
@RequestMapping("/api/v1/usuarios")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class UsuarioController {

	private static final Map<String, String> CAMPOS_ORDENABLES = Map.of(
			"apellidos", "apellidos",
			"nombres", "nombres",
			"username", "username",
			"rol", "rol",
			"fechaRegistro", "fechaRegistro");

	private final UsuarioService usuarios;

	public UsuarioController(UsuarioService usuarios) {
		this.usuarios = usuarios;
	}

	@GetMapping
	public PaginaDto<UsuarioDto> listar(
			@RequestParam(required = false) String q,
			@RequestParam(required = false) String rol,
			@RequestParam(required = false) Boolean estadoActivo,
			@RequestParam(defaultValue = "1") @Min(1) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize,
			@RequestParam(required = false) String orden) {
		Pageable paginacion = Ordenamiento.paginacion(page, pageSize, orden, CAMPOS_ORDENABLES,
				Sort.by("apellidos", "nombres"));
		return PaginaDto.de(usuarios.listar(q, rolOpcional(rol), estadoActivo, paginacion), UsuarioDto::de);
	}

	@PostMapping
	public ResponseEntity<UsuarioDto> registrar(@Valid @RequestBody Nuevo solicitud) {
		var creado = usuarios.registrar(solicitud.nombres(), solicitud.apellidos(), solicitud.username(),
				solicitud.claveTemporal(), RolUsuario.desdeValor(solicitud.rol()));
		return ResponseEntity.created(URI.create("/api/v1/usuarios/" + creado.getId())).body(UsuarioDto.de(creado));
	}

	@GetMapping("/{id}")
	public UsuarioDto consultar(@PathVariable long id) {
		return UsuarioDto.de(usuarios.consultar(id));
	}

	@PutMapping("/{id}")
	public UsuarioDto actualizar(@PathVariable long id, @Valid @RequestBody Actualizacion solicitud) {
		return UsuarioDto.de(usuarios.actualizar(id, solicitud.nombres(), solicitud.apellidos(),
				RolUsuario.desdeValor(solicitud.rol())));
	}

	@PutMapping("/{id}/estado")
	public UsuarioDto cambiarEstado(@PathVariable long id, @Valid @RequestBody EstadoAcceso solicitud,
			@AuthenticationPrincipal UsuarioAutenticado actor) {
		return UsuarioDto.de(usuarios.cambiarEstado(id, solicitud.estadoActivo(), actor.id()));
	}

	private static RolUsuario rolOpcional(String rol) {
		if (rol == null || rol.isBlank()) {
			return null;
		}
		try {
			return RolUsuario.desdeValor(rol);
		} catch (IllegalArgumentException e) {
			throw new ErrorDeNegocio(CodigoError.VALIDACION_FALLIDA,
					"El rol '" + rol + "' no es válido. Valores: administrador, vendedor, almacenero, reponedor.");
		}
	}
}
