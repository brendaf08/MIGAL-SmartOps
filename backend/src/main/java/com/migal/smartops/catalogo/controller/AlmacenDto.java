package com.migal.smartops.catalogo.controller;

import com.migal.smartops.catalogo.model.Almacen;

/** Ubicación de inventario, según el esquema {@code Almacen} del contrato. */
public record AlmacenDto(String id, String nombre, String tipoUbicacion, String direccionReferencia,
		boolean estadoActivo) {

	public static AlmacenDto de(Almacen almacen) {
		return new AlmacenDto(String.valueOf(almacen.getId()), almacen.getNombre(), almacen.getTipoUbicacion(),
				almacen.getDireccionReferencia(), almacen.isEstadoActivo());
	}
}
