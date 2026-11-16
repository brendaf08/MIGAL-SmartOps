package com.migal.smartops.documentosocr.service;

import org.springframework.context.ApplicationEventPublisher;

/**
 * Proveedor que envía los documentos al servicio de OCR por HTTP (ADR-012, punto 3). El encolado solo publica la
 * solicitud como evento de aplicación: {@link EnvioOcrHttp} la envía después de confirmar la transacción que registró el
 * documento, de modo que el resultado no puede llegar antes de que el documento exista. Por eso el encolado no falla por
 * la red; un envío fallido se registra después como SERVICIO_NO_DISPONIBLE.
 */
public class ProveedorOcrHttp implements ProveedorOcr {

	private final ApplicationEventPublisher eventos;

	public ProveedorOcrHttp(ApplicationEventPublisher eventos) {
		this.eventos = eventos;
	}

	@Override
	public void encolar(SolicitudOcr solicitud) {
		eventos.publishEvent(solicitud);
	}
}
