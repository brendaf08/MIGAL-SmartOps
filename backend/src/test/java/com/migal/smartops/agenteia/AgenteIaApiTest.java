package com.migal.smartops.agenteia;

import static com.migal.smartops.contrato.ValidadorContrato.cumple;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.jayway.jsonpath.JsonPath;
import com.migal.smartops.TestcontainersConfiguration;
import com.migal.smartops.contrato.ValidadorContrato;
import com.migal.smartops.inventario.EscenarioInventario;
import com.migal.smartops.seguridad.model.RolUsuario;
import com.migal.smartops.seguridad.model.Usuario;
import com.migal.smartops.seguridad.repository.UsuarioRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Prueba de contrato del módulo del agente de IA: cada solicitud y respuesta se valida contra
 * {@code backend/api/modulos/agente-ia.yaml} (ADR-010). Se comprueban los roles, las respuestas de error declaradas, la
 * propiedad de las conversaciones y el catálogo de herramientas por rol. El proveedor es el simulado.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class AgenteIaApiTest {

	private static final String CLAVE = "Clave12345";
	private static final String JSON = MediaType.APPLICATION_JSON_VALUE;
	private static final String RUTA = "/api/v1/agente";

	private final OpenApiInteractionValidator contrato = ValidadorContrato.paraModulo("agente-ia");
	private final OpenApiInteractionValidator contratoSoloRespuesta = ValidadorContrato.soloRespuesta("agente-ia");

	@Autowired
	private MockMvc mvc;
	@Autowired
	private UsuarioRepository usuarios;
	@Autowired
	private PasswordEncoder codificador;
	@Autowired
	private JdbcTemplate jdbc;
	@Autowired
	private JwtEncoder emisor;

	private EscenarioInventario escenario;
	private String sku;

	@BeforeEach
	void prepararDatos() {
		escenario = new EscenarioInventario(jdbc);
		crearUsuario("agente-vendedor", RolUsuario.VENDEDOR);
		crearUsuario("agente-almacenero", RolUsuario.ALMACENERO);
		crearUsuario("agente-administrador", RolUsuario.ADMINISTRADOR);
		crearUsuario("agente-reponedor", RolUsuario.REPONEDOR);
		crearUsuario("agente-otro", RolUsuario.ALMACENERO);
		long producto = escenario.producto("0");
		escenario.inventario(producto, escenario.almacen("almacen"), "10");
		sku = jdbc.queryForObject("SELECT sku FROM producto WHERE id_producto = ?", String.class, producto);
	}

	@AfterEach
	void limpiarDatos() {
		jdbc.update("""
				DELETE FROM conversacion_agente WHERE id_usuario IN (
				    SELECT id_usuario FROM usuario WHERE username LIKE 'agente-%')
				""");
		jdbc.update("DELETE FROM usuario WHERE username LIKE 'agente-%'");
		escenario.limpiar();
	}

	@Test
	void vendedorEnviaConsultaSinDatosYLaRespuestaEsIncierta() throws Exception {
		mvc.perform(post(RUTA + "/mensajes").contentType(JSON).content("{\"mensaje\":\"Hola\"}")
						.header("Authorization", bearer("agente-vendedor")))
				.andExpect(status().isCreated())
				.andExpect(cumple(contrato))
				.andExpect(jsonPath("$.incierta", is(true)))
				.andExpect(jsonPath("$.herramientas", hasSize(0)));
	}

	@Test
	void almaceneroConsultaDisponibilidadYObtieneLaReferencia() throws Exception {
		mvc.perform(post(RUTA + "/mensajes").contentType(JSON).content("{\"mensaje\":\"Disponibilidad de " + sku + "\"}")
						.header("Authorization", bearer("agente-almacenero")))
				.andExpect(status().isCreated())
				.andExpect(cumple(contrato))
				.andExpect(jsonPath("$.incierta", is(false)))
				.andExpect(jsonPath("$.herramientas[0].nombre", is("consultarDisponibilidad")))
				.andExpect(jsonPath("$.herramientas[0].codigo", is((Object) null)))
				.andExpect(jsonPath("$.referencias[0].tipo", is("producto")))
				.andExpect(jsonPath("$.referencias[0].codigo", is(sku)));
	}

	@Test
	void continuarConversacionYListarlaYConsultarlaCumplenElContrato() throws Exception {
		String token = bearer("agente-almacenero");
		String conversacion = JsonPath.read(mvc.perform(post(RUTA + "/mensajes").contentType(JSON)
						.content("{\"mensaje\":\"Disponibilidad de " + sku + "\"}").header("Authorization", token))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.conversacionId");

		mvc.perform(post(RUTA + "/mensajes").contentType(JSON)
						.content("{\"mensaje\":\"Alertas de stock\",\"conversacionId\":\"" + conversacion + "\"}")
						.header("Authorization", token))
				.andExpect(status().isCreated())
				.andExpect(cumple(contrato));

		mvc.perform(get(RUTA + "/conversaciones/" + conversacion).header("Authorization", token))
				.andExpect(status().isOk())
				.andExpect(cumple(contrato))
				.andExpect(jsonPath("$.mensajes", hasSize(4)))
				.andExpect(jsonPath("$.mensajes[0].autor", is("usuario")))
				.andExpect(jsonPath("$.mensajes[1].autor", is("agente")));

		mvc.perform(get(RUTA + "/conversaciones").param("page", "1").param("pageSize", "5")
						.param("orden", "fechaUltimoMensaje:desc").header("Authorization", token))
				.andExpect(status().isOk())
				.andExpect(cumple(contrato))
				.andExpect(jsonPath("$.total", greaterThanOrEqualTo(1)));

		mvc.perform(get(RUTA + "/conversaciones").param("orden", "titulo:asc").header("Authorization", token))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(cumple(contratoSoloRespuesta))
				.andExpect(jsonPath("$.code", is("VALIDACION_FALLIDA")));
	}

	@Test
	void mensajeVacioAusenteOMalformadoRespondeSegunElContrato() throws Exception {
		String token = bearer("agente-almacenero");

		mvc.perform(post(RUTA + "/mensajes").contentType(JSON).content("{\"mensaje\":\"   \"}")
						.header("Authorization", token))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(cumple(contratoSoloRespuesta))
				.andExpect(jsonPath("$.code", is("VALIDACION_FALLIDA")));

		mvc.perform(post(RUTA + "/mensajes").contentType(JSON).content("{}").header("Authorization", token))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(cumple(contratoSoloRespuesta));

		mvc.perform(post(RUTA + "/mensajes").contentType(JSON).content("{").header("Authorization", token))
				.andExpect(status().isBadRequest())
				.andExpect(cumple(contratoSoloRespuesta));
	}

	@Test
	void solicitudSinTokenRespondeNoAutenticado() throws Exception {
		mvc.perform(post(RUTA + "/mensajes").contentType(JSON).content("{\"mensaje\":\"Hola\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(cumple(contratoSoloRespuesta))
				.andExpect(jsonPath("$.code", is("NO_AUTENTICADO")));
	}

	@Test
	void tokenDeCuentaDesactivadaSeRechazaComoNoAutenticado() throws Exception {
		String token = bearer("agente-vendedor");
		Usuario usuario = usuarios.findByUsername("agente-vendedor").orElseThrow();
		usuario.cambiarEstado(false, Instant.now());
		usuarios.saveAndFlush(usuario);

		mvc.perform(get(RUTA + "/herramientas").header("Authorization", token))
				.andExpect(status().isUnauthorized())
				.andExpect(cumple(contratoSoloRespuesta))
				.andExpect(jsonPath("$.code", is("NO_AUTENTICADO")));
	}

	@Test
	void conversacionAjenaOInexistenteRespondeNoEncontrado() throws Exception {
		String propietario = bearer("agente-almacenero");
		String conversacion = JsonPath.read(mvc.perform(post(RUTA + "/mensajes").contentType(JSON)
						.content("{\"mensaje\":\"Hola\"}").header("Authorization", propietario))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.conversacionId");
		String otro = bearer("agente-otro");

		mvc.perform(get(RUTA + "/conversaciones/" + conversacion).header("Authorization", otro))
				.andExpect(status().isNotFound())
				.andExpect(cumple(contratoSoloRespuesta))
				.andExpect(jsonPath("$.code", is("RECURSO_NO_ENCONTRADO")));

		mvc.perform(delete(RUTA + "/conversaciones/" + conversacion).header("Authorization", otro))
				.andExpect(status().isNotFound())
				.andExpect(cumple(contratoSoloRespuesta));

		mvc.perform(get(RUTA + "/conversaciones/999999999").header("Authorization", propietario))
				.andExpect(status().isNotFound())
				.andExpect(cumple(contratoSoloRespuesta));
	}

	@Test
	void eliminarConversacionPropiaResponde204YLaConsultaPosteriorNoLaEncuentra() throws Exception {
		String token = bearer("agente-reponedor");
		String conversacion = JsonPath.read(mvc.perform(post(RUTA + "/mensajes").contentType(JSON)
						.content("{\"mensaje\":\"Hola\"}").header("Authorization", token))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.conversacionId");

		mvc.perform(delete(RUTA + "/conversaciones/" + conversacion).header("Authorization", token))
				.andExpect(status().isNoContent());

		mvc.perform(get(RUTA + "/conversaciones/" + conversacion).header("Authorization", token))
				.andExpect(status().isNotFound())
				.andExpect(cumple(contratoSoloRespuesta));
	}

	@Test
	void catalogoDelRolCumpleElContratoYExcluyeLasHerramientasRestringidas() throws Exception {
		mvc.perform(get(RUTA + "/herramientas").header("Authorization", bearer("agente-vendedor")))
				.andExpect(status().isOk())
				.andExpect(cumple(contrato))
				.andExpect(jsonPath("$[*].nombre", hasItem("consultarDisponibilidad")))
				.andExpect(jsonPath("$[*].nombre", not(hasItem("listarAlertasStock"))));

		mvc.perform(get(RUTA + "/herramientas").header("Authorization", bearer("agente-administrador")))
				.andExpect(status().isOk())
				.andExpect(cumple(contrato))
				.andExpect(jsonPath("$[*].nombre", hasItem("listarAlertasStock")));
	}

	@Test
	void tokenDeServicioSinRolDeUsuarioResponde403() throws Exception {
		Instant ahora = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder().subject("servicio-ocr").claim("tipo", "servicio")
				.claim("alcances", List.of("ocr:resultados:escribir")).issuedAt(ahora)
				.expiresAt(ahora.plusSeconds(300)).build();
		String token = "Bearer " + emisor.encode(JwtEncoderParameters.from(
				JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();

		mvc.perform(post(RUTA + "/mensajes").contentType(JSON).content("{\"mensaje\":\"Hola\"}")
						.header("Authorization", token))
				.andExpect(status().isForbidden())
				.andExpect(cumple(contratoSoloRespuesta))
				.andExpect(jsonPath("$.code", is("PERMISO_INSUFICIENTE")));
	}

	private void crearUsuario(String username, RolUsuario rol) {
		Usuario usuario = new Usuario("Prueba", "Agente", username, codificador.encode(CLAVE), rol);
		usuario.cambiarEstado(true, Instant.now());
		usuarios.saveAndFlush(usuario);
	}

	private String bearer(String username) throws Exception {
		ResultActions sesion = mvc.perform(post("/api/v1/sesiones").contentType(JSON)
				.content("{\"username\":\"" + username + "\",\"clave\":\"" + CLAVE + "\"}"));
		MvcResult resultado = sesion.andExpect(status().isOk()).andReturn();
		return "Bearer " + JsonPath.read(resultado.getResponse().getContentAsString(), "$.token");
	}
}
