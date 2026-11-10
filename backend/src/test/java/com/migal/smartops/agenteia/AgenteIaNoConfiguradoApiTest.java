package com.migal.smartops.agenteia;

import static com.migal.smartops.contrato.ValidadorContrato.cumple;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.jayway.jsonpath.JsonPath;
import com.migal.smartops.TestcontainersConfiguration;
import com.migal.smartops.contrato.ValidadorContrato;
import com.migal.smartops.seguridad.model.RolUsuario;
import com.migal.smartops.seguridad.model.Usuario;
import com.migal.smartops.seguridad.repository.UsuarioRepository;
import java.time.Instant;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Prueba del contrato del agente de IA con el proveedor no configurado: toda consulta responde SERVICIO_NO_DISPONIBLE (503),
 * y la respuesta cumple el contrato. Se ejecuta en un contexto propio con {@code migal.agente.proveedor=no-configurado}.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "migal.agente.proveedor=no-configurado")
@AutoConfigureMockMvc
class AgenteIaNoConfiguradoApiTest {

	private static final String CLAVE = "Clave12345";
	private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

	private final OpenApiInteractionValidator contratoSoloRespuesta = ValidadorContrato.soloRespuesta("agente-ia");

	@Autowired
	private MockMvc mvc;
	@Autowired
	private UsuarioRepository usuarios;
	@Autowired
	private PasswordEncoder codificador;
	@Autowired
	private JdbcTemplate jdbc;

	@BeforeEach
	void crearUsuario() {
		Usuario usuario = new Usuario("Prueba", "Agente", "agente-sin-proveedor", codificador.encode(CLAVE),
				RolUsuario.ALMACENERO);
		usuario.cambiarEstado(true, Instant.now());
		usuarios.saveAndFlush(usuario);
	}

	@AfterEach
	void limpiarUsuario() {
		jdbc.update("DELETE FROM usuario WHERE username LIKE 'agente-sin-proveedor%'");
	}

	@Test
	void consultaResponde503SinInventarRespuesta() throws Exception {
		String token = bearer();

		mvc.perform(post("/api/v1/agente/mensajes").contentType(JSON).content("{\"mensaje\":\"Disponibilidad de cemento\"}")
						.header("Authorization", token))
				.andExpect(status().isServiceUnavailable())
				.andExpect(cumple(contratoSoloRespuesta))
				.andExpect(jsonPath("$.code", is("SERVICIO_NO_DISPONIBLE")));
	}

	private String bearer() throws Exception {
		MvcResult resultado = mvc.perform(post("/api/v1/sesiones").contentType(JSON)
						.content("{\"username\":\"agente-sin-proveedor\",\"clave\":\"" + CLAVE + "\"}"))
				.andExpect(status().isOk()).andReturn();
		return "Bearer " + JsonPath.read(resultado.getResponse().getContentAsString(), "$.token");
	}
}
