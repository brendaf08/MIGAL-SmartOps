package com.migal.smartops.catalogo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.migal.smartops.catalogo.model.Categoria;
import com.migal.smartops.catalogo.model.Producto;
import com.migal.smartops.catalogo.repository.ProductoRepository;
import com.migal.smartops.catalogo.service.CategoriaService;
import com.migal.smartops.catalogo.service.ProductoService;
import com.migal.smartops.catalogo.service.ProductoService.DatosProducto;
import com.migal.smartops.comun.errores.CodigoError;
import com.migal.smartops.comun.errores.ErrorDeNegocio;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

/** Verifica las reglas de administración del catálogo de productos (CU-03) sin acceso a la base de datos. */
class ProductoServiceTest {

	private static final Instant AHORA = Instant.parse("2026-11-10T08:00:00Z");
	private static final long ID_CEMENTO = 3L;

	private final ProductoRepository productos = mock(ProductoRepository.class);
	private final CategoriaService categorias = mock(CategoriaService.class);
	private final ProductoService servicio =
			new ProductoService(productos, categorias, Clock.fixed(AHORA, ZoneOffset.UTC));

	private Categoria categoriaCemento() {
		Categoria categoria = new Categoria("Cemento y aglomerantes", null, true);
		ReflectionTestUtils.setField(categoria, "id", ID_CEMENTO);
		return categoria;
	}

	private Producto existente(long id, String sku) {
		Producto producto = new Producto(sku, "Cemento Sol", null, "bolsa", new BigDecimal("28.50"),
				new BigDecimal("20.000"), categoriaCemento(), "Sol", null, true);
		ReflectionTestUtils.setField(producto, "id", id);
		when(productos.findById(id)).thenReturn(Optional.of(producto));
		when(productos.save(any(Producto.class))).thenAnswer(i -> i.getArgument(0));
		return producto;
	}

	private DatosProducto datos(String precio, String stock, long idCategoria) {
		return new DatosProducto("Cemento Sol tipo I 42.5 kg", "Cemento de uso general.", "bolsa",
				new BigDecimal(precio), new BigDecimal(stock), idCategoria, "Sol", "Bolsa 42.5 kg");
	}

	@Test
	void registraUnProductoActivoConImportesRedondeadosYSinFechaDeActualizacion() {
		when(productos.existsBySku("CEM-049")).thenReturn(false);
		when(categorias.validarExistencia(ID_CEMENTO)).thenReturn(categoriaCemento());
		when(productos.saveAndFlush(any(Producto.class))).thenAnswer(i -> i.getArgument(0));

		Producto registrado = servicio.registrar("CEM-049", datos("28.456", "20", ID_CEMENTO), true);

		assertEquals(0, new BigDecimal("28.46").compareTo(registrado.getPrecioReferencial()));
		assertEquals(0, new BigDecimal("20.000").compareTo(registrado.getStockMinimo()));
		assertTrue(registrado.isEstadoActivo());
		assertNull(registrado.getFechaActualizacion());
		assertEquals("Cemento y aglomerantes", registrado.getCategoria().getNombre());
	}

	@Test
	void registraUnProductoInactivoCuandoSeIndicaLaVigencia() {
		when(productos.existsBySku("CEM-050")).thenReturn(false);
		when(categorias.validarExistencia(ID_CEMENTO)).thenReturn(categoriaCemento());
		when(productos.saveAndFlush(any(Producto.class))).thenAnswer(i -> i.getArgument(0));

		assertFalse(servicio.registrar("CEM-050", datos("10", "1", ID_CEMENTO), false).isEstadoActivo());
	}

	@Test
	void rechazaUnSkuExistenteSinGuardarElProducto() {
		when(productos.existsBySku("CEM-049")).thenReturn(true);

		ErrorDeNegocio error = assertThrows(ErrorDeNegocio.class,
				() -> servicio.registrar("CEM-049", datos("28.5", "20", ID_CEMENTO), true));

		assertEquals(CodigoError.SKU_DUPLICADO, error.codigo());
		verify(productos, never()).saveAndFlush(any());
	}

