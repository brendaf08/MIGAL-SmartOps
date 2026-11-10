package com.migal.smartops.despacho.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Cuerpos de solicitud del módulo de despacho, con las restricciones de los esquemas del contrato. Las reglas de negocio
 * (estado de la venta, reserva completa, guía existente) las verifica el servicio.
 */
public final class SolicitudesDespacho {

	private static final String IDENTIFICADOR = "^[0-9]{1,18}$";

	private SolicitudesDespacho() {
	}

	/** {@code GuiaSolicitudEmision}: solo se identifica la venta. */
	public record SolicitudEmisionGuia(@NotBlank @Pattern(regexp = IDENTIFICADOR) String idVenta) {
	}

	/** {@code GuiaSolicitudReimpresion}: el motivo no puede estar vacío ni contener solo espacios. */
	public record SolicitudReimpresionGuia(@NotNull @Size(min = 1, max = 500) @Pattern(regexp = "[\\s\\S]*\\S[\\s\\S]*") String motivo) {
	}
}
