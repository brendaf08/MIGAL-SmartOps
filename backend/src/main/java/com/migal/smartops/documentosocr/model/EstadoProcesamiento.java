package com.migal.smartops.documentosocr.model;

/**
 * Estado de procesamiento de un documento digital (columna {@code estado_procesamiento}). El documento nace pendiente,
 * pasa a procesado_ocr al registrarse las líneas extraídas y a validado cuando el usuario confirma dichas líneas.
 */
public enum EstadoProcesamiento {

	PENDIENTE("pendiente"),
	PROCESADO_OCR("procesado_ocr"),
	VALIDADO("validado");

	private final String valor;

	EstadoProcesamiento(String valor) {
		this.valor = valor;
	}

	/** Valor persistido y expuesto en el contrato. */
	public String valor() {
		return valor;
	}

	/** Estado correspondiente a un valor persistido. */
	public static EstadoProcesamiento de(String valor) {
		for (EstadoProcesamiento estado : values()) {
			if (estado.valor.equals(valor)) {
				return estado;
			}
		}
		throw new IllegalArgumentException("Estado de procesamiento desconocido: " + valor);
	}
}
