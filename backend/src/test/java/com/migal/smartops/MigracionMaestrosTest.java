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
 * Verifica la migración V2 (tablas maestras) con PostgreSQL real.
 * Cada prueba elimina las filas que inserta, en orden inverso a las claves foráneas.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class MigracionMaestrosTest {

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
	void existenLasTablasMaestras() {
		Integer cantidad = jdbc.queryForObject("""
				SELECT count(*) FROM information_schema.tables
				WHERE table_schema = 'public'
				  AND table_name IN ('almacen', 'cliente', 'producto', 'proveedor', 'catalogo_proveedor')
				""", Integer.class);
		assertEquals(5, cantidad);
	}

	@Test
	void insertaMaestrosYCatalogoValidos() {
		Long idProducto = insertarProducto("FER-0001", 12.50, 0);
		Long idProveedor = insertarProveedor("20123456789");
		insertarAlmacen("Tienda principal");
		insertarCliente("Cliente mostrador");

		jdbc.update("""
				INSERT INTO catalogo_proveedor (id_proveedor, id_producto, precio_compra, plazo_entrega_referencial_dias)
				VALUES (?, ?, 9.90, 5)
				""", idProveedor, idProducto);

		Integer cantidad = jdbc.queryForObject(
				"SELECT count(*) FROM catalogo_proveedor WHERE id_proveedor = ? AND id_producto = ?",
				Integer.class, idProveedor, idProducto);
		assertEquals(1, cantidad);
	}

	@Test
	void rechazaSkuDuplicado() {
		insertarProducto("CEM-0001", 25.00, 10);

		assertThrows(DataIntegrityViolationException.class,
				() -> insertarProducto("CEM-0001", 30.00, 10));
	}

	@Test
	void rechazaRucDuplicado() {
		insertarProveedor("20555555551");

		assertThrows(DataIntegrityViolationException.class,
				() -> insertarProveedor("20555555551"));
	}

	@Test
	void rechazaPrecioReferencialNegativo() {
		assertThrows(DataIntegrityViolationException.class,
				() -> insertarProducto("LAD-0001", -1.00, 0));
	}

	@Test
	void rechazaStockMinimoNegativo() {
		assertThrows(DataIntegrityViolationException.class,
				() -> insertarProducto("FIE-0001", 5.00, -0.001));
	}

	@Test
	void rechazaCatalogoConProductoInexistente() {
		Long idProveedor = insertarProveedor("20666666662");

		assertThrows(DataIntegrityViolationException.class,
				() -> jdbc.update("""
						INSERT INTO catalogo_proveedor (id_proveedor, id_producto, precio_compra)
						VALUES (?, 999999, 3.50)
						""", idProveedor));
	}

	private Long insertarProducto(String sku, double precioReferencial, double stockMinimo) {
		return jdbc.queryForObject("""
				INSERT INTO producto (sku, nombre_oficial, unidad_medida, precio_referencial, stock_minimo, categoria_producto)
				VALUES (?, ?, 'unidad', ?, ?, 'general')
				RETURNING id_producto
				""", Long.class, sku, "Producto " + sku, precioReferencial, stockMinimo);
	}

	private Long insertarProveedor(String ruc) {
		return jdbc.queryForObject("""
				INSERT INTO proveedor (ruc, razon_social, contacto)
				VALUES (?, 'Proveedor ficticio', 'contacto@ejemplo.test')
				RETURNING id_proveedor
				""", Long.class, ruc);
	}

	private void insertarAlmacen(String nombre) {
		jdbc.update("""
				INSERT INTO almacen (nombre, tipo_ubicacion)
				VALUES (?, 'tienda')
				""", nombre);
	}

	private void insertarCliente(String nombre) {
		jdbc.update("""
				INSERT INTO cliente (nombre_razon_social)
				VALUES (?)
				""", nombre);
	}

	private void limpiar() {
		jdbc.update("DELETE FROM catalogo_proveedor");
		jdbc.update("DELETE FROM producto");
		jdbc.update("DELETE FROM proveedor");
		jdbc.update("DELETE FROM cliente");
		jdbc.update("DELETE FROM almacen");
	}

}
