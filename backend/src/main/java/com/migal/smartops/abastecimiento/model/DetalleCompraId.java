package com.migal.smartops.abastecimiento.model;

import jakarta.persistence.Embeddable;

/**
 * Clave compuesta de la línea de una orden de compra: orden y producto (clave primaria de {@code detalle_compra}).
 * Los componentes coinciden con los atributos {@code @MapsId} de {@link DetalleCompra}.
 */
@Embeddable
public record DetalleCompraId(Long idOrden, Long idProducto) {
}
