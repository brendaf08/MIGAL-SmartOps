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
 * Verifica la migración V3 (tabla inventario) con PostgreSQL real.
 * Cada prueba elimina las filas que inserta, en orden inverso a las claves foráneas.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class MigracionInventarioTest {

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
	void existeLaTablaInventario() {
		Integer cantidad = jdbc.queryForObject("""
				SELECT count(*) FROM information_schema.tables
				WHERE table_schema = 'public' AND table_name = 'inventario'
				""", Integer.class);
		assertEquals(1, cantidad);
	}

	@Test
	void insertaInventarioConCantidadFraccionaria() {
		Long idProducto = insertarProducto("MAD-0001");
		Long idAlmacen = insertarAlmacen("Almacen 1");

		insertarInventario(idProducto, idAlmacen, "2.500");

		Integer cantidad = jdbc.queryForObject(
				"SELECT count(*) FROM inventario WHERE id_producto = ? AND id_almacen = ? AND cantidad_fisica = 2.500",
				Integer.class, idProducto, idAlmacen);
		assertEquals(1, cantidad);
	}

	@Test
	void rechazaCombinacionProductoAlmacenDuplicada() {
		Long idProducto = insertarProducto("MAD-0002");
		Long idAlmacen = insertarAlmacen("Almacen 2");
		insertarInventario(idProducto, idAlmacen, "10");

		assertThrows(DataIntegrityViolationException.class,
				() -> insertarInventario(idProducto, idAlmacen, "5"));
	}

	@Test
	void rechazaCantidadFisicaNegativa() {
		Long idProducto = insertarProducto("MAD-0003");
		Long idAlmacen = insertarAlmacen("Almacen 3");

		assertThrows(DataIntegrityViolationException.class,
				() -> insertarInventario(idProducto, idAlmacen, "-0.001"));
	}

	@Test
	void rechazaProductoInexistente() {
		Long idAlmacen = insertarAlmacen("Almacen 4");

		assertThrows(DataIntegrityViolationException.class,
				() -> insertarInventario(999999L, idAlmacen, "1"));
	}

	private Long insertarProducto(String sku) {
		return jdbc.queryForObject("""
				INSERT INTO producto (sku, nombre_oficial, unidad_medida, precio_referencial, stock_minimo, id_categoria)
				VALUES (?, ?, 'metro', 4.00, 0, (SELECT id_categoria FROM categoria WHERE nombre = 'Bazar y varios'))
				RETURNING id_producto
				""", Long.class, sku, "Producto " + sku);
	}

	private Long insertarAlmacen(String nombre) {
		return jdbc.queryForObject("""
				INSERT INTO almacen (nombre, tipo_ubicacion)
				VALUES (?, 'almacen')
				RETURNING id_almacen
				""", Long.class, nombre);
	}

	private void insertarInventario(Long idProducto, Long idAlmacen, String cantidad) {
		jdbc.update("""
				INSERT INTO inventario (id_producto, id_almacen, cantidad_fisica)
				VALUES (?, ?, CAST(? AS numeric))
				""", idProducto, idAlmacen, cantidad);
	}

	private void limpiar() {
		jdbc.update("DELETE FROM inventario");
		jdbc.update("DELETE FROM catalogo_proveedor");
		jdbc.update("DELETE FROM producto");
		jdbc.update("DELETE FROM almacen");
	}

}
