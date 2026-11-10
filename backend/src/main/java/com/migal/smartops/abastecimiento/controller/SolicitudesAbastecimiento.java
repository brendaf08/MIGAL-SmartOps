package com.migal.smartops.abastecimiento.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Cuerpos de solicitud del módulo de abastecimiento, con las restricciones de los esquemas del contrato. Las reglas de
 * negocio (vigencia del proveedor, RUC único, SKU del catálogo, estado de la orden) las verifican los servicios.
 */
public final class SolicitudesAbastecimiento {

	private static final String IDENTIFICADOR = "^[0-9]{1,18}$";
	private static final String RUC = "^(10|20)[0-9]{9}$";

	private SolicitudesAbastecimiento() {
	}

	/** {@code ProveedorRegistro}: la vigencia es opcional y, si se omite, el proveedor queda activo. */
	public record ProveedorRegistro(@NotBlank @Pattern(regexp = RUC) String ruc,
			@NotBlank @Size(max = 200) String razonSocial, @NotBlank @Size(max = 200) String contacto,
			String condiciones, Boolean estadoActivo) {
	}

	/** {@code ProveedorActualizacion}: la vigencia se cambia con {@code ActualizacionEstado}. */
	public record ProveedorActualizacion(@NotBlank @Pattern(regexp = RUC) String ruc,
			@NotBlank @Size(max = 200) String razonSocial, @NotBlank @Size(max = 200) String contacto,
			String condiciones) {
	}

	/** {@code ActualizacionEstado}: cambio de la vigencia de un proveedor. */
	public record ActualizacionEstado(@NotNull Boolean estadoActivo) {
	}

	/** {@code CatalogoEntrada}: producto del catálogo de un proveedor. */
	public record CatalogoEntrada(@NotBlank String sku, @NotNull @DecimalMin("0") BigDecimal precioCompra,
			@Min(0) Integer plazoEntregaReferencialDias) {
	}

	/** {@code ProveedorCatalogoActualizacion}: conjunto completo de productos del proveedor. */
	public record CatalogoActualizacion(@NotNull @Valid List<CatalogoEntrada> productos) {
	}

	/** {@code OrdenCompraLineaRegistro}: línea de una orden nueva o modificada. */
	public record LineaOrdenSolicitud(@NotBlank String sku,
			@NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal cantidadComprada,
			@NotNull @DecimalMin("0") BigDecimal costoUnitario) {
	}

	/** {@code OrdenCompraRegistro}: orden nueva en estado pendiente. */
	public record OrdenRegistro(@NotBlank @Pattern(regexp = IDENTIFICADOR) String proveedorId, LocalDate fechaEntregaEstimada,
			String observaciones, @Size(max = 100) String modalidadPago,
			@NotEmpty @Valid List<LineaOrdenSolicitud> lineas) {
	}

	/** {@code OrdenCompraActualizacion}: reemplazo de las líneas de una orden pendiente. */
	public record OrdenActualizacion(LocalDate fechaEntregaEstimada, String observaciones,
			@Size(max = 100) String modalidadPago, @NotEmpty @Valid List<LineaOrdenSolicitud> lineas) {
	}

	/** Cuerpo opcional del cierre. El motivo vacío se trata como ausente en el servicio. */
	public record CierreOrden(@Size(max = 500) String motivo) {
	}
}
