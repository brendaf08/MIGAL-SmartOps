package com.migal.smartops.documentosocr.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.migal.smartops.documentosocr.config.PropiedadesOcr;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

/** Verifica que la vigilancia vence los pendientes cuyo último encolado supera el plazo configurado (ADR-012). */
class VigilanciaOcrTest {

	@Test
	void venceLosPendientesAnterioresAlPlazo() {
		DocumentoOcrService documentos = mock(DocumentoOcrService.class);
		PropiedadesOcr propiedades = new PropiedadesOcr(PropiedadesOcr.PROVEEDOR_HTTP, null,
				new PropiedadesOcr.Servicio("http://ocr.local", Duration.ofSeconds(90), null, null, null));
		Clock reloj = Clock.fixed(Instant.parse("2026-11-16T15:00:00Z"), ZoneOffset.UTC);

		new VigilanciaOcr(documentos, propiedades, reloj).revisar();

		verify(documentos).vencerPendientes(Instant.parse("2026-11-16T14:58:30Z"));
	}
}
