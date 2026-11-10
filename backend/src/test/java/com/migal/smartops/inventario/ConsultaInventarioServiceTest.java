package com.migal.smartops.inventario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.migal.smartops.TestcontainersConfiguration;
import com.migal.smartops.comun.errores.CodigoError;
import com.migal.smartops.comun.errores.ErrorDeNegocio;
import com.migal.smartops.inventario.model.TipoOperacion;
import com.migal.smartops.inventario.service.ConsultaInventarioService;
import com.migal.smartops.inventario.service.ConsultaInventarioService.AlertaStock;
import com.migal.smartops.inventario.service.ConsultaInventarioService.ProductoExistencias;
import com.migal.smartops.inventario.service.MovimientoDetalle;
import com.migal.smartops.inventario.service.MovimientoInventarioService;
import com.migal.smartops.inventario.service.MovimientoInventarioService.SolicitudAjuste;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.jdbc.core.JdbcTemplate;

/** Verifica las consultas de disponibilidad, existencias, alertas y Kardex con PostgreSQL real. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ConsultaInventarioServiceTest {

	@Autowired
	private ConsultaInventarioService consultas;
	@Autowired
	private MovimientoInventarioService movimientos;
	@Autowired
	private JdbcTemplate jdbc;

	private EscenarioInventario escenario;
	private long usuario;
	private long tienda;
	private long deposito;

	@BeforeEach
	void preparar() {
		escenario = new EscenarioInventario(jdbc);
		usuario = escenario.usuario();
		tienda = escenario.almacen("tienda");
		deposito = escenario.almacen("almacen");
	}

	@AfterEach
	void limpiar() {
		escenario.limpiar();
	}

	private String sku(long producto) {
		return jdbc.queryForObject("SELECT sku FROM producto WHERE id_producto = ?", String.class, producto);
	}

	@Test
	void consolidaLasExistenciasYLasOrdenaConLaTiendaPrimero() {
		long producto = escenario.producto("10");
		escenario.inventario(producto, deposito, "120");
		escenario.inventario(producto, tienda, "40");

		ProductoExistencias p = consultas.existencias(producto);

		assertEquals(0, new BigDecimal("160").compareTo(p.fisico()));
		assertEquals(0, BigDecimal.ZERO.compareTo(p.reservado()));
		assertEquals(0, new BigDecimal("160").compareTo(p.libre()));
		assertEquals("normal", p.nivelAlerta());
		assertEquals(List.of(tienda, deposito), p.ubicaciones().stream().map(u -> u.ubicacionId()).toList());
	}

	@Test
	void existenciasDeUnProductoInexistenteResponde404() {
		assertEquals(CodigoError.RECURSO_NO_ENCONTRADO,
				assertThrows(ErrorDeNegocio.class, () -> consultas.existencias(-1)).codigo());
	}

	@Test
	void calculaElNivelDeAlertaSobreElStockLibre() {
		long critico = escenario.producto("10");
		long minimo = escenario.producto("10");
		long normal = escenario.producto("10");
		escenario.inventario(minimo, tienda, "10");
		escenario.inventario(normal, tienda, "11");

		assertEquals("critico", consultas.existencias(critico).nivelAlerta());
		assertEquals("minimo", consultas.existencias(minimo).nivelAlerta());
		assertEquals("normal", consultas.existencias(normal).nivelAlerta());
	}

	@Test
	void disponibilidadFiltraPorSkuYPagina() {
		long a = escenario.producto("10");
		long b = escenario.producto("10");
		escenario.inventario(a, tienda, "50");
		escenario.inventario(b, tienda, "5");

		var porSku = consultas.disponibilidad(null, sku(a), null, false, 1, 20);
		var soloAlerta = consultas.disponibilidad(sku(b), null, null, true, 1, 20);
		var pagina = consultas.disponibilidad(null, null, null, false, 1, 1);

		assertEquals(1, porSku.pagina().getTotalElements());
		assertEquals(a, porSku.pagina().getContent().get(0).productoId());
		assertEquals(1, soloAlerta.pagina().getTotalElements());
		assertEquals("minimo", soloAlerta.pagina().getContent().get(0).nivelAlerta());
		assertEquals(1, pagina.pagina().getContent().size());
		assertTrue(pagina.totalGeneral() >= 2);
		assertTrue(pagina.enAlerta() >= 1);
	}

	@Test
	void disponibilidadFiltraPorCategoriaYTexto() {
		long a = escenario.producto("10");
		long categoria = jdbc.queryForObject("SELECT id_categoria FROM producto WHERE id_producto = ?", Long.class, a);

		var r = consultas.disponibilidad("Producto de prueba", sku(a), categoria, false, 1, 20);

		assertEquals(1, r.pagina().getTotalElements());
	}

	@Test
	void alertasSeOrdenanPorNivelFaltanteYSku() {
		long critico = escenario.producto("10");
		long minimo = escenario.producto("10");
		escenario.inventario(minimo, tienda, "8");

		Page<AlertaStock> todas = consultas.alertas(null, 1, 100);
		List<Long> ids = todas.getContent().stream().map(AlertaStock::productoId).toList();
		AlertaStock enCritico = todas.getContent().get(ids.indexOf(critico));
		AlertaStock enMinimo = todas.getContent().get(ids.indexOf(minimo));

		assertTrue(ids.indexOf(critico) < ids.indexOf(minimo));
		assertEquals(0, new BigDecimal("10").compareTo(enCritico.faltante()));
		assertEquals(0, new BigDecimal("2").compareTo(enMinimo.faltante()));
		assertTrue(consultas.alertas("minimo", 1, 100).getContent().stream().allMatch(a -> a.nivelAlerta().equals("minimo")));
		assertEquals(CodigoError.VALIDACION_FALLIDA,
				assertThrows(ErrorDeNegocio.class, () -> consultas.alertas("grave", 1, 20)).codigo());
	}

	@Test
	void elKardexSeListaDelMasRecienteYAdmiteFiltros() {
		long producto = escenario.producto("10");
		escenario.inventario(producto, tienda, "40");
		movimientos.registrarAjuste(usuario, new SolicitudAjuste(TipoOperacion.SALIDA, producto, tienda, new BigDecimal("5"), "Merma", null));
		movimientos.registrarAjuste(usuario, new SolicitudAjuste(TipoOperacion.ENTRADA, producto, tienda, new BigDecimal("2"), "Sobrante", null));
		LocalDate hoy = LocalDate.now(java.time.ZoneId.of("America/Lima"));

		Page<MovimientoDetalle> todos = consultas.kardex(null, sku(producto), null, null, null, null, null, 1, 20);
		Page<MovimientoDetalle> salidas = consultas.kardex(null, sku(producto), tienda, "salida", "ajuste", hoy.minusDays(1), hoy.plusDays(1), 1, 20);
		Page<MovimientoDetalle> futuro = consultas.kardex(null, sku(producto), null, null, null, hoy.plusDays(2), null, 1, 20);

		assertEquals(2, todos.getTotalElements());
		assertEquals(TipoOperacion.ENTRADA, todos.getContent().get(0).tipoOperacion());
		assertEquals(1, salidas.getTotalElements());
		assertEquals(0, futuro.getTotalElements());
		assertEquals(CodigoError.VALIDACION_FALLIDA, assertThrows(ErrorDeNegocio.class,
				() -> consultas.kardex(null, null, null, "otro", null, null, null, 1, 20)).codigo());
		assertEquals(CodigoError.VALIDACION_FALLIDA, assertThrows(ErrorDeNegocio.class,
				() -> consultas.kardex(null, null, null, null, null, hoy, hoy.minusDays(1), 1, 20)).codigo());
	}
}
