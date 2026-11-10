package com.migal.smartops.despacho.model;

/**
 * Situación de una guía de despacho (tabla {@code guia_despacho}, migraciones V6 y V12). El valor persistido es el
 * nombre en minúsculas (ADR-008). La emisión crea la guía en estado {@code emitida}; la confirmación de la salida la
 * pasa a {@code salida_confirmada}, la entrega a {@code entregada} y la cancelación de la venta a {@code anulada}.
 * El estado {@code preparacion} pertenece al modelo de datos y la versión 1 no define una operación que lo asigne.
 */
public enum EstadoGuia {

	PREPARACION, EMITIDA, SALIDA_CONFIRMADA, ENTREGADA, ANULADA;

	/** Valor persistido y expuesto en el contrato. */
	public String valor() {
		return name().toLowerCase();
	}

	/** Obtiene el estado a partir de su valor persistido. */
	public static EstadoGuia de(String valor) {
		return valueOf(valor.toUpperCase());
	}
}
