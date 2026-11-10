package com.migal.smartops.comercial.model;

import jakarta.persistence.Embeddable;

/**
 * Clave compuesta de la línea de cotización: cotización y producto (clave primaria de {@code detalle_cotizacion}).
 * Los componentes coinciden con los atributos {@code @MapsId} de {@link DetalleCotizacion}.
 */
@Embeddable
public record DetalleCotizacionId(Long idCotizacion, Long idProducto) {
}
