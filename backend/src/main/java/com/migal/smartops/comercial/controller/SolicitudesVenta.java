package com.migal.smartops.comercial.controller;

import com.migal.smartops.comercial.service.VentaService.DatosVentaDirecta;
import com.migal.smartops.comercial.service.VentaService.LineaDirecta;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Cuerpos de solicitud del módulo de ventas, con las restricciones de los esquemas del contrato. Los identificadores
 * son cadenas numéricas. Las reglas de negocio (cliente requerido, motivo, descuento) las verifica el servicio.
 */
public final class SolicitudesVenta {

	private static final String IDENTIFICADOR = "^[0-9]{1,18}$";

	private SolicitudesVenta() {
	}

	/** {@code SolicitudVentaDesdeCotizacion}. */
	public record SolicitudVentaDesdeCotizacion(@NotBlank @Pattern(regexp = IDENTIFICADOR) String idCotizacion) {
	}

	/** Línea de una venta directa. */
	public record LineaVentaDirecta(
			@NotBlank @Pattern(regexp = IDENTIFICADOR) String productoId,
			@NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 11, fraction = 3) BigDecimal cantidad,
			@NotNull @DecimalMin("0") @Digits(integer = 12, fraction = 2) BigDecimal precioUnitario) {
	}

	/**
	 * {@code SolicitudVentaDirecta}. El cliente es opcional en la validación de formato porque su ausencia responde
	 * {@code CLIENTE_REQUERIDO} en el servicio.
	 */
	public record SolicitudVentaDirecta(
			@Pattern(regexp = IDENTIFICADOR) String clienteId,
			@NotNull @DecimalMin("0") @Digits(integer = 12, fraction = 2) BigDecimal descuento,
			@Size(max = 500) String condicionesComerciales,
			LocalDate fechaVencimiento,
			@NotEmpty @Valid List<LineaVentaDirecta> lineas) {

		public DatosVentaDirecta datos() {
			List<LineaDirecta> lineasDatos = lineas.stream()
					.map(linea -> new LineaDirecta(Long.parseLong(linea.productoId()), linea.cantidad(),
							linea.precioUnitario()))
					.toList();
			return new DatosVentaDirecta(clienteId == null ? null : Long.parseLong(clienteId), descuento,
					condicionesComerciales, fechaVencimiento, lineasDatos);
		}
	}

	/** {@code SolicitudCancelacionVenta}. El motivo vacío o formado solo por espacios responde {@code MOTIVO_REQUERIDO}. */
	public record SolicitudCancelacionVenta(@NotNull @Size(max = 500) String motivo) {
	}
}
