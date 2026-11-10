package com.migal.smartops.comercial.controller;

import com.migal.smartops.comercial.model.TipoDocumentoCliente;
import com.migal.smartops.comercial.service.ClienteService.DatosCliente;
import com.migal.smartops.comercial.service.CotizacionService.DatosCotizacion;
import com.migal.smartops.comercial.service.CotizacionService.LineaDatos;
import com.migal.smartops.comercial.service.CotizacionService.LineaDivision;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/**
 * Cuerpos de solicitud del módulo comercial, con las restricciones de los esquemas del contrato. Los identificadores
 * son cadenas numéricas. Los valores enumerados se reciben como texto en minúsculas, igual que en el contrato.
 */
public final class SolicitudesComercial {

	private static final String IDENTIFICADOR = "^[0-9]{1,18}$";
	private static final String TIPO_DOCUMENTO = "^(dni|ruc)$";
	private static final String ORIGEN = "^(cliente|migal)$";

	private SolicitudesComercial() {
	}

	/** Referencia a una cotización para la clave de idempotencia. */
	public record Referencia(long id) {
	}

	/** {@code ClienteDatos}. El tipo y el número se informan juntos o se omiten juntos; la regla se valida en el servicio. */
	public record ClienteRegistro(
			@Pattern(regexp = TIPO_DOCUMENTO) String tipoDocumento,
			@Size(max = 20) String numeroDocumento,
			@NotBlank @Size(max = 200) String nombreRazonSocial,
			@Size(max = 30) String telefonoContacto,
			@Size(max = 255) String direccionReferencia) {

		public DatosCliente datos() {
			return new DatosCliente(tipoDocumento == null ? null : TipoDocumentoCliente.de(tipoDocumento),
					numeroDocumento, nombreRazonSocial, telefonoContacto, direccionReferencia);
		}
	}

	/** {@code ClienteActualizacionEstado}: cambio de vigencia de un cliente. */
	public record ClienteEstado(@NotNull Boolean estadoActivo) {
	}

	/** Línea de una cotización solicitada. */
	public record LineaCotizacion(
			@NotBlank @Pattern(regexp = IDENTIFICADOR) String productoId,
			@NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 11, fraction = 3) BigDecimal cantidad,
			@NotNull @DecimalMin("0") @Digits(integer = 12, fraction = 2) BigDecimal precioUnitario) {
	}

	/** {@code CotizacionDatos}. Sin cliente, la operación responde {@code CLIENTE_REQUERIDO} en el servicio. */
	public record CotizacionRegistro(
			@Pattern(regexp = IDENTIFICADOR) String clienteId,
			@NotEmpty @Valid List<LineaCotizacion> lineas,
			@DecimalMin("0") @Digits(integer = 12, fraction = 2) BigDecimal descuento,
			@Min(1) @Max(60) Integer validezDias,
			String condicionesComerciales) {

		public DatosCotizacion datos() {
			List<LineaDatos> lineasDatos = lineas.stream()
					.map(linea -> new LineaDatos(Long.parseLong(linea.productoId()), linea.cantidad(),
							linea.precioUnitario()))
					.toList();
			return new DatosCotizacion(clienteId == null ? null : Long.parseLong(clienteId), lineasDatos, descuento,
					validezDias == null ? 7 : validezDias, condicionesComerciales);
		}
	}

	/** {@code CotizacionRechazoSolicitud}: origen y motivo obligatorio del rechazo. */
	public record RechazoRegistro(
			@NotNull @Pattern(regexp = ORIGEN) String origen,
			@NotBlank @Size(max = 500) String motivo) {
	}

	/** Línea de una división: cantidad que pasa a la cotización nueva. */
	public record LineaDivisionRegistro(
			@NotBlank @Pattern(regexp = IDENTIFICADOR) String productoId,
			@NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 11, fraction = 3) BigDecimal cantidad) {

		public LineaDivision datos() {
			return new LineaDivision(Long.parseLong(productoId), cantidad);
		}
	}

	/** {@code CotizacionDivisionSolicitud}. */
	public record DivisionRegistro(@NotEmpty @Valid List<LineaDivisionRegistro> lineas) {
	}
}
