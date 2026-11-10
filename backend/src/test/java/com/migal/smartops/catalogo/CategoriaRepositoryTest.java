package com.migal.smartops.catalogo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.migal.smartops.TestcontainersConfiguration;
import com.migal.smartops.catalogo.model.Categoria;
import com.migal.smartops.catalogo.repository.CategoriaRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verifica el mapeo de la autorreferencia de {@link Categoria} con las categorías sembradas por V10. Las
 * pruebas no modifican las categorías sembradas; las filas que crean se revierten al final de cada prueba.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class CategoriaRepositoryTest {

	@Autowired
	private CategoriaRepository repositorio;

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	void leeUnaSubcategoriaConSuPadreYSuIndicadorDeAprobacion() {
		Categoria cemento = repositorio.findByNombre("Cemento y aglomerantes").orElseThrow();

		assertNotNull(cemento.getPadre());
		assertEquals("Materiales de construcción", cemento.getPadre().getNombre());
		assertTrue(cemento.isAprobacionAutomatica());
		assertTrue(cemento.isEstadoActivo());
	}

	@Test
	void leeUnaCategoriaDePrimerNivelSinPadre() {
		Categoria herramientas = repositorio.findByNombre("Herramientas").orElseThrow();

		assertNull(herramientas.getPadre());
		assertFalse(herramientas.isAprobacionAutomatica());
	}

	@Test
	void guardaUnaSubcategoriaBajoUnPadreExistente() {
		Categoria padre = repositorio.findByNombre("Bazar y varios").orElseThrow();

		Categoria guardada = repositorio.saveAndFlush(new Categoria("Subcategoria de prueba", padre, false));
		entityManager.clear();

		Categoria leida = repositorio.findById(guardada.getId()).orElseThrow();
		assertEquals("Bazar y varios", leida.getPadre().getNombre());
	}

	@Test
	void rechazaUnaCategoriaQueSeDeclaraPropioPadre() {
		Categoria guardada = repositorio.saveAndFlush(new Categoria("Autorreferencia de prueba", null, false));

		assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
				"UPDATE categoria SET id_categoria_padre = id_categoria WHERE id_categoria = ?", guardada.getId()));
	}
}
