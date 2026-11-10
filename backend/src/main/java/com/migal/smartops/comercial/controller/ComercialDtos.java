package com.migal.smartops.comercial.controller;

import com.migal.smartops.comercial.model.Cliente;
import com.migal.smartops.comercial.model.Cotizacion;
import com.migal.smartops.comercial.model.DetalleCotizacion;
import com.migal.smartops.comercial.service.CotizacionService.ResultadoDivision;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Respuestas del módulo comercial, según los esquemas del contrato. Los identificadores se expresan como texto. */
public final class ComercialDtos {

	private ComercialDtos() {
	}

	/** {@code ClienteRegistrado}. */
	public record ClienteDto(String id, String tipoDocumento, String numeroDocumento, String nombreRazonSocial,
			String telefonoContacto, String direccionReferencia, boolean estadoActivo, Instant fechaRegistro) {

		public static ClienteDto de(Cliente cliente) {
			return new ClienteDto(String.valueOf(cliente.getId()),
					cliente.getTipoDocumento() == null ? null : cliente.getTipoDocumento().valor(),
					cliente.getNumeroDocumento(), cliente.getNombreRazonSocial(), cliente.getTelefonoContacto(),
					cliente.getDireccionReferencia(), cliente.isEstadoActivo(), cliente.getFechaRegistro());
		}
	}

	/** {@code ClienteResumen}, embebido en las cotizaciones. */
	public record ClienteResumenDto(String id, String nombreRazonSocial, boolean estadoActivo) {

		static ClienteResumenDto de(Cliente cliente) {
			return new ClienteResumenDto(String.valueOf(cliente.getId()), cliente.getNombreRazonSocial(),
					cliente.isEstadoActivo());
		}
	}

	/** {@code CotizacionLinea}. */
	public record LineaCotizacionDto(String productoId, String sku, String nombreProducto, BigDecimal cantidad,
			BigDecimal precioUnitario, BigDecimal subtotal) {

		static LineaCotizacionDto de(DetalleCotizacion linea) {
			return new LineaCotizacionDto(String.valueOf(linea.getProducto().getId()), linea.getProducto().getSku(),
					linea.getProducto().getNombreOficial(), linea.getCantidadCotizada(), linea.getPrecioUnitario(),
					linea.subtotal());
		}
	}

	/** {@code CotizacionRechazo}. */
	public record RechazoDto(String origen, String usuarioId, Instant fechaRechazo, String motivo) {
	}

	/** {@code CotizacionRegistrada}. */
	public record CotizacionDto(String id, String estado, ClienteResumenDto cliente, String usuarioResponsableId,
			Instant fechaEmision, LocalDate fechaVencimiento, String condicionesComerciales, BigDecimal descuento,
			List<LineaCotizacionDto> lineas, BigDecimal subtotal, BigDecimal total, Instant fechaAceptacionCliente,
			Instant fechaValidacionMigal, RechazoDto rechazo) {

		public static CotizacionDto de(Cotizacion cotizacion) {
			RechazoDto rechazo = cotizacion.getOrigenRechazo() == null ? null
					: new RechazoDto(cotizacion.getOrigenRechazo().valor(),
							String.valueOf(cotizacion.getUsuarioRechazo().getId()), cotizacion.getFechaRechazo(),
							cotizacion.getMotivoRechazo());
			return new CotizacionDto(String.valueOf(cotizacion.getId()), cotizacion.getEstado().valor(),
					ClienteResumenDto.de(cotizacion.getCliente()),
					String.valueOf(cotizacion.getUsuarioResponsable().getId()), cotizacion.getFechaEmision(),
					cotizacion.getFechaVencimiento(), cotizacion.getCondicionesComerciales(),
					cotizacion.getDescuento(), cotizacion.getLineas().stream().map(LineaCotizacionDto::de).toList(),
					cotizacion.subtotal(), cotizacion.total(), cotizacion.getFechaAceptacionCliente(),
					cotizacion.getFechaValidacionMigal(), rechazo);
		}
	}

	/** Respuesta de la división: cotización original y cotización nueva. */
	public record DivisionDto(CotizacionDto cotizacionOriginal, CotizacionDto cotizacionNueva) {

		public static DivisionDto de(ResultadoDivision resultado) {
			return new DivisionDto(CotizacionDto.de(resultado.original()), CotizacionDto.de(resultado.nueva()));
		}
	}
}
