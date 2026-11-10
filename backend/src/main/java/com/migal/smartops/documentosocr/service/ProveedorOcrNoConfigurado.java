package com.migal.smartops.documentosocr.service;

import com.migal.smartops.comun.errores.CodigoError;
import com.migal.smartops.comun.errores.ErrorDeNegocio;

/**
 * Proveedor usado cuando no hay un servicio de OCR configurado. Todo encolado responde SERVICIO_NO_DISPONIBLE, de modo
 * que la carga no deja documentos pendientes sin procesar.
 */
public class ProveedorOcrNoConfigurado implements ProveedorOcr {

	@Override
	public void encolar(SolicitudOcr solicitud) {
		throw new ErrorDeNegocio(CodigoError.SERVICIO_NO_DISPONIBLE,
				"El servicio de OCR no está configurado. Intente más tarde.");
	}
}
