package com.migal.smartops.seguridad.service;

import com.migal.smartops.comun.errores.CodigoError;
import com.migal.smartops.comun.errores.ErrorDeNegocio;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Limita los intentos fallidos de inicio de sesión (CU-01) contra la fuerza bruta: tras {@value #MAXIMO_FALLOS} fallos
 * de un mismo usuario desde un mismo origen en {@link #VENTANA}, ese par queda bloqueado durante {@link #VENTANA} y la
 * solicitud responde DEMASIADOS_INTENTOS (429). Un inicio correcto reinicia la cuenta. Se combina usuario y origen para
 * que un tercero no pueda bloquear una cuenta desde otra máquina. El estado vive en memoria: el backend es una sola
 * instancia (ADR-002) y un reinicio solo concede intentos nuevos.
 */
@Component
public class LimitadorIntentosSesion {

	static final int MAXIMO_FALLOS = 5;
	static final Duration VENTANA = Duration.ofMinutes(15);

	private final Clock reloj;
	private final Map<String, Deque<Instant>> fallos = new ConcurrentHashMap<>();

	public LimitadorIntentosSesion(Clock reloj) {
		this.reloj = reloj;
	}

	/** Rechaza el intento si el par usuario y origen superó el máximo de fallos recientes. */
	public void verificar(String username, String origen) {
		Deque<Instant> recientes = recientes(clave(username, origen));
		synchronized (recientes) {
			if (recientes.size() >= MAXIMO_FALLOS) {
				throw new ErrorDeNegocio(CodigoError.DEMASIADOS_INTENTOS,
						"Se superó el número de intentos. Esperar 15 minutos antes de volver a intentar.");
			}
		}
	}

	public void registrarFallo(String username, String origen) {
		Deque<Instant> recientes = recientes(clave(username, origen));
		synchronized (recientes) {
			recientes.addLast(reloj.instant());
		}
	}

	public void registrarExito(String username, String origen) {
		fallos.remove(clave(username, origen));
	}

	/** Descarta todos los intentos registrados. Lo usan las pruebas que comparten el contexto de la aplicación. */
	public void limpiar() {
		fallos.clear();
	}

	/** Fallos dentro de la ventana; descarta los anteriores. */
	private Deque<Instant> recientes(String clave) {
		Deque<Instant> recientes = fallos.computeIfAbsent(clave, c -> new ArrayDeque<>());
		Instant limite = reloj.instant().minus(VENTANA);
		synchronized (recientes) {
			while (!recientes.isEmpty() && recientes.peekFirst().isBefore(limite)) {
				recientes.pollFirst();
			}
		}
		return recientes;
	}

	private static String clave(String username, String origen) {
		return (username == null ? "" : username.trim().toLowerCase(Locale.ROOT)) + "|" + origen;
	}
}
