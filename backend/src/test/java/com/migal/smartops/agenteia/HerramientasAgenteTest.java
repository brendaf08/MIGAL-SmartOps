package com.migal.smartops.agenteia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.migal.smartops.TestcontainersConfiguration;
import com.migal.smartops.agenteia.service.CatalogoHerramientas;
import com.migal.smartops.agenteia.service.DefinicionHerramienta;
import com.migal.smartops.agenteia.service.InvocacionHerramienta;
import com.migal.smartops.agenteia.service.ReferenciaAgente;
import com.migal.smartops.comun.errores.CodigoError;
import com.migal.smartops.inventario.EscenarioInventario;
import com.migal.smartops.seguridad.model.RolUsuario;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verifica el catálogo de herramientas del agente y su ejecución contra PostgreSQL real (RF-IA-09): visibilidad por rol,
 * ejecución con los servicios de inventario y catálogo, y códigos de error. Cada prueba se revierte.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class HerramientasAgenteTest {

	@Autowired
	private CatalogoHerramientas catalogo;
	@Autowired
	private JdbcTemplate jdbc;

	private EscenarioInventario escenario;
	private long producto;
	private String sku;

	@BeforeEach
	void preparar() {
		escenario = new EscenarioInventario(jdbc);
		producto = escenario.producto("0");
		long deposito = escenario.almacen("almacen");
		escenario.inventario(producto, deposito, "10");
		sku = jdbc.queryForObject("SELECT sku FROM producto WHERE id_producto = ?", String.class, producto);
	}

	@Test
	void almaceneroConsultaDisponibilidadPorSkuYObtieneLaReferencia() {
		InvocacionHerramienta invocacion = catalogo.ejecutar(RolUsuario.ALMACENERO, "consultarDisponibilidad",
				Map.of("sku", sku));

		assertNull(invocacion.codigo());
		assertTrue(invocacion.resumen().contains(sku));
		assertEquals(List.of(new ReferenciaAgente(ReferenciaAgente.PRODUCTO, String.valueOf(producto), sku)),
				invocacion.referencias());
	}

	@Test
	void consultaDeExistenciasInformaLaCantidadLibre() {
		InvocacionHerramienta invocacion = catalogo.ejecutar(RolUsuario.VENDEDOR, "consultarExistenciasProducto",
				Map.of("id", String.valueOf(producto)));

		assertNull(invocacion.codigo());
		assertTrue(invocacion.resumen().contains("libre 10"));
	}

	@Test
	void vendedorNoPuedeEjecutarListarAlertasNiKardex() {
		InvocacionHerramienta alertas = catalogo.ejecutar(RolUsuario.VENDEDOR, "listarAlertasStock", Map.of());
		InvocacionHerramienta kardex = catalogo.ejecutar(RolUsuario.VENDEDOR, "consultarKardex", Map.of());

		assertEquals(CodigoError.PERMISO_INSUFICIENTE, alertas.codigo());
		assertEquals(CodigoError.PERMISO_INSUFICIENTE, kardex.codigo());
		assertTrue(alertas.referencias().isEmpty());
		assertTrue(kardex.referencias().isEmpty());
	}

	@Test
	void catalogoDelVendedorNoOfreceHerramientasRestringidas() {
		List<String> nombres = catalogo.disponiblesPara(RolUsuario.VENDEDOR).stream()
				.map(DefinicionHerramienta::nombre).toList();

		assertTrue(nombres.contains("consultarDisponibilidad"));
		assertTrue(nombres.contains("consultarExistenciasProducto"));
		assertFalse(nombres.contains("listarAlertasStock"));
		assertFalse(nombres.contains("consultarKardex"));
		assertFalse(nombres.contains("listarProductos"));
	}

	@Test
	void herramientaInexistenteRespondeValidacionFallida() {
		InvocacionHerramienta invocacion = catalogo.ejecutar(RolUsuario.ADMINISTRADOR, "eliminarProducto", Map.of());

		assertEquals(CodigoError.VALIDACION_FALLIDA, invocacion.codigo());
	}

	@Test
	void productoInexistenteRespondeRecursoNoEncontrado() {
		InvocacionHerramienta invocacion = catalogo.ejecutar(RolUsuario.ALMACENERO, "consultarProducto",
				Map.of("id", "999999"));

		assertEquals(CodigoError.RECURSO_NO_ENCONTRADO, invocacion.codigo());
	}

	@Test
	void parametroMalFormadoRespondeValidacionFallida() {
		InvocacionHerramienta invocacion = catalogo.ejecutar(RolUsuario.ALMACENERO, "consultarExistenciasProducto",
				Map.of("id", "abc"));

		assertEquals(CodigoError.VALIDACION_FALLIDA, invocacion.codigo());
	}

	@Test
	void nivelDeAlertaFueraDelDominioRespondeValidacionFallida() {
		InvocacionHerramienta invocacion = catalogo.ejecutar(RolUsuario.REPONEDOR, "listarAlertasStock",
				Map.of("nivel", "urgente"));

		assertEquals(CodigoError.VALIDACION_FALLIDA, invocacion.codigo());
	}
}
