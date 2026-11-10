package com.migal.smartops.comercial.model;

/**
 * Tipo de documento de identificación del cliente. El valor almacenado en la columna {@code tipo_documento} y
 * expuesto en el contrato es el texto en minúsculas ({@code dni} o {@code ruc}), según la migración V11.
 */
public enum TipoDocumentoCliente {

	DNI, RUC;

	/** Valor persistido y expuesto en el contrato. */
	public String valor() {
		return name().toLowerCase();
	}

	/** Obtiene el tipo a partir de su valor persistido. */
	public static TipoDocumentoCliente de(String valor) {
		return valueOf(valor.toUpperCase());
	}
}
