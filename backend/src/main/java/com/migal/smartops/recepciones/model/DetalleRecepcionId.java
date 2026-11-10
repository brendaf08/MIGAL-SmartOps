package com.migal.smartops.recepciones.model;

import jakarta.persistence.Embeddable;

/**
 * Clave compuesta de la línea de una recepción: recepción, producto y ubicación de destino (clave primaria de
 * {@code detalle_recepcion}). Los componentes coinciden con los atributos {@code @MapsId} de {@link DetalleRecepcion}.
 */
@Embeddable
public record DetalleRecepcionId(Long idRecepcion, Long idProducto, Long idAlmacenDestino) {
}
