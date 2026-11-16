package com.migal.smartops.comun.idempotencia;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Elimina una vez al día los registros de idempotencia vencidos (ADR-010). Un registro vence 24 horas después de su
 * creación; a partir de ese momento una solicitud con la misma clave se ejecuta como nueva.
 */
@Component
public class LimpiezaIdempotencia {

	private static final Logger REGISTRO = LoggerFactory.getLogger(LimpiezaIdempotencia.class);

	private final ServicioIdempotencia idempotencia;

	public LimpiezaIdempotencia(ServicioIdempotencia idempotencia) {
		this.idempotencia = idempotencia;
	}

	@Scheduled(cron = "${migal.idempotencia.limpieza-cron:0 0 3 * * *}", zone = "America/Lima")
	public void limpiar() {
		int eliminados = idempotencia.eliminarVencidos();
		REGISTRO.info("Limpieza de idempotencia: {} registro(s) vencido(s) eliminado(s)", eliminados);
	}
}
