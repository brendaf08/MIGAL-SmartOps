package com.migal.smartops.documentosocr.service;

import org.springframework.context.ApplicationEventPublisher;

/**
 * Proveedor simulado para desarrollo y pruebas. No realiza llamadas de red: publica la solicitud como evento de
 * aplicación y el procesador simulado registra su resultado después de confirmar la transacción de carga. El resultado
 * es determinista para un mismo archivo (ver {@link ProcesadorOcrSimulado}).
 */
public class ProveedorOcrSimulado implements ProveedorOcr {

	private final ApplicationEventPublisher eventos;

	public ProveedorOcrSimulado(ApplicationEventPublisher eventos) {
		this.eventos = eventos;
	}

	@Override
	public void encolar(SolicitudOcr solicitud) {
		eventos.publishEvent(solicitud);
	}
}
