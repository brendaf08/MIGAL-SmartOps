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
 * Verifica la migración V4 (tablas comerciales) con PostgreSQL real.
 * Cada prueba elimina las filas que inserta, en orden inverso a las claves foráneas.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class MigracionComercialTest {

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
	void existenLasTablasComerciales() {
		Integer cantidad = jdbc.queryForObject("""
				SELECT count(*) FROM information_schema.tables
				WHERE table_schema = 'public'
				  AND table_name IN ('cotizacion', 'detalle_cotizacion', 'venta', 'detalle_venta')
				""", Integer.class);
		assertEquals(4, cantidad);
	}

	@Test
	void insertaCotizacionVentaYDetallesValidos() {
		Long idUsuario = crearUsuario("vendedor1");
		Long idCliente = crearCliente();
		Long idProducto = crearProducto("CEM-0100");
		Long idCotizacion = crearCotizacion(idCliente, idUsuario, "validada");

		jdbc.update("""
				INSERT INTO detalle_cotizacion (id_cotizacion, id_producto, cantidad_cotizada, precio_unitario)
				VALUES (?, ?, 10.000, 24.50)
				""", idCotizacion, idProducto);

		Long idVenta = crearVenta(idCotizacion, idCliente, idUsuario, "pendiente_abastecimiento");

		jdbc.update("""
				INSERT INTO detalle_venta (id_venta, id_producto, cantidad_vendida, precio_unitario)
				VALUES (?, ?, 10.000, 24.50)
				""", idVenta, idProducto);

		Integer cantidad = jdbc.queryForObject(
				"SELECT count(*) FROM detalle_venta WHERE id_venta = ?", Integer.class, idVenta);
		assertEquals(1, cantidad);
	}

	@Test
	void insertaVentaCanceladaConEvidenciaCompleta() {
		Long idUsuario = crearUsuario("vendedor2");
		Long idCliente = crearCliente();
		Long idCotizacion = crearCotizacion(idCliente, idUsuario, "validada");

		jdbc.update("""
				INSERT INTO venta (id_cotizacion, id_cliente, id_usuario_responsable, descuento, estado,
				                   motivo_cancelacion, fecha_cancelacion, id_usuario_autorizante)
				VALUES (?, ?, ?, 0, 'cancelada', 'Cliente desistio', now(), ?)
				""", idCotizacion, idCliente, idUsuario, idUsuario);

		Integer cantidad = jdbc.queryForObject(
				"SELECT count(*) FROM venta WHERE id_cotizacion = ? AND estado = 'cancelada'",
				Integer.class, idCotizacion);
		assertEquals(1, cantidad);
	}

	@Test
	void rechazaDosVentasParaLaMismaCotizacion() {
		Long idUsuario = crearUsuario("vendedor3");
		Long idCliente = crearCliente();
		Long idCotizacion = crearCotizacion(idCliente, idUsuario, "validada");
		crearVenta(idCotizacion, idCliente, idUsuario, "pendiente_despacho");

		assertThrows(DataIntegrityViolationException.class,
				() -> crearVenta(idCotizacion, idCliente, idUsuario, "pendiente_despacho"));
	}

	@Test
	void rechazaEstadoDeCotizacionFueraDelDominio() {
		Long idUsuario = crearUsuario("vendedor4");
		Long idCliente = crearCliente();

		assertThrows(DataIntegrityViolationException.class,
				() -> crearCotizacion(idCliente, idUsuario, "pendiente"));
	}

	@Test
	void rechazaEstadoDeVentaFueraDelDominio() {
		Long idUsuario = crearUsuario("vendedor5");
		Long idCliente = crearCliente();
		Long idCotizacion = crearCotizacion(idCliente, idUsuario, "validada");

		assertThrows(DataIntegrityViolationException.class,
				() -> crearVenta(idCotizacion, idCliente, idUsuario, "entregada"));
	}

	@Test
	void rechazaCantidadVendidaNoPositiva() {
		Long idUsuario = crearUsuario("vendedor6");
		Long idCliente = crearCliente();
		Long idProducto = crearProducto("FIE-0100");
		Long idCotizacion = crearCotizacion(idCliente, idUsuario, "validada");
		Long idVenta = crearVenta(idCotizacion, idCliente, idUsuario, "pendiente_abastecimiento");

		assertThrows(DataIntegrityViolationException.class,
				() -> jdbc.update("""
						INSERT INTO detalle_venta (id_venta, id_producto, cantidad_vendida, precio_unitario)
						VALUES (?, ?, 0, 5.00)
						""", idVenta, idProducto));
	}

	@Test
	void rechazaCancelacionSinMotivo() {
		Long idUsuario = crearUsuario("vendedor7");
		Long idCliente = crearCliente();
		Long idCotizacion = crearCotizacion(idCliente, idUsuario, "validada");

		assertThrows(DataIntegrityViolationException.class,
				() -> jdbc.update("""
						INSERT INTO venta (id_cotizacion, id_cliente, id_usuario_responsable, descuento, estado,
						                   motivo_cancelacion, fecha_cancelacion, id_usuario_autorizante)
						VALUES (?, ?, ?, 0, 'cancelada', NULL, now(), ?)
						""", idCotizacion, idCliente, idUsuario, idUsuario));
	}

	@Test
	void rechazaCancelacionSinUsuarioAutorizante() {
		Long idUsuario = crearUsuario("vendedor8");
		Long idCliente = crearCliente();
		Long idCotizacion = crearCotizacion(idCliente, idUsuario, "validada");

		assertThrows(DataIntegrityViolationException.class,
				() -> jdbc.update("""
						INSERT INTO venta (id_cotizacion, id_cliente, id_usuario_responsable, descuento, estado,
						                   motivo_cancelacion, fecha_cancelacion)
						VALUES (?, ?, ?, 0, 'cancelada', 'Duplicado', now())
						""", idCotizacion, idCliente, idUsuario));
	}

	@Test
	void rechazaMotivoDeCancelacionEnVentaActiva() {
		Long idUsuario = crearUsuario("vendedor9");
		Long idCliente = crearCliente();
		Long idCotizacion = crearCotizacion(idCliente, idUsuario, "validada");

		assertThrows(DataIntegrityViolationException.class,
				() -> jdbc.update("""
						INSERT INTO venta (id_cotizacion, id_cliente, id_usuario_responsable, descuento, estado,
						                   motivo_cancelacion)
						VALUES (?, ?, ?, 0, 'pendiente_abastecimiento', 'Sin motivo previo')
						""", idCotizacion, idCliente, idUsuario));
	}

	@Test
	void rechazaVentaConClienteDistintoDeLaCotizacion() {
		Long idUsuario = crearUsuario("vendedor10");
		Long idClienteOriginal = crearCliente();
		Long idOtroCliente = crearCliente();
		Long idCotizacion = crearCotizacion(idClienteOriginal, idUsuario, "validada");

		assertThrows(DataIntegrityViolationException.class,
				() -> crearVenta(idCotizacion, idOtroCliente, idUsuario, "pendiente_abastecimiento"));
	}

	@Test
	void rechazaDetalleDeVentaInexistente() {
		Long idProducto = crearProducto("LAD-0100");

		assertThrows(DataIntegrityViolationException.class,
				() -> jdbc.update("""
						INSERT INTO detalle_venta (id_venta, id_producto, cantidad_vendida, precio_unitario)
						VALUES (999999, ?, 1, 5.00)
						""", idProducto));
	}

	private Long crearUsuario(String username) {
		return jdbc.queryForObject("""
				INSERT INTO usuario (nombres, apellidos, username, password_hash, rol)
				VALUES ('Nombre', 'Apellido', ?, 'hash-ficticio', 'vendedor')
				RETURNING id_usuario
				""", Long.class, username);
	}

	private Long crearCliente() {
		return jdbc.queryForObject("""
				INSERT INTO cliente (nombre_razon_social)
				VALUES ('Cliente ficticio')
				RETURNING id_cliente
				""", Long.class);
	}

	private Long crearProducto(String sku) {
		return jdbc.queryForObject("""
				INSERT INTO producto (sku, nombre_oficial, unidad_medida, precio_referencial, stock_minimo, categoria_producto)
				VALUES (?, ?, 'bolsa', 24.50, 0, 'cementos')
				RETURNING id_producto
				""", Long.class, sku, "Producto " + sku);
	}

	private Long crearCotizacion(Long idCliente, Long idUsuario, String estado) {
		return jdbc.queryForObject("""
				INSERT INTO cotizacion (id_cliente, id_usuario_responsable, fecha_vencimiento, descuento, estado)
				VALUES (?, ?, CURRENT_DATE + 30, 0, ?)
				RETURNING id_cotizacion
				""", Long.class, idCliente, idUsuario, estado);
	}

	private Long crearVenta(Long idCotizacion, Long idCliente, Long idUsuario, String estado) {
		return jdbc.queryForObject("""
				INSERT INTO venta (id_cotizacion, id_cliente, id_usuario_responsable, descuento, estado)
				VALUES (?, ?, ?, 0, ?)
				RETURNING id_venta
				""", Long.class, idCotizacion, idCliente, idUsuario, estado);
	}

	private void limpiar() {
		jdbc.update("DELETE FROM detalle_venta");
		jdbc.update("DELETE FROM venta");
		jdbc.update("DELETE FROM detalle_cotizacion");
		jdbc.update("DELETE FROM cotizacion");
		jdbc.update("DELETE FROM inventario");
		jdbc.update("DELETE FROM catalogo_proveedor");
		jdbc.update("DELETE FROM producto");
		jdbc.update("DELETE FROM cliente");
		jdbc.update("DELETE FROM almacen");
		jdbc.update("DELETE FROM proveedor");
		jdbc.update("DELETE FROM usuario");
	}

}
