package com.migal.smartops.agenteia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.migal.smartops.TestcontainersConfiguration;
import com.migal.smartops.agenteia.model.RolMensaje;
import com.migal.smartops.agenteia.service.ReferenciaAgente;
import com.migal.smartops.agenteia.service.ServicioAgente;
import com.migal.smartops.comun.errores.CodigoError;
import com.migal.smartops.comun.errores.ErrorDeNegocio;
import com.migal.smartops.inventario.EscenarioInventario;
import com.migal.smartops.seguridad.config.UsuarioAutenticado;
import com.migal.smartops.seguridad.model.RolUsuario;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verifica el servicio de conversaciones del agente con PostgreSQL real y el proveedor simulado: envío de consultas,
 * registro de mensajes, aislamiento por propietario, listado y eliminación. Cada prueba se revierte.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class ServicioAgenteTest {

	@Autowired
	private ServicioAgente agente;
	@Autowired
	private JdbcTemplate jdbc;

	private UsuarioAutenticado actor;
	private long producto;
	private String sku;

	@BeforeEach
	void preparar() {
		EscenarioInventario escenario = new EscenarioInventario(jdbc);
		long usuario = escenario.usuario();
		actor = new UsuarioAutenticado(usuario, "almacenero-prueba", RolUsuario.ALMACENERO);
		producto = escenario.producto("0");
		escenario.inventario(producto, escenario.almacen("almacen"), "10");
		sku = jdbc.queryForObject("SELECT sku FROM producto WHERE id_producto = ?", String.class, producto);
	}

	@Test
	void consultaNuevaCreaConversacionConMensajesDelUsuarioYDelAgente() {
		var respuesta = agente.enviar(actor, "Disponibilidad de " + sku, null);

		assertFalse(respuesta.incierta());
		assertEquals(1, respuesta.herramientas().size());
		assertEquals("consultarDisponibilidad", respuesta.herramientas().get(0).nombre());
		assertNull(respuesta.herramientas().get(0).codigo());
		assertTrue(respuesta.texto().contains(sku));
		assertEquals(List.of(new ReferenciaAgente(ReferenciaAgente.PRODUCTO, String.valueOf(producto), sku)),
				respuesta.referencias());

		var conversacion = agente.consultar(actor, respuesta.conversacionId());
		assertEquals("Disponibilidad de " + sku, conversacion.titulo());
		assertEquals(2, conversacion.mensajes().size());
		assertEquals(RolMensaje.USUARIO, conversacion.mensajes().get(0).autor());
		assertEquals(RolMensaje.AGENTE, conversacion.mensajes().get(1).autor());
		assertEquals(respuesta.mensajeId(), conversacion.mensajes().get(1).id());
		assertEquals(1, conversacion.mensajes().get(1).herramientas().size());
	}

	@Test
	void consultaEnConversacionExistenteAgregaDosMensajes() {
		long conversacion = agente.enviar(actor, "Disponibilidad de " + sku, null).conversacionId();

		agente.enviar(actor, "Alertas de stock", conversacion);

		assertEquals(4, agente.consultar(actor, conversacion).mensajes().size());
	}

	@Test
	void conversacionAjenaRespondeRecursoNoEncontrado() {
		long conversacion = agente.enviar(actor, "Disponibilidad de " + sku, null).conversacionId();
		UsuarioAutenticado otro = new UsuarioAutenticado(new EscenarioInventario(jdbc).usuario(), "otro-usuario",
				RolUsuario.ALMACENERO);

		assertEquals(CodigoError.RECURSO_NO_ENCONTRADO, assertThrows(ErrorDeNegocio.class,
				() -> agente.consultar(otro, conversacion)).codigo());
		assertEquals(CodigoError.RECURSO_NO_ENCONTRADO, assertThrows(ErrorDeNegocio.class,
				() -> agente.enviar(otro, "Hola", conversacion)).codigo());
		assertEquals(CodigoError.RECURSO_NO_ENCONTRADO, assertThrows(ErrorDeNegocio.class,
				() -> agente.eliminar(otro, conversacion)).codigo());
	}

	@Test
	void vendedorNoRecibeHerramientasFueraDeSuRol() {
		UsuarioAutenticado vendedor = new UsuarioAutenticado(actor.id(), "vendedor-prueba", RolUsuario.VENDEDOR);

		var respuesta = agente.enviar(vendedor, "Alertas de stock", null);

		assertTrue(respuesta.herramientas().isEmpty());
		assertTrue(respuesta.incierta());
		assertTrue(respuesta.texto().startsWith("No dispongo"));
	}

	@Test
	void consultaSinHerramientaEsIncierta() {
		var respuesta = agente.enviar(actor, "Hola", null);

		assertTrue(respuesta.incierta());
		assertTrue(respuesta.herramientas().isEmpty());
		assertTrue(respuesta.referencias().isEmpty());
	}

	@Test
	void mensajeVacioRespondeValidacionFallida() {
		assertEquals(CodigoError.VALIDACION_FALLIDA,
				assertThrows(ErrorDeNegocio.class, () -> agente.enviar(actor, "   ", null)).codigo());
	}

	@Test
	void eliminarConversacionLaQuitaJuntoConSusMensajes() {
		long conversacion = agente.enviar(actor, "Disponibilidad de " + sku, null).conversacionId();

		agente.eliminar(actor, conversacion);

		assertEquals(CodigoError.RECURSO_NO_ENCONTRADO, assertThrows(ErrorDeNegocio.class,
				() -> agente.consultar(actor, conversacion)).codigo());
	}

	@Test
	void listaSoloLasConversacionesDelUsuario() {
		agente.enviar(actor, "Disponibilidad de " + sku, null);
		agente.enviar(actor, "Alertas de stock", null);
		UsuarioAutenticado otro = new UsuarioAutenticado(new EscenarioInventario(jdbc).usuario(), "otro-usuario",
				RolUsuario.ALMACENERO);
		agente.enviar(otro, "Hola", null);

		var pagina = agente.listar(actor, 1, 10, null);

		assertEquals(2, pagina.total());
		assertEquals(List.of("Alertas de stock", "Disponibilidad de " + sku),
				pagina.items().stream().map(item -> item.titulo()).sorted().toList());
		assertTrue(pagina.items().stream().allMatch(item -> item.cantidadMensajes() == 2));
	}
}
