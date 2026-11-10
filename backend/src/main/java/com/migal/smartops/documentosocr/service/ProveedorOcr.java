package com.migal.smartops.documentosocr.service;

/**
 * Puerto hacia el servicio de OCR y Fuzzy Matching (RNF-03, ADR-004). El módulo encola los documentos mediante esta
 * interfaz; el resultado regresa de forma asíncrona a {@code DocumentoOcrService#registrarResultados}. Las
 * implementaciones son intercambiables mediante la propiedad {@code migal.ocr.proveedor}.
 */
public interface ProveedorOcr {

	/**
	 * Encola el procesamiento de un documento.
	 *
	 * @throws com.migal.smartops.comun.errores.ErrorDeNegocio con SERVICIO_NO_DISPONIBLE si el documento no puede encolarse
	 */
	void encolar(SolicitudOcr solicitud);
}