	@Test
	void traduceLaViolacionDeUnicidadDeLaBaseDeDatosASkuDuplicado() {
		when(productos.existsBySku("CEM-051")).thenReturn(false);
		when(categorias.validarExistencia(ID_CEMENTO)).thenReturn(categoriaCemento());
		when(productos.saveAndFlush(any(Producto.class)))
				.thenThrow(new DataIntegrityViolationException("uq_producto_sku"));

		assertEquals(CodigoError.SKU_DUPLICADO,
				assertThrows(ErrorDeNegocio.class,
						() -> servicio.registrar("CEM-051", datos("28.5", "20", ID_CEMENTO), true)).codigo());
	}

	@Test
	void rechazaUnaCategoriaInexistenteConValidacionFallida() {
		when(productos.existsBySku("CEM-052")).thenReturn(false);
		when(categorias.validarExistencia(99L))
				.thenThrow(new ErrorDeNegocio(CodigoError.CATEGORIA_NO_ENCONTRADA, "La categoría indicada no existe."));

		ErrorDeNegocio error = assertThrows(ErrorDeNegocio.class,
				() -> servicio.registrar("CEM-052", datos("28.5", "20", 99L), true));

		assertEquals(CodigoError.VALIDACION_FALLIDA, error.codigo());
		verify(productos, never()).saveAndFlush(any());
	}

	@Test
	void rechazaUnPrecioOUnStockNegativos() {
		assertEquals(CodigoError.VALIDACION_FALLIDA,
				assertThrows(ErrorDeNegocio.class,
						() -> servicio.registrar("CEM-053", datos("-1", "20", ID_CEMENTO), true)).codigo());
		assertEquals(CodigoError.VALIDACION_FALLIDA,
				assertThrows(ErrorDeNegocio.class,
						() -> servicio.registrar("CEM-053", datos("1", "-0.001", ID_CEMENTO), true)).codigo());
		verify(productos, never()).saveAndFlush(any());
	}

	@Test
	void actualizaLosDatosSinModificarElSku() {
		Producto producto = existente(7L, "CEM-049");
		when(categorias.validarExistencia(ID_CEMENTO)).thenReturn(categoriaCemento());

		Producto actualizado = servicio.actualizar(7L, datos("29", "25", ID_CEMENTO));

		assertEquals("CEM-049", actualizado.getSku());
		assertEquals("Cemento Sol tipo I 42.5 kg", actualizado.getNombreOficial());
		assertEquals(0, new BigDecimal("29.00").compareTo(actualizado.getPrecioReferencial()));
		assertEquals(AHORA, producto.getFechaActualizacion());
	}

	@Test
	void actualizarUnProductoInexistenteResponde404() {
		when(productos.findById(99L)).thenReturn(Optional.empty());

		assertEquals(CodigoError.RECURSO_NO_ENCONTRADO,
				assertThrows(ErrorDeNegocio.class,
						() -> servicio.actualizar(99L, datos("29", "25", ID_CEMENTO))).codigo());
		verify(categorias, never()).validarExistencia(anyLong());
	}

	@Test
	void desactivaUnProductoYRegistraLaFechaDeModificacion() {
		Producto producto = existente(8L, "CEM-060");

		Producto resultado = servicio.cambiarEstado(8L, false);

		assertFalse(resultado.isEstadoActivo());
		assertEquals(AHORA, producto.getFechaActualizacion());
	}

	@Test
	void consultarUnProductoInexistenteResponde404() {
		when(productos.findById(99L)).thenReturn(Optional.empty());

		assertEquals(CodigoError.RECURSO_NO_ENCONTRADO,
				assertThrows(ErrorDeNegocio.class, () -> servicio.consultar(99L)).codigo());
	}

	@Test
	void listaLaPaginaQueDevuelveElRepositorio() {
		Pageable paginacion = PageRequest.of(0, 20);
		Producto producto = existente(9L, "CEM-070");
		when(productos.findAll(any(Specification.class), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(producto), paginacion, 1));

		var pagina = servicio.listar("cem", "CEM-070", ID_CEMENTO, true, paginacion);

		assertEquals(1, pagina.getTotalElements());
		assertEquals("CEM-070", pagina.getContent().get(0).getSku());
	}
}
