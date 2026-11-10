package com.migal.smartops.catalogo.controller;

import com.migal.smartops.catalogo.model.Producto;
import java.math.BigDecimal;
import java.time.Instant;

/** Producto del catálogo, según el esquema {@code Producto} del contrato. */
public record ProductoDto(String id, String sku, String nombreOficial, String descripcion, String unidadMedida,
		BigDecimal precioReferencial, BigDecimal stockMinimo, boolean estadoActivo, String idCategoria, String marca,
		String presentacion, Instant fechaRegistro, Instant fechaActualizacion) {

	public static ProductoDto de(Producto producto) {
		return new ProductoDto(String.valueOf(producto.getId()), producto.getSku(), producto.getNombreOficial(),
				producto.getDescripcion(), producto.getUnidadMedida(), producto.getPrecioReferencial(),
				producto.getStockMinimo(), producto.isEstadoActivo(), String.valueOf(producto.getCategoria().getId()),
				producto.getMarca(), producto.getPresentacion(), producto.getFechaRegistro(),
				producto.getFechaActualizacion());
	}
}
