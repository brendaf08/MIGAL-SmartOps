package com.migal.smartops;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Verifica la migración V9 (documento_digital y detalle_extraido) con PostgreSQL real.
 * Cada prueba elimina las filas que inserta, en orden inverso a las claves foráneas.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class MigracionDocumentosOcrTest {

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
	void existenLasTablasDeDocumentosOcr() {
		Integer cantidad = jdbc.queryForObject("""
				SELECT count(*) FROM information_schema.tables
				WHERE table_schema = 'public' AND table_name IN ('documento_digital', 'detalle_extraido')
				""", Integer.class);
		assertEquals(2, cantidad);
	}

	@Test
	void aceptaDocumentoSinAsociacionOperativa() {
		Long idUsuario = crearUsuario();

		insertarDocumento(idUsuario, "pendiente", null, null);

		assertEquals(1, contarDocumentos(idUsuario));
	}

	@Test
	void aceptaDocumentoAsociadoACotizacion() {
		Long idUsuario = crearUsuario();

		insertarDocumento(idUsuario, "procesado_ocr", crearCotizacion(idUsuario), null);

		assertEquals(1, contarDocumentos(idUsuario));
	}

	@Test
	void aceptaDocumentoAsociadoARecepcion() {
		Long idUsuario = crearUsuario();

		insertarDocumento(idUsuario, "validado", null, crearRecepcion(idUsuario));

		assertEquals(1, contarDocumentos(idUsuario));
	}

	@Test
	void rechazaDocumentoAsociadoACotizacionYRecepcionSimultaneamente() {
		Long idUsuario = crearUsuario();

		assertThrows(DataIntegrityViolationException.class,
				() -> insertarDocumento(idUsuario, "pendiente", crearCotizacion(idUsuario), crearRecepcion(idUsuario)));
	}

	@Test
	void rechazaEstadoDeProcesamientoFueraDelDominio() {
		Long idUsuario = crearUsuario();

		assertThrows(DataIntegrityViolationException.class,
				() -> insertarDocumento(idUsuario, "procesado", null, null));
	}

	@Test
	void rechazaDocumentoDeUsuarioInexistente() {
		assertThrows(DataIntegrityViolationException.class,
				() -> insertarDocumento(999999L, "pendiente", null, null));
	}

	@Test
	void aceptaDetalleExtraidoSinProductoAsociado() {
		Long idUsuario = crearUsuario();
		Long idDocumento = insertarDocumento(idUsuario, "procesado_ocr", null, null);

		jdbc.update("""
				INSERT INTO detalle_extraido (id_documento, texto_crudo, cantidad_leida)
				VALUES (?, 'CEMENTO SOL X 42.5KG', 10.000)
				""", idDocumento);

		assertEquals(1, contarDetalles(idDocumento));
	}

	@Test
	void aceptaDetalleExtraidoConProductoConfirmado() {
		Long idUsuario = crearUsuario();
		Long idDocumento = insertarDocumento(idUsuario, "validado", null, null);
		Long idProducto = crearProducto();

		jdbc.update("""
				INSERT INTO detalle_extraido (id_documento, texto_crudo, id_producto, texto_corregido)
				VALUES (?, 'CEM SOL 42.5', ?, 'CEMENTO SOL TIPO I 42.5KG')
				""", idDocumento, idProducto);

		assertEquals(1, contarDetalles(idDocumento));
	}

	@Test
	void rechazaDetalleExtraidoSinDocumento() {
		assertThrows(DataIntegrityViolationException.class,
				() -> jdbc.update("INSERT INTO detalle_extraido (id_documento, texto_crudo) VALUES (999999, 'texto')"));
	}

	@Test
	void rechazaDetalleExtraidoConProductoInexistente() {
		Long idUsuario = crearUsuario();
		Long idDocumento = insertarDocumento(idUsuario, "procesado_ocr", null, null);

		assertThrows(DataIntegrityViolationException.class,
				() -> jdbc.update("""
						INSERT INTO detalle_extraido (id_documento, texto_crudo, id_producto)
						VALUES (?, 'texto', 999999)
						""", idDocumento));
	}

	private Long insertarDocumento(Long idUsuario, String estado, Long idCotizacion, Long idRecepcion) {
		return jdbc.queryForObject("""
				INSERT INTO documento_digital (ruta_archivo, nombre_archivo, tamano_bytes, estado_procesamiento,
				                               tipo_documento, id_usuario, id_cotizacion, id_recepcion)
				VALUES (?, ?, 2048, ?, 'solicitud_cliente', ?, ?, ?)
				RETURNING id_documento
				""", Long.class, "documentos/" + sufijo() + ".pdf", "archivo-" + sufijo() + ".pdf", estado, idUsuario,
				idCotizacion, idRecepcion);
	}

	private int contarDocumentos(Long idUsuario) {
		return jdbc.queryForObject("SELECT count(*) FROM documento_digital WHERE id_usuario = ?",
				Integer.class, idUsuario);
	}

	private int contarDetalles(Long idDocumento) {
		return jdbc.queryForObject("SELECT count(*) FROM detalle_extraido WHERE id_documento = ?",
				Integer.class, idDocumento);
	}

	private String sufijo() {
		return UUID.randomUUID().toString().substring(0, 8);
	}

	private Long crearUsuario() {
		return jdbc.queryForObject("""
				INSERT INTO usuario (nombres, apellidos, username, password_hash, rol)
				VALUES ('Nombre', 'Apellido', ?, 'hash-ficticio', 'vendedor')
				RETURNING id_usuario
				""", Long.class, "ocr-" + sufijo());
	}

	private Long crearProducto() {
		return jdbc.queryForObject("""
				INSERT INTO producto (sku, nombre_oficial, unidad_medida, precio_referencial, stock_minimo, id_categoria)
				VALUES (?, 'Producto ficticio', 'bolsa', 10.00, 0, (SELECT id_categoria FROM categoria WHERE nombre = 'Cemento y aglomerantes'))
				RETURNING id_producto
				""", Long.class, "OCR-" + sufijo());
	}

	private Long crearCotizacion(Long idUsuario) {
		Long idCliente = jdbc.queryForObject("""
				INSERT INTO cliente (nombre_razon_social)
				VALUES ('Cliente ficticio')
				RETURNING id_cliente
				""", Long.class);
		return jdbc.queryForObject("""
				INSERT INTO cotizacion (id_cliente, id_usuario_responsable, fecha_vencimiento, descuento, estado)
				VALUES (?, ?, CURRENT_DATE + 30, 0, 'emitida')
				RETURNING id_cotizacion
				""", Long.class, idCliente, idUsuario);
	}

	private Long crearRecepcion(Long idUsuario) {
		Long idProveedor = jdbc.queryForObject("""
				INSERT INTO proveedor (ruc, razon_social, contacto)
				VALUES (?, 'Proveedor ficticio', 'contacto ficticio')
				RETURNING id_proveedor
				""", Long.class, "OC" + sufijo());
		Long idOrden = jdbc.queryForObject("""
				INSERT INTO orden_compra (id_proveedor, id_usuario_responsable, estado)
				VALUES (?, ?, 'pendiente')
				RETURNING id_orden
				""", Long.class, idProveedor, idUsuario);
		return jdbc.queryForObject("""
				INSERT INTO orden_recepcion (id_orden, id_usuario_responsable, fecha_recepcion, estado)
				VALUES (?, ?, CURRENT_DATE, 'pendiente')
				RETURNING id_recepcion
				""", Long.class, idOrden, idUsuario);
	}

	private void limpiar() {
		jdbc.update("DELETE FROM detalle_extraido");
		jdbc.update("DELETE FROM documento_digital");
		jdbc.update("DELETE FROM detalle_recepcion");
		jdbc.update("DELETE FROM orden_recepcion");
		jdbc.update("DELETE FROM detalle_compra");
		jdbc.update("DELETE FROM orden_compra");
		jdbc.update("DELETE FROM proveedor");
		jdbc.update("DELETE FROM cotizacion");
		jdbc.update("DELETE FROM cliente");
		jdbc.update("DELETE FROM producto");
		jdbc.update("DELETE FROM usuario");
	}

}
