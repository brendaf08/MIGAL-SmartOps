package com.migal.smartops.comercial.controller;

import com.migal.smartops.comercial.model.DetalleVenta;
import com.migal.smartops.comercial.model.Venta;
import com.migal.smartops.comercial.service.VentaService.Cobertura;
import com.migal.smartops.comercial.service.VentaService.ReservaVista;
import com.migal.smartops.comercial.service.VentaService.VentaDetalle;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Respuestas del módulo de ventas, según los esquemas del contrato. Los identificadores se expresan como texto. */
public final class VentaDtos {

	private VentaDtos() {
	}

	/** {@code VentaLinea}: línea con su cobertura derivada. */
	public record LineaVentaDto(String id, String productoId, String sku, String nombreProducto,
			BigDecimal cantidadVendida, BigDecimal precioUnitario, BigDecimal subtotal,
			BigDecimal cantidadReservadaActual, BigDecimal cantidadPendienteAbastecimiento) {

		static LineaVentaDto de(DetalleVenta linea, Cobertura cobertura) {
			return new LineaVentaDto(String.valueOf(linea.getId()), String.valueOf(linea.getProducto().getId()),
					linea.getProducto().getSku(), linea.getProducto().getNombreOficial(), linea.getCantidadVendida(),
					linea.getPrecioUnitario(), linea.subtotal(), cobertura.reservada(), cobertura.pendiente());
		}
	}

	/** {@code VentaReserva}: reserva de una línea desde una ubicación. */
	public record ReservaVentaDto(String id, String lineaId, String productoId, String ubicacionId,
			String nombreUbicacion, BigDecimal cantidadReservada, String estado, Instant fechaReserva,
			Instant fechaCierre) {

		static ReservaVentaDto de(ReservaVista reserva) {
			return new ReservaVentaDto(String.valueOf(reserva.id()), String.valueOf(reserva.idLinea()),
					String.valueOf(reserva.idProducto()), String.valueOf(reserva.idUbicacion()),
					reserva.nombreUbicacion(), reserva.cantidad(), reserva.estado(), reserva.fechaReserva(),
					reserva.fechaCierre());
		}
	}

	/** {@code VentaDetalle}: venta con sus líneas, su cobertura, sus reservas y sus totales. */
	public record VentaDto(String id, Instant fechaVenta, String cotizacionId, String clienteId, String clienteNombre,
			String usuarioResponsableId, String estado, BigDecimal descuento, BigDecimal subtotal, BigDecimal total,
			Instant fechaCancelacion, String motivoCancelacion, String usuarioAutorizanteId,
			List<LineaVentaDto> lineas, List<ReservaVentaDto> reservas) {

		public static VentaDto de(VentaDetalle detalle) {
			Venta venta = detalle.venta();
			return new VentaDto(String.valueOf(venta.getId()), venta.getFechaVenta(),
					String.valueOf(venta.getCotizacion().getId()), String.valueOf(venta.getCliente().getId()),
					venta.getCliente().getNombreRazonSocial(), String.valueOf(venta.getUsuarioResponsable().getId()),
					venta.getEstado().valor(), venta.getDescuento(), venta.subtotal(), venta.total(),
					venta.getFechaCancelacion(), venta.getMotivoCancelacion(),
					venta.getUsuarioAutorizante() == null ? null : String.valueOf(venta.getUsuarioAutorizante().getId()),
					venta.getLineas().stream().map(l -> LineaVentaDto.de(l, detalle.coberturaPorLinea().get(l.getId())))
							.toList(),
					detalle.reservas().stream().map(ReservaVentaDto::de).toList());
		}
	}

	/** {@code VentaResumen}: venta del listado, sin líneas ni reservas. */
	public record VentaResumenDto(String id, Instant fechaVenta, String cotizacionId, String clienteId,
			String clienteNombre, String estado, BigDecimal total) {

		public static VentaResumenDto de(Venta venta) {
			return new VentaResumenDto(String.valueOf(venta.getId()), venta.getFechaVenta(),
					String.valueOf(venta.getCotizacion().getId()), String.valueOf(venta.getCliente().getId()),
					venta.getCliente().getNombreRazonSocial(), venta.getEstado().valor(), venta.total());
		}
	}
}
