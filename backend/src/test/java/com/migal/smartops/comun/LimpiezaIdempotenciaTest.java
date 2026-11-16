package com.migal.smartops.comun;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.migal.smartops.comun.idempotencia.LimpiezaIdempotencia;
import com.migal.smartops.comun.idempotencia.ServicioIdempotencia;
import com.migal.smartops.documentosocr.config.PropiedadesOcr;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

/** Verifica la limpieza programada de idempotencia (ADR-010) y la validación de la configuración del servicio de OCR. */
class LimpiezaIdempotenciaTest {

	@Test
	void eliminaLosRegistrosVencidosUnaVezAlDiaEnLaZonaDeLima() throws NoSuchMethodException {
		ServicioIdempotencia idempotencia = mock(ServicioIdempotencia.class);

		new LimpiezaIdempotencia(idempotencia).limpiar();

		verify(idempotencia).eliminarVencidos();
		Method limpiar = LimpiezaIdempotencia.class.getMethod("limpiar");
		Scheduled programacion = limpiar.getAnnotation(Scheduled.class);
		assertEquals("${migal.idempotencia.limpieza-cron:0 0 3 * * *}", programacion.cron());
		assertEquals("America/Lima", programacion.zone());
	}

	@Test
	void elProveedorHttpExigeLaDireccionDelServicio() {
		assertThrows(IllegalStateException.class,
				() -> new PropiedadesOcr(PropiedadesOcr.PROVEEDOR_HTTP, null, null));
	}
}
