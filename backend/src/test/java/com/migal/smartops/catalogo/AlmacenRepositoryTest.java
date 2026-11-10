package com.migal.smartops.catalogo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.migal.smartops.TestcontainersConfiguration;
import com.migal.smartops.catalogo.model.Almacen;
import com.migal.smartops.catalogo.repository.AlmacenRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

/** Verifica el mapeo de la entidad {@link Almacen} con PostgreSQL real. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class AlmacenRepositoryTest {

	@Autowired
	private AlmacenRepository repositorio;

	@Autowired
	private EntityManager entityManager;

	@Test
	void guardaUnAlmacenConEstadoActivoPorDefecto() {
		Almacen guardado = repositorio.saveAndFlush(new Almacen("Almacen de prueba", "almacen", null));

		assertNotNull(guardado.getId());
		assertTrue(guardado.isEstadoActivo());
		assertNull(guardado.getDireccionReferencia());
	}

	@Test
	void desactivaElAlmacenSinEliminarlo() {
		Almacen guardado = repositorio.saveAndFlush(new Almacen("Almacen inactivo", "almacen", "Av. de prueba 100"));
		long cantidadAntes = repositorio.count();

		guardado.cambiarEstado(false);
		repositorio.saveAndFlush(guardado);
		entityManager.clear();

		Almacen leido = repositorio.findById(guardado.getId()).orElseThrow();
		assertFalse(leido.isEstadoActivo());
		assertEquals("Av. de prueba 100", leido.getDireccionReferencia());
		assertEquals(cantidadAntes, repositorio.count());
	}
}
