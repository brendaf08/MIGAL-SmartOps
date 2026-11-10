package com.migal.smartops.seguridad.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Cuerpos de solicitud del módulo, con las restricciones de los esquemas del contrato. */
public final class SolicitudesUsuario {

	private static final String ROLES = "^(administrador|vendedor|almacenero|reponedor)$";

	private SolicitudesUsuario() {
	}

	/** {@code CredencialesSesion}. */
	public record Credenciales(
			@NotBlank @Size(max = 50) String username,
			@NotBlank @Size(max = 255) String clave) {
	}

	/** {@code UsuarioNuevo}. */
	public record Nuevo(
			@NotBlank @Size(max = 100) String nombres,
			@NotBlank @Size(max = 100) String apellidos,
			@NotBlank @Size(min = 4, max = 20) @Pattern(regexp = "^[a-z0-9._-]+$") String username,
			@NotBlank @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*[0-9]).{8,}$") String claveTemporal,
			@NotBlank @Pattern(regexp = ROLES) String rol) {
	}

	/** {@code UsuarioActualizacion}. */
	public record Actualizacion(
			@NotBlank @Size(max = 100) String nombres,
			@NotBlank @Size(max = 100) String apellidos,
			@NotBlank @Pattern(regexp = ROLES) String rol) {
	}

	/** {@code EstadoAccesoUsuario}. */
	public record EstadoAcceso(@NotNull Boolean estadoActivo) {
	}
}
