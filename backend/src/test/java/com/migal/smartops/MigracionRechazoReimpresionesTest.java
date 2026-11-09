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
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Verifica la migración V12 (rechazo de cotización, guía anulada, fecha de entrega y reimpresiones) con PostgreSQL real.
 * Cada prueba elimina las filas que crea. La tabla reimpresion_guia tiene un disparador de solo anexión, por lo que
 * la limpieza lo desactiva de forma temporal, como en las pruebas de Kardex.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class MigracionRechazoReimpresionesTest {

	private static final String CLIENTE = "Cliente V12 ficticio";

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

	// Cotización rechazada

	@Test
	void aceptaCotizacionRechazadaPorClienteConMotivo() {
		Long idUsuario = crearUsuario("v12-rechazo-1");
		Long idCotizacion = insertarCotizacion("rechazada", "cliente", idUsuario, Instant.now(), "El cliente solicita otro plazo.");

		assertTrue(idCotizacion > 0);
	}

	@Test
	void aceptaCotizacionRechazadaPorMigal() {
		Long idUsuario = crearUsuario("v12-rechazo-2");
		Long idCotizacion = insertarCotizacion("rechazada", "migal", idUsuario, Instant.now(), "Stock no viable.");

		assertTrue(idCotizacion > 0);
	}

	@Test
	void aceptaCotizacionEmitidaSinDatosDeRechazo() {
		Long idCotizacion = insertarCotizacion("emitida", null, null, null, null);

		assertTrue(idCotizacion > 0);
	}

	@Test
	void rechazaCotizacionRechazadaSinMotivo() {
		Long idUsuario = crearUsuario("v12-rechazo-3");

		assertThrows(DataIntegrityViolationException.class,
				() -> insertarCotizacion("rechazada", "cliente", idUsuario, Instant.now(), null));
	}

	@Test
	void rechazaCotizacionRechazadaConMotivoFormadoSoloPorEspacios() {
		Long idUsuario = crearUsuario("v12-rechazo-4");

		assertThrows(DataIntegrityViolationException.class,
				() -> insertarCotizacion("rechazada", "cliente", idUsuario, Instant.now(), "   "));
	}

	@Test
	void rechazaCotizacionRechazadaSinOrigen() {
		Long idUsuario = crearUsuario("v12-rechazo-5");

		assertThrows(DataIntegrityViolationException.class,
				() -> insertarCotizacion("rechazada", null, idUsuario, Instant.now(), "Motivo informado."));
	}

	@Test
	void rechazaCotizacionRechazadaSinFechaDeRechazo() {
		Long idUsuario = crearUsuario("v12-rechazo-6");

		assertThrows(DataIntegrityViolationException.class,
				() -> insertarCotizacion("rechazada", "cliente", idUsuario, null, "Motivo informado."));
	}

	@Test
	void rechazaCotizacionEmitidaConDatosDeRechazo() {
		Long idUsuario = crearUsuario("v12-rechazo-7");

		assertThrows(DataIntegrityViolationException.class,
				() -> insertarCotizacion("emitida", "cliente", idUsuario, Instant.now(), "Motivo informado."));
	}

	@Test
	void rechazaOrigenDeRechazoFueraDelDominio() {
		Long idUsuario = crearUsuario("v12-rechazo-8");

		assertThrows(DataIntegrityViolationException.class,
				() -> insertarCotizacion("rechazada", "vendedor", idUsuario, Instant.now(), "Motivo informado."));
	}

	@Test
	void rechazaUsuarioDeRechazoInexistente() {
		assertThrows(DataIntegrityViolationException.class,
				() -> insertarCotizacion("rechazada", "cliente", 999999L, Instant.now(), "Motivo informado."));
	}

	// Guía de despacho: estado anulada, fecha de entrega y salida

	@Test
	void aceptaGuiaAnuladaDesdeEmitidaSinDatosDeSalida() {
		Long idVenta = crearVenta("anulada1");

		Long idGuia = insertarGuia(idVenta, "anulada", null, null, null);

		assertTrue(idGuia > 0);
	}

	@Test
	void rechazaGuiaAnuladaConUnicoDatoDeSalida() {
		Long idVenta = crearVenta("anulada2");

		assertThrows(DataIntegrityViolationException.class,
				() -> insertarGuia(idVenta, "anulada", Instant.now(), null, null));
	}

	@Test
	void rechazaEstadoDeGuiaFueraDelDominio() {
		Long idVenta = crearVenta("estado1");

		assertThrows(DataIntegrityViolationException.class,
				() -> insertarGuia(idVenta, "cancelada", null, null, null));
	}

	@Test
	void aceptaGuiaEntregadaConFechaDeEntrega() {
		Long idUsuario = crearUsuario("v12-guia-2");
		Long idVenta = crearVenta("entrega1");

		Long idGuia = insertarGuia(idVenta, "entregada", Instant.now(), idUsuario, Instant.now());

		assertTrue(idGuia > 0);
	}

	@Test
	void rechazaGuiaEntregadaSinFechaDeEntrega() {
		Long idUsuario = crearUsuario("v12-guia-3");
		Long idVenta = crearVenta("entrega2");

		assertThrows(DataIntegrityViolationException.class,
				() -> insertarGuia(idVenta, "entregada", Instant.now(), idUsuario, null));
	}

	@Test
	void rechazaGuiaEmitidaConFechaDeEntrega() {
		Long idVenta = crearVenta("entrega3");

		assertThrows(DataIntegrityViolationException.class,
				() -> insertarGuia(idVenta, "emitida", null, null, Instant.now()));
	}

	@Test
	void rechazaSalidaConfirmadaSinUsuarioResponsableTrasLaAmpliacion() {
		Long idVenta = crearVenta("salida1");

		assertThrows(DataIntegrityViolationException.class,
				() -> insertarGuia(idVenta, "salida_confirmada", Instant.now(), null, null));
	}

	// Reimpresiones de la guía

	@Test
	void registraReimpresionConMotivo() {
		Long idUsuario = crearUsuario("v12-reimpresion-1");
		Long idGuia = insertarGuia(crearVenta("reimpresion1"), "emitida", null, null, null);

		Long idReimpresion = registrarReimpresion(idGuia, idUsuario, "Documento dañado durante el traslado");

		assertTrue(idReimpresion > 0);
	}

	@Test
	void rechazaReimpresionSinMotivo() {
		Long idUsuario = crearUsuario("v12-reimpresion-2");
		Long idGuia = insertarGuia(crearVenta("reimpresion2"), "emitida", null, null, null);

		assertThrows(DataIntegrityViolationException.class,
				() -> registrarReimpresion(idGuia, idUsuario, null));
	}

	@Test
	void rechazaReimpresionConMotivoFormadoSoloPorEspacios() {
		Long idUsuario = crearUsuario("v12-reimpresion-3");
		Long idGuia = insertarGuia(crearVenta("reimpresion3"), "emitida", null, null, null);

		assertThrows(DataIntegrityViolationException.class,
				() -> registrarReimpresion(idGuia, idUsuario, "  "));
	}

	@Test
	void rechazaReimpresionParaGuiaInexistente() {
		Long idUsuario = crearUsuario("v12-reimpresion-4");

		assertThrows(DataIntegrityViolationException.class,
				() -> registrarReimpresion(999999L, idUsuario, "Motivo informado."));
	}

	@Test
	void rechazaActualizacionDeReimpresion() {
		Long idUsuario = crearUsuario("v12-reimpresion-5");
		Long idGuia = insertarGuia(crearVenta("reimpresion5"), "emitida", null, null, null);
		Long idReimpresion = registrarReimpresion(idGuia, idUsuario, "Motivo original");

		assertThrows(DataAccessException.class,
				() -> jdbc.update("UPDATE reimpresion_guia SET motivo = 'Motivo alterado' WHERE id_reimpresion = ?",
						idReimpresion));
	}

	@Test
	void rechazaEliminacionDeReimpresion() {
		Long idUsuario = crearUsuario("v12-reimpresion-6");
		Long idGuia = insertarGuia(crearVenta("reimpresion6"), "emitida", null, null, null);
		Long idReimpresion = registrarReimpresion(idGuia, idUsuario, "Motivo original");

		assertThrows(DataAccessException.class,
				() -> jdbc.update("DELETE FROM reimpresion_guia WHERE id_reimpresion = ?", idReimpresion));
	}

	@Test
	void habilitaRlsEnReimpresionGuia() {
		Boolean rls = jdbc.queryForObject(
				"SELECT relrowsecurity FROM pg_class WHERE relname = 'reimpresion_guia'", Boolean.class);

		assertTrue(rls);
	}

	@Test
	void existeIndiceEnLasClavesForaneasDeReimpresion() {
		Integer cantidad = jdbc.queryForObject("""
				SELECT count(*) FROM pg_indexes
				WHERE schemaname = 'public' AND tablename = 'reimpresion_guia'
				  AND indexname IN ('ix_reimpresion_guia_id_guia', 'ix_reimpresion_guia_id_usuario')
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

	private Long crearVenta(String sufijo) {
		Long idUsuario = crearUsuario("v12-vendedor-" + sufijo);
		Long idCliente = jdbc.queryForObject("""
				INSERT INTO cliente (nombre_razon_social)
				VALUES (?)
				RETURNING id_cliente
				""", Long.class, CLIENTE + " " + sufijo);
		Long idCotizacion = jdbc.queryForObject("""
				INSERT INTO cotizacion (id_cliente, id_usuario_responsable, fecha_vencimiento, descuento, estado)
				VALUES (?, ?, CURRENT_DATE + 30, 0, 'validada')
				RETURNING id_cotizacion
				""", Long.class, idCliente, idUsuario);
		return jdbc.queryForObject("""
				INSERT INTO venta (id_cotizacion, id_cliente, id_usuario_responsable, descuento, estado)
				VALUES (?, ?, ?, 0, 'lista_para_despacho')
				RETURNING id_venta
				""", Long.class, idCotizacion, idCliente, idUsuario);
	}

	private Long insertarCotizacion(String estado, String origen, Long idUsuarioRechazo, Instant fechaRechazo,
			String motivo) {
		Long idUsuario = crearUsuario("v12-responsable-" + estado + "-" + System.nanoTime());
		Long idCliente = jdbc.queryForObject("""
				INSERT INTO cliente (nombre_razon_social)
				VALUES (?)
				RETURNING id_cliente
				""", Long.class, CLIENTE + " cotizacion " + System.nanoTime());
		return jdbc.queryForObject("""
				INSERT INTO cotizacion (id_cliente, id_usuario_responsable, fecha_vencimiento, descuento, estado,
				    origen_rechazo, id_usuario_rechazo, fecha_rechazo, motivo_rechazo)
				VALUES (?, ?, CURRENT_DATE + 30, 0, ?, ?, ?, ?, ?)
				RETURNING id_cotizacion
				""", Long.class, idCliente, idUsuario, estado, origen, idUsuarioRechazo, marca(fechaRechazo), motivo);
	}

	private Long insertarGuia(Long idVenta, String estado, Instant fechaSalida, Long idUsuario, Instant fechaEntrega) {
		return jdbc.queryForObject("""
				INSERT INTO guia_despacho (id_venta, direccion_entrega, estado, fecha_salida_real,
				    id_usuario_responsable, fecha_entrega)
				VALUES (?, 'Av. Ficticia 500', ?, ?, ?, ?)
				RETURNING id_guia
				""", Long.class, idVenta, estado, marca(fechaSalida), idUsuario, marca(fechaEntrega));
	}

	private Long registrarReimpresion(Long idGuia, Long idUsuario, String motivo) {
		return jdbc.queryForObject("""
				INSERT INTO reimpresion_guia (id_guia, id_usuario, motivo)
				VALUES (?, ?, ?)
				RETURNING id_reimpresion
				""", Long.class, idGuia, idUsuario, motivo);
	}

	private static Timestamp marca(Instant instante) {
		return instante == null ? null : Timestamp.from(instante);
	}

	private void limpiar() {
		jdbc.update("ALTER TABLE reimpresion_guia DISABLE TRIGGER tg_reimpresion_guia_solo_anexion");
		jdbc.update("""
				DELETE FROM reimpresion_guia WHERE id_guia IN (
				    SELECT g.id_guia FROM guia_despacho g
				    JOIN venta v ON v.id_venta = g.id_venta
				    JOIN cliente c ON c.id_cliente = v.id_cliente
				    WHERE c.nombre_razon_social LIKE 'Cliente V12%')
				""");
		jdbc.update("ALTER TABLE reimpresion_guia ENABLE TRIGGER tg_reimpresion_guia_solo_anexion");
		jdbc.update("""
				DELETE FROM guia_despacho WHERE id_venta IN (
				    SELECT v.id_venta FROM venta v
				    JOIN cliente c ON c.id_cliente = v.id_cliente
				    WHERE c.nombre_razon_social LIKE 'Cliente V12%')
				""");
		jdbc.update("""
				DELETE FROM venta WHERE id_cliente IN (
				    SELECT id_cliente FROM cliente WHERE nombre_razon_social LIKE 'Cliente V12%')
				""");
		jdbc.update("""
				DELETE FROM cotizacion WHERE id_cliente IN (
				    SELECT id_cliente FROM cliente WHERE nombre_razon_social LIKE 'Cliente V12%')
				""");
		jdbc.update("DELETE FROM cliente WHERE nombre_razon_social LIKE 'Cliente V12%'");
		jdbc.update("DELETE FROM cotizacion WHERE id_usuario_rechazo IN (SELECT id_usuario FROM usuario WHERE username LIKE 'v12-%')");
		jdbc.update("DELETE FROM usuario WHERE username LIKE 'v12-%'");
	}

}
