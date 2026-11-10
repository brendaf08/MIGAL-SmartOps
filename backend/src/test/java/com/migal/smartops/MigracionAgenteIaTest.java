package com.migal.smartops;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Verifica la migración V14 (conversacion_agente y mensaje_agente) con PostgreSQL real.
 * Los datos de prueba utilizan el prefijo v14 en los nombres de usuario y se eliminan en cada prueba,
 * en orden inverso a las claves foráneas. La eliminación de una conversación borra sus mensajes en cascada.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class MigracionAgenteIaTest {

	private static final String VACIO = "[]";

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
	void existenLasTablasDelAgente() {
		Integer cantidad = jdbc.queryForObject("""
				SELECT count(*) FROM information_schema.tables
				WHERE table_schema = 'public' AND table_name IN ('conversacion_agente', 'mensaje_agente')
				""", Integer.class);
		assertEquals(2, cantidad);
	}

	// Conversación

	@Test
	void aceptaConversacionConMensajesDeUsuarioYAgente() {
		Long idConversacion = crearConversacion(crearUsuario("v14-conv-1"), "Disponibilidad de cemento");

		insertarMensaje(idConversacion, "usuario", "Disponibilidad de cemento Sol 42.5 kg", false, VACIO, VACIO);
		insertarMensaje(idConversacion, "agente", "Hay 80 bolsas libres.", false, VACIO, VACIO);

		assertEquals(2, contarMensajes(idConversacion));
	}

	@Test
	void rechazaConversacionConTituloFormadoSoloPorEspacios() {
		Long idUsuario = crearUsuario("v14-conv-2");

		assertThrows(DataIntegrityViolationException.class, () -> crearConversacion(idUsuario, "   "));
	}

	@Test
	void rechazaConversacionSinTitulo() {
		Long idUsuario = crearUsuario("v14-conv-3");

		assertThrows(DataIntegrityViolationException.class, () -> crearConversacion(idUsuario, null));
	}

	@Test
	void rechazaConversacionDeUsuarioInexistente() {
		assertThrows(DataIntegrityViolationException.class, () -> crearConversacion(999999L, "Consulta"));
	}

	@Test
	void aplicaValoresInicialesDeFechasALaConversacion() {
		Long idConversacion = jdbc.queryForObject("""
				INSERT INTO conversacion_agente (id_usuario, titulo)
				VALUES (?, 'Consulta inicial')
				RETURNING id_conversacion
				""", Long.class, crearUsuario("v14-conv-4"));

		Boolean ordenadas = jdbc.queryForObject("""
				SELECT fecha_ultimo_mensaje >= fecha_creacion FROM conversacion_agente WHERE id_conversacion = ?
				""", Boolean.class, idConversacion);
		assertTrue(ordenadas);
	}

	@Test
	void rechazaUltimoMensajeAnteriorALaCreacion() {
		Long idUsuario = crearUsuario("v14-conv-5");

		assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("""
				INSERT INTO conversacion_agente (id_usuario, titulo, fecha_creacion, fecha_ultimo_mensaje)
				VALUES (?, 'Consulta', '2026-11-08 10:00:00+00', '2026-11-07 10:00:00+00')
				""", idUsuario));
	}

	// Mensaje: rol, texto, respuesta incierta y consultas

	@Test
	void aplicaValoresInicialesAlMensaje() {
		Long idConversacion = crearConversacion(crearUsuario("v14-msg-1"), "Consulta");

		Long idMensaje = jdbc.queryForObject("""
				INSERT INTO mensaje_agente (id_conversacion, rol, texto)
				VALUES (?, 'usuario', 'Consulta sin valores explícitos')
				RETURNING id_mensaje
				""", Long.class, idConversacion);

		Boolean incierta = jdbc.queryForObject(
				"SELECT incierta FROM mensaje_agente WHERE id_mensaje = ?", Boolean.class, idMensaje);
		Boolean herramientasVacias = jdbc.queryForObject(
				"SELECT herramientas = '[]'::jsonb FROM mensaje_agente WHERE id_mensaje = ?", Boolean.class, idMensaje);
		Boolean referenciasVacias = jdbc.queryForObject(
				"SELECT referencias = '[]'::jsonb FROM mensaje_agente WHERE id_mensaje = ?", Boolean.class, idMensaje);
		assertFalse(incierta);
		assertTrue(herramientasVacias);
		assertTrue(referenciasVacias);
	}

	@Test
	void rechazaRolFueraDelDominio() {
		Long idConversacion = crearConversacion(crearUsuario("v14-msg-2"), "Consulta");

		assertThrows(DataIntegrityViolationException.class,
				() -> insertarMensaje(idConversacion, "sistema", "Texto", false, VACIO, VACIO));
	}

	@Test
	void rechazaTextoVacio() {
		Long idConversacion = crearConversacion(crearUsuario("v14-msg-3"), "Consulta");

		assertThrows(DataIntegrityViolationException.class,
				() -> insertarMensaje(idConversacion, "usuario", "   ", false, VACIO, VACIO));
	}

	@Test
	void rechazaTextoNulo() {
		Long idConversacion = crearConversacion(crearUsuario("v14-msg-4"), "Consulta");

		assertThrows(DataIntegrityViolationException.class,
				() -> insertarMensaje(idConversacion, "agente", null, false, VACIO, VACIO));
	}

	@Test
	void aceptaRespuestaIncierta() {
		Long idConversacion = crearConversacion(crearUsuario("v14-msg-5"), "Consulta");

		insertarMensaje(idConversacion, "agente", "No dispongo de información suficiente.", true, VACIO, VACIO);

		assertEquals(1, contarMensajes(idConversacion));
	}

	@Test
	void rechazaMensajeDelUsuarioMarcadoComoIncierto() {
		Long idConversacion = crearConversacion(crearUsuario("v14-msg-6"), "Consulta");

		assertThrows(DataIntegrityViolationException.class,
				() -> insertarMensaje(idConversacion, "usuario", "Consulta", true, VACIO, VACIO));
	}

	@Test
	void rechazaHerramientasQueNoSonArreglo() {
		Long idConversacion = crearConversacion(crearUsuario("v14-msg-7"), "Consulta");

		assertThrows(DataIntegrityViolationException.class,
				() -> insertarMensaje(idConversacion, "agente", "Respuesta", false, "{}", VACIO));
	}

	@Test
	void rechazaReferenciasQueNoSonArreglo() {
		Long idConversacion = crearConversacion(crearUsuario("v14-msg-8"), "Consulta");

		assertThrows(DataIntegrityViolationException.class,
				() -> insertarMensaje(idConversacion, "agente", "Respuesta", false, VACIO, "\"CEM-001\""));
	}

	@Test
	void rechazaHerramientasEnMensajeDelUsuario() {
		Long idConversacion = crearConversacion(crearUsuario("v14-msg-9"), "Consulta");

		assertThrows(DataIntegrityViolationException.class, () -> insertarMensaje(idConversacion, "usuario",
				"Consulta", false, "[{\"nombre\": \"consultarDisponibilidad\"}]", VACIO));
	}

	@Test
	void rechazaReferenciasEnMensajeDelUsuario() {
		Long idConversacion = crearConversacion(crearUsuario("v14-msg-10"), "Consulta");

		assertThrows(DataIntegrityViolationException.class, () -> insertarMensaje(idConversacion, "usuario",
				"Consulta", false, VACIO, "[{\"tipo\": \"producto\", \"id\": \"42\"}]"));
	}

	@Test
	void aceptaHerramientasYReferenciasDelAgenteComoArreglos() {
		Long idConversacion = crearConversacion(crearUsuario("v14-msg-11"), "Consulta");

		Long idMensaje = insertarMensaje(idConversacion, "agente", "Hay 80 bolsas libres.", false,
				"[{\"nombre\": \"consultarDisponibilidad\", \"parametros\": {\"q\": \"cemento sol\"}, "
						+ "\"resumen\": \"Cemento Sol con 80 bolsas libres.\", \"codigo\": null}]",
				"[{\"tipo\": \"producto\", \"id\": \"42\", \"codigo\": \"CEM-001\"}]");

		Integer herramientas = jdbc.queryForObject(
				"SELECT jsonb_array_length(herramientas) FROM mensaje_agente WHERE id_mensaje = ?", Integer.class, idMensaje);
		Integer referencias = jdbc.queryForObject(
				"SELECT jsonb_array_length(referencias) FROM mensaje_agente WHERE id_mensaje = ?", Integer.class, idMensaje);
		assertEquals(1, herramientas);
		assertEquals(1, referencias);
	}

	@Test
	void rechazaMensajeDeConversacionInexistente() {
		assertThrows(DataIntegrityViolationException.class,
				() -> insertarMensaje(999999L, "usuario", "Consulta", false, VACIO, VACIO));
	}

	// Eliminación: la conversación borra sus mensajes; el usuario no puede eliminarse con conversaciones

	@Test
	void eliminarConversacionBorraSusMensajes() {
		Long idConversacion = crearConversacion(crearUsuario("v14-del-1"), "Consulta a eliminar");
		insertarMensaje(idConversacion, "usuario", "Consulta", false, VACIO, VACIO);
		insertarMensaje(idConversacion, "agente", "Respuesta", false, VACIO, VACIO);

		jdbc.update("DELETE FROM conversacion_agente WHERE id_conversacion = ?", idConversacion);

		assertEquals(0, contarMensajes(idConversacion));
	}

	@Test
	void rechazaEliminarUsuarioConConversaciones() {
		Long idUsuario = crearUsuario("v14-del-2");
		crearConversacion(idUsuario, "Consulta");

		assertThrows(DataAccessException.class,
				() -> jdbc.update("DELETE FROM usuario WHERE id_usuario = ?", idUsuario));
	}

	// Índices y seguridad a nivel de fila

	@Test
	void existeIndiceDeConversacionesPorUsuarioYUltimoMensaje() {
		String definicion = jdbc.queryForObject("""
				SELECT indexdef FROM pg_indexes
				WHERE schemaname = 'public' AND indexname = 'ix_conversacion_agente_usuario_fecha'
				""", String.class);

		assertTrue(definicion.contains("fecha_ultimo_mensaje DESC"));
	}

	@Test
	void existeIndiceDeMensajesPorConversacionYFecha() {
		Integer cantidad = jdbc.queryForObject("""
				SELECT count(*) FROM pg_indexes
				WHERE schemaname = 'public' AND tablename = 'mensaje_agente'
				  AND indexname = 'ix_mensaje_agente_conversacion_fecha'
				""", Integer.class);

		assertEquals(1, cantidad);
	}

	@Test
	void habilitaRlsEnAmbasTablasDelAgente() {
		Integer cantidad = jdbc.queryForObject("""
				SELECT count(*) FROM pg_class
				WHERE relname IN ('conversacion_agente', 'mensaje_agente') AND relrowsecurity
				""", Integer.class);

		assertEquals(2, cantidad);
	}

	// Auxiliares

	private Long crearUsuario(String username) {
		return jdbc.queryForObject("""
				INSERT INTO usuario (nombres, apellidos, username, password_hash, rol)
				VALUES ('Nombre', 'Apellido', ?, 'hash-ficticio', 'vendedor')
				RETURNING id_usuario
				""", Long.class, username);
	}

	private Long crearConversacion(Long idUsuario, String titulo) {
		return jdbc.queryForObject("""
				INSERT INTO conversacion_agente (id_usuario, titulo)
				VALUES (?, ?)
				RETURNING id_conversacion
				""", Long.class, idUsuario, titulo);
	}

	private Long insertarMensaje(Long idConversacion, String rol, String texto, boolean incierta,
			String herramientas, String referencias) {
		return jdbc.queryForObject("""
				INSERT INTO mensaje_agente (id_conversacion, rol, texto, incierta, herramientas, referencias)
				VALUES (?, ?, ?, ?, CAST(? AS jsonb), CAST(? AS jsonb))
				RETURNING id_mensaje
				""", Long.class, idConversacion, rol, texto, incierta, herramientas, referencias);
	}

	private int contarMensajes(Long idConversacion) {
		return jdbc.queryForObject("SELECT count(*) FROM mensaje_agente WHERE id_conversacion = ?",
				Integer.class, idConversacion);
	}

	private void limpiar() {
		jdbc.update("""
				DELETE FROM conversacion_agente WHERE id_usuario IN (
				    SELECT id_usuario FROM usuario WHERE username LIKE 'v14-%')
				""");
		jdbc.update("DELETE FROM usuario WHERE username LIKE 'v14-%'");
	}

}
