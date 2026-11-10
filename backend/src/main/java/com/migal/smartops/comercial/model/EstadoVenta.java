package com.migal.smartops.comercial.model;

/**
 * Situación operativa de una venta (tabla {@code venta}, ADR-003). El valor persistido es el nombre en minúsculas
 * (ADR-008). La transición a {@code lista_para_despacho} la realiza el módulo de despacho al emitir la guía, y la
 * transición a {@code despachada} al confirmar la salida física. Los estados {@code despachada} y {@code cancelada}
 * son finales.
 */
public enum EstadoVenta {

	PENDIENTE_ABASTECIMIENTO, PENDIENTE_DESPACHO, LISTA_PARA_DESPACHO, DESPACHADA, CANCELADA;

	/** Valor persistido y expuesto en el contrato. */
	public String valor() {
		return name().toLowerCase();
	}

	/** Obtiene el estado a partir de su valor persistido. */
	public static EstadoVenta de(String valor) {
		return valueOf(valor.toUpperCase());
	}
}
