package com.migal.smartops.documentosocr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.migal.smartops.catalogo.model.Producto;
import com.migal.smartops.documentosocr.model.CoincidenciaDetalle;
import com.migal.smartops.documentosocr.model.DetalleExtraido;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Verifica la asociación automática de una línea del OCR (contrato, DocumentoUmbrales): solo una coincidencia única
 * con confianza alta asocia el producto sin advertencia; un empate sobre el umbral o una confianza media exigen
 * revisión.
 */
class DetalleExtraidoTest {

	private static final BigDecimal ALTA = new BigDecimal("0.85");

	private final Producto fierroAm60 = producto("00754", "FIERRO AM60 (1/2\")");
	private final Producto fierroSiderperu = producto("00755", "FIERRO SIDERPERU (1/2\")");
	private final Producto tuboPavco = producto("00303", "TUBO LUZ L PAVCO (3/4\")");
	private final Producto tuboNicoll = producto("00306", "TUBO LUZ L NICOLL (3/4\")");

	@Test
	void unaCoincidenciaUnicaConConfianzaAltaQuedaAsociadaSinRevision() {
		DetalleExtraido linea = linea("ϕ 1/2");

		linea.proponerCoincidencias(List.of(coincidencia(fierroAm60, "0.897", 1),
				coincidencia(fierroSiderperu, "0.820", 2)), ALTA);

		assertSame(fierroAm60, linea.getProducto());
		assertEquals("00754", linea.getSkuSugerido());
		assertFalse(linea.isRequiereRevision());
	}

	@Test
	void dosCoincidenciasSobreElUmbralAltoSonAmbiguasYRequierenRevision() {
		DetalleExtraido linea = linea("tubo luz 3/4");

		linea.proponerCoincidencias(List.of(coincidencia(tuboPavco, "0.894", 1),
				coincidencia(tuboNicoll, "0.880", 2)), ALTA);

		assertNull(linea.getProducto());
		assertEquals("00303", linea.getSkuSugerido());
		assertTrue(linea.isRequiereRevision());
	}

	@Test
	void unaConfianzaMediaSeProponeSinAsociarElProducto() {
		DetalleExtraido linea = linea("codo 2\"");

		linea.proponerCoincidencias(List.of(coincidencia(tuboPavco, "0.691", 1)), ALTA);

		assertNull(linea.getProducto());
		assertTrue(linea.isRequiereRevision());
	}

	@Test
	void sinCoincidenciasLaLineaRequiereRevision() {
		DetalleExtraido linea = linea("baños juegos");

		linea.proponerCoincidencias(List.of(), ALTA);

		assertNull(linea.getProducto());
		assertNull(linea.getSkuSugerido());
		assertTrue(linea.isRequiereRevision());
	}

	@Test
	void elProductoElegidoPorElUsuarioNoSeReemplaza() {
		DetalleExtraido linea = linea("ϕ 1/2");
		linea.confirmarProducto(fierroSiderperu, ALTA);

		linea.proponerCoincidencias(List.of(coincidencia(fierroAm60, "0.897", 1)), ALTA);

		assertSame(fierroSiderperu, linea.getProducto());
	}

	private static DetalleExtraido linea(String texto) {
		return new DetalleExtraido(texto, BigDecimal.TEN);
	}

	private static CoincidenciaDetalle coincidencia(Producto producto, String confianza, int posicion) {
		return new CoincidenciaDetalle(producto, new BigDecimal(confianza), (short) posicion);
	}

	private static Producto producto(String sku, String nombre) {
		return new Producto(sku, nombre, null, "unidad", BigDecimal.ONE, BigDecimal.ONE, null, null, null, true);
	}
}
