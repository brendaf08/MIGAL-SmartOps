package com.migal.smartops.despacho.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/**
 * Cuerpos de solicitud del módulo de despacho, con las restricciones de los esquemas del contrato. Las reglas de negocio
 * (estado de la venta, reserva completa, guía existente) las verifica el servicio.
 */
public final class SolicitudesDespacho {

	private static final String IDENTIFICADOR = "^[0-9]{1,18}$";

	private SolicitudesDespacho() {
	}

	/** {@code GuiaSolicitudSalidaUbicacion}: cuánto de un producto sale de una ubicación. */
	public record SalidaUbicacionSolicitud(@NotBlank @Pattern(regexp = IDENTIFICADOR) String ubicacionId,
			@NotNull @DecimalMin("0.001") BigDecimal cantidad) {
	}

	/** {@code GuiaSolicitudSalidaProducto}: de qué ubicaciones sale un producto de la venta. */
	public record SalidaProductoSolicitud(@NotBlank @Pattern(regexp = IDENTIFICADOR) String productoId,
			@NotNull @Size(min = 1, max = 10) List<@Valid SalidaUbicacionSolicitud> ubicaciones) {
	}

	/**
	 * {@code GuiaSolicitudEmision}: la venta, cómo se entrega (envío por defecto), la dirección del envío (opcional: por
	 * defecto la del cliente) y, si se desea, de qué ubicaciones sale cada producto.
	 */
	public record SolicitudEmisionGuia(@NotBlank @Pattern(regexp = IDENTIFICADOR) String idVenta,
			@Pattern(regexp = "^(envio|retiro_tienda)$") String tipoEntrega, @Size(max = 255) String direccionEntrega,
			@Size(max = 200) List<@Valid SalidaProductoSolicitud> salidas) {

		/** Solicitud de solo la venta, como antes de elegir la forma de entrega. */
		public SolicitudEmisionGuia(String idVenta) {
			this(idVenta, null, null, null);
		}
	}

	/** {@code GuiaSolicitudReimpresion}: el motivo no puede estar vacío ni contener solo espacios. */
	public record SolicitudReimpresionGuia(@NotNull @Size(min = 1, max = 500) @Pattern(regexp = "[\\s\\S]*\\S[\\s\\S]*") String motivo) {
	}
}
