package com.migal.smartops.inventario.model;

/** Operación que origina un movimiento de Kardex. El valor en la base de datos es el nombre en minúsculas. */
public enum OrigenMovimiento {
	DESPACHO, RECEPCION, AJUSTE, TRASLADO;

	public String valor() {
		return name().toLowerCase();
	}

	public static OrigenMovimiento de(String valor) {
		return valueOf(valor.toUpperCase());
	}
}
