package com.migal.smartops.inventario.controller;

import com.migal.smartops.catalogo.controller.Identificadores;
import com.migal.smartops.inventario.model.TipoOperacion;
import com.migal.smartops.inventario.service.MovimientoInventarioService.SolicitudAjuste;
import com.migal.smartops.inventario.service.MovimientoInventarioService.SolicitudTraslado;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Cuerpos de solicitud del módulo de inventario, con las restricciones de los esquemas del contrato. Los
 * identificadores son cadenas numéricas en esta implementación y la cantidad admite hasta tres decimales.
 */
public final class SolicitudesInventario {

	private static final String IDENTIFICADOR = "^[0-9]{1,18}$";

	private SolicitudesInventario() {
	}

	/**
	 * {@code SolicitudMovimiento}. La cantidad se limita a tres decimales; el motivo no puede ser vacío, lo que
	 * el servicio verifica con el código {@code MOTIVO_REQUERIDO}.
	 */
	public record SolicitudMovimiento(
			@NotBlank @Pattern(regexp = "^(entrada|salida)$") String tipoOperacion,
			@NotBlank @Pattern(regexp = IDENTIFICADOR) String productoId,
			@NotBlank @Pattern(regexp = IDENTIFICADOR) String ubicacionId,
			@NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 11, fraction = 3) BigDecimal cantidad,
			@NotNull @Size(max = 500) String motivo,
			@Pattern(regexp = IDENTIFICADOR) String idMovimientoCorregido) {

		SolicitudAjuste aServicio() {
			return new SolicitudAjuste(TipoOperacion.de(tipoOperacion), Identificadores.numero(productoId),
					Identificadores.numero(ubicacionId), cantidad, motivo,
					idMovimientoCorregido == null ? null : Identificadores.numero(idMovimientoCorregido));
		}
	}

	/** {@code SolicitudTraslado}. */
	public record SolicitudTrasladoDto(
			@NotBlank @Pattern(regexp = IDENTIFICADOR) String productoId,
			@NotBlank @Pattern(regexp = IDENTIFICADOR) String ubicacionOrigenId,
			@NotBlank @Pattern(regexp = IDENTIFICADOR) String ubicacionDestinoId,
			@NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 11, fraction = 3) BigDecimal cantidad,
			@NotNull @Size(max = 500) String motivo) {

		SolicitudTraslado aServicio() {
			return new SolicitudTraslado(Identificadores.numero(productoId), Identificadores.numero(ubicacionOrigenId),
					Identificadores.numero(ubicacionDestinoId), cantidad, motivo);
		}
	}
}
