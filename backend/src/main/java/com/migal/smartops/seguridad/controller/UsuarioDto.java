package com.migal.smartops.seguridad.controller;

import com.migal.smartops.seguridad.model.Usuario;
import java.time.Instant;

/** Usuario registrado, según el esquema {@code UsuarioRegistrado} del contrato. Nunca incluye la clave. */
public record UsuarioDto(String id, String nombres, String apellidos, String username, String rol,
		boolean estadoActivo, Instant fechaRegistro, Instant fechaActualizacion) {

	public static UsuarioDto de(Usuario usuario) {
		return new UsuarioDto(String.valueOf(usuario.getId()), usuario.getNombres(), usuario.getApellidos(),
				usuario.getUsername(), usuario.getRol().valor(), usuario.isEstadoActivo(), usuario.getFechaRegistro(),
				usuario.getFechaActualizacion());
	}
}
