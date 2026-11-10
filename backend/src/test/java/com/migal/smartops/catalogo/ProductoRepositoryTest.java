package com.migal.smartops.catalogo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.migal.smartops.TestcontainersConfiguration;
import com.migal.smartops.catalogo.model.Categoria;
import com.migal.smartops.catalogo.model.Producto;
import com.migal.smartops.catalogo.repository.CategoriaRepository;
import com.migal.smartops.catalogo.repository.ProductoRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

/** Verifica el mapeo de la entidad {@link Producto} y las consultas del repositorio con PostgreSQL real. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class ProductoRepositoryTest {

	private static final PageRequest PRIMERA_PAGINA = PageRequest.of(0, 100);

	@Autowired
	private ProductoRepository repositorio;

	@Autowired
	private CategoriaRepository categorias;

	@Autowired
	private EntityManager entityManager;

	private Categoria categoriaCemento() {
		return categorias.findByNombre("Cemento y aglomerantes").orElseThrow();
	}

	private Producto nuevo(String sku, String nombre, Categoria categoria) {
		return new Producto(sku, nombre, null, "bolsa", new BigDecimal("28.50"), new BigDecimal("20.000"),
				categoria, "Sol", "Bolsa 42.5 kg", true);
	}

	@Test
	void guardaUnProductoYLeeLosValoresGeneradosPorLaBaseDeDatos() {
		Producto guardado = repositorio.saveAndFlush(nuevo("PRB-0001", "Cemento de prueba", categoriaCemento()));

		assertNotNull(guardado.getId());
		assertNotNull(guardado.getFechaRegistro());
		assertTrue(guardado.isEstadoActivo());
		assertNull(guardado.getFechaActualizacion());
	}

	@Test
	void admiteUnaDescripcionNulaYRecuperaLaCategoriaComoEntidad() {
		Producto guardado = repositorio.saveAndFlush(nuevo("PRB-0002", "Sin descripcion", categoriaCemento()));
		entityManager.clear();

		Producto leido = repositorio.findById(guardado.getId()).orElseThrow();
		assertNull(leido.getDescripcion());
		assertEquals("Cemento y aglomerantes", leido.getCategoria().getNombre());
		assertEquals("Materiales de construcción", leido.getCategoria().getPadre().getNombre());
	}

	@Test
	void persisteImportesConDosDecimalesYCantidadesConTres() {
		Producto guardado = repositorio.saveAndFlush(nuevo("PRB-0003", "Importes", categoriaCemento()));
		entityManager.clear();

		Producto leido = repositorio.findById(guardado.getId()).orElseThrow();
		assertEquals(0, new BigDecimal("28.50").compareTo(leido.getPrecioReferencial()));
		assertEquals(0, new BigDecimal("20.000").compareTo(leido.getStockMinimo()));
	}

	@Test
	void buscaPorSkuExacto() {
		repositorio.saveAndFlush(nuevo("PRB-0004", "Buscable", categoriaCemento()));

		assertTrue(repositorio.findBySku("PRB-0004").isPresent());
		assertTrue(repositorio.existsBySku("PRB-0004"));
		assertFalse(repositorio.existsBySku("PRB-9999"));
	}

	@Test
	void rechazaUnSkuDuplicado() {
		repositorio.saveAndFlush(nuevo("PRB-0005", "Primero", categoriaCemento()));

		assertThrows(DataIntegrityViolationException.class,
				() -> repositorio.saveAndFlush(nuevo("PRB-0005", "Segundo", categoriaCemento())));
	}

	@Test
	void buscaPorTextoEnElSkuOEnElNombreSinDistinguirMayusculas() {
		Producto guardado = repositorio.saveAndFlush(nuevo("TXT-7788", "Cemento Prueba Texto", categoriaCemento()));

		List<Long> porNombre = repositorio
				.findBySkuContainingIgnoreCaseOrNombreOficialContainingIgnoreCase("zzz", "cemento prueba", PRIMERA_PAGINA)
				.map(Producto::getId).getContent();
		List<Long> porSku = repositorio
				.findBySkuContainingIgnoreCaseOrNombreOficialContainingIgnoreCase("txt-77", "zzz", PRIMERA_PAGINA)
				.map(Producto::getId).getContent();

		assertTrue(porNombre.contains(guardado.getId()));
		assertTrue(porSku.contains(guardado.getId()));
	}

	@Test
	void filtraPorCategoriaYVigencia() {
		Categoria cemento = categoriaCemento();
		Categoria agregados = categorias.findByNombre("Agregados").orElseThrow();
		Producto activo = repositorio.saveAndFlush(nuevo("CAT-0001", "Activo en cemento", cemento));
		Producto inactivo = repositorio.saveAndFlush(nuevo("CAT-0002", "Inactivo en cemento", cemento));
		inactivo.cambiarEstado(false, Instant.parse("2026-11-10T08:00:00Z"));
		repositorio.saveAndFlush(inactivo);
		repositorio.saveAndFlush(nuevo("CAT-0003", "Activo en agregados", agregados));

		List<Long> activos = repositorio
				.findByCategoriaIdAndEstadoActivo(cemento.getId(), true, PRIMERA_PAGINA).map(Producto::getId).getContent();
		List<Long> inactivos = repositorio
				.findByCategoriaIdAndEstadoActivo(cemento.getId(), false, PRIMERA_PAGINA).map(Producto::getId).getContent();

		assertTrue(activos.contains(activo.getId()));
		assertFalse(activos.contains(inactivo.getId()));
		assertTrue(inactivos.contains(inactivo.getId()));
		assertFalse(inactivos.contains(activo.getId()));
	}

	@Test
	void actualizaLosDatosSinModificarElSku() {
		Producto guardado = repositorio.saveAndFlush(nuevo("UPD-0001", "Antes", categoriaCemento()));
		Instant ahora = Instant.parse("2026-11-10T12:00:00Z");

		guardado.actualizar("Despues", "Nueva descripcion", "unidad", new BigDecimal("30.00"),
				new BigDecimal("25.000"), categorias.findByNombre("Pinturas").orElseThrow(), null, null, ahora);
		repositorio.saveAndFlush(guardado);
		entityManager.clear();

		Producto leido = repositorio.findById(guardado.getId()).orElseThrow();
		assertEquals("UPD-0001", leido.getSku());
		assertEquals("Despues", leido.getNombreOficial());
		assertEquals("Pinturas", leido.getCategoria().getNombre());
		assertEquals(ahora, leido.getFechaActualizacion());
	}
}
