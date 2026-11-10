package com.migal.smartops;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Verifica la migración V13 (documento_digital, detalle_extraido y coincidencia_detalle) con PostgreSQL real.
 * Los datos de prueba utilizan los prefijos v13 (usuarios) y V13- (productos) y se eliminan en cada prueba,
 * en orden inverso a las claves foráneas.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class MigracionOcrDetalleTest {

	private static final Instant SUBIDA = Instant.parse("2026-11-01T10:00:00Z");

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
	void existeLaTablaCoincidenciaDetalle() {
		Integer cantidad = jdbc.queryForObject("""
				SELECT count(*) FROM information_schema.tables
				WHERE table_schema = 'public' AND table_name = 'coincidencia_detalle'
				""", Integer.class);
		assertEquals(1, cantidad);
	}

	// Documento digital: nombre, tamaño, tipo, código de error y eliminación lógica

	@Test
	void aceptaDocumentoConNombreYTamanoValidos() {
		Long idUsuario = crearUsuario("v13-doc-1");

		assertTrue(insertarDocumento(idUsuario, "cotizacion-cliente.pdf", 2048L, "solicitud_cliente", "pendiente",
				null, null) > 0);
	}

	@Test
	void aceptaDocumentoDeRecepcionProcesado() {
		Long idUsuario = crearUsuario("v13-doc-2");

		assertTrue(insertarDocumento(idUsuario, "factura-proveedor.png", 512L, "documento_recepcion", "procesado_ocr",
				null, null) > 0);
	}

	@Test
	void rechazaDocumentoSinNombreDeArchivo() {
		Long idUsuario = crearUsuario("v13-doc-3");

		assertThrows(DataIntegrityViolationException.class,
				() -> insertarDocumento(idUsuario, null, 2048L, "solicitud_cliente", "pendiente", null, null));
	}

	@Test
	void rechazaDocumentoConNombreFormadoSoloPorEspacios() {
		Long idUsuario = crearUsuario("v13-doc-4");

		assertThrows(DataIntegrityViolationException.class,
				() -> insertarDocumento(idUsuario, "   ", 2048L, "solicitud_cliente", "pendiente", null, null));
	}

	@Test
	void rechazaDocumentoSinTamano() {
		Long idUsuario = crearUsuario("v13-doc-5");

		assertThrows(DataIntegrityViolationException.class,
				() -> insertarDocumento(idUsuario, "sin-tamano.pdf", null, "solicitud_cliente", "pendiente", null, null));
	}

	@Test
	void rechazaDocumentoConTamanoCero() {
		Long idUsuario = crearUsuario("v13-doc-6");

		assertThrows(DataIntegrityViolationException.class,
				() -> insertarDocumento(idUsuario, "vacio.pdf", 0L, "solicitud_cliente", "pendiente", null, null));
	}

	@Test
	void rechazaDocumentoConTamanoNegativo() {
		Long idUsuario = crearUsuario("v13-doc-7");

		assertThrows(DataIntegrityViolationException.class,
				() -> insertarDocumento(idUsuario, "negativo.pdf", -1L, "solicitud_cliente", "pendiente", null, null));
	}

	@Test
	void rechazaTipoDocumentoFueraDelDominio() {
		Long idUsuario = crearUsuario("v13-doc-8");

		assertThrows(DataIntegrityViolationException.class,
				() -> insertarDocumento(idUsuario, "anterior.pdf", 2048L, "SolicitudCliente", "pendiente", null, null));
	}

	@Test
	void aceptaCodigoErrorDocumentoIlegibleConDocumentoPendiente() {
		Long idUsuario = crearUsuario("v13-doc-9");

		assertTrue(insertarDocumento(idUsuario, "borroso.jpg", 4096L, "solicitud_cliente", "pendiente",
				"DOCUMENTO_ILEGIBLE", null) > 0);
	}

	@Test
	void aceptaCodigoErrorServicioNoDisponibleConDocumentoPendiente() {
		Long idUsuario = crearUsuario("v13-doc-10");

		assertTrue(insertarDocumento(idUsuario, "reintento.pdf", 4096L, "solicitud_cliente", "pendiente",
				"SERVICIO_NO_DISPONIBLE", null) > 0);
	}

	@Test
	void rechazaCodigoErrorFueraDelCatalogoDelProcesamiento() {
		Long idUsuario = crearUsuario("v13-doc-11");

		assertThrows(DataIntegrityViolationException.class,
				() -> insertarDocumento(idUsuario, "error.pdf", 4096L, "solicitud_cliente", "pendiente",
						"ERROR_DESCONOCIDO", null));
	}

	@Test
	void rechazaCodigoErrorConDocumentoProcesadoOValidado() {
		Long idUsuario = crearUsuario("v13-doc-12");

		assertThrows(DataIntegrityViolationException.class,
				() -> insertarDocumento(idUsuario, "procesado.pdf", 4096L, "solicitud_cliente", "procesado_ocr",
						"DOCUMENTO_ILEGIBLE", null));
	}

	@Test
	void aceptaEliminacionLogicaPosteriorALaSubida() {
		Long idUsuario = crearUsuario("v13-doc-13");

		assertTrue(insertarDocumento(idUsuario, "eliminado.pdf", 2048L, "solicitud_cliente", "pendiente", null,
				Instant.parse("2026-11-02T10:00:00Z")) > 0);
	}

	@Test
	void rechazaEliminacionLogicaAnteriorALaSubida() {
		Long idUsuario = crearUsuario("v13-doc-14");

		assertThrows(DataIntegrityViolationException.class,
				() -> insertarDocumento(idUsuario, "anterior-subida.pdf", 2048L, "solicitud_cliente", "pendiente",
						null, Instant.parse("2026-10-31T10:00:00Z")));
	}

	@Test
	void eliminacionLogicaConservaLaFilaYLaExcluyeDeLosDocumentosVigentes() {
		Long idUsuario = crearUsuario("v13-doc-15");
		Long idDocumento = insertarDocumento(idUsuario, "vigente.pdf", 2048L, "solicitud_cliente", "pendiente", null,
				null);

		jdbc.update("UPDATE documento_digital SET fecha_eliminacion = ? WHERE id_documento = ?",
				Timestamp.from(Instant.parse("2026-11-02T10:00:00Z")), idDocumento);

		assertEquals(1, contarDocumentos(idUsuario));
		assertEquals(0, contarDocumentosVigentes(idUsuario));
	}

	@Test
	void existeIndiceParcialDeDocumentosVigentes() {
		String definicion = jdbc.queryForObject("""
				SELECT indexdef FROM pg_indexes
				WHERE schemaname = 'public' AND indexname = 'ix_documento_digital_usuario_activos'
				""", String.class);

		assertTrue(definicion.contains("WHERE (fecha_eliminacion IS NULL)"));
	}

	// Detalle extraído: sugerencia, confianza, revisión y descarte

	@Test
	void aplicaValoresInicialesDeRevisionYDescarteAlDetalle() {
		Long idDocumento = insertarDocumentoVigente("v13-det-1");

		Long idDetalle = insertarDetalle(idDocumento, "CEM SOL 42.5", null, null);

		Boolean requiereRevision = jdbc.queryForObject(
				"SELECT requiere_revision FROM detalle_extraido WHERE id_detalle_extraido = ?", Boolean.class, idDetalle);
		Boolean descartado = jdbc.queryForObject(
				"SELECT descartado FROM detalle_extraido WHERE id_detalle_extraido = ?", Boolean.class, idDetalle);
		assertFalse(requiereRevision);
		assertFalse(descartado);
	}

	@Test
	void aceptaDetalleConSkuSugeridoYConfianza() {
		Long idDocumento = insertarDocumentoVigente("v13-det-2");

		Long idDetalle = insertarDetalle(idDocumento, "CEM SOL 42.5", "CEM-001", new BigDecimal("0.876"));

		assertTrue(idDetalle > 0);
	}

	@Test
	void aceptaConfianzaEnLosLimitesDelRango() {
		Long idDocumento = insertarDocumentoVigente("v13-det-3");

		insertarDetalle(idDocumento, "minimo", null, new BigDecimal("0.000"));
		insertarDetalle(idDocumento, "maximo", null, new BigDecimal("1.000"));

		assertEquals(2, contarDetalles(idDocumento));
	}

	@Test
	void rechazaConfianzaMayorQueUno() {
		Long idDocumento = insertarDocumentoVigente("v13-det-4");

		assertThrows(DataIntegrityViolationException.class,
				() -> insertarDetalle(idDocumento, "texto", null, new BigDecimal("1.001")));
	}

	@Test
	void rechazaConfianzaNegativa() {
		Long idDocumento = insertarDocumentoVigente("v13-det-5");

		assertThrows(DataIntegrityViolationException.class,
				() -> insertarDetalle(idDocumento, "texto", null, new BigDecimal("-0.001")));
	}

	@Test
	void rechazaSkuSugeridoFormadoSoloPorEspacios() {
		Long idDocumento = insertarDocumentoVigente("v13-det-6");

		assertThrows(DataIntegrityViolationException.class,
				() -> insertarDetalle(idDocumento, "texto", "   ", null));
	}

	@Test
	void aceptaCantidadLeidaCero() {
		Long idDocumento = insertarDocumentoVigente("v13-det-7");

		jdbc.update("INSERT INTO detalle_extraido (id_documento, texto_crudo, cantidad_leida) VALUES (?, 'texto', 0.000)",
				idDocumento);

		assertEquals(1, contarDetalles(idDocumento));
	}

	@Test
	void rechazaCantidadLeidaNegativa() {
		Long idDocumento = insertarDocumentoVigente("v13-det-8");

		assertThrows(DataIntegrityViolationException.class,
				() -> jdbc.update("INSERT INTO detalle_extraido (id_documento, texto_crudo, cantidad_leida) VALUES (?, 'texto', -1.000)",
						idDocumento));
	}

	@Test
	void aceptaCantidadCorregidaDistintaDeLaCantidadLeida() {
		Long idDocumento = insertarDocumentoVigente("v13-det-9");

		Long idDetalle = jdbc.queryForObject("""
				INSERT INTO detalle_extraido (id_documento, texto_crudo, cantidad_leida, cantidad_corregida)
				VALUES (?, 'CEM SOL 42.5', 4.000, 5.000)
				RETURNING id_detalle_extraido
				""", Long.class, idDocumento);

		Integer iguales = jdbc.queryForObject("""
				SELECT count(*) FROM detalle_extraido
				WHERE id_detalle_extraido = ? AND cantidad_leida = 4.000 AND cantidad_corregida = 5.000
				""", Integer.class, idDetalle);
		assertEquals(1, iguales);
	}

	@Test
	void rechazaCantidadCorregidaNegativa() {
		Long idDocumento = insertarDocumentoVigente("v13-det-10");

		assertThrows(DataIntegrityViolationException.class,
				() -> jdbc.update("""
						INSERT INTO detalle_extraido (id_documento, texto_crudo, cantidad_corregida)
						VALUES (?, 'texto', -2.000)
						""", idDocumento));
	}

	// Coincidencias de detalle

	@Test
	void aceptaCoincidenciasConSimilitudYPosicionValidas() {
		Long idDetalle = insertarDetalleVigente("v13-coi-1");
		Long idPrimero = crearProducto("V13-COI-1");
		Long idSegundo = crearProducto("V13-COI-2");

		insertarCoincidencia(idDetalle, idPrimero, new BigDecimal("0.950"), 1);
		insertarCoincidencia(idDetalle, idSegundo, new BigDecimal("0.700"), 2);

		assertEquals(2, contarCoincidencias(idDetalle));
	}

	@Test
	void aceptaElMismoProductoEnDetallesDistintos() {
		Long idProducto = crearProducto("V13-COI-3");
		Long idPrimero = insertarDetalleVigente("v13-coi-2");
		Long idSegundo = insertarDetalleVigente("v13-coi-3");

		insertarCoincidencia(idPrimero, idProducto, new BigDecimal("0.800"), 1);
		insertarCoincidencia(idSegundo, idProducto, new BigDecimal("0.650"), 1);

		assertEquals(1, contarCoincidencias(idPrimero));
		assertEquals(1, contarCoincidencias(idSegundo));
	}

	@Test
	void rechazaSimilitudMayorQueUno() {
		Long idDetalle = insertarDetalleVigente("v13-coi-4");
		Long idProducto = crearProducto("V13-COI-4");

		assertThrows(DataIntegrityViolationException.class,
				() -> insertarCoincidencia(idDetalle, idProducto, new BigDecimal("1.500"), 1));
	}

	@Test
	void rechazaSimilitudNegativa() {
		Long idDetalle = insertarDetalleVigente("v13-coi-5");
		Long idProducto = crearProducto("V13-COI-5");

		assertThrows(DataIntegrityViolationException.class,
				() -> insertarCoincidencia(idDetalle, idProducto, new BigDecimal("-0.100"), 1));
	}

	@Test
	void rechazaPosicionCero() {
		Long idDetalle = insertarDetalleVigente("v13-coi-6");
		Long idProducto = crearProducto("V13-COI-6");

		assertThrows(DataIntegrityViolationException.class,
				() -> insertarCoincidencia(idDetalle, idProducto, new BigDecimal("0.900"), 0));
	}

	@Test
	void rechazaPosicionRepetidaEnElMismoDetalle() {
		Long idDetalle = insertarDetalleVigente("v13-coi-7");
		Long idPrimero = crearProducto("V13-COI-7");
		Long idSegundo = crearProducto("V13-COI-8");
		insertarCoincidencia(idDetalle, idPrimero, new BigDecimal("0.900"), 1);

		assertThrows(DataIntegrityViolationException.class,
				() -> insertarCoincidencia(idDetalle, idSegundo, new BigDecimal("0.800"), 1));
	}

	@Test
	void rechazaProductoRepetidoEnElMismoDetalle() {
		Long idDetalle = insertarDetalleVigente("v13-coi-8");
		Long idProducto = crearProducto("V13-COI-9");
		insertarCoincidencia(idDetalle, idProducto, new BigDecimal("0.900"), 1);

		assertThrows(DataIntegrityViolationException.class,
				() -> insertarCoincidencia(idDetalle, idProducto, new BigDecimal("0.800"), 2));
	}

	@Test
	void rechazaCoincidenciaConProductoInexistente() {
		Long idDetalle = insertarDetalleVigente("v13-coi-9");

		assertThrows(DataIntegrityViolationException.class,
				() -> insertarCoincidencia(idDetalle, 999999L, new BigDecimal("0.900"), 1));
	}

	@Test
	void rechazaCoincidenciaConDetalleInexistente() {
		Long idProducto = crearProducto("V13-COI-10");

		assertThrows(DataIntegrityViolationException.class,
				() -> insertarCoincidencia(999999L, idProducto, new BigDecimal("0.900"), 1));
	}

	@Test
	void rechazaEliminarDetalleConCoincidencias() {
		Long idDetalle = insertarDetalleVigente("v13-coi-11");
		Long idProducto = crearProducto("V13-COI-11");
		insertarCoincidencia(idDetalle, idProducto, new BigDecimal("0.900"), 1);

		assertThrows(DataAccessException.class,
				() -> jdbc.update("DELETE FROM detalle_extraido WHERE id_detalle_extraido = ?", idDetalle));
	}

	@Test
	void rechazaEliminarProductoConCoincidencias() {
		Long idDetalle = insertarDetalleVigente("v13-coi-12");
		Long idProducto = crearProducto("V13-COI-12");
		insertarCoincidencia(idDetalle, idProducto, new BigDecimal("0.900"), 1);

		assertThrows(DataAccessException.class,
				() -> jdbc.update("DELETE FROM producto WHERE id_producto = ?", idProducto));
	}

	@Test
	void existeIndiceEnLaClaveForaneaDeProducto() {
		Integer cantidad = jdbc.queryForObject("""
				SELECT count(*) FROM pg_indexes
				WHERE schemaname = 'public' AND tablename = 'coincidencia_detalle'
				  AND indexname = 'ix_coincidencia_detalle_id_producto'
				""", Integer.class);

		assertEquals(1, cantidad);
	}

	@Test
	void habilitaRlsEnCoincidenciaDetalle() {
		Boolean rls = jdbc.queryForObject(
				"SELECT relrowsecurity FROM pg_class WHERE relname = 'coincidencia_detalle'", Boolean.class);

		assertTrue(rls);
	}

	// Auxiliares

	private Long insertarDocumento(Long idUsuario, String nombre, Long tamano, String tipo, String estado,
			String codigoError, Instant fechaEliminacion) {
		return jdbc.queryForObject("""
				INSERT INTO documento_digital (ruta_archivo, fecha_subida, nombre_archivo, tamano_bytes,
				                               estado_procesamiento, tipo_documento, id_usuario, codigo_error,
				                               fecha_eliminacion)
				VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
				RETURNING id_documento
				""", Long.class, "documentos/v13-" + System.nanoTime() + ".pdf", Timestamp.from(SUBIDA), nombre, tamano,
				estado, tipo, idUsuario, codigoError, marca(fechaEliminacion));
	}

	private Long insertarDocumentoVigente(String username) {
		Long idUsuario = crearUsuario(username);
		return insertarDocumento(idUsuario, "vigente.pdf", 2048L, "solicitud_cliente", "procesado_ocr", null, null);
	}

	private Long insertarDetalle(Long idDocumento, String textoCrudo, String skuSugerido, BigDecimal confianza) {
		return jdbc.queryForObject("""
				INSERT INTO detalle_extraido (id_documento, texto_crudo, sku_sugerido, confianza)
				VALUES (?, ?, ?, ?)
				RETURNING id_detalle_extraido
				""", Long.class, idDocumento, textoCrudo, skuSugerido, confianza);
	}

	private Long insertarDetalleVigente(String username) {
		return insertarDetalle(insertarDocumentoVigente(username), "CEM SOL 42.5", null, null);
	}

	private void insertarCoincidencia(Long idDetalle, Long idProducto, BigDecimal similitud, int posicion) {
		jdbc.update("""
				INSERT INTO coincidencia_detalle (id_detalle_extraido, id_producto, similitud, posicion)
				VALUES (?, ?, ?, ?)
				""", idDetalle, idProducto, similitud, posicion);
	}

	private int contarDocumentos(Long idUsuario) {
		return jdbc.queryForObject("SELECT count(*) FROM documento_digital WHERE id_usuario = ?",
				Integer.class, idUsuario);
	}

	private int contarDocumentosVigentes(Long idUsuario) {
		return jdbc.queryForObject("""
				SELECT count(*) FROM documento_digital WHERE id_usuario = ? AND fecha_eliminacion IS NULL
				""", Integer.class, idUsuario);
	}

	private int contarDetalles(Long idDocumento) {
		return jdbc.queryForObject("SELECT count(*) FROM detalle_extraido WHERE id_documento = ?",
				Integer.class, idDocumento);
	}

	private int contarCoincidencias(Long idDetalle) {
		return jdbc.queryForObject("SELECT count(*) FROM coincidencia_detalle WHERE id_detalle_extraido = ?",
				Integer.class, idDetalle);
	}

	private Long crearUsuario(String username) {
		return jdbc.queryForObject("""
				INSERT INTO usuario (nombres, apellidos, username, password_hash, rol)
				VALUES ('Nombre', 'Apellido', ?, 'hash-ficticio', 'vendedor')
				RETURNING id_usuario
				""", Long.class, username);
	}

	private Long crearProducto(String sku) {
		return jdbc.queryForObject("""
				INSERT INTO producto (sku, nombre_oficial, unidad_medida, precio_referencial, stock_minimo, id_categoria)
				VALUES (?, 'Producto ficticio', 'bolsa', 10.00, 0, (SELECT id_categoria FROM categoria WHERE nombre = 'Cemento y aglomerantes'))
				RETURNING id_producto
				""", Long.class, sku);
	}

	private static Timestamp marca(Instant instante) {
		return instante == null ? null : Timestamp.from(instante);
	}

	private void limpiar() {
		jdbc.update("""
				DELETE FROM coincidencia_detalle WHERE id_detalle_extraido IN (
				    SELECT d.id_detalle_extraido FROM detalle_extraido d
				    JOIN documento_digital doc ON doc.id_documento = d.id_documento
				    JOIN usuario u ON u.id_usuario = doc.id_usuario
				    WHERE u.username LIKE 'v13-%')
				""");
		jdbc.update("""
				DELETE FROM detalle_extraido WHERE id_documento IN (
				    SELECT doc.id_documento FROM documento_digital doc
				    JOIN usuario u ON u.id_usuario = doc.id_usuario
				    WHERE u.username LIKE 'v13-%')
				""");
		jdbc.update("""
				DELETE FROM documento_digital WHERE id_usuario IN (
				    SELECT id_usuario FROM usuario WHERE username LIKE 'v13-%')
				""");
		jdbc.update("DELETE FROM producto WHERE sku LIKE 'V13-%'");
		jdbc.update("DELETE FROM usuario WHERE username LIKE 'v13-%'");
	}

}
