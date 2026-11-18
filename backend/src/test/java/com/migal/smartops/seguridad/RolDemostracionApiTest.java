package com.migal.smartops.seguridad;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import com.migal.smartops.seguridad.service.LimitadorIntentosSesion;
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

/**
 * Rol de demostración habilitado (ADR-013): inicia sesión, cumple el contrato y accede a operaciones exclusivas de
 * roles distintos, mientras la API de usuarios sigue sin poder asignarlo.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "migal.seguridad.demostracion.habilitada=true")
@AutoConfigureMockMvc
class RolDemostracionApiTest {

	private static final String CLAVE = "Clave12345";
	private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

	private final OpenApiInteractionValidator contrato = ValidadorContrato.paraModulo("autenticacion-usuarios");

	@Autowired
	private MockMvc mvc;

	@Autowired
	private UsuarioRepository usuarios;

	@Autowired
	private PasswordEncoder codificador;

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private LimitadorIntentosSesion limitador;

	@BeforeEach
	void prepararDatos() {
		limitador.limpiar();
		jdbc.update("DELETE FROM usuario");
		usuarios.saveAndFlush(new Usuario("Usuario", "Demostración", "demo01", codificador.encode(CLAVE),
				RolUsuario.DEMOSTRACION));
		usuarios.saveAndFlush(new Usuario("Lucía", "Torres", "vende01", codificador.encode(CLAVE), RolUsuario.VENDEDOR));
	}

	private String bearer(String username) throws Exception {
		String cuerpo = mvc.perform(post("/api/v1/sesiones").contentType(JSON)
						.content("{\"username\":\"" + username + "\",\"clave\":\"" + CLAVE + "\"}"))
				.andExpect(status().isOk())
				.andExpect(ValidadorContrato.cumple(contrato))
				.andReturn().getResponse().getContentAsString();
		return "Bearer " + JsonPath.read(cuerpo, "$.token");
	}

	@Test
	void iniciaSesionConElRolDeDemostracion() throws Exception {
		mvc.perform(get("/api/v1/sesiones/actual").header("Authorization", bearer("demo01")))
				.andExpect(status().isOk())
				.andExpect(ValidadorContrato.cumple(contrato))
				.andExpect(jsonPath("$.rol", is("demostracion")));
	}

	@Test
	void accedeAOperacionesExclusivasDeDistintosRoles() throws Exception {
		String demo = bearer("demo01");
		String vendedor = bearer("vende01");

		// Solo Administrador.
		mvc.perform(get("/api/v1/usuarios").header("Authorization", demo))
				.andExpect(status().isOk())
				.andExpect(ValidadorContrato.cumple(contrato));
		mvc.perform(get("/api/v1/usuarios").header("Authorization", vendedor)).andExpect(status().isForbidden());
		// Solo Almacenero.
		mvc.perform(get("/api/v1/ordenes-compra/recepcionables").header("Authorization", demo))
				.andExpect(status().isOk());
		mvc.perform(get("/api/v1/ordenes-compra/recepcionables").header("Authorization", vendedor))
				.andExpect(status().isForbidden());
		// Reponedor y Administrador.
		mvc.perform(get("/api/v1/proveedores").header("Authorization", demo)).andExpect(status().isOk());
	}

	@Test
	void laApiDeUsuariosNoAsignaElRolDeDemostracion() throws Exception {
		mvc.perform(post("/api/v1/usuarios").header("Authorization", bearer("demo01")).contentType(JSON)
						.content("{\"nombres\":\"Otro\",\"apellidos\":\"Demo\",\"username\":\"demo02\","
								+ "\"claveTemporal\":\"Temporal2026\",\"rol\":\"demostracion\"}"))
				.andExpect(status().isUnprocessableEntity());
	}
}
