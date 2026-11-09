package com.migal.smartops;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Verifica la migración V10 (categorías jerárquicas de producto) con PostgreSQL real.
 * Las categorías sembradas son datos de referencia y no se eliminan; las pruebas solo eliminan
 * los productos y las categorías que crean.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class MigracionCategoriasTest {

	private static final int CATEGORIAS_PRIMER_NIVEL = 8;
	private static final int SUBCATEGORIAS = 24;
	private static final List<String> APROBADAS_AUTOMATICAMENTE = List.of(
			"Cemento y aglomerantes", "Agregados", "Ladrillos y bloques", "Fierro y acero de construcción");

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
	void eliminaLaColumnaLibreYAgregaIdCategoria() {
		Integer libre = jdbc.queryForObject("""
				SELECT count(*) FROM information_schema.columns
				WHERE table_schema = 'public' AND table_name = 'producto' AND column_name = 'categoria_producto'
				""", Integer.class);
		Integer identificador = jdbc.queryForObject("""
				SELECT count(*) FROM information_schema.columns
				WHERE table_schema = 'public' AND table_name = 'producto'
				  AND column_name = 'id_categoria' AND is_nullable = 'NO'
				""", Integer.class);

		assertEquals(0, libre);
		assertEquals(1, identificador);
	}

	@Test
	void sembraOchoCategoriasDePrimerNivel() {
		Integer cantidad = jdbc.queryForObject(
				"SELECT count(*) FROM categoria WHERE id_categoria_padre IS NULL", Integer.class);

		assertEquals(CATEGORIAS_PRIMER_NIVEL, cantidad);
	}

	@Test
	void sembraVeinticuatroSubcategoriasConPadre() {
		Integer cantidad = jdbc.queryForObject(
				"SELECT count(*) FROM categoria WHERE id_categoria_padre IS NOT NULL", Integer.class);
		Integer total = jdbc.queryForObject("SELECT count(*) FROM categoria", Integer.class);

		assertEquals(SUBCATEGORIAS, cantidad);
		assertEquals(CATEGORIAS_PRIMER_NIVEL + SUBCATEGORIAS, total);
	}

	@Test
	void vinculaCadaSubcategoriaASuPadreCorrecto() {
		String padre = jdbc.queryForObject("""
				SELECT p.nombre FROM categoria h
				JOIN categoria p ON p.id_categoria = h.id_categoria_padre
				WHERE h.nombre = 'Fierro y acero de construcción'
				""", String.class);

		assertEquals("Materiales de construcción", padre);
	}

	@Test
	void marcaSoloCuatroCategoriasConAprobacionAutomatica() {
		List<String> aprobadas = jdbc.queryForList("""
				SELECT nombre FROM categoria WHERE aprobacion_automatica ORDER BY nombre
				""", String.class);

		assertEquals(APROBADAS_AUTOMATICAMENTE.size(), aprobadas.size());
		assertTrue(aprobadas.containsAll(APROBADAS_AUTOMATICAMENTE));
	}

	@Test
	void rechazaNombreDuplicadoEnMismoPadre() {
		assertThrows(DataIntegrityViolationException.class,
				() -> insertarCategoria("Agregados", idCategoria("Materiales de construcción"), false));
	}

	@Test
	void rechazaNombreDuplicadoEnPrimerNivel() {
		assertThrows(DataIntegrityViolationException.class,
				() -> insertarCategoria("Bazar y varios", null, false));
	}

	@Test
	void permiteMismoNombreBajoPadresDistintos() {
		Long idCreada = insertarCategoria("Pinturas", idCategoria("Herramientas"), false);

		assertTrue(idCreada > 0);
	}

	@Test
	void rechazaCategoriaComoPropioPadre() {
		assertThrows(DataIntegrityViolationException.class,
				() -> jdbc.update("""
						UPDATE categoria SET id_categoria_padre = id_categoria WHERE nombre = 'Agregados'
						"""));
	}

	@Test
	void rechazaProductoSinCategoria() {
		assertThrows(DataIntegrityViolationException.class,
				() -> jdbc.update("""
						INSERT INTO producto (sku, nombre_oficial, unidad_medida, precio_referencial, stock_minimo)
						VALUES ('CAT-SIN-0001', 'Producto ficticio', 'unidad', 5.00, 0)
						"""));
	}

	@Test
	void rechazaProductoConCategoriaInexistente() {
		assertThrows(DataIntegrityViolationException.class,
				() -> jdbc.update("""
						INSERT INTO producto (sku, nombre_oficial, unidad_medida, precio_referencial, stock_minimo, id_categoria)
						VALUES ('CAT-INE-0001', 'Producto ficticio', 'unidad', 5.00, 0, 999999)
						"""));
	}

	@Test
	void aceptaProductoConCategoriaSembrada() {
		Integer cantidad = jdbc.update("""
				INSERT INTO producto (sku, nombre_oficial, unidad_medida, precio_referencial, stock_minimo, id_categoria)
				VALUES ('CAT-OK-0001', 'Producto ficticio', 'bolsa', 5.00, 0, ?)
				""", idCategoria("Agregados"));

		assertEquals(1, cantidad);
	}

	@Test
	void habilitaRlsEnCategoria() {
		Boolean rls = jdbc.queryForObject(
				"SELECT relrowsecurity FROM pg_class WHERE relname = 'categoria'", Boolean.class);

		assertTrue(rls);
	}

	private Long idCategoria(String nombre) {
		return jdbc.queryForObject("SELECT id_categoria FROM categoria WHERE nombre = ?", Long.class, nombre);
	}

	private Long insertarCategoria(String nombre, Long idPadre, boolean aprobacion) {
		return jdbc.queryForObject("""
				INSERT INTO categoria (nombre, id_categoria_padre, aprobacion_automatica)
				VALUES (?, ?, ?)
				RETURNING id_categoria
				""", Long.class, nombre, idPadre, aprobacion);
	}

	private void limpiar() {
		jdbc.update("DELETE FROM producto WHERE sku LIKE 'CAT-%'");
		jdbc.update("DELETE FROM categoria WHERE nombre = 'Pinturas' AND id_categoria_padre IS NOT NULL"
				+ " AND id_categoria_padre = (SELECT id_categoria FROM categoria WHERE nombre = 'Herramientas')");
	}

}
