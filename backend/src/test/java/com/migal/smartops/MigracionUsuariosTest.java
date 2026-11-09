package com.migal.smartops;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Verifica la migración V1 (tabla usuario) con PostgreSQL real.
 * Cada prueba elimina las filas que inserta, de modo que no depende del orden de ejecución.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class MigracionUsuariosTest {

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
	void existeLaTablaUsuario() {
		Integer cantidad = jdbc.queryForObject(
				"SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'usuario'",
				Integer.class);
		assertEquals(1, cantidad);
	}

	@Test
	void insertaUsuarioValidoConEstadoActivoPorDefecto() {
		insertarUsuario("jperez", "vendedor");

		Boolean activo = jdbc.queryForObject(
				"SELECT estado_activo FROM usuario WHERE username = 'jperez'", Boolean.class);
		assertEquals(Boolean.TRUE, activo);
	}

	@Test
	void rechazaUsernameDuplicado() {
		insertarUsuario("mlopez", "almacenero");

		assertThrows(DataIntegrityViolationException.class,
				() -> insertarUsuario("mlopez", "reponedor"));
	}

	@Test
	void rechazaRolFueraDelDominio() {
		assertThrows(DataIntegrityViolationException.class,
				() -> insertarUsuario("clara", "gerente"));
	}

	@Test
	void rechazaPasswordHashNulo() {
		assertThrows(DataIntegrityViolationException.class,
				() -> jdbc.update("""
						INSERT INTO usuario (nombres, apellidos, username, password_hash, rol)
						VALUES ('Ana', 'Ruiz', 'aruiz', NULL, 'administrador')
						"""));
	}

	private void insertarUsuario(String username, String rol) {
		jdbc.update("""
				INSERT INTO usuario (nombres, apellidos, username, password_hash, rol)
				VALUES (?, ?, ?, ?, ?)
				""", "Nombre", "Apellido", username, "hash-ficticio", rol);
	}

	private void limpiar() {
		jdbc.update("DELETE FROM usuario");
	}

}
