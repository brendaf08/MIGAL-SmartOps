package com.migal.smartops;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/** Verifica la migración V17: índices de los listados y unicidad del documento que apoya una recepción. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class MigracionIndicesOperativosTest {

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	void unaRecepcionSoloSeApoyaEnUnDocumentoVigente() {
		String definicion = jdbc.queryForObject(
				"SELECT indexdef FROM pg_indexes WHERE indexname = 'uq_documento_digital_recepcion_vigente'", String.class);

		assertTrue(definicion.startsWith("CREATE UNIQUE INDEX"), definicion);
		assertTrue(definicion.contains("(id_recepcion)"), definicion);
		assertTrue(definicion.contains("fecha_eliminacion IS NULL"), definicion);
	}

	@Test
	void existenLosIndicesDeLosListados() {
		List<String> esperados = List.of("ix_movimiento_kardex_fecha_hora", "ix_venta_estado_fecha",
				"ix_cotizacion_estado_fecha", "ix_orden_compra_estado_fecha", "ix_cotizacion_vencimiento_vigentes",
				"ix_guia_despacho_fecha_emision", "ix_orden_recepcion_fecha_recepcion",
				"ix_documento_digital_tipo_estado_fecha");
		List<Map<String, Object>> filas = jdbc.queryForList(
				"SELECT indexname FROM pg_indexes WHERE indexname = ANY (?)", (Object) esperados.toArray(String[]::new));

		assertEquals(esperados.size(), filas.size());
	}
}
