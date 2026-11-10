package com.migal.smartops.recepciones.controller;

import com.migal.smartops.abastecimiento.model.DetalleCompra;
import com.migal.smartops.abastecimiento.service.OrdenCompraService.OrdenDetalle;
import com.migal.smartops.inventario.service.MovimientoDetalle;
import com.migal.smartops.recepciones.model.DetalleRecepcion;
import com.migal.smartops.recepciones.model.Recepcion;
import com.migal.smartops.recepciones.service.RecepcionService.CambioExistencia;
import com.migal.smartops.recepciones.service.RecepcionService.RecepcionDetalle;
import com.migal.smartops.recepciones.service.RecepcionService.RecepcionResumen;
import com.migal.smartops.recepciones.service.RecepcionService.ResultadoValidacion;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Respuestas del módulo de recepciones, según los esquemas del contrato. Los identificadores se expresan como texto.
 */
public final class RecepcionesDtos {

	private RecepcionesDtos() {
	}

	/** {@code RecepcionResumen}: fila del listado. */
	public record ResumenDto(String id, String codigo, String ordenCompraId, String codigoOrdenCompra, String proveedor,
			String estado, LocalDate fechaRecepcion, Instant fechaValidacion, String usuarioResponsableId) {

		public static ResumenDto de(RecepcionResumen r) {
			return new ResumenDto(String.valueOf(r.id()), r.codigo(), String.valueOf(r.ordenCompraId()), r.codigoOrdenCompra(),
					r.proveedor(), r.estado().valor(), r.fechaRecepcion(), r.fechaValidacion(),
					String.valueOf(r.usuarioResponsableId()));
		}
	}

	/** {@code RecepcionLinea}: línea de la recepción con su ubicación. */
	public record LineaDto(String productoId, String sku, String nombre, String unidad, String ubicacionId,
			String nombreUbicacion, BigDecimal cantidadRecibida) {

		static LineaDto de(DetalleRecepcion linea) {
			return new LineaDto(String.valueOf(linea.getProducto().getId()), linea.getProducto().getSku(),
					linea.getProducto().getNombreOficial(), linea.getProducto().getUnidadMedida(),
					String.valueOf(linea.getAlmacen().getId()), linea.getAlmacen().getNombre(),
					linea.getCantidadRecibida());
		}
	}

	/** {@code RecepcionOrdenLinea}: producto de la orden con la cantidad pendiente de recepción. */
	public record OrdenLineaDto(String productoId, String sku, String nombre, String unidad, BigDecimal cantidadComprada,
			BigDecimal cantidadPendiente) {
	}

	/** {@code RecepcionOrdenReferencia}: orden de compra de origen en su estado vigente. */
	public record OrdenReferenciaDto(String id, String codigo, String proveedor, String estado, List<OrdenLineaDto> lineas) {

		static OrdenReferenciaDto de(OrdenDetalle detalle) {
			var orden = detalle.orden();
			List<OrdenLineaDto> lineas = orden.getLineas().stream().map(l -> lineaOrden(l, detalle)).toList();
			return new OrdenReferenciaDto(String.valueOf(orden.getId()), orden.getCodigo(),
					orden.getProveedor().getRazonSocial(), orden.getEstado().valor(), lineas);
		}

		private static OrdenLineaDto lineaOrden(DetalleCompra linea, OrdenDetalle detalle) {
			BigDecimal recibido = detalle.recibidoPorProducto().getOrDefault(linea.getProducto().getId(), BigDecimal.ZERO);
			return new OrdenLineaDto(String.valueOf(linea.getProducto().getId()), linea.getProducto().getSku(),
					linea.getProducto().getNombreOficial(), linea.getProducto().getUnidadMedida(),
					linea.getCantidadComprada(), linea.getCantidadComprada().subtract(recibido).max(BigDecimal.ZERO));
		}
	}

	/** {@code RecepcionDetalle}: recepción con sus líneas, su documento de precarga y la orden de compra. */
	public record DetalleDto(String id, String codigo, String ordenCompraId, String codigoOrdenCompra, String proveedor,
			String estado, LocalDate fechaRecepcion, Instant fechaValidacion, String usuarioResponsableId,
			String observaciones, String documentoId, List<LineaDto> lineas, OrdenReferenciaDto ordenCompra) {

		public static DetalleDto de(RecepcionDetalle detalle) {
			Recepcion recepcion = detalle.recepcion();
			return new DetalleDto(String.valueOf(recepcion.getId()), recepcion.getCodigo(),
					String.valueOf(recepcion.getOrden().getId()), recepcion.getOrden().getCodigo(),
					recepcion.getOrden().getProveedor().getRazonSocial(), recepcion.getEstado().valor(),
					recepcion.getFechaRecepcion(), recepcion.getFechaValidacion(),
					String.valueOf(recepcion.getUsuarioResponsable().getId()), recepcion.getObservaciones(),
					detalle.documentoId() == null ? null : String.valueOf(detalle.documentoId()),
					recepcion.getLineas().stream().map(LineaDto::de).toList(),
					OrdenReferenciaDto.de(detalle.orden()));
		}
	}

	/** {@code RecepcionMovimientoEntrada}: movimiento de Kardex generado por la validación. */
	public record MovimientoDto(String id, Instant fechaHora, String productoId, String sku, String tipoOperacion,
			BigDecimal cantidad, String origenMovimiento, String ubicacionId, String usuarioId, String idRecepcion) {

		static MovimientoDto de(MovimientoDetalle m) {
			return new MovimientoDto(String.valueOf(m.id()), m.fechaHora(), String.valueOf(m.productoId()), m.sku(),
					m.tipoOperacion().valor(), m.cantidad(), m.origenMovimiento().valor(), String.valueOf(m.ubicacionId()),
					String.valueOf(m.usuarioId()), String.valueOf(m.idRecepcion()));
		}
	}

	/** {@code RecepcionCambioExistencia}: existencia antes y después de la validación. */
	public record CambioDto(String productoId, String sku, String ubicacionId, String unidad, BigDecimal antes,
			BigDecimal despues) {

		static CambioDto de(CambioExistencia c) {
			return new CambioDto(String.valueOf(c.productoId()), c.sku(), String.valueOf(c.ubicacionId()), c.unidad(),
					c.antes(), c.despues());
		}
	}

	/** {@code RecepcionResultadoValidacion}: recepción validada con sus movimientos, existencias y orden actualizada. */
	public record ResultadoDto(DetalleDto recepcion, List<MovimientoDto> movimientos, List<CambioDto> cambios,
			OrdenReferenciaDto ordenCompra) {

		public static ResultadoDto de(ResultadoValidacion resultado) {
			return new ResultadoDto(DetalleDto.de(resultado.recepcion()),
					resultado.movimientos().stream().map(MovimientoDto::de).toList(),
					resultado.cambios().stream().map(CambioDto::de).toList(),
					OrdenReferenciaDto.de(resultado.ordenCompra()));
		}
	}
}
