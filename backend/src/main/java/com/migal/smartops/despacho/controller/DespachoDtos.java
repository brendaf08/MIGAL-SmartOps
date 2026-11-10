package com.migal.smartops.despacho.controller;

import com.migal.smartops.comercial.model.DetalleVenta;
import com.migal.smartops.comercial.model.Venta;
import com.migal.smartops.despacho.model.GuiaDespacho;
import com.migal.smartops.despacho.model.ReimpresionGuia;
import com.migal.smartops.despacho.service.DespachoService.Cambio;
import com.migal.smartops.despacho.service.DespachoService.GuiaDetalle;
import com.migal.smartops.despacho.service.DespachoService.LineaGuia;
import com.migal.smartops.despacho.service.DespachoService.ResultadoSalida;
import com.migal.smartops.despacho.service.DespachoService.UbicacionSalida;
import com.migal.smartops.inventario.service.MovimientoDetalle;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

/**
 * Respuestas del módulo de despacho, según los esquemas del contrato. Los identificadores se expresan como texto y las
 * fechas se serializan en el formato ISO del contrato.
 */
public final class DespachoDtos {

	private static final ZoneId ZONA = ZoneId.of("America/Lima");

	private DespachoDtos() {
	}

	/** {@code GuiaUbicacionSalida}. */
	public record UbicacionSalidaDto(String ubicacionId, String nombre, BigDecimal cantidad) {

		static UbicacionSalidaDto de(UbicacionSalida ubicacion) {
			return new UbicacionSalidaDto(String.valueOf(ubicacion.ubicacionId()), ubicacion.nombre(), ubicacion.cantidad());
		}
	}

	/** {@code GuiaLinea}: línea de la guía con sus ubicaciones de salida. */
	public record GuiaLineaDto(String productoId, String sku, String nombre, String unidad, BigDecimal cantidad,
			List<UbicacionSalidaDto> ubicacionesSalida) {

		static GuiaLineaDto de(LineaGuia linea) {
			DetalleVenta detalle = linea.linea();
			return new GuiaLineaDto(String.valueOf(detalle.getProducto().getId()), detalle.getProducto().getSku(),
					detalle.getProducto().getNombreOficial(), detalle.getProducto().getUnidadMedida(),
					detalle.getCantidadVendida(), linea.ubicaciones().stream().map(UbicacionSalidaDto::de).toList());
		}
	}

	/** {@code GuiaResumen}: guía del listado, sin líneas. */
	public record GuiaResumenDto(String id, String codigo, String idVenta, String codigoVenta, String cliente,
			String estado, Instant fechaEmision, Instant fechaSalidaReal, Instant fechaEntrega,
			String usuarioResponsableId) {

		static GuiaResumenDto de(GuiaDespacho guia) {
			Venta venta = guia.getVenta();
			return new GuiaResumenDto(String.valueOf(guia.getId()), guia.getCodigo(), String.valueOf(venta.getId()),
					venta.getCodigo(), venta.getCliente().getNombreRazonSocial(), guia.getEstado().valor(),
					guia.getFechaEmision(), guia.getFechaSalidaReal(), guia.getFechaEntrega(),
					guia.getUsuarioResponsable() == null ? null : String.valueOf(guia.getUsuarioResponsable().getId()));
		}
	}

	/** {@code GuiaDespacho}: guía con sus líneas y sus ubicaciones de salida. */
	public record GuiaDespachoDto(String id, String codigo, String idVenta, String codigoVenta, String cliente,
			String estado, Instant fechaEmision, Instant fechaSalidaReal, Instant fechaEntrega,
			String usuarioResponsableId, String direccionEntrega, LocalDate fechaDespacho,
			List<GuiaLineaDto> lineas) {

		public static GuiaDespachoDto de(GuiaDetalle detalle) {
			GuiaDespacho guia = detalle.guia();
			GuiaResumenDto resumen = GuiaResumenDto.de(guia);
			return new GuiaDespachoDto(resumen.id(), resumen.codigo(), resumen.idVenta(), resumen.codigoVenta(),
					resumen.cliente(), resumen.estado(), resumen.fechaEmision(), resumen.fechaSalidaReal(),
					resumen.fechaEntrega(), resumen.usuarioResponsableId(), guia.getDireccionEntrega(),
					guia.getFechaDespacho(), detalle.lineas().stream().map(GuiaLineaDto::de).toList());
		}
	}

