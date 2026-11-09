package com.migal.smartops;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;

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
 * Verifica la migración V11 (códigos de negocio y documento de cliente) con PostgreSQL real.
 * Cada prueba elimina los clientes que inserta. Los clientes creados por otras pruebas no se modifican.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class MigracionCodigosClientesTest {

	private static final String MARCA = "Cliente V11 ";

	/** Prefijo de código por tabla con código de negocio. */
	private static final Map<String, String> PREFIJOS = new LinkedHashMap<>();

	static {
		PREFIJOS.put("cliente", "CLI-");
		PREFIJOS.put("cotizacion", "COT-");
		PREFIJOS.put("venta", "V-");
		PREFIJOS.put("guia_despacho", "D-");
		PREFIJOS.put("orden_compra", "OC-");
		PREFIJOS.put("orden_recepcion", "R-");
	}

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
	void declaraCodigoGeneradoAlmacenadoEnLasSeisTablas() {
		Integer cantidad = jdbc.queryForObject("""
				SELECT count(*) FROM information_schema.columns
				WHERE table_schema = 'public' AND column_name = 'codigo' AND is_generated = 'ALWAYS'
				  AND table_name IN ('cliente', 'cotizacion', 'venta', 'guia_despacho', 'orden_compra', 'orden_recepcion')
				""", Integer.class);

		assertEquals(PREFIJOS.size(), cantidad);
	}

	@Test
	void expresionDeCodigoUsaElPrefijoDeCadaTabla() {
		for (Map.Entry<String, String> entrada : PREFIJOS.entrySet()) {
			String expresion = jdbc.queryForObject("""
					SELECT generation_expression FROM information_schema.columns
					WHERE table_schema = 'public' AND table_name = ? AND column_name = 'codigo'
					""", String.class, entrada.getKey());

			assertTrue(expresion.contains(entrada.getValue()),
					() -> "La expresión de " + entrada.getKey() + " no contiene el prefijo " + entrada.getValue());
		}
	}

	@Test
	void existeRestriccionUnicaDeCodigoEnLasSeisTablas() {
		Integer cantidad = jdbc.queryForObject("""
				SELECT count(*) FROM pg_constraint
				WHERE contype = 'u' AND conname IN ('uq_cliente_codigo', 'uq_cotizacion_codigo', 'uq_venta_codigo',
				    'uq_guia_despacho_codigo', 'uq_orden_compra_codigo', 'uq_orden_recepcion_codigo')
				""", Integer.class);

		assertEquals(PREFIJOS.size(), cantidad);
	}

	@Test
	void generaCodigoDeClienteConCerosALaIzquierda() {
		Long idCliente = insertarCliente("Cliente V11 mostrador", null, null);

		String codigo = codigoDeCliente(idCliente);

		assertEquals(String.format("CLI-%04d", idCliente), codigo);
	}

	@Test
	void generaCodigoCompletoCuandoElIdentificadorSuperaCuatroDigitos() {
		jdbc.update("""
				INSERT INTO cliente (id_cliente, nombre_razon_social) OVERRIDING SYSTEM VALUE
				VALUES (9000001, 'Cliente V11 de identificador grande')
				""");

		assertEquals("CLI-9000001", codigoDeCliente(9000001L));
	}

	@Test
	void rechazaEscrituraDirectaDelCodigo() {
		Long idCliente = insertarCliente("Cliente V11 sin escritura", null, null);

		assertThrows(DataAccessException.class,
				() -> jdbc.update("UPDATE cliente SET codigo = 'CLI-9999' WHERE id_cliente = ?", idCliente));
	}

	@Test
	void aceptaClientesDeMostradorSinDocumento() {
		insertarCliente("Cliente V11 mostrador uno", null, null);
		insertarCliente("Cliente V11 mostrador dos", null, null);

		Integer cantidad = jdbc.queryForObject(
				"SELECT count(*) FROM cliente WHERE nombre_razon_social LIKE ? AND numero_documento IS NULL",
				Integer.class, MARCA + "mostrador%");

		assertEquals(2, cantidad);
	}

	@Test
	void aceptaDniDeOchoDigitos() {
		Long idCliente = insertarCliente("Cliente V11 con DNI", "dni", "12345678");

		assertTrue(idCliente > 0);
	}

	@Test
	void aceptaRucDeOnceDigitos() {
		Long idCliente = insertarCliente("Cliente V11 con RUC", "ruc", "20601234567");

		assertTrue(idCliente > 0);
	}

	@Test
	void rechazaDniDeSieteDigitos() {
		assertThrows(DataIntegrityViolationException.class,
				() -> insertarCliente("Cliente V11 DNI corto", "dni", "1234567"));
	}

	@Test
	void rechazaDniConLetras() {
		assertThrows(DataIntegrityViolationException.class,
				() -> insertarCliente("Cliente V11 DNI alfanumerico", "dni", "1234567A"));
	}

	@Test
	void rechazaRucDeDiezDigitos() {
		assertThrows(DataIntegrityViolationException.class,
				() -> insertarCliente("Cliente V11 RUC corto", "ruc", "2060123456"));
	}

	@Test
	void rechazaTipoDeDocumentoFueraDelDominio() {
		assertThrows(DataIntegrityViolationException.class,
				() -> insertarCliente("Cliente V11 pasaporte", "pasaporte", "123456789"));
	}

	@Test
	void rechazaTipoSinNumeroDeDocumento() {
		assertThrows(DataIntegrityViolationException.class,
				() -> insertarCliente("Cliente V11 tipo sin numero", "ruc", null));
	}

	@Test
	void rechazaNumeroSinTipoDeDocumento() {
		assertThrows(DataIntegrityViolationException.class,
				() -> insertarCliente("Cliente V11 numero sin tipo", null, "12345678"));
	}

	@Test
	void rechazaDniRepetido() {
		insertarCliente("Cliente V11 DNI original", "dni", "87654321");

		assertThrows(DataIntegrityViolationException.class,
				() -> insertarCliente("Cliente V11 DNI repetido", "dni", "87654321"));
	}

	@Test
	void rechazaRucRepetido() {
		insertarCliente("Cliente V11 RUC original", "ruc", "10456789012");

		assertThrows(DataIntegrityViolationException.class,
				() -> insertarCliente("Cliente V11 RUC repetido", "ruc", "10456789012"));
	}

	@Test
	void indiceDeDocumentoEsUnicoYParcial() {
		String definicion = jdbc.queryForObject(
				"SELECT indexdef FROM pg_indexes WHERE schemaname = 'public' AND indexname = 'uq_cliente_documento'",
				String.class);

		assertTrue(definicion.startsWith("CREATE UNIQUE INDEX"));
		assertTrue(definicion.contains("numero_documento IS NOT NULL"));
	}

	private Long insertarCliente(String nombre, String tipo, String numero) {
		return jdbc.queryForObject("""
				INSERT INTO cliente (nombre_razon_social, tipo_documento, numero_documento)
				VALUES (?, ?, ?)
				RETURNING id_cliente
				""", Long.class, nombre, tipo, numero);
	}

	private String codigoDeCliente(Long idCliente) {
		return jdbc.queryForObject("SELECT codigo FROM cliente WHERE id_cliente = ?", String.class, idCliente);
	}

	private void limpiar() {
		jdbc.update("DELETE FROM cliente WHERE nombre_razon_social LIKE 'Cliente V11%'");
	}

}
