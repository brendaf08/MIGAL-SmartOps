package com.migal.smartops.abastecimiento.model;

/**
 * Estado de cumplimiento de una orden de compra (tabla {@code orden_compra}, migración V5). El valor persistido es el
 * nombre en minúsculas (ADR-008). Pendiente indica que la orden no tiene recepciones validadas; parcial, que tiene
 * recepciones validadas incompletas; y completada, que la orden está recibida en su totalidad o cerrada.
 */
public enum EstadoOrdenCompra {

	PENDIENTE, PARCIAL, COMPLETADA;

	/** Valor persistido y expuesto en el contrato. */
	public String valor() {
		return name().toLowerCase();
	}

	/** Obtiene el estado a partir de su valor persistido. */
	public static EstadoOrdenCompra de(String valor) {
		return valueOf(valor.toUpperCase());
	}
}
