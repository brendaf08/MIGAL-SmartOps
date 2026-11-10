package com.migal.smartops.documentosocr.model;

/**
 * Clasificación funcional de un documento digital (columna {@code tipo_documento}). El valor de la base de datos y del
 * contrato coincide con el nombre en minúsculas: {@code solicitud_cliente} o {@code documento_recepcion}.
 */
public enum TipoDocumento {

	SOLICITUD_CLIENTE("solicitud_cliente"),
	DOCUMENTO_RECEPCION("documento_recepcion");

	private final String valor;

	TipoDocumento(String valor) {
		this.valor = valor;
	}

	/** Valor persistido y expuesto en el contrato. */
	public String valor() {
		return valor;
	}

	/** Tipo correspondiente a un valor del contrato. Un valor desconocido no tiene tipo y se informa como nulo. */
	public static TipoDocumento de(String valor) {
		for (TipoDocumento tipo : values()) {
			if (tipo.valor.equals(valor)) {
				return tipo;
			}
		}
		return null;
	}
}
