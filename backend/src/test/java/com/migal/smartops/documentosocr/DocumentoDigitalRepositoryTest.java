package com.migal.smartops.documentosocr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.migal.smartops.TestcontainersConfiguration;
import com.migal.smartops.catalogo.model.Producto;
import com.migal.smartops.catalogo.repository.ProductoRepository;
import com.migal.smartops.documentosocr.model.CoincidenciaDetalle;
import com.migal.smartops.documentosocr.model.DetalleExtraido;
import com.migal.smartops.documentosocr.model.DocumentoDigital;
import com.migal.smartops.documentosocr.model.EstadoProcesamiento;
import com.migal.smartops.documentosocr.model.TipoDocumento;
import com.migal.smartops.documentosocr.repository.DetalleExtraidoRepository;
import com.migal.smartops.documentosocr.repository.DocumentoDigitalRepository;
import com.migal.smartops.inventario.EscenarioInventario;
import com.migal.smartops.seguridad.model.Usuario;
import com.migal.smartops.seguridad.repository.UsuarioRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verifica el mapeo de los documentos digitales, de sus líneas extraídas y de las coincidencias con PostgreSQL real:
 * cascada, orden de las coincidencias, eliminación lógica y restricciones de la migración V13. Cada prueba se revierte,
 * incluidos los datos auxiliares (usuario y producto), que se crean con {@link EscenarioInventario} dentro de la transacción.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class DocumentoDigitalRepositoryTest {

	@Autowired
	private DocumentoDigitalRepository documentos;
	@Autowired
	private DetalleExtraidoRepository detalles;
	@Autowired
	private UsuarioRepository usuarios;
	@Autowired
	private ProductoRepository productos;
	@Autowired
	private JdbcTemplate jdbc;

	private EscenarioInventario escenario;
	private Usuario usuario;
	private Producto producto;

	@BeforeEach
	void preparar() {
		escenario = new EscenarioInventario(jdbc);
		usuario = usuarios.findById(escenario.usuario()).orElseThrow();
		producto = productos.findById(escenario.producto("0")).orElseThrow();
	}

	@Test
	void persisteDocumentoConLineasYCoincidenciasEnOrden() {
		DocumentoDigital documento = documentos.saveAndFlush(nuevoDocumento());
		DetalleExtraido linea = new DetalleExtraido("CEMENTO SOL 42.5KG", new BigDecimal("100"));
		linea.proponerCoincidencias(List.of(
				new CoincidenciaDetalle(producto, new BigDecimal("0.940"), (short) 1)), new BigDecimal("0.85"));
		documento.registrarLineas(List.of(linea));
		documentos.saveAndFlush(documento);

		DocumentoDigital recuperado = documentos.findById(documento.getId()).orElseThrow();

		assertEquals(EstadoProcesamiento.PROCESADO_OCR, recuperado.getEstado());
		assertEquals(1, recuperado.getDetalles().size());
		DetalleExtraido detalle = recuperado.getDetalles().get(0);
		assertEquals(0, new BigDecimal("100.000").compareTo(detalle.getCantidadLeida()));
		assertEquals(producto.getSku(), detalle.getSkuSugerido());
		assertEquals(1, detalle.getCoincidencias().size());
		assertTrue(detalle.isRequiereRevision());
		assertFalse(detalle.isDescartado());
	}

	@Test
	void eliminacionLogicaExcluyeElDocumentoDeLaBusquedaVigente() {
		DocumentoDigital documento = documentos.saveAndFlush(nuevoDocumento());

		documento.eliminar(Instant.now());
		documentos.saveAndFlush(documento);

		assertTrue(documentos.findByIdAndFechaEliminacionIsNull(documento.getId()).isEmpty());
		assertTrue(documentos.findById(documento.getId()).isPresent());
	}

	@Test
	void bloqueoDelDocumentoDevuelveElMismoRegistro() {
		DocumentoDigital documento = documentos.saveAndFlush(nuevoDocumento());

		DocumentoDigital bloqueado = documentos.bloquearPorId(documento.getId()).orElseThrow();

		assertEquals(documento.getId(), bloqueado.getId());
		assertNull(bloqueado.getIdCotizacion());
	}

	@Test
	void rechazaEstadoDeProcesamientoFueraDelDominio() {
		DocumentoDigital documento = nuevoDocumento();
		documentos.saveAndFlush(documento);

		assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
				"UPDATE documento_digital SET estado_procesamiento = 'procesado' WHERE id_documento = ?", documento.getId()));
	}

	@Test
	void rechazaCodigoDeErrorNoPendienteDeProcesamiento() {
		DocumentoDigital documento = documentos.saveAndFlush(nuevoDocumento());
		documento.registrarError("DOCUMENTO_ILEGIBLE");
		documento.validar();

		assertThrows(DataIntegrityViolationException.class, () -> documentos.saveAndFlush(documento));
	}

	@Test
	void rechazaDosCoincidenciasConLaMismaPosicion() {
		DocumentoDigital documento = documentos.saveAndFlush(nuevoDocumento());
		DetalleExtraido linea = detalles.saveAndFlush(lineaDe(documento));
		Producto otro = productos.findById(escenario.producto("0")).orElseThrow();

		jdbc.update("INSERT INTO coincidencia_detalle (id_detalle_extraido, id_producto, similitud, posicion) "
				+ "VALUES (?, ?, 0.900, 1)", linea.getId(), producto.getId());

		assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
				"INSERT INTO coincidencia_detalle (id_detalle_extraido, id_producto, similitud, posicion) "
						+ "VALUES (?, ?, 0.800, 1)", linea.getId(), otro.getId()));
	}

	private DocumentoDigital nuevoDocumento() {
		return new DocumentoDigital(usuario, TipoDocumento.DOCUMENTO_RECEPCION, "guia-proveedor.jpg",
				"documentos/prueba-" + System.nanoTime() + ".jpg", 2048, Instant.now());
	}

	private DetalleExtraido lineaDe(DocumentoDigital documento) {
		DetalleExtraido linea = new DetalleExtraido("LINEA DE PRUEBA", null);
		documento.agregarDetalle(linea);
		return linea;
	}
}
