package com.migal.smartops.abastecimiento.controller;

import com.migal.smartops.abastecimiento.model.CatalogoProveedor;
import com.migal.smartops.abastecimiento.model.DetalleCompra;
import com.migal.smartops.abastecimiento.model.OrdenCompra;
import com.migal.smartops.abastecimiento.model.Proveedor;
import com.migal.smartops.abastecimiento.service.OrdenCompraService.OrdenDetalle;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Respuestas del módulo de abastecimiento, según los esquemas del contrato. Los identificadores se expresan como texto
 * y las fechas se serializan en formato RFC 3339.
 */
public final class AbastecimientoDtos {

	private AbastecimientoDtos() {
	}

	/** {@code Proveedor}: proveedor del directorio. */
	public record ProveedorDto(String id, String ruc, String razonSocial, String contacto, String condiciones,
			boolean estadoActivo, Instant fechaRegistro) {

		public static ProveedorDto de(Proveedor proveedor) {
			return new ProveedorDto(String.valueOf(proveedor.getId()), proveedor.getRuc(), proveedor.getRazonSocial(),
					proveedor.getContacto(), proveedor.getCondiciones(), proveedor.isEstadoActivo(),
					proveedor.getFechaRegistro());
		}
	}

	/** {@code ProveedorCatalogoItem}: producto que suministra el proveedor. */
	public record CatalogoItemDto(String sku, String nombre, BigDecimal precioCompra,
			Integer plazoEntregaReferencialDias, Instant fechaActualizacionPrecio) {

		public static CatalogoItemDto de(CatalogoProveedor linea) {
			return new CatalogoItemDto(linea.getProducto().getSku(), linea.getProducto().getNombreOficial(),
					linea.getPrecioCompra(), linea.getPlazoEntregaReferencialDias(), linea.getFechaActualizacionPrecio());
		}
	}

	/** {@code OrdenCompraLinea}: línea con la cantidad recibida y la pendiente calculadas. */
	public record OrdenLineaDto(String sku, String nombre, BigDecimal cantidadComprada, BigDecimal costoUnitario,
			BigDecimal subtotal, BigDecimal cantidadRecibida, BigDecimal cantidadPendienteRecepcion) {

		static OrdenLineaDto de(DetalleCompra linea, BigDecimal recibida) {
			BigDecimal pendiente = linea.getCantidadComprada().subtract(recibida).max(BigDecimal.ZERO);
			return new OrdenLineaDto(linea.getProducto().getSku(), linea.getProducto().getNombreOficial(),
					linea.getCantidadComprada(), linea.getCostoUnitario(), linea.subtotal(), recibida, pendiente);
		}
	}

	/** {@code OrdenCompra}: orden con sus líneas, su total y su estado de cumplimiento. */
	public record OrdenCompraDto(String id, String codigo, String proveedorId, String usuarioResponsableId,
			Instant fechaEmision, String estado, LocalDate fechaEntregaEstimada, String observaciones,
			String modalidadPago, BigDecimal total, List<OrdenLineaDto> lineas) {

		public static OrdenCompraDto de(OrdenDetalle detalle) {
			OrdenCompra orden = detalle.orden();
			List<OrdenLineaDto> lineas = orden.getLineas().stream()
					.sorted(java.util.Comparator.comparing(l -> l.getProducto().getSku()))
					.map(l -> OrdenLineaDto.de(l, detalle.recibidoPorProducto()
							.getOrDefault(l.getProducto().getId(), BigDecimal.ZERO)))
					.toList();
			return new OrdenCompraDto(String.valueOf(orden.getId()), orden.getCodigo(),
					String.valueOf(orden.getProveedor().getId()), String.valueOf(orden.getUsuarioResponsable().getId()),
					orden.getFechaEmision(), orden.getEstado().valor(), orden.getFechaEntregaEstimada(),
					orden.getObservaciones(), orden.getModalidadPago(), orden.total(), lineas);
		}
	}
}
