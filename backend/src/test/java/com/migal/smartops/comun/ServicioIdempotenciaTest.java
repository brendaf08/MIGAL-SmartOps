package com.migal.smartops.comun;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.migal.smartops.TestcontainersConfiguration;
import com.migal.smartops.comun.errores.CodigoError;
import com.migal.smartops.comun.errores.ErrorDeNegocio;
import com.migal.smartops.comun.idempotencia.ServicioIdempotencia;
import com.migal.smartops.comun.idempotencia.ServicioIdempotencia.Resultado;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/** Verifica la ejecución única por {@code Idempotency-Key} con PostgreSQL real. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ServicioIdempotenciaTest {

	private static final String OPERACION = "POST /inventario/traslados";

	/** Respuesta de prueba. */
	public record Respuesta(String id, int cantidad) {
	}

	@Autowired
	private ServicioIdempotencia servicio;

	@Autowired
	private JdbcTemplate jdbc;

	private long idUsuario;

	@BeforeEach
	void prepararUsuario() {
		limpiar();
		idUsuario = jdbc.queryForObject("""
				INSERT INTO usuario (nombres, apellidos, username, password_hash, rol)
				VALUES ('Prueba', 'Idempotencia', 'idem-prueba', '{hash}', 'almacenero')
				RETURNING id_usuario
				""", Long.class);
	}

	@AfterEach
	void limpiar() {
		jdbc.update("DELETE FROM idempotencia WHERE id_usuario IN (SELECT id_usuario FROM usuario WHERE username = 'idem-prueba')");
		jdbc.update("DELETE FROM usuario WHERE username = 'idem-prueba'");
	}

	private Resultado<Respuesta> ejecutar(String clave, Object solicitud, AtomicInteger ejecuciones) {
		return servicio.ejecutar(idUsuario, OPERACION, clave, solicitud, 201, Respuesta.class,
				() -> new Respuesta("T-" + ejecuciones.incrementAndGet(), 5));
	}

	@Test
	void ejecutaLaAccionUnaVezYGuardaLaRespuesta() {
		AtomicInteger ejecuciones = new AtomicInteger();

		Resultado<Respuesta> resultado = ejecutar("clave-a", Map.of("cantidad", 5), ejecuciones);

		assertEquals(201, resultado.estado());
		assertEquals("T-1", resultado.cuerpo().id());
		assertFalse(resultado.repetida());
		Integer guardadas = jdbc.queryForObject(
				"SELECT count(*) FROM idempotencia WHERE id_usuario = ? AND estado_http = 201", Integer.class, idUsuario);
		assertEquals(1, guardadas);
	}

	@Test
	void devuelveLaRespuestaGuardadaSinEjecutarDeNuevoLaAccion() {
		AtomicInteger ejecuciones = new AtomicInteger();
		ejecutar("clave-b", Map.of("cantidad", 5), ejecuciones);

		Resultado<Respuesta> repetida = ejecutar("clave-b", Map.of("cantidad", 5), ejecuciones);

		assertTrue(repetida.repetida());
		assertEquals(201, repetida.estado());
		assertEquals("T-1", repetida.cuerpo().id());
		assertEquals(1, ejecuciones.get());
	}

	@Test
	void rechazaLaMismaClaveConUnaSolicitudDistinta() {
		AtomicInteger ejecuciones = new AtomicInteger();
		ejecutar("clave-c", Map.of("cantidad", 5), ejecuciones);

		ErrorDeNegocio error = assertThrows(ErrorDeNegocio.class,
				() -> ejecutar("clave-c", Map.of("cantidad", 9), ejecuciones));

		assertEquals(CodigoError.IDEMPOTENCIA_CONFLICTO, error.codigo());
		assertEquals(1, ejecuciones.get());
	}

	@Test
	void liberaLaClaveCuandoLaAccionFalla() {
		AtomicInteger ejecuciones = new AtomicInteger();
		ErrorDeNegocio fallo = new ErrorDeNegocio(CodigoError.STOCK_INSUFICIENTE, "Sin stock.");

		ErrorDeNegocio error = assertThrows(ErrorDeNegocio.class, () -> servicio.ejecutar(idUsuario, OPERACION,
				"clave-d", Map.of("cantidad", 5), 201, Respuesta.class, () -> {
					throw fallo;
				}));
		assertEquals(CodigoError.STOCK_INSUFICIENTE, error.codigo());

		Resultado<Respuesta> reintento = ejecutar("clave-d", Map.of("cantidad", 5), ejecuciones);
		assertFalse(reintento.repetida());
		assertEquals(1, ejecuciones.get());
	}

	@Test
	void respondeConcurrenteMientrasLaOperacionOriginalNoTermina() {
		AtomicInteger ejecuciones = new AtomicInteger();
		servicio.ejecutar(idUsuario, OPERACION, "clave-e", Map.of("cantidad", 5), 201, Respuesta.class, () -> {
			// Dentro de la acción, la clave está reservada y sin respuesta: una segunda solicitud la ve en curso.
			ErrorDeNegocio error = assertThrows(ErrorDeNegocio.class,
					() -> ejecutar("clave-e", Map.of("cantidad", 5), ejecuciones));
			assertEquals(CodigoError.PETICION_CONCURRENTE, error.codigo());
			return new Respuesta("T-0", 5);
		});

		assertEquals(0, ejecuciones.get());
	}

	@Test
	void exigeUnaClaveNoVaciaDeHastaCienCaracteres() {
		AtomicInteger ejecuciones = new AtomicInteger();
		for (String clave : new String[] {null, "", "   ", "x".repeat(101)}) {
			ErrorDeNegocio error = assertThrows(ErrorDeNegocio.class,
					() -> ejecutar(clave, Map.of("cantidad", 5), ejecuciones));
			assertEquals(CodigoError.SOLICITUD_MAL_FORMADA, error.codigo());
		}
		assertEquals(0, ejecuciones.get());
	}

	@Test
	void usaClavesIndependientesPorOperacion() {
		AtomicInteger ejecuciones = new AtomicInteger();
		ejecutar("clave-f", Map.of("cantidad", 5), ejecuciones);

		Resultado<Respuesta> otra = servicio.ejecutar(idUsuario, "POST /inventario/movimientos", "clave-f",
				Map.of("cantidad", 5), 201, Respuesta.class, () -> new Respuesta("M-1", 5));

		assertFalse(otra.repetida());
		assertEquals("M-1", otra.cuerpo().id());
	}

	@Test
	void reutilizaUnaClaveVencida() {
		AtomicInteger ejecuciones = new AtomicInteger();
		ejecutar("clave-g", Map.of("cantidad", 5), ejecuciones);
		jdbc.update("UPDATE idempotencia SET fecha_creacion = now() - interval '2 days', "
				+ "fecha_expiracion = now() - interval '1 day' WHERE clave = 'clave-g'");

		Resultado<Respuesta> nueva = ejecutar("clave-g", Map.of("cantidad", 5), ejecuciones);

		assertFalse(nueva.repetida());
		assertEquals("T-2", nueva.cuerpo().id());
	}

	@Test
	void eliminaLosRegistrosVencidos() {
		AtomicInteger ejecuciones = new AtomicInteger();
		ejecutar("clave-h", Map.of("cantidad", 5), ejecuciones);
		jdbc.update("UPDATE idempotencia SET fecha_creacion = now() - interval '2 days', "
				+ "fecha_expiracion = now() - interval '1 day' WHERE clave = 'clave-h'");

		assertTrue(servicio.eliminarVencidos() >= 1);
		assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM idempotencia WHERE clave = 'clave-h'", Integer.class));
	}
}
