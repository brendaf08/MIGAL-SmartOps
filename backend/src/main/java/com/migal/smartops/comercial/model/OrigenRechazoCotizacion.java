package com.migal.smartops.comercial.model;

/**
 * Parte que rechaza una cotización: el cliente rechaza una cotización emitida y MIGAL rechaza una cotización
 * aceptada (V12). El valor persistido es el nombre en minúsculas.
 */
public enum OrigenRechazoCotizacion {

	CLIENTE, MIGAL;

	/** Valor persistido y expuesto en el contrato. */
	public String valor() {
		return name().toLowerCase();
	}

	/** Obtiene el origen a partir de su valor persistido. */
	public static OrigenRechazoCotizacion de(String valor) {
		return valueOf(valor.toUpperCase());
	}
}
