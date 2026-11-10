package com.migal.smartops.agenteia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.migal.smartops.TestcontainersConfiguration;
import com.migal.smartops.agenteia.model.ConversacionAgente;
import com.migal.smartops.agenteia.model.MensajeAgente;
import com.migal.smartops.agenteia.model.RolMensaje;
import com.migal.smartops.agenteia.repository.ConversacionAgenteRepository;
import com.migal.smartops.agenteia.repository.MensajeAgenteRepository;
import com.migal.smartops.inventario.EscenarioInventario;
import com.migal.smartops.seguridad.model.RolUsuario;
import com.migal.smartops.seguridad.model.Usuario;
import com.migal.smartops.seguridad.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verifica el mapeo de las conversaciones y los mensajes del agente con PostgreSQL real (migración V14): cascada en la
 * eliminación, actualización de la fecha del último mensaje, aislamiento por propietario y columnas JSON. Cada prueba se
 * revierte; los datos auxiliares se crean dentro de la transacción.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class AgenteRepositoryTest {

	private static final String HERRAMIENTAS = "[{\"nombre\": \"consultarDisponibilidad\", \"parametros\": {\"q\": \"cemento\"}, "
			+ "\"resumen\": \"Cemento Sol con 80 bolsas libres.\", \"codigo\": null}]";
	private static final String REFERENCIAS = "[{\"tipo\": \"producto\", \"id\": \"42\", \"codigo\": \"CEM-001\"}]";
	private static final Instant BASE = Instant.parse("2026-11-08T15:00:00Z");

	@Autowired
	private ConversacionAgenteRepository conversaciones;
	@Autowired
	private MensajeAgenteRepository mensajes;
	@Autowired
	private UsuarioRepository usuarios;
	@Autowired
	private EntityManager em;
	@Autowired
	private JdbcTemplate jdbc;

	private Usuario usuario;
	private Usuario otroUsuario;

	@BeforeEach
	void preparar() {
		EscenarioInventario escenario = new EscenarioInventario(jdbc);
		usuario = usuarios.findById(escenario.usuario()).orElseThrow();
		otroUsuario = usuarios.saveAndFlush(
				new Usuario("Otro", "Usuario", "agenteia-otro", "hash-ficticio", RolUsuario.VENDEDOR));
	}

	@Test
	void persisteConversacionConMensajesEnOrdenCronologico() {
		ConversacionAgente conversacion = nuevaConversacion("Disponibilidad de cemento");
		conversacion.agregarMensaje(new MensajeAgente(RolMensaje.USUARIO, "Disponibilidad de cemento Sol", false,
				"[]", "[]", BASE));
		conversacion.agregarMensaje(new MensajeAgente(RolMensaje.AGENTE, "Hay 80 bolsas libres.", false,
				HERRAMIENTAS, REFERENCIAS, BASE.plusSeconds(2)));
		conversaciones.saveAndFlush(conversacion);
		em.clear();

		ConversacionAgente recuperada = conversaciones.findById(conversacion.getId()).orElseThrow();

		assertEquals("Disponibilidad de cemento", recuperada.getTitulo());
		assertEquals(2, recuperada.getMensajes().size());
		MensajeAgente respuesta = recuperada.getMensajes().get(1);
		assertEquals(RolMensaje.AGENTE, respuesta.getRol());
		assertTrue(respuesta.getHerramientas().contains("consultarDisponibilidad"));
		assertTrue(respuesta.getReferencias().contains("CEM-001"));
		assertFalse(respuesta.isIncierta());
	}

	@Test
	void actualizaLaFechaDelUltimoMensajeAlAgregarUnMensaje() {
		ConversacionAgente conversacion = nuevaConversacion("Consulta");
		conversacion.agregarMensaje(new MensajeAgente(RolMensaje.USUARIO, "Consulta", false, "[]", "[]", BASE));
		conversacion.agregarMensaje(new MensajeAgente(RolMensaje.AGENTE, "Respuesta", true, "[]", "[]",
				BASE.plusSeconds(60)));
		conversaciones.saveAndFlush(conversacion);
		em.clear();

		ConversacionAgente recuperada = conversaciones.findById(conversacion.getId()).orElseThrow();

		assertEquals(BASE, recuperada.getFechaCreacion());
		assertEquals(BASE.plusSeconds(60), recuperada.getFechaUltimoMensaje());
		assertTrue(recuperada.getMensajes().get(1).isIncierta());
	}

	@Test
	void buscaLaConversacionSoloEntreLasDelPropietario() {
		ConversacionAgente conversacion = conversaciones.saveAndFlush(nuevaConversacion("Propia"));

		assertTrue(conversaciones.findByIdAndUsuarioId(conversacion.getId(), usuario.getId()).isPresent());
		assertFalse(conversaciones.findByIdAndUsuarioId(conversacion.getId(), otroUsuario.getId()).isPresent());
		assertTrue(conversacion.perteneceA(usuario.getId()));
		assertFalse(conversacion.perteneceA(otroUsuario.getId()));
	}

	@Test
	void listaLasConversacionesDelUsuarioDeLaMasRecienteALaMasAntigua() {
		ConversacionAgente antigua = nuevaConversacion("Antigua");
		antigua.agregarMensaje(new MensajeAgente(RolMensaje.USUARIO, "Antigua", false, "[]", "[]", BASE));
		ConversacionAgente reciente = nuevaConversacion("Reciente");
		reciente.agregarMensaje(new MensajeAgente(RolMensaje.USUARIO, "Reciente", false, "[]", "[]",
				BASE.plusSeconds(3600)));
		conversaciones.saveAll(List.of(antigua, reciente));
		ConversacionAgente deOtro = new ConversacionAgente(otroUsuario, "Ajena", BASE.plusSeconds(7200));
		conversaciones.saveAndFlush(deOtro);

		var pagina = conversaciones.findByUsuarioId(usuario.getId(),
				PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "fechaUltimoMensaje")));

		assertEquals(2, pagina.getTotalElements());
		assertEquals("Reciente", pagina.getContent().get(0).getTitulo());
		assertEquals("Antigua", pagina.getContent().get(1).getTitulo());
	}

	@Test
	void eliminarLaConversacionBorraSusMensajes() {
		ConversacionAgente conversacion = nuevaConversacion("A eliminar");
		conversacion.agregarMensaje(new MensajeAgente(RolMensaje.USUARIO, "Consulta", false, "[]", "[]", BASE));
		conversacion.agregarMensaje(new MensajeAgente(RolMensaje.AGENTE, "Respuesta", false, "[]", "[]",
				BASE.plusSeconds(1)));
		conversaciones.saveAndFlush(conversacion);
		long idConversacion = conversacion.getId();

		conversaciones.deleteById(idConversacion);
		conversaciones.flush();

		assertEquals(0, mensajes.countByConversacionId(idConversacion));
		assertFalse(conversaciones.existsById(idConversacion));
	}

	@Test
	void recuperaLosMensajesDeUnaConversacionEnOrden() {
		ConversacionAgente conversacion = nuevaConversacion("Orden");
		conversacion.agregarMensaje(new MensajeAgente(RolMensaje.USUARIO, "Primero", false, "[]", "[]", BASE));
		conversacion.agregarMensaje(new MensajeAgente(RolMensaje.AGENTE, "Segundo", false, "[]", "[]",
				BASE.plusSeconds(1)));
		conversaciones.saveAndFlush(conversacion);
		em.clear();

		List<MensajeAgente> recuperados = mensajes.findByConversacionIdOrderByFechaAscIdAsc(conversacion.getId());

		assertEquals(List.of("Primero", "Segundo"), recuperados.stream().map(MensajeAgente::getTexto).toList());
	}

	private ConversacionAgente nuevaConversacion(String titulo) {
		return new ConversacionAgente(usuario, titulo, BASE);
	}
}
