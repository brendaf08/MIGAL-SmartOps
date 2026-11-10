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
 * Verifica la migración V6 (tabla guia_despacho) con PostgreSQL real.
 * Cada prueba elimina las filas que inserta, en orden inverso a las claves foráneas.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class MigracionDespachoTest {

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
	void existeLaTablaGuiaDespacho() {
		Integer cantidad = jdbc.queryForObject("""
				SELECT count(*) FROM information_schema.tables
				WHERE table_schema = 'public' AND table_name = 'guia_despacho'
				""", Integer.class);
		assertEquals(1, cantidad);
	}

	@Test
	void insertaGuiaEnPreparacionSinDatosDeSalida() {
		Long idVenta = crearVenta("despacho1");

		insertarGuia(idVenta, "preparacion", null);

		Integer cantidad = jdbc.queryForObject(
				"SELECT count(*) FROM guia_despacho WHERE id_venta = ? AND estado = 'preparacion'",
				Integer.class, idVenta);
		assertEquals(1, cantidad);
	}

	@Test
	void insertaGuiaEntregadaConSalidaConfirmada() {
		Long idUsuario = crearUsuario("despachador1");
		Long idVenta = crearVenta("despacho2");

		jdbc.update("""
				INSERT INTO guia_despacho (id_venta, direccion_entrega, estado, fecha_salida_real, id_usuario_responsable, fecha_entrega)
				VALUES (?, 'Av. Ficticia 100', 'entregada', now(), ?, now())
				""", idVenta, idUsuario);

		Integer cantidad = jdbc.queryForObject(
				"SELECT count(*) FROM guia_despacho WHERE id_venta = ? AND id_usuario_responsable = ?",
				Integer.class, idVenta, idUsuario);
		assertEquals(1, cantidad);
	}

	@Test
	void rechazaEstadoDeGuiaFueraDelDominio() {
		Long idVenta = crearVenta("despacho3");

		assertThrows(DataIntegrityViolationException.class,
				() -> insertarGuia(idVenta, "pendiente", null));
	}

	@Test
	void rechazaSegundaGuiaParaLaMismaVenta() {
		Long idVenta = crearVenta("despacho4");
		insertarGuia(idVenta, "emitida", null);

		assertThrows(DataIntegrityViolationException.class,
				() -> insertarGuia(idVenta, "preparacion", null));
	}

	@Test
	void rechazaSalidaConfirmadaSinUsuarioResponsable() {
		Long idVenta = crearVenta("despacho5");

		assertThrows(DataIntegrityViolationException.class,
				() -> jdbc.update("""
						INSERT INTO guia_despacho (id_venta, direccion_entrega, estado, fecha_salida_real)
						VALUES (?, 'Av. Ficticia 200', 'salida_confirmada', now())
						""", idVenta));
	}

	@Test
	void rechazaSalidaConfirmadaSinFechaDeSalidaReal() {
		Long idUsuario = crearUsuario("despachador2");
		Long idVenta = crearVenta("despacho6");

		assertThrows(DataIntegrityViolationException.class,
				() -> jdbc.update("""
						INSERT INTO guia_despacho (id_venta, direccion_entrega, estado, id_usuario_responsable)
						VALUES (?, 'Av. Ficticia 300', 'salida_confirmada', ?)
						""", idVenta, idUsuario));
	}

	@Test
	void rechazaEmitidaConDatosDeSalidaConfirmada() {
		Long idUsuario = crearUsuario("despachador3");
		Long idVenta = crearVenta("despacho7");

		assertThrows(DataIntegrityViolationException.class,
				() -> jdbc.update("""
						INSERT INTO guia_despacho (id_venta, direccion_entrega, estado, fecha_salida_real, id_usuario_responsable)
						VALUES (?, 'Av. Ficticia 400', 'emitida', now(), ?)
						""", idVenta, idUsuario));
	}

	@Test
	void rechazaGuiaParaVentaInexistente() {
		assertThrows(DataIntegrityViolationException.class,
				() -> insertarGuia(999999L, "preparacion", null));
	}

	private Long crearUsuario(String username) {
		return jdbc.queryForObject("""
				INSERT INTO usuario (nombres, apellidos, username, password_hash, rol)
				VALUES ('Nombre', 'Apellido', ?, 'hash-ficticio', 'almacenero')
				RETURNING id_usuario
				""", Long.class, username);
	}

	private Long crearVenta(String sufijo) {
		Long idUsuario = crearUsuario("vendedor-" + sufijo);
		Long idCliente = jdbc.queryForObject("""
				INSERT INTO cliente (nombre_razon_social)
				VALUES ('Cliente ficticio')
				RETURNING id_cliente
				""", Long.class);
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

	private void insertarGuia(Long idVenta, String estado, Long idUsuario) {
		jdbc.update("""
				INSERT INTO guia_despacho (id_venta, direccion_entrega, estado, id_usuario_responsable)
				VALUES (?, 'Av. Ficticia 500', ?, ?)
				""", idVenta, estado, idUsuario);
	}

	private void limpiar() {
		jdbc.update("DELETE FROM guia_despacho");
		jdbc.update("DELETE FROM venta");
		jdbc.update("DELETE FROM cotizacion");
		jdbc.update("DELETE FROM cliente");
		jdbc.update("DELETE FROM usuario");
	}

}
