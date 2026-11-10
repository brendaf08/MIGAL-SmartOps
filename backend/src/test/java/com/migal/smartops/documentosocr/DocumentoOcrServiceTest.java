package com.migal.smartops.documentosocr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import com.migal.smartops.TestcontainersConfiguration;
import com.migal.smartops.comun.errores.CodigoError;
import com.migal.smartops.comun.errores.ErrorDeNegocio;
import com.migal.smartops.documentosocr.model.DocumentoDigital;
import com.migal.smartops.documentosocr.model.EstadoProcesamiento;
import com.migal.smartops.documentosocr.model.TipoDocumento;
import com.migal.smartops.documentosocr.service.DocumentoOcrService;
import com.migal.smartops.documentosocr.service.ProveedorOcr;
import com.migal.smartops.seguridad.config.UsuarioAutenticado;
import com.migal.smartops.seguridad.model.RolUsuario;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * Verifica la carga, la consulta, el listado y el descarte de documentos con PostgreSQL real: restricción por rol, límite
 * de tamaño, formatos admitidos, atomicidad del encolado y eliminación lógica. La clase no es transaccional porque la
 * carga confirma su transacción antes de encolar el archivo.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class DocumentoOcrServiceTest {

	private static final Path CARPETA = Path.of("target/documentos-prueba");

	@Autowired
	private DocumentoOcrService documentos;
	@Autowired
	private JdbcTemplate jdbc;
	@MockitoSpyBean
	private ProveedorOcr proveedor;

	private DatosOcr datos;
	private UsuarioAutenticado almacenero;
	private UsuarioAutenticado vendedor;
	private UsuarioAutenticado administrador;

	@BeforeEach
	void preparar() {
		datos = new DatosOcr(jdbc);
		almacenero = datos.usuario(RolUsuario.ALMACENERO);
		vendedor = datos.usuario(RolUsuario.VENDEDOR);
		administrador = datos.usuario(RolUsuario.ADMINISTRADOR);
	}

	@AfterEach
	void limpiar() {
		datos.limpiar();
	}

	@Test
	void cargaRegistraElDocumentoPendienteYGuardaElArchivo() {
		DocumentoDigital documento = documentos.cargar(almacenero, TipoDocumento.DOCUMENTO_RECEPCION, "guia-0812.png",
				DatosOcr.PNG);

		assertEquals(EstadoProcesamiento.PENDIENTE, documento.getEstado());
		assertEquals("guia-0812.png", documento.getNombreArchivo());
		assertEquals(DatosOcr.PNG.length, documento.getTamanoBytes());
		assertTrue(Files.exists(CARPETA.toAbsolutePath().normalize().resolve(documento.getRutaArchivo())));
	}

	@Test
	void vendedorNoCargaDocumentosDeRecepcion() {
		ErrorDeNegocio error = assertThrows(ErrorDeNegocio.class, () -> documentos.cargar(vendedor,
				TipoDocumento.DOCUMENTO_RECEPCION, "guia.jpg", DatosOcr.JPG));

		assertEquals(CodigoError.PERMISO_INSUFICIENTE, error.codigo());
	}

	@Test
	void almaceneroNoCargaSolicitudesDeCliente() {
		ErrorDeNegocio error = assertThrows(ErrorDeNegocio.class, () -> documentos.cargar(almacenero,
				TipoDocumento.SOLICITUD_CLIENTE, "pedido.pdf", DatosOcr.PDF));

		assertEquals(CodigoError.PERMISO_INSUFICIENTE, error.codigo());
	}

	@Test
	void formatoNoAdmitidoResponseValidacionFallidaConElCampoArchivo() {
		ErrorDeNegocio error = assertThrows(ErrorDeNegocio.class, () -> documentos.cargar(vendedor,
				TipoDocumento.SOLICITUD_CLIENTE, "pedido.txt", "pedido de cemento".getBytes()));

		assertEquals(CodigoError.VALIDACION_FALLIDA, error.codigo());
		assertTrue(error.propiedades().containsKey("errores"));
	}

	@Test
	void archivoQueSuperaElLimiteResponseValidacionFallida() {
		byte[] excesivo = DatosOcr.pdfDeTamano(10 * 1024 * 1024 + 1);

		ErrorDeNegocio error = assertThrows(ErrorDeNegocio.class, () -> documentos.cargar(vendedor,
				TipoDocumento.SOLICITUD_CLIENTE, "pedido.pdf", excesivo));

		assertEquals(CodigoError.VALIDACION_FALLIDA, error.codigo());
	}

	@Test
	void archivoEnElLimiteExactoSeAcepta() {
		DocumentoDigital documento = documentos.cargar(vendedor, TipoDocumento.SOLICITUD_CLIENTE, "pedido.pdf",
				DatosOcr.pdfDeTamano(10 * 1024 * 1024));

		assertEquals(10L * 1024 * 1024, documento.getTamanoBytes());
	}

	@Test
	void encolarFallidoNoDejaDocumentoNiArchivo() {
		long antes = contarArchivos();
		doThrow(new ErrorDeNegocio(CodigoError.SERVICIO_NO_DISPONIBLE, "Sin servicio")).when(proveedor).encolar(any());

		ErrorDeNegocio error = assertThrows(ErrorDeNegocio.class, () -> documentos.cargar(almacenero,
				TipoDocumento.DOCUMENTO_RECEPCION, "guia.jpg", DatosOcr.JPG));

		assertEquals(CodigoError.SERVICIO_NO_DISPONIBLE, error.codigo());
		assertEquals(0, documentosDe(almacenero));
		assertEquals(antes, contarArchivos());
	}

	@Test
	void listadoMuestraSoloLosTiposQueElRolOpera() {
		documentos.cargar(almacenero, TipoDocumento.DOCUMENTO_RECEPCION, "guia.jpg", DatosOcr.JPG);
		documentos.cargar(vendedor, TipoDocumento.SOLICITUD_CLIENTE, "pedido.pdf", DatosOcr.PDF);

		long totalAlmacenero = documentos.listar(RolUsuario.ALMACENERO, null, null, null, null,
				PageRequest.of(0, 100)).getTotalElements();
		long totalVendedor = documentos.listar(RolUsuario.VENDEDOR, null, TipoDocumento.DOCUMENTO_RECEPCION, null, null,
				PageRequest.of(0, 100)).getTotalElements();

		assertFalse(documentos.listar(RolUsuario.ALMACENERO, null, null, null, null, PageRequest.of(0, 100))
				.getContent().isEmpty());
		assertEquals(0, totalVendedor);
		assertTrue(totalAlmacenero >= 1);
		assertTrue(documentos.listar(RolUsuario.ADMINISTRADOR, null, null, null, null, PageRequest.of(0, 100))
				.getTotalElements() >= 2);
	}

	@Test
	void consultaDeOtroTipoResponsePermisoInsuficiente() {
		DocumentoDigital documento = documentos.cargar(almacenero, TipoDocumento.DOCUMENTO_RECEPCION, "guia.jpg",
				DatosOcr.JPG);

		ErrorDeNegocio error = assertThrows(ErrorDeNegocio.class,
				() -> documentos.consultar(vendedor, documento.getId()));

		assertEquals(CodigoError.PERMISO_INSUFICIENTE, error.codigo());
	}

	@Test
	void consultaDeIdentificadorInexistenteResponseRecursoNoEncontrado() {
		ErrorDeNegocio error = assertThrows(ErrorDeNegocio.class,
				() -> documentos.consultar(administrador, 9_999_999_999L));

		assertEquals(CodigoError.RECURSO_NO_ENCONTRADO, error.codigo());
	}

	@Test
	void descarteEsLogicoYOcultaElDocumento() {
		DocumentoDigital documento = documentos.cargar(almacenero, TipoDocumento.DOCUMENTO_RECEPCION, "guia.jpg",
				DatosOcr.JPG);

		documentos.descartar(almacenero, documento.getId());

		assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM documento_digital WHERE id_documento = ? "
				+ "AND fecha_eliminacion IS NOT NULL", Integer.class, documento.getId()));
		ErrorDeNegocio error = assertThrows(ErrorDeNegocio.class,
				() -> documentos.consultar(administrador, documento.getId()));
		assertEquals(CodigoError.RECURSO_NO_ENCONTRADO, error.codigo());
		assertThrows(ErrorDeNegocio.class, () -> documentos.descartar(almacenero, documento.getId()));
	}

	private int documentosDe(UsuarioAutenticado actor) {
		return jdbc.queryForObject("SELECT count(*) FROM documento_digital WHERE id_usuario = ?", Integer.class, actor.id());
	}

	private static long contarArchivos() {
		Path carpeta = CARPETA.toAbsolutePath().normalize();
		if (!Files.isDirectory(carpeta)) {
			return 0;
		}
		try (Stream<Path> archivos = Files.list(carpeta)) {
			return archivos.count();
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}
}
