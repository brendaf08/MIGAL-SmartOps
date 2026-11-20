package com.migal.smartops.despacho.model;

import java.util.Arrays;

/**
 * Forma de entrega de una guía de despacho. El valor persistido es el nombre en minúsculas (ADR-008). El retiro en
 * tienda corresponde a la venta de mostrador: el cliente se lleva la mercadería en el local.
 */
public enum TipoEntrega {

	ENVIO("envio"), RETIRO_TIENDA("retiro_tienda");

	/** Texto con el que se registra la dirección de una guía de retiro en tienda. */
	public static final String DIRECCION_RETIRO = "Retiro en tienda";

	private final String valor;

	TipoEntrega(String valor) {
		this.valor = valor;
	}

	/** Valor persistido y expuesto en el contrato. */
	public String valor() {
		return valor;
	}

	/** Obtiene el tipo a partir de su valor persistido. */
	public static TipoEntrega de(String valor) {
		return Arrays.stream(values()).filter(t -> t.valor.equals(valor)).findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Tipo de entrega desconocido: " + valor));
	}
}
