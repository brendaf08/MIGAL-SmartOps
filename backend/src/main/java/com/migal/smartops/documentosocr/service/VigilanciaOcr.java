package com.migal.smartops.documentosocr.service;

import com.migal.smartops.documentosocr.config.PropiedadesOcr;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Vigilancia de documentos pendientes (ADR-012, punto 4). Registra SERVICIO_NO_DISPONIBLE en los documentos cuyo último
 * encolado supera el plazo sin resultado, por ejemplo porque el servicio de OCR se detuvo, la API del modelo falló o el
 * resultado se perdió. Así ningún documento queda pendiente sin posibilidad de reprocesarse. Un resultado que llega
 * después se registra igual mientras el documento siga pendiente.
 */
public class VigilanciaOcr {

	private static final Logger REGISTRO = LoggerFactory.getLogger(VigilanciaOcr.class);

	private final DocumentoOcrService documentos;
	private final PropiedadesOcr propiedades;
	private final Clock reloj;

	public VigilanciaOcr(DocumentoOcrService documentos, PropiedadesOcr propiedades, Clock reloj) {
		this.documentos = documentos;
		this.propiedades = propiedades;
		this.reloj = reloj;
	}

	@Scheduled(fixedDelayString = "${migal.ocr.servicio.intervalo-vigilancia:PT30S}",
			initialDelayString = "${migal.ocr.servicio.intervalo-vigilancia:PT30S}")
	public void revisar() {
		int vencidos = documentos.vencerPendientes(reloj.instant().minus(propiedades.servicio().plazoResultado()));
		if (vencidos > 0) {
			REGISTRO.warn("{} documento(s) sin resultado del servicio de OCR dentro del plazo", vencidos);
		}
	}
}
