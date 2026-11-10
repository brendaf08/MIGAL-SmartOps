package com.migal.smartops.recepciones.model;

/**
 * Estado de una recepción de mercadería (tabla {@code orden_recepcion}, migración V5). El valor persistido es el nombre
 * en minúsculas (ADR-008). El registro crea la recepción en estado pendiente y la validación la pasa a validada, que es
 * un estado final.
 */
public enum EstadoRecepcion {

	PENDIENTE, VALIDADA;

	/** Valor persistido y expuesto en el contrato. */
	public String valor() {
		return name().toLowerCase();
	}

	/** Obtiene el estado a partir de su valor persistido. */
	public static EstadoRecepcion de(String valor) {
		return valueOf(valor.toUpperCase());
	}
}