	/** {@code GuiaMovimientoSalida}: movimiento de Kardex generado por la confirmación de la salida. */
	public record GuiaMovimientoSalidaDto(String id, Instant fechaHora, String productoId, String sku,
			String tipoOperacion, BigDecimal cantidad, String origenMovimiento, String ubicacionId, String usuarioId,
			String idGuia) {

		static GuiaMovimientoSalidaDto de(MovimientoDetalle movimiento) {
			return new GuiaMovimientoSalidaDto(String.valueOf(movimiento.id()), movimiento.fechaHora(),
					String.valueOf(movimiento.productoId()), movimiento.sku(), movimiento.tipoOperacion().valor(),
					movimiento.cantidad(), movimiento.origenMovimiento().valor(), String.valueOf(movimiento.ubicacionId()),
					String.valueOf(movimiento.usuarioId()), String.valueOf(movimiento.idGuia()));
		}
	}

	/** {@code GuiaCambioExistencia}: existencia antes y después de la salida. */
	public record GuiaCambioExistenciaDto(String productoId, String sku, String ubicacionId, String unidad,
			BigDecimal antes, BigDecimal despues) {

		static GuiaCambioExistenciaDto de(Cambio cambio) {
			return new GuiaCambioExistenciaDto(String.valueOf(cambio.productoId()), cambio.sku(),
					String.valueOf(cambio.ubicacionId()), cambio.unidad(), cambio.antes(), cambio.despues());
		}
	}

	/** {@code GuiaResultadoSalida}: guía con la salida confirmada, sus movimientos y las existencias afectadas. */
	public record ResultadoSalidaDto(GuiaDespachoDto guia, List<GuiaMovimientoSalidaDto> movimientos,
			List<GuiaCambioExistenciaDto> cambios) {

		static ResultadoSalidaDto de(ResultadoSalida resultado) {
			return new ResultadoSalidaDto(GuiaDespachoDto.de(resultado.guia()),
					resultado.movimientos().stream().map(GuiaMovimientoSalidaDto::de).toList(),
					resultado.cambios().stream().map(GuiaCambioExistenciaDto::de).toList());
		}
	}

	/** {@code GuiaReimpresion}: reimpresión registrada con su motivo. */
	public record GuiaReimpresionDto(String id, String idGuia, Instant fechaHora, String usuarioId, String motivo) {

		static GuiaReimpresionDto de(ReimpresionGuia reimpresion) {
			return new GuiaReimpresionDto(String.valueOf(reimpresion.getId()), String.valueOf(reimpresion.getGuia().getId()),
					reimpresion.getFechaReimpresion(), String.valueOf(reimpresion.getUsuario().getId()),
					reimpresion.getMotivo());
		}
	}

	/** {@code DespachoVentaPendiente}: resumen de una venta lista para emitir su guía. */
	public record VentaPendienteDto(String idVenta, String codigoVenta, LocalDate fecha, String cliente,
			String direccionEntrega, int cantidadLineas, String estado) {

		static VentaPendienteDto de(Venta venta) {
			return new VentaPendienteDto(String.valueOf(venta.getId()), venta.getCodigo(),
					venta.getFechaVenta().atZone(ZONA).toLocalDate(), venta.getCliente().getNombreRazonSocial(),
					Objects.toString(venta.getCliente().getDireccionReferencia(), ""), venta.getLineas().size(),
					venta.getEstado().valor());
		}
	}
}
