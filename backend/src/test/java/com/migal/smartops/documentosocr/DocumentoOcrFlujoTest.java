package com.migal.smartops.documentosocr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;

import com.migal.smartops.TestcontainersConfiguration;
import com.migal.smartops.comun.errores.CodigoError;
import com.migal.smartops.comun.errores.ErrorDeNegocio;
import com.migal.smartops.documentosocr.model.DetalleExtraido;
import com.migal.smartops.documentosocr.model.DocumentoDigital;
import com.migal.smartops.documentosocr.model.EstadoProcesamiento;
import com.migal.smartops.documentosocr.model.TipoDocumento;
import com.migal.smartops.documentosocr.service.DocumentoOcrService;
import com.migal.smartops.documentosocr.service.DocumentoOcrService.CoincidenciaOcr;
import com.migal.smartops.documentosocr.service.DocumentoOcrService.CorreccionDetalle;
import com.migal.smartops.documentosocr.service.DocumentoOcrService.LineaOcr;
import com.migal.smartops.documentosocr.service.DocumentoOcrService.LineaPrecargada;
import com.migal.smartops.documentosocr.service.DocumentoOcrService.ResultadoOcr;
import com.migal.smartops.documentosocr.service.ProveedorOcr;
import com.migal.smartops.seguridad.config.UsuarioAutenticado;
import com.migal.smartops.seguridad.model.RolUsuario;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * Verifica el flujo de revisión de un documento con PostgreSQL real: coincidencias con el catálogo y umbrales, correcciones,
 * validación (con concurrencia y atomicidad), asociación y precarga para recepciones. El proveedor OCR se espía para
 * controlar cuándo un documento queda pendiente y cuándo se procesa.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class DocumentoOcrFlujoTest {

	@Autowired
	private DocumentoOcrService documentos;
	@Autowired
	private JdbcTemplate jdbc;
	@MockitoSpyBean
	private ProveedorOcr proveedor;

	private DatosOcr datos;
	private UsuarioAutenticado almacenero;
	private UsuarioAutenticado administrador;

	@BeforeEach
	void preparar() {
		datos = new DatosOcr(jdbc);
		almacenero = datos.usuario(RolUsuario.ALMACENERO);
		administrador = datos.usuario(RolUsuario.ADMINISTRADOR);
	}

	@AfterEach
	void limpiar() {
		datos.limpiar();
	}

	@Test
	void resultadoProcesadoProponeSoloCoincidenciasActivasSobreElUmbralBajoOrdenadas() {
		String skuAlto = DatosOcr.sku();
		String skuMedio = DatosOcr.sku();
		String skuInactivo = DatosOcr.sku();
		datos.producto(skuAlto, true);
		datos.producto(skuMedio, true);
		datos.producto(skuInactivo, false);
		DocumentoDigital documento = pendiente(almacenero);

		DocumentoDigital procesado = documentos.registrarResultados(documento.getId(), new ResultadoOcr(false, List.of(
				new LineaOcr("CEMENTO SOL 42.5KG", new BigDecimal("100"), List.of(
						new CoincidenciaOcr(skuMedio, new BigDecimal("0.700")),
						new CoincidenciaOcr(skuInactivo, new BigDecimal("0.990")),
						new CoincidenciaOcr(DatosOcr.sku(), new BigDecimal("0.960")),
						new CoincidenciaOcr(skuAlto, new BigDecimal("0.920")),
						new CoincidenciaOcr(skuAlto, new BigDecimal("0.300")))))));

		assertEquals(EstadoProcesamiento.PROCESADO_OCR, procesado.getEstado());
		DetalleExtraido linea = procesado.getDetalles().get(0);
		assertEquals(skuAlto, linea.getSkuSugerido());
		assertEquals(0, new BigDecimal("0.920").compareTo(linea.getConfianza()));
		assertEquals(2, linea.getCoincidencias().size());
		assertEquals(skuAlto, linea.getCoincidencias().get(0).getProducto().getSku());
		assertEquals(1, linea.getCoincidencias().get(0).getPosicion());
		assertEquals(skuMedio, linea.getCoincidencias().get(1).getProducto().getSku());
		assertTrue(linea.isRequiereRevision());
	}

	@Test
	void resultadoIlegibleConservaPendienteYRegistraElErrorParaReprocesar() {
		DocumentoDigital documento = pendiente(almacenero);

		DocumentoDigital ilegible = documentos.registrarResultados(documento.getId(), new ResultadoOcr(true, List.of()));

		assertEquals(EstadoProcesamiento.PENDIENTE, ilegible.getEstado());
		assertEquals("DOCUMENTO_ILEGIBLE", ilegible.getCodigoError());
		documentos.reprocesar(almacenero, documento.getId());
		assertNull(documentos.consultar(almacenero, documento.getId()).getCodigoError());
		ErrorDeNegocio sinError = assertThrows(ErrorDeNegocio.class,
				() -> documentos.reprocesar(almacenero, documento.getId()));
		assertEquals(CodigoError.TRANSICION_NO_PERMITIDA, sinError.codigo());
	}

	@Test
	void resultadoConUnaLineaInvalidaNoRegistraNinguna() {
		DocumentoDigital documento = pendiente(almacenero);

		ErrorDeNegocio error = assertThrows(ErrorDeNegocio.class, () -> documentos.registrarResultados(documento.getId(),
				new ResultadoOcr(false, List.of(new LineaOcr("CEMENTO", BigDecimal.ONE, List.of()),
						new LineaOcr("   ", BigDecimal.ONE, List.of())))));

		assertEquals(CodigoError.VALIDACION_FALLIDA, error.codigo());
		assertEquals(0, lineas(documento.getId()));
		assertEquals(EstadoProcesamiento.PENDIENTE, documentos.consultar(almacenero, documento.getId()).getEstado());
	}

	@Test
	void correccionAplicaSoloLosCamposInformadosYValidaElProducto() {
		String sku = DatosOcr.sku();
		long producto = datos.producto(sku, true);
		long inactivo = datos.producto(DatosOcr.sku(), false);
		DocumentoDigital documento = procesado(almacenero, "CEM");
		long linea = documento.getDetalles().get(0).getId();

		DetalleExtraido corregida = documentos.corregirDetalle(almacenero, documento.getId(), linea,
				new CorreccionDetalle(true, "CEMENTO SOL", true, producto, true, new BigDecimal("25")));

		assertEquals("CEMENTO SOL", corregida.getTextoCorregido());
		assertEquals(sku, corregida.getProducto().getSku());
		assertTrue(corregida.isRequiereRevision(), "Sin coincidencia previa, la línea sigue requiriendo revisión");
		assertEquals(0, new BigDecimal("25").compareTo(corregida.getCantidadCorregida()));

		ErrorDeNegocio desactivado = assertThrows(ErrorDeNegocio.class, () -> documentos.corregirDetalle(almacenero,
				documento.getId(), linea, new CorreccionDetalle(false, null, true, inactivo, false, null)));
		assertEquals(CodigoError.PRODUCTO_INACTIVO, desactivado.codigo());
		ErrorDeNegocio inexistente = assertThrows(ErrorDeNegocio.class, () -> documentos.corregirDetalle(almacenero,
				documento.getId(), linea, new CorreccionDetalle(false, null, true, 999_999_999L, false, null)));
		assertEquals(CodigoError.VALIDACION_FALLIDA, inexistente.codigo());
		ErrorDeNegocio cero = assertThrows(ErrorDeNegocio.class, () -> documentos.corregirDetalle(almacenero,
				documento.getId(), linea, new CorreccionDetalle(false, null, false, null, true, BigDecimal.ZERO)));
		assertEquals(CodigoError.VALIDACION_FALLIDA, cero.codigo());
	}

	@Test
	void validarExigeProductoEnCadaLineaYEsAtomica() {
		long producto = datos.producto(DatosOcr.sku(), true);
		DocumentoDigital documento = procesado(almacenero, "CEM", "ARENA");
		long primera = documento.getDetalles().get(0).getId();
		documentos.corregirDetalle(almacenero, documento.getId(), primera,
				new CorreccionDetalle(false, null, true, producto, false, null));

		ErrorDeNegocio error = assertThrows(ErrorDeNegocio.class, () -> documentos.validar(almacenero, documento.getId()));

		assertEquals(CodigoError.VALIDACION_FALLIDA, error.codigo());
		assertEquals(1, ((List<?>) error.propiedades().get("errores")).size());
		assertEquals(EstadoProcesamiento.PROCESADO_OCR, documentos.consultar(almacenero, documento.getId()).getEstado());

		documentos.descartarDetalle(almacenero, documento.getId(), documento.getDetalles().get(1).getId());
		assertEquals(EstadoProcesamiento.VALIDADO, documentos.validar(almacenero, documento.getId()).getEstado());
		ErrorDeNegocio repetida = assertThrows(ErrorDeNegocio.class, () -> documentos.validar(almacenero, documento.getId()));
		assertEquals(CodigoError.DOCUMENTO_NO_VALIDABLE, repetida.codigo());
	}

	@Test
	void validacionSimultaneaDelMismoDocumentoConfirmaUnaSola() throws Exception {
		long producto = datos.producto(DatosOcr.sku(), true);
		DocumentoDigital documento = procesado(almacenero, "CEM");
		long linea = documento.getDetalles().get(0).getId();
		documentos.corregirDetalle(almacenero, documento.getId(), linea,
				new CorreccionDetalle(false, null, true, producto, false, null));
		CountDownLatch inicio = new CountDownLatch(1);
		ExecutorService hilos = Executors.newFixedThreadPool(2);
		try {
			Callable<String> validar = () -> {
				inicio.await();
				try {
					documentos.validar(administrador, documento.getId());
					return "OK";
				} catch (ErrorDeNegocio e) {
					return e.codigo().name();
				}
			};
			Future<String> primera = hilos.submit(validar);
			Future<String> segunda = hilos.submit(validar);
			inicio.countDown();

			List<String> resultados = List.of(primera.get(), segunda.get());
			assertEquals(1, resultados.stream().filter("OK"::equals).count());
			assertTrue(resultados.contains("DOCUMENTO_NO_VALIDABLE"));
		} finally {
			hilos.shutdownNow();
		}
	}

	@Test
	void asociacionRechazaDocumentoNoValidadoYTipoIncorrecto() {
		DocumentoDigital documento = pendiente(almacenero);

		ErrorDeNegocio noValidado = assertThrows(ErrorDeNegocio.class,
				() -> documentos.asociar(almacenero, documento.getId(), null, 1L));
		assertEquals(CodigoError.TRANSICION_NO_PERMITIDA, noValidado.codigo());
		ErrorDeNegocio ambas = assertThrows(ErrorDeNegocio.class,
				() -> documentos.asociar(almacenero, documento.getId(), 1L, 1L));
		assertEquals(CodigoError.VALIDACION_FALLIDA, ambas.codigo());
	}

	@Test
	void precargaUsaCantidadCorregidaYOmiteLasLineasDescartadas() {
		long producto = datos.producto(DatosOcr.sku(), true);
		long otro = datos.producto(DatosOcr.sku(), true);
		DocumentoDigital documento = procesado(almacenero, "CEM", "ARENA", "GRAVA");
		List<DetalleExtraido> lineas = documento.getDetalles();
		documentos.corregirDetalle(almacenero, documento.getId(), lineas.get(0).getId(),
				new CorreccionDetalle(false, null, true, producto, true, new BigDecimal("5")));
		documentos.corregirDetalle(almacenero, documento.getId(), lineas.get(1).getId(),
				new CorreccionDetalle(false, null, true, otro, false, null));
		documentos.descartarDetalle(almacenero, documento.getId(), lineas.get(1).getId());
		documentos.corregirDetalle(almacenero, documento.getId(), lineas.get(2).getId(),
				new CorreccionDetalle(false, null, true, otro, false, null));
		documentos.validar(almacenero, documento.getId());

		List<LineaPrecargada> precarga = documentos.precargaRecepcion(documento.getId());

		assertEquals(2, precarga.size());
		assertEquals(producto, precarga.get(0).productoId());
		assertEquals(0, new BigDecimal("5").compareTo(precarga.get(0).cantidad()));
		assertEquals(otro, precarga.get(1).productoId());
		assertEquals(0, new BigDecimal("10.000").compareTo(precarga.get(1).cantidad()));
	}

	/** Documento pendiente que no se procesa automáticamente, para controlar cada transición. */
	private DocumentoDigital pendiente(UsuarioAutenticado actor) {
		doNothing().when(proveedor).encolar(any());
		return documentos.cargar(actor, TipoDocumento.DOCUMENTO_RECEPCION, "guia.jpg", DatosOcr.JPG);
	}

	/** Documento procesado con una línea por nombre de producto leído, sin coincidencias. */
	private DocumentoDigital procesado(UsuarioAutenticado actor, String... textos) {
		DocumentoDigital documento = pendiente(actor);
		List<LineaOcr> lineas = Arrays.stream(textos)
				.map(t -> new LineaOcr(t, new BigDecimal("10"), List.of())).toList();
		return documentos.registrarResultados(documento.getId(), new ResultadoOcr(false, lineas));
	}

	private int lineas(long idDocumento) {
		return jdbc.queryForObject("SELECT count(*) FROM detalle_extraido WHERE id_documento = ?", Integer.class, idDocumento);
	}
}
