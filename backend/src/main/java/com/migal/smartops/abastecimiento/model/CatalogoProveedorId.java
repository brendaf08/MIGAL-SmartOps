package com.migal.smartops.abastecimiento.model;

import jakarta.persistence.Embeddable;

/**
 * Clave compuesta del catálogo de un proveedor: proveedor y producto (clave primaria de {@code catalogo_proveedor}).
 * Los componentes coinciden con los atributos {@code @MapsId} de {@link CatalogoProveedor}.
 */
@Embeddable
public record CatalogoProveedorId(Long idProveedor, Long idProducto) {
}
