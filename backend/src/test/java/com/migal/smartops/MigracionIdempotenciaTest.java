package com.migal.smartops;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.Timestamp;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Verifica la migración V15 (tabla idempotencia) con PostgreSQL real. Cada prueba elimina las filas que
 * inserta, en orden inverso a las claves foráneas.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class MigracionIdempotenciaTest {

	private static final String HUELLA = "a".repeat(64);

	@Autowired
	private JdbcTemplate jdbc;

	private Long idUsuario;

	@BeforeEach
	void prepararUsuario() {
		limpiar();
		idUsuario = jdbc.queryForObject("""
				INSERT INTO usuario (nombres, apellidos, username, password_hash, rol)
				VALUES ('Prueba', 'Idempotencia', 'v15-prueba', '{hash}', 'vendedor')
				RETURNING id_usuario
				""", Long.class);
	}

	@AfterEach
	void limpiar() {
		jdbc.update("DELETE FROM idempotencia WHERE id_usuario IN (SELECT id_usuario FROM usuario WHERE username = 'v15-prueba')");
		jdbc.update("DELETE FROM usuario WHERE username = 'v15-prueba'");
	}

	private int insertar(String clave, String operacion, String huella, Integer estado, String respuesta) {
		return jdbc.update("""
				INSERT INTO idempotencia (clave, id_usuario, operacion, huella, estado_http, respuesta, fecha_expiracion)
				VALUES (?, ?, ?, ?, ?, CAST(? AS jsonb), ?)
				""", clave, idUsuario, operacion, huella, estado, respuesta,
				Timestamp.from(Instant.now().plusSeconds(86400)));
	}

	@Test
	void existeLaTablaIdempotencia() {
		Integer cantidad = jdbc.queryForObject(
				"SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'idempotencia'",
				Integer.class);
		assertEquals(1, cantidad);
	}

	@Test
	void guardaUnaOperacionEnCursoSinRespuesta() {
		assertEquals(1, insertar("clave-1", "POST /inventario/traslados", HUELLA, null, null));
	}

	@Test
	void guardaLaRespuestaTerminada() {
		assertEquals(1, insertar("clave-2", "POST /inventario/traslados", HUELLA, 201, "{\"id\":\"1\"}"));
	}

	@Test
	void rechazaLaMismaClaveParaElMismoUsuarioYOperacion() {
		insertar("clave-3", "POST /inventario/traslados", HUELLA, null, null);

		assertThrows(DataIntegrityViolationException.class,
				() -> insertar("clave-3", "POST /inventario/traslados", HUELLA, null, null));
	}

	@Test
	void admiteLaMismaClaveEnOtraOperacion() {
		insertar("clave-4", "POST /inventario/traslados", HUELLA, null, null);

		assertEquals(1, insertar("clave-4", "POST /inventario/movimientos", HUELLA, null, null));
	}

	@Test
	void rechazaUnaClaveVacia() {
		assertThrows(DataIntegrityViolationException.class,
				() -> insertar("   ", "POST /inventario/traslados", HUELLA, null, null));
	}

	@Test
	void rechazaUnaHuellaQueNoEsSha256EnHexadecimal() {
		assertThrows(DataIntegrityViolationException.class,
				() -> insertar("clave-5", "POST /inventario/traslados", "xyz", null, null));
		assertThrows(DataIntegrityViolationException.class,
				() -> insertar("clave-6", "POST /inventario/traslados", "A".repeat(64), null, null));
	}

	@Test
	void exigeEstadoYRespuestaJuntosONinguno() {
		assertThrows(DataIntegrityViolationException.class,
				() -> insertar("clave-7", "POST /inventario/traslados", HUELLA, 201, null));
		assertThrows(DataIntegrityViolationException.class,
				() -> insertar("clave-8", "POST /inventario/traslados", HUELLA, null, "{}"));
	}

	@Test
	void rechazaUnEstadoHttpFueraDeRango() {
		assertThrows(DataIntegrityViolationException.class,
				() -> insertar("clave-9", "POST /inventario/traslados", HUELLA, 99, "{}"));
		assertThrows(DataIntegrityViolationException.class,
				() -> insertar("clave-10", "POST /inventario/traslados", HUELLA, 600, "{}"));
	}

	@Test
	void rechazaUnaExpiracionAnteriorALaCreacion() {
		assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("""
				INSERT INTO idempotencia (clave, id_usuario, operacion, huella, fecha_expiracion)
				VALUES ('clave-11', ?, 'POST /x', ?, now() - interval '1 hour')
				""", idUsuario, HUELLA));
	}

	@Test
	void rechazaUnUsuarioInexistente() {
		assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("""
				INSERT INTO idempotencia (clave, id_usuario, operacion, huella, fecha_expiracion)
				VALUES ('clave-12', 999999999, 'POST /x', ?, now() + interval '1 day')
				""", HUELLA));
	}
}
