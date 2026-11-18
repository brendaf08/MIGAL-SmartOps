package com.migal.smartops.seguridad.controller;

import com.migal.smartops.seguridad.config.UsuarioAutenticado;
import com.migal.smartops.seguridad.controller.SolicitudesUsuario.CambioClave;
import com.migal.smartops.seguridad.controller.SolicitudesUsuario.Credenciales;
import com.migal.smartops.seguridad.service.SesionService;
import com.migal.smartops.seguridad.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Inicio de sesión y consulta de la sesión activa (CU-01). */
@RestController
@RequestMapping("/api/v1/sesiones")
public class SesionController {

	private static final String ROLES_USUARIO = "hasAnyRole('ADMINISTRADOR', 'VENDEDOR', 'ALMACENERO', 'REPONEDOR')";

	private final SesionService sesiones;
	private final UsuarioService usuarios;

	public SesionController(SesionService sesiones, UsuarioService usuarios) {
		this.sesiones = sesiones;
		this.usuarios = usuarios;
	}

	/** Resultado de un inicio de sesión, según el esquema {@code SesionIniciada}. */
	public record SesionIniciadaDto(String token, long expiraEn, UsuarioDto usuario) {
	}

	@PostMapping
	public SesionIniciadaDto iniciarSesion(@Valid @RequestBody Credenciales credenciales, HttpServletRequest solicitud) {
		SesionService.SesionIniciada sesion = sesiones.iniciarSesion(credenciales.username(), credenciales.clave(),
				solicitud.getRemoteAddr());
		return new SesionIniciadaDto(sesion.token().valor(), sesion.token().expiraEn(), UsuarioDto.de(sesion.usuario()));
	}

	/** Solo con un token de usuario: un token de servicio no tiene usuario y respondería 500. */
	@GetMapping("/actual")
	@PreAuthorize(ROLES_USUARIO)
	public UsuarioDto sesionActual(@AuthenticationPrincipal UsuarioAutenticado autenticado) {
		return UsuarioDto.de(usuarios.consultar(autenticado.id()));
	}

	@PutMapping("/actual/clave")
	@PreAuthorize(ROLES_USUARIO)
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void cambiarClave(@AuthenticationPrincipal UsuarioAutenticado autenticado,
			@Valid @RequestBody CambioClave cambio) {
		sesiones.cambiarClave(autenticado.id(), cambio.claveActual(), cambio.claveNueva());
	}
}
