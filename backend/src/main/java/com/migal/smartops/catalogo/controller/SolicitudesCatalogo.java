package com.migal.smartops.catalogo.controller;

import com.migal.smartops.catalogo.service.ProductoService.DatosProducto;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Cuerpos de solicitud del módulo de catálogo, con las restricciones de los esquemas del contrato. Los
 * identificadores de categoría son cadenas numéricas en esta implementación. Los importes se validan con dos
 * decimales y las cantidades con tres, conforme a los esquemas {@code Importe} y {@code Cantidad}.
 */
public final class SolicitudesCatalogo {

	private static final String IDENTIFICADOR = "^[0-9]{1,18}$";

	private SolicitudesCatalogo() {
	}

	/** {@code ProductoRegistro}. Si se omite la vigencia, el producto se registra como activo. */
	public record ProductoRegistro(
			@NotBlank @Size(max = 50) String sku,
			@NotBlank @Size(max = 200) String nombreOficial,
			String descripcion,
			@NotBlank @Size(max = 30) String unidadMedida,
			@NotNull @DecimalMin("0") @Digits(integer = 12, fraction = 2) BigDecimal precioReferencial,
			@NotNull @DecimalMin("0") @Digits(integer = 11, fraction = 3) BigDecimal stockMinimo,
			@NotBlank @Pattern(regexp = IDENTIFICADOR) String idCategoria,
			@Size(max = 100) String marca,
			@Size(max = 100) String presentacion,
			Boolean estadoActivo) {

		public DatosProducto datos() {
			return new DatosProducto(nombreOficial, descripcion, unidadMedida, precioReferencial, stockMinimo,
					Long.parseLong(idCategoria), marca, presentacion);
		}
	}

	/** {@code ProductoActualizacion}. El SKU y la vigencia no forman parte del cuerpo. */
	public record ProductoActualizacion(
			@NotBlank @Size(max = 200) String nombreOficial,
			String descripcion,
			@NotBlank @Size(max = 30) String unidadMedida,
			@NotNull @DecimalMin("0") @Digits(integer = 12, fraction = 2) BigDecimal precioReferencial,
			@NotNull @DecimalMin("0") @Digits(integer = 11, fraction = 3) BigDecimal stockMinimo,
			@NotBlank @Pattern(regexp = IDENTIFICADOR) String idCategoria,
			@Size(max = 100) String marca,
			@Size(max = 100) String presentacion) {

		public DatosProducto datos() {
			return new DatosProducto(nombreOficial, descripcion, unidadMedida, precioReferencial, stockMinimo,
					Long.parseLong(idCategoria), marca, presentacion);
		}
	}

	/** {@code ActualizacionEstado}: cambio de estado de activación de un maestro. */
	public record ActualizacionEstado(@NotNull Boolean estadoActivo) {
	}
}
