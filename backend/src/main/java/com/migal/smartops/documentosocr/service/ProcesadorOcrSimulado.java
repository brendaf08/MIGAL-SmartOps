package com.migal.smartops.documentosocr.service;

import com.migal.smartops.documentosocr.config.PropiedadesOcr;
import java.math.BigDecimal;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Registra el resultado determinista del proveedor simulado una vez confirmada la carga. El resultado depende solo del
 * nombre del archivo: un nombre que contiene {@code ilegible} produce un resultado ilegible; cualquier otro produce una
 * línea con cantidad 10 y sin coincidencias, de modo que la revisión manual sea necesaria.
 */
@Component
@ConditionalOnProperty(prefix = "migal.ocr", name = "proveedor", havingValue = PropiedadesOcr.PROVEEDOR_SIMULADO,
		matchIfMissing = true)
public class ProcesadorOcrSimulado {

	private static final Logger REGISTRO = LoggerFactory.getLogger(ProcesadorOcrSimulado.class);
	private static final String MARCA_ILEGIBLE = "ilegible";

	private final DocumentoOcrService documentos;

	public ProcesadorOcrSimulado(DocumentoOcrService documentos) {
		this.documentos = documentos;
	}

	/**
	 * Procesa la solicitud después de confirmar la transacción que la encoló. Se ejecuta en una transacción nueva, porque la
	 * original ya está confirmada. Un fallo se registra sin revertir la carga.
	 */
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void procesar(SolicitudOcr solicitud) {
		try {
			documentos.registrarResultados(solicitud.idDocumento(), resultadoDeterminista(solicitud));
		} catch (RuntimeException e) {
			REGISTRO.warn("No fue posible registrar el resultado simulado del documento {}", solicitud.idDocumento(), e);
		}
	}

	/** Resultado determinista para un nombre de archivo. */
	static DocumentoOcrService.ResultadoOcr resultadoDeterminista(SolicitudOcr solicitud) {
		String nombre = solicitud.nombreArchivo() == null ? "" : solicitud.nombreArchivo().toLowerCase();
		if (nombre.contains(MARCA_ILEGIBLE)) {
			return new DocumentoOcrService.ResultadoOcr(true, List.of());
		}
		return new DocumentoOcrService.ResultadoOcr(false, List.of(new DocumentoOcrService.LineaOcr(
				"LINEA SIMULADA " + solicitud.idDocumento(), new BigDecimal("10"), List.of())));
	}
}
