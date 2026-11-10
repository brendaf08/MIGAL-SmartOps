package com.migal.smartops.recepciones.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Cuerpos de solicitud del módulo de recepciones, con las restricciones de los esquemas del contrato. Las reglas de negocio
 * (estado de la orden, sobrantes, documento validado, productos y ubicaciones activos) las verifica el servicio.
 */
public final class SolicitudesRecepciones {

	private static final String IDENTIFICADOR = "^[0-9]{1,18}$";

	private SolicitudesRecepciones() {
	}

	/** {@code RecepcionLineaSolicitud}: cantidad recibida de un producto en una ubicación. */
	public record LineaSolicitud(@NotBlank @Pattern(regexp = IDENTIFICADOR) String productoId,
			@NotBlank @Pattern(regexp = IDENTIFICADOR) String ubicacionId,
			@NotNull @DecimalMin("0") BigDecimal cantidadRecibida) {
	}

	/**
	 * {@code RecepcionSolicitud}: registro de una recepción pendiente. Debe informarse al menos un bloque de líneas o un
	 * documento de precarga; esta condición la verifica el servicio.
	 */
	public record RecepcionRegistro(@NotBlank @Pattern(regexp = IDENTIFICADOR) String ordenCompraId,
			@NotNull LocalDate fechaRecepcion, @Size(max = 500) String observaciones,
			@Pattern(regexp = IDENTIFICADOR) String documentoId, @Pattern(regexp = IDENTIFICADOR) String ubicacionPrecargaId,
			@Valid List<LineaSolicitud> lineas) {
	}

	/** {@code RecepcionActualizacion}: reemplazo de la fecha, las observaciones y las líneas de una recepción pendiente. */
	public record RecepcionActualizacion(@NotNull LocalDate fechaRecepcion, @Size(max = 500) String observaciones,
			@NotEmpty @Valid List<LineaSolicitud> lineas) {
	}

	/** {@code RecepcionValidacion}: cuerpo opcional; las observaciones informadas reemplazan a las de la recepción. */
	public record RecepcionValidacion(@Size(max = 500) String observaciones) {
	}
}
