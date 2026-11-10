package com.migal.smartops.inventario.model;

/** Estado de una reserva de stock. El valor en la base de datos es el nombre en minúsculas. */
public enum EstadoReserva {
	ACTIVA, LIBERADA, CONSUMIDA;

	public String valor() {
		return name().toLowerCase();
	}

	public static EstadoReserva de(String valor) {
		return valueOf(valor.toUpperCase());
	}
}
