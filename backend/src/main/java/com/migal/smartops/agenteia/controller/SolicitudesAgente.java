package com.migal.smartops.agenteia.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Cuerpos de solicitud del módulo del agente, con las restricciones del esquema {@code AgenteMensajeEntrada}. */
public final class SolicitudesAgente {

	private static final String IDENTIFICADOR = "^[0-9]{1,18}$";

	private SolicitudesAgente() {
	}

	/**
	 * Consulta del usuario. La conversación es opcional: si falta o es nula, se crea una nueva. El texto no puede estar
	 * vacío ni formado solo por espacios.
	 */
	public record MensajeSolicitud(@NotBlank @Size(max = 2000) String mensaje,
			@Pattern(regexp = IDENTIFICADOR) String conversacionId) {
	}
}
