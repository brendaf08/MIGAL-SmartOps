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
 * Verifica la migración V5 (tablas de abastecimiento) con PostgreSQL real.
 * Cada prueba elimina las filas que inserta, en orden inverso a las claves foráneas.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class MigracionAbastecimientoTest {

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
	void existenLasTablasDeAbastecimiento() {
		Integer cantidad = jdbc.queryForObject("""
				SELECT count(*) FROM information_schema.tables
				WHERE table_schema = 'public'
				  AND table_name IN ('orden_compra', 'detalle_compra', 'orden_recepcion', 'detalle_recepcion')
				""", Integer.class);
		assertEquals(4, cantidad);
	}

	@Test
	void insertaOrdenConDetalleYRecepcionValidada() {
		Long idUsuario = crearUsuario("almacenero1");
		Long idProveedor = crearProveedor("20100000001");
		Long idProducto = crearProducto("CEM-0200");
		Long idAlmacen = crearAlmacen("Tienda principal");
		Long idOrden = crearOrden(idProveedor, idUsuario, "parcial");

		jdbc.update("""
				INSERT INTO detalle_compra (id_orden, id_producto, cantidad_comprada, costo_unitario)
				VALUES (?, ?, 50.000, 18.40)
				""", idOrden, idProducto);

		Long idRecepcion = crearRecepcion(idOrden, idUsuario, "validada");
		jdbc.update("""
				INSERT INTO detalle_recepcion (id_recepcion, id_producto, id_almacen_destino, cantidad_recibida)
				VALUES (?, ?, ?, 20.000)
				""", idRecepcion, idProducto, idAlmacen);

		Integer cantidad = jdbc.queryForObject(
				"SELECT count(*) FROM detalle_recepcion WHERE id_recepcion = ?", Integer.class, idRecepcion);
		assertEquals(1, cantidad);
	}

	@Test
	void aceptaMismoProductoEnDosAlmacenesDeLaMismaRecepcion() {
		Long idUsuario = crearUsuario("almacenero2");
		Long idProveedor = crearProveedor("20100000002");
		Long idProducto = crearProducto("FIE-0200");
		Long idAlmacen1 = crearAlmacen("Almacen 1");
		Long idAlmacen2 = crearAlmacen("Almacen 2");
		Long idOrden = crearOrden(idProveedor, idUsuario, "pendiente");
		Long idRecepcion = crearRecepcion(idOrden, idUsuario, "pendiente");

		insertarDetalleRecepcion(idRecepcion, idProducto, idAlmacen1, "5.000");
		insertarDetalleRecepcion(idRecepcion, idProducto, idAlmacen2, "7.500");

		Integer cantidad = jdbc.queryForObject(
				"SELECT count(*) FROM detalle_recepcion WHERE id_recepcion = ? AND id_producto = ?",
				Integer.class, idRecepcion, idProducto);
		assertEquals(2, cantidad);
	}

	@Test
	void rechazaEstadoDeOrdenFueraDelDominio() {
		Long idUsuario = crearUsuario("almacenero3");
		Long idProveedor = crearProveedor("20100000003");

		assertThrows(DataIntegrityViolationException.class,
				() -> crearOrden(idProveedor, idUsuario, "emitida"));
	}

	@Test
	void rechazaEstadoDeRecepcionFueraDelDominio() {
		Long idUsuario = crearUsuario("almacenero4");
		Long idProveedor = crearProveedor("20100000004");
		Long idOrden = crearOrden(idProveedor, idUsuario, "pendiente");

		assertThrows(DataIntegrityViolationException.class,
				() -> crearRecepcion(idOrden, idUsuario, "completada"));
	}

	@Test
	void rechazaCantidadCompradaNoPositiva() {
		Long idUsuario = crearUsuario("almacenero5");
		Long idProveedor = crearProveedor("20100000005");
		Long idProducto = crearProducto("LAD-0200");
		Long idOrden = crearOrden(idProveedor, idUsuario, "pendiente");

		assertThrows(DataIntegrityViolationException.class,
				() -> jdbc.update("""
						INSERT INTO detalle_compra (id_orden, id_producto, cantidad_comprada, costo_unitario)
						VALUES (?, ?, 0, 10.00)
						""", idOrden, idProducto));
	}

	@Test
	void rechazaCostoUnitarioNegativo() {
		Long idUsuario = crearUsuario("almacenero6");
		Long idProveedor = crearProveedor("20100000006");
		Long idProducto = crearProducto("AGR-0200");
		Long idOrden = crearOrden(idProveedor, idUsuario, "pendiente");

		assertThrows(DataIntegrityViolationException.class,
				() -> jdbc.update("""
						INSERT INTO detalle_compra (id_orden, id_producto, cantidad_comprada, costo_unitario)
						VALUES (?, ?, 1.000, -0.01)
						""", idOrden, idProducto));
	}

	@Test
	void rechazaProductoDuplicadoEnLaMismaOrden() {
		Long idUsuario = crearUsuario("almacenero7");
		Long idProveedor = crearProveedor("20100000007");
		Long idProducto = crearProducto("MAD-0200");
		Long idOrden = crearOrden(idProveedor, idUsuario, "pendiente");
		insertarDetalleCompra(idOrden, idProducto);

		assertThrows(DataIntegrityViolationException.class,
				() -> insertarDetalleCompra(idOrden, idProducto));
	}

	@Test
	void rechazaRecepcionValidadaSinFechaDeValidacion() {
		Long idUsuario = crearUsuario("almacenero8");
		Long idProveedor = crearProveedor("20100000008");
		Long idOrden = crearOrden(idProveedor, idUsuario, "pendiente");

		assertThrows(DataIntegrityViolationException.class,
				() -> jdbc.update("""
						INSERT INTO orden_recepcion (id_orden, id_usuario_responsable, fecha_recepcion, estado)
						VALUES (?, ?, CURRENT_DATE, 'validada')
						""", idOrden, idUsuario));
	}

	@Test
	void rechazaRecepcionPendienteConFechaDeValidacion() {
		Long idUsuario = crearUsuario("almacenero9");
		Long idProveedor = crearProveedor("20100000009");
		Long idOrden = crearOrden(idProveedor, idUsuario, "pendiente");

		assertThrows(DataIntegrityViolationException.class,
				() -> jdbc.update("""
						INSERT INTO orden_recepcion (id_orden, id_usuario_responsable, fecha_recepcion, estado, fecha_validacion)
						VALUES (?, ?, CURRENT_DATE, 'pendiente', now())
						""", idOrden, idUsuario));
	}

	@Test
	void rechazaDetalleDeRecepcionDuplicadoEnLaMismaUbicacion() {
		Long idUsuario = crearUsuario("almacenero10");
		Long idProveedor = crearProveedor("20100000010");
		Long idProducto = crearProducto("ACE-0200");
		Long idAlmacen = crearAlmacen("Almacen 3");
		Long idOrden = crearOrden(idProveedor, idUsuario, "pendiente");
		Long idRecepcion = crearRecepcion(idOrden, idUsuario, "pendiente");
		insertarDetalleRecepcion(idRecepcion, idProducto, idAlmacen, "3.000");

		assertThrows(DataIntegrityViolationException.class,
				() -> insertarDetalleRecepcion(idRecepcion, idProducto, idAlmacen, "4.000"));
	}

	@Test
	void rechazaCantidadRecibidaNegativa() {
		Long idUsuario = crearUsuario("almacenero11");
		Long idProveedor = crearProveedor("20100000011");
		Long idProducto = crearProducto("PIN-0200");
		Long idAlmacen = crearAlmacen("Almacen 4");
		Long idOrden = crearOrden(idProveedor, idUsuario, "pendiente");
		Long idRecepcion = crearRecepcion(idOrden, idUsuario, "pendiente");

		assertThrows(DataIntegrityViolationException.class,
				() -> insertarDetalleRecepcion(idRecepcion, idProducto, idAlmacen, "-1.000"));
	}

	@Test
	void rechazaOrdenDeProveedorInexistente() {
		Long idUsuario = crearUsuario("almacenero12");

		assertThrows(DataIntegrityViolationException.class,
				() -> crearOrden(999999L, idUsuario, "pendiente"));
	}

	private Long crearUsuario(String username) {
		return jdbc.queryForObject("""
				INSERT INTO usuario (nombres, apellidos, username, password_hash, rol)
				VALUES ('Nombre', 'Apellido', ?, 'hash-ficticio', 'almacenero')
				RETURNING id_usuario
				""", Long.class, username);
	}

	private Long crearProveedor(String ruc) {
		return jdbc.queryForObject("""
				INSERT INTO proveedor (ruc, razon_social, contacto)
				VALUES (?, ?, 'contacto ficticio')
				RETURNING id_proveedor
				""", Long.class, ruc, "Proveedor " + ruc);
	}

	private Long crearProducto(String sku) {
		return jdbc.queryForObject("""
				INSERT INTO producto (sku, nombre_oficial, unidad_medida, precio_referencial, stock_minimo, id_categoria)
				VALUES (?, ?, 'bolsa', 24.50, 0, (SELECT id_categoria FROM categoria WHERE nombre = 'Cemento y aglomerantes'))
				RETURNING id_producto
				""", Long.class, sku, "Producto " + sku);
	}

	private Long crearAlmacen(String nombre) {
		return jdbc.queryForObject("""
				INSERT INTO almacen (nombre, tipo_ubicacion)
				VALUES (?, 'almacen')
				RETURNING id_almacen
				""", Long.class, nombre);
	}

	private Long crearOrden(Long idProveedor, Long idUsuario, String estado) {
		return jdbc.queryForObject("""
				INSERT INTO orden_compra (id_proveedor, id_usuario_responsable, estado)
				VALUES (?, ?, ?)
				RETURNING id_orden
				""", Long.class, idProveedor, idUsuario, estado);
	}

	private void insertarDetalleCompra(Long idOrden, Long idProducto) {
		jdbc.update("""
				INSERT INTO detalle_compra (id_orden, id_producto, cantidad_comprada, costo_unitario)
				VALUES (?, ?, 10.000, 12.00)
				""", idOrden, idProducto);
	}

	private Long crearRecepcion(Long idOrden, Long idUsuario, String estado) {
		return jdbc.queryForObject("""
				INSERT INTO orden_recepcion (id_orden, id_usuario_responsable, fecha_recepcion, estado, fecha_validacion)
				VALUES (?, ?, CURRENT_DATE, ?, CASE WHEN ? = 'validada' THEN now() ELSE NULL END)
				RETURNING id_recepcion
				""", Long.class, idOrden, idUsuario, estado, estado);
	}

	private void insertarDetalleRecepcion(Long idRecepcion, Long idProducto, Long idAlmacen, String cantidad) {
		jdbc.update("""
				INSERT INTO detalle_recepcion (id_recepcion, id_producto, id_almacen_destino, cantidad_recibida)
				VALUES (?, ?, ?, CAST(? AS numeric))
				""", idRecepcion, idProducto, idAlmacen, cantidad);
	}

	private void limpiar() {
		jdbc.update("DELETE FROM detalle_recepcion");
		jdbc.update("DELETE FROM orden_recepcion");
		jdbc.update("DELETE FROM detalle_compra");
		jdbc.update("DELETE FROM orden_compra");
		jdbc.update("DELETE FROM producto");
		jdbc.update("DELETE FROM almacen");
		jdbc.update("DELETE FROM proveedor");
		jdbc.update("DELETE FROM usuario");
	}

}
