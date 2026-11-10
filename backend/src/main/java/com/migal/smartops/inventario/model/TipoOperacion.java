package com.migal.smartops.inventario.model;

/** Clasificación de un movimiento de Kardex. El valor en la base de datos es el nombre en minúsculas. */
public enum TipoOperacion {
	ENTRADA, SALIDA;

	public String valor() {
		return name().toLowerCase();
	}

	public static TipoOperacion de(String valor) {
		return valueOf(valor.toUpperCase());
	}

	public TipoOperacion opuesto() {
		return this == ENTRADA ? SALIDA : ENTRADA;
	}
}
