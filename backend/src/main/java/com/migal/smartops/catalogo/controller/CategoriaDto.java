package com.migal.smartops.catalogo.controller;

import com.migal.smartops.catalogo.model.Categoria;

/** Categoría de producto, según el esquema {@code Categoria} del contrato. */
public record CategoriaDto(String id, String nombre, String idCategoriaPadre, boolean aprobacionAutomatica) {

	public static CategoriaDto de(Categoria categoria) {
		return new CategoriaDto(String.valueOf(categoria.getId()), categoria.getNombre(),
				categoria.getPadre() == null ? null : String.valueOf(categoria.getPadre().getId()),
				categoria.isAprobacionAutomatica());
	}
}
