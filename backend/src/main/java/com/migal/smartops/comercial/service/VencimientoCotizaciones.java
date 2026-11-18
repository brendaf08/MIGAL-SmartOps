package com.migal.smartops.comercial.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Vence las cotizaciones fuera de las consultas: al iniciar la aplicación (cubre el tiempo en que estuvo detenida) y cada
 * día a las 00:00:05 en Lima, cuando cambia la fecha que decide el vencimiento. Así GET /cotizaciones no abre una
 * transacción de escritura en cada lectura.
 */
@Component
public class VencimientoCotizaciones {

	private static final Logger REGISTRO = LoggerFactory.getLogger(VencimientoCotizaciones.class);

	private final CotizacionService cotizaciones;

	public VencimientoCotizaciones(CotizacionService cotizaciones) {
		this.cotizaciones = cotizaciones;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void alIniciar() {
		vencer();
	}

	@Scheduled(cron = "5 0 0 * * *", zone = "America/Lima")
	public void cadaMedianoche() {
		vencer();
	}

	private void vencer() {
		int vencidas = cotizaciones.vencerVencidasProgramado();
		if (vencidas > 0) {
			REGISTRO.info("Cotizaciones marcadas como vencidas: {}", vencidas);
		}
	}
}
