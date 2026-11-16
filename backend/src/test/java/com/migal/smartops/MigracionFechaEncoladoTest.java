package com.migal.smartops;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
 * Verifica la migración V16 (fecha_encolado de documento_digital) con PostgreSQL real. Los datos de prueba utilizan el
 * prefijo v16 y se eliminan en cada prueba.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class MigracionFechaEncoladoTest {

	private static final Instant SUBIDA = Instant.parse("2026-11-01T10:00:00Z");

	@Autowired
	private JdbcTemplate jdbc;

	@BeforeEach
	void limpiarAntesDeCada() {
		limpiar();
	}

	@AfterEach
	void limpiarDespuesDeCada() {
		limpiar();
	}

	@Test
	void fechaEncoladoEsObligatoria() {
		String nulable = jdbc.queryForObject("""
				SELECT is_nullable FROM information_schema.columns
				WHERE table_name = 'documento_digital' AND column_name = 'fecha_encolado'
				""", String.class);

		assertEquals("NO", nulable);
	}

	@Test
	void aceptaUnEncoladoIgualALaSubida() {
		Long id = insertarDocumento(crearUsuario("v16-doc-1"), SUBIDA);

		assertEquals(Timestamp.from(SUBIDA), fechaEncolado(id));
	}

	@Test
	void aceptaUnEncoladoPosteriorALaSubida() {
		Long id = insertarDocumento(crearUsuario("v16-doc-2"), SUBIDA.plusSeconds(90));

		assertEquals(Timestamp.from(SUBIDA.plusSeconds(90)), fechaEncolado(id));
	}

	@Test
	void rechazaUnEncoladoAnteriorALaSubida() {
		Long idUsuario = crearUsuario("v16-doc-3");

		assertThrows(DataIntegrityViolationException.class,
				() -> insertarDocumento(idUsuario, SUBIDA.minusSeconds(1)));
	}

	@Test
	void existeIndiceParcialDePendientesPorEncolado() {
		String definicion = jdbc.queryForObject(
				"SELECT indexdef FROM pg_indexes WHERE indexname = 'ix_documento_digital_pendientes_encolado'",
				String.class);

		assertTrue(definicion.contains("(fecha_encolado)"));
		assertTrue(definicion.contains("codigo_error IS NULL"));
		assertTrue(definicion.contains("fecha_eliminacion IS NULL"));
	}

	// Auxiliares

	private Long insertarDocumento(Long idUsuario, Instant encolado) {
		return jdbc.queryForObject("""
				INSERT INTO documento_digital (ruta_archivo, fecha_subida, fecha_encolado, nombre_archivo, tamano_bytes,
				                               estado_procesamiento, tipo_documento, id_usuario)
				VALUES (?, ?, ?, 'nota.jpg', 1024, 'pendiente', 'solicitud_cliente', ?)
				RETURNING id_documento
				""", Long.class, "documentos/v16-" + System.nanoTime() + ".jpg", Timestamp.from(SUBIDA),
				Timestamp.from(encolado), idUsuario);
	}

	private Timestamp fechaEncolado(Long id) {
		return jdbc.queryForObject("SELECT fecha_encolado FROM documento_digital WHERE id_documento = ?",
				Timestamp.class, id);
	}

	private Long crearUsuario(String username) {
		return jdbc.queryForObject("""
				INSERT INTO usuario (nombres, apellidos, username, password_hash, rol)
				VALUES ('Nombre', 'Apellido', ?, 'hash-ficticio', 'vendedor')
				RETURNING id_usuario
				""", Long.class, username);
	}

	private void limpiar() {
		jdbc.update("""
				DELETE FROM documento_digital WHERE id_usuario IN (
				    SELECT id_usuario FROM usuario WHERE username LIKE 'v16-%')
				""");
		jdbc.update("DELETE FROM usuario WHERE username LIKE 'v16-%'");
	}
}
