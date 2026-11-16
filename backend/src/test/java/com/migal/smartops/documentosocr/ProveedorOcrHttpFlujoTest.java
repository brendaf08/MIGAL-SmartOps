package com.migal.smartops.documentosocr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.migal.smartops.TestcontainersConfiguration;
import com.migal.smartops.documentosocr.model.DocumentoDigital;
import com.migal.smartops.documentosocr.model.EstadoProcesamiento;
import com.migal.smartops.documentosocr.model.TipoDocumento;
import com.migal.smartops.documentosocr.service.DocumentoOcrService;
import com.migal.smartops.documentosocr.service.ProveedorOcr;
import com.migal.smartops.documentosocr.service.ProveedorOcrHttp;
import com.migal.smartops.seguridad.config.UsuarioAutenticado;
import com.migal.smartops.seguridad.model.RolUsuario;
import java.sql.Timestamp;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Verifica el proveedor http con PostgreSQL real y un servicio de OCR inalcanzable (ADR-012, puntos 3 y 4): el envío
 * ocurre después de confirmar la carga, un envío fallido deja el documento listo para reprocesarse, y la vigilancia
 * marca los pendientes vencidos sin tocar los demás.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = {"migal.ocr.proveedor=http", "migal.ocr.servicio.url=http://127.0.0.1:1",
		"migal.ocr.servicio.tiempo-conexion=PT1S"})
class ProveedorOcrHttpFlujoTest {

	@Autowired
	private DocumentoOcrService documentos;
	@Autowired
	private ProveedorOcr proveedor;
	@Autowired
	private JdbcTemplate jdbc;

	private DatosOcr datos;
	private UsuarioAutenticado almacenero;

	@BeforeEach
	void preparar() {
		datos = new DatosOcr(jdbc);
		almacenero = datos.usuario(RolUsuario.ALMACENERO);
	}

	@AfterEach
	void limpiar() {
		datos.limpiar();
	}

	@Test
	void usaElProveedorHttp() {
		assertTrue(proveedor instanceof ProveedorOcrHttp);
	}

	@Test
	void unEnvioFallidoRegistraServicioNoDisponibleYPermiteReprocesar() {
		DocumentoDigital cargado = documentos.cargar(almacenero, TipoDocumento.DOCUMENTO_RECEPCION, "guia.png",
				DatosOcr.PNG);

		DocumentoDigital documento = documentos.consultar(almacenero, cargado.getId());
		assertEquals(EstadoProcesamiento.PENDIENTE, documento.getEstado());
		assertEquals("SERVICIO_NO_DISPONIBLE", documento.getCodigoError());

		documentos.reprocesar(almacenero, cargado.getId());
		assertEquals("SERVICIO_NO_DISPONIBLE", documentos.consultar(almacenero, cargado.getId()).getCodigoError());
	}

	@Test
	void laVigilanciaMarcaSoloLosPendientesVencidosSinError() {
		long vencido = insertarPendiente(Instant.parse("2026-11-01T10:00:00Z"), null);
		long reciente = insertarPendiente(Instant.parse("2026-11-01T10:05:00Z"), null);
		long ilegible = insertarPendiente(Instant.parse("2026-11-01T10:00:00Z"), "DOCUMENTO_ILEGIBLE");

		documentos.vencerPendientes(Instant.parse("2026-11-01T10:01:00Z"));

		assertEquals("SERVICIO_NO_DISPONIBLE", codigoError(vencido));
		assertNull(codigoError(reciente));
		assertEquals("DOCUMENTO_ILEGIBLE", codigoError(ilegible));
	}

	@Test
	void unFalloDeEnvioNoAlteraUnDocumentoQueYaTieneResultado() {
		long conError = insertarPendiente(Instant.parse("2026-11-01T10:00:00Z"), "DOCUMENTO_ILEGIBLE");

		assertFalse(documentos.registrarServicioNoDisponible(conError));
		assertEquals("DOCUMENTO_ILEGIBLE", codigoError(conError));
	}

	private long insertarPendiente(Instant encolado, String codigoError) {
		return jdbc.queryForObject("""
				INSERT INTO documento_digital (ruta_archivo, fecha_subida, fecha_encolado, nombre_archivo, tamano_bytes,
				                               estado_procesamiento, tipo_documento, id_usuario, codigo_error)
				VALUES (?, ?, ?, 'guia.png', 12, 'pendiente', 'documento_recepcion', ?, ?)
				RETURNING id_documento
				""", Long.class, "inexistente-" + System.nanoTime() + ".png",
				Timestamp.from(Instant.parse("2026-11-01T09:00:00Z")), Timestamp.from(encolado),
				almacenero.id(), codigoError);
	}

	private String codigoError(long id) {
		return jdbc.queryForObject("SELECT codigo_error FROM documento_digital WHERE id_documento = ?", String.class, id);
	}
}
