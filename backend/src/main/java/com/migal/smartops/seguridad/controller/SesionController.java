package com.migal.smartops.seguridad.controller;

import com.migal.smartops.seguridad.config.UsuarioAutenticado;
import com.migal.smartops.seguridad.controller.SolicitudesUsuario.Credenciales;
import com.migal.smartops.seguridad.service.SesionService;
import com.migal.smartops.seguridad.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Inicio de sesión y consulta de la sesión activa (CU-01). */
@RestController
@RequestMapping("/api/v1/sesiones")
public class SesionController {

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
	public SesionIniciadaDto iniciarSesion(@Valid @RequestBody Credenciales credenciales) {
		SesionService.SesionIniciada sesion = sesiones.iniciarSesion(credenciales.username(), credenciales.clave());
		return new SesionIniciadaDto(sesion.token().valor(), sesion.token().expiraEn(), UsuarioDto.de(sesion.usuario()));
	}

	@GetMapping("/actual")
	public UsuarioDto sesionActual(@AuthenticationPrincipal UsuarioAutenticado autenticado) {
		return UsuarioDto.de(usuarios.consultar(autenticado.id()));
	}
}
