package com.migal.smartops;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Verifica la migración V7 (movimiento_kardex) con PostgreSQL real: coherencia por origen y solo anexión.
 * La limpieza desactiva el disparador de solo anexión solo para eliminar las filas de prueba.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class MigracionKardexTest {

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
	void existeLaTablaMovimientoKardex() {
		Integer cantidad = jdbc.queryForObject("""
				SELECT count(*) FROM information_schema.tables
				WHERE table_schema = 'public' AND table_name = 'movimiento_kardex'
				""", Integer.class);
		assertEquals(1, cantidad);
	}

	@Test
	void aceptaSalidaDeDespachoYEntradaDeRecepcion() {
		Long idUsuario = crearUsuario();
		Long idInventario = crearInventario();

		insertarMovimiento(idInventario, idUsuario, "salida", "2.000", "despacho", null, crearGuia(idUsuario), null, null, null);
		insertarMovimiento(idInventario, idUsuario, "entrada", "3.000", "recepcion", null, null, crearRecepcion(idUsuario), null, null);

		assertEquals(2, contarMovimientos(idInventario));
	}

	@Test
	void aceptaAjusteCompensatorioQueCorrigeUnMovimiento() {
		Long idUsuario = crearUsuario();
		Long idInventario = crearInventario();
		Long idOriginal = insertarMovimiento(idInventario, idUsuario, "salida", "4.000", "ajuste", "Merma por rotura", null, null, null, null);

		insertarMovimiento(idInventario, idUsuario, "entrada", "4.000", "ajuste", "Correccion de merma", null, null, null, idOriginal);

		assertEquals(2, contarMovimientos(idInventario));
	}

	@Test
	void rechazaDespachoSinGuia() {
		assertThrows(DataIntegrityViolationException.class, () -> insertarMovimiento(
				crearInventario(), crearUsuario(), "salida", "1.000", "despacho", null, null, null, null, null));
	}

	@Test
	void rechazaDespachoComoEntrada() {
		assertThrows(DataIntegrityViolationException.class, () -> {
			Long idUsuario = crearUsuario();
			insertarMovimiento(crearInventario(), idUsuario, "entrada", "1.000", "despacho", null, crearGuia(idUsuario), null, null, null);
		});
	}

	@Test
	void rechazaRecepcionConGuiaDeDespacho() {
		assertThrows(DataIntegrityViolationException.class, () -> {
			Long idUsuario = crearUsuario();
			insertarMovimiento(crearInventario(), idUsuario, "entrada", "1.000", "recepcion", null,
					crearGuia(idUsuario), crearRecepcion(idUsuario), null, null);
		});
	}

	@Test
	void rechazaAjusteSinMotivoOConMotivoVacio() {
		assertThrows(DataIntegrityViolationException.class, () -> insertarMovimiento(
				crearInventario(), crearUsuario(), "salida", "1.000", "ajuste", null, null, null, null, null));
		assertThrows(DataIntegrityViolationException.class, () -> insertarMovimiento(
				crearInventario(), crearUsuario(), "salida", "1.000", "ajuste", "   ", null, null, null, null));
	}

	@Test
	void rechazaTrasladoSinReferencia() {
		assertThrows(DataIntegrityViolationException.class, () -> insertarMovimiento(
				crearInventario(), crearUsuario(), "salida", "1.000", "traslado", "Reubicacion", null, null, null, null));
	}

	@Test
	void rechazaReferenciaDeTrasladoEnAjuste() {
		assertThrows(DataIntegrityViolationException.class, () -> insertarMovimiento(
				crearInventario(), crearUsuario(), "salida", "1.000", "ajuste", "Merma", null, null, UUID.randomUUID(), null));
	}

	@Test
	void rechazaCorreccionFueraDeAjuste() {
		assertThrows(DataIntegrityViolationException.class, () -> {
			Long idUsuario = crearUsuario();
			Long idInventario = crearInventario();
			Long idOriginal = insertarMovimiento(idInventario, idUsuario, "salida", "1.000", "ajuste", "Merma", null, null, null, null);
			insertarMovimiento(idInventario, idUsuario, "salida", "1.000", "despacho", null,
					crearGuia(idUsuario), null, null, idOriginal);
		});
	}

	@Test
	void rechazaCantidadNoPositiva() {
		assertThrows(DataIntegrityViolationException.class, () -> insertarMovimiento(
				crearInventario(), crearUsuario(), "salida", "0.000", "ajuste", "Merma", null, null, null, null));
	}

	@Test
	void rechazaTipoDeOperacionFueraDelDominio() {
		assertThrows(DataIntegrityViolationException.class, () -> insertarMovimiento(
				crearInventario(), crearUsuario(), "ajuste", "1.000", "ajuste", "Merma", null, null, null, null));
	}

	@Test
	void rechazaActualizacionYEliminacionDeMovimiento() {
		Long idInventario = crearInventario();
		insertarMovimiento(idInventario, crearUsuario(), "salida", "1.000", "ajuste", "Merma", null, null, null, null);

		assertThrows(DataIntegrityViolationException.class,
				() -> jdbc.update("UPDATE movimiento_kardex SET cantidad = 9.000 WHERE id_inventario = ?", idInventario));
		assertThrows(DataIntegrityViolationException.class,
				() -> jdbc.update("DELETE FROM movimiento_kardex WHERE id_inventario = ?", idInventario));
	}

	@Test
	void rechazaVaciadoDelKardex() {
		Long idUsuario = crearUsuario();
		insertarMovimiento(crearInventario(), idUsuario, "salida", "1.000", "ajuste", "Merma", null, null, null, null);

		assertThrows(DataIntegrityViolationException.class, () -> jdbc.execute("TRUNCATE movimiento_kardex"));
	}

	private Long insertarMovimiento(Long idInventario, Long idUsuario, String tipo, String cantidad, String origen,
			String motivo, Long idGuia, Long idRecepcion, UUID referencia, Long idCorregido) {
		return jdbc.queryForObject("""
				INSERT INTO movimiento_kardex (id_inventario, tipo_operacion, cantidad, motivo, id_usuario_responsable,
				                               origen_movimiento, id_guia, id_recepcion, referencia_traslado, id_movimiento_corregido)
				VALUES (?, ?, CAST(? AS numeric), ?, ?, ?, ?, ?, ?, ?)
				RETURNING id_movimiento
				""", Long.class, idInventario, tipo, cantidad, motivo, idUsuario, origen,
				idGuia, idRecepcion, referencia, idCorregido);
	}

	private int contarMovimientos(Long idInventario) {
		return jdbc.queryForObject("SELECT count(*) FROM movimiento_kardex WHERE id_inventario = ?",
				Integer.class, idInventario);
	}

	private String sufijo() {
		return UUID.randomUUID().toString().substring(0, 8);
	}

	private Long crearUsuario() {
		return jdbc.queryForObject("""
				INSERT INTO usuario (nombres, apellidos, username, password_hash, rol)
				VALUES ('Nombre', 'Apellido', ?, 'hash-ficticio', 'almacenero')
				RETURNING id_usuario
				""", Long.class, "kardex-" + sufijo());
	}

	private Long crearInventario() {
		Long idProducto = jdbc.queryForObject("""
				INSERT INTO producto (sku, nombre_oficial, unidad_medida, precio_referencial, stock_minimo, id_categoria)
				VALUES (?, 'Producto ficticio', 'bolsa', 10.00, 0, (SELECT id_categoria FROM categoria WHERE nombre = 'Cemento y aglomerantes'))
				RETURNING id_producto
				""", Long.class, "KAR-" + sufijo());
		Long idAlmacen = jdbc.queryForObject("""
				INSERT INTO almacen (nombre, tipo_ubicacion)
				VALUES (?, 'almacen')
				RETURNING id_almacen
				""", Long.class, "Almacen " + sufijo());
		return jdbc.queryForObject("""
				INSERT INTO inventario (id_producto, id_almacen, cantidad_fisica)
				VALUES (?, ?, 100.000)
				RETURNING id_inventario
				""", Long.class, idProducto, idAlmacen);
	}

	private Long crearGuia(Long idUsuario) {
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
		Long idVenta = jdbc.queryForObject("""
				INSERT INTO venta (id_cotizacion, id_cliente, id_usuario_responsable, descuento, estado)
				VALUES (?, ?, ?, 0, 'lista_para_despacho')
				RETURNING id_venta
				""", Long.class, idCotizacion, idCliente, idUsuario);
		return jdbc.queryForObject("""
				INSERT INTO guia_despacho (id_venta, direccion_entrega, estado)
				VALUES (?, 'Av. Ficticia 600', 'emitida')
				RETURNING id_guia
				""", Long.class, idVenta);
	}

	private Long crearRecepcion(Long idUsuario) {
		Long idProveedor = jdbc.queryForObject("""
				INSERT INTO proveedor (ruc, razon_social, contacto)
				VALUES (?, 'Proveedor ficticio', 'contacto ficticio')
				RETURNING id_proveedor
				""", Long.class, "KAR" + sufijo());
		Long idOrden = jdbc.queryForObject("""
				INSERT INTO orden_compra (id_proveedor, id_usuario_responsable, estado)
				VALUES (?, ?, 'pendiente')
				RETURNING id_orden
				""", Long.class, idProveedor, idUsuario);
		return jdbc.queryForObject("""
				INSERT INTO orden_recepcion (id_orden, id_usuario_responsable, fecha_recepcion, estado)
				VALUES (?, ?, CURRENT_DATE, 'pendiente')
				RETURNING id_recepcion
				""", Long.class, idOrden, idUsuario);
	}

	private void limpiar() {
		jdbc.update("ALTER TABLE movimiento_kardex DISABLE TRIGGER tg_movimiento_kardex_solo_anexion");
		jdbc.update("DELETE FROM movimiento_kardex WHERE id_movimiento_corregido IS NOT NULL");
		jdbc.update("DELETE FROM movimiento_kardex");
		jdbc.update("ALTER TABLE movimiento_kardex ENABLE TRIGGER tg_movimiento_kardex_solo_anexion");
		jdbc.update("DELETE FROM guia_despacho");
		jdbc.update("DELETE FROM detalle_recepcion");
		jdbc.update("DELETE FROM orden_recepcion");
		jdbc.update("DELETE FROM detalle_compra");
		jdbc.update("DELETE FROM orden_compra");
		jdbc.update("DELETE FROM venta");
		jdbc.update("DELETE FROM cotizacion");
		jdbc.update("DELETE FROM cliente");
		jdbc.update("DELETE FROM inventario");
		jdbc.update("DELETE FROM producto");
		jdbc.update("DELETE FROM almacen");
		jdbc.update("DELETE FROM proveedor");
		jdbc.update("DELETE FROM usuario");
	}

}
