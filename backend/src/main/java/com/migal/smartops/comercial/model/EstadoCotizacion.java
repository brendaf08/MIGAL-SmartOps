package com.migal.smartops.comercial.model;

/**
 * Estado comercial de una cotización. El valor en la base de datos es el nombre en minúsculas (ADR-008). Los
 * estados {@code rechazada} y {@code vencida} son finales.
 */
public enum EstadoCotizacion {

	EMITIDA, ACEPTADA, VALIDADA, RECHAZADA, VENCIDA;

	/** Valor persistido y expuesto en el contrato. */
	public String valor() {
		return name().toLowerCase();
	}

	/** Obtiene el estado a partir de su valor persistido. */
	public static EstadoCotizacion de(String valor) {
		return valueOf(valor.toUpperCase());
	}
}
