package com.migal.smartops.seguridad;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
 * Prueba de contrato del módulo de autenticación y usuarios: cada solicitud y cada respuesta se valida contra
 * {@code backend/api/modulos/autenticacion-usuarios.yaml} (ADR-010) y se comprueba el comportamiento.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class SeguridadApiTest {

	private static final String CLAVE = "Clave12345";
	private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

	private final OpenApiInteractionValidator contrato = ValidadorContrato.paraModulo("autenticacion-usuarios");
	private final OpenApiInteractionValidator contratoSoloRespuesta =
			ValidadorContrato.soloRespuesta("autenticacion-usuarios");

	@Autowired
	private MockMvc mvc;

	@Autowired
	private UsuarioRepository usuarios;

	@Autowired
	private PasswordEncoder codificador;

	@Autowired
	private JdbcTemplate jdbc;

	@BeforeEach
	void prepararDatos() {
		jdbc.update("DELETE FROM usuario");
		crear("admin01", RolUsuario.ADMINISTRADOR, true);
		crear("vende01", RolUsuario.VENDEDOR, true);
		crear("baja01", RolUsuario.ALMACENERO, false);
	}

	private Usuario crear(String username, RolUsuario rol, boolean activo) {
		Usuario usuario = new Usuario("Rosa", "Quispe", username, codificador.encode(CLAVE), rol);
		usuario.cambiarEstado(activo, Instant.now());
		return usuarios.saveAndFlush(usuario);
	}

	private String iniciarSesion(String username) throws Exception {
		MvcResult resultado = mvc.perform(post("/api/v1/sesiones").contentType(JSON)
						.content("{\"username\":\"" + username + "\",\"clave\":\"" + CLAVE + "\"}"))
				.andExpect(status().isOk())
				.andExpect(ValidadorContrato.cumple(contrato))
				.andReturn();
		return JsonPath.read(resultado.getResponse().getContentAsString(), "$.token");
	}

	private String bearer(String username) throws Exception {
		return "Bearer " + iniciarSesion(username);
	}

	@Test
	void iniciaSesionYDevuelveTokenExpiracionYUsuarioSinClave() throws Exception {
		mvc.perform(post("/api/v1/sesiones").contentType(JSON)
						.content("{\"username\":\"admin01\",\"clave\":\"" + CLAVE + "\"}"))
				.andExpect(status().isOk())
				.andExpect(ValidadorContrato.cumple(contrato))
				.andExpect(jsonPath("$.token", notNullValue()))
				.andExpect(jsonPath("$.expiraEn", is(28800)))
				.andExpect(jsonPath("$.usuario.rol", is("administrador")))
				.andExpect(jsonPath("$.usuario.passwordHash").doesNotExist());
	}

	@Test
	void rechazaCredencialesIncorrectasConProblemaJson() throws Exception {
		mvc.perform(post("/api/v1/sesiones").contentType(JSON)
						.content("{\"username\":\"admin01\",\"clave\":\"otra\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(ValidadorContrato.cumple(contrato))
				.andExpect(jsonPath("$.code", is("NO_AUTENTICADO")));
	}

	@Test
	void rechazaUnaCuentaDesactivadaConCredencialesCorrectas() throws Exception {
		mvc.perform(post("/api/v1/sesiones").contentType(JSON)
						.content("{\"username\":\"baja01\",\"clave\":\"" + CLAVE + "\"}"))
				.andExpect(status().isForbidden())
				.andExpect(ValidadorContrato.cumple(contrato))
				.andExpect(jsonPath("$.code", is("CUENTA_INACTIVA")));
	}

	@Test
	void validaElCuerpoDelInicioDeSesion() throws Exception {
		mvc.perform(post("/api/v1/sesiones").contentType(JSON).content("{\"username\":\"\",\"clave\":\"x\"}"))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(ValidadorContrato.cumple(contratoSoloRespuesta))
				.andExpect(jsonPath("$.code", is("VALIDACION_FALLIDA")))
				.andExpect(jsonPath("$.errores[0].campo", is("username")));
	}

	@Test
	void devuelveLaSesionActual() throws Exception {
		mvc.perform(get("/api/v1/sesiones/actual").header("Authorization", bearer("vende01")))
				.andExpect(status().isOk())
				.andExpect(ValidadorContrato.cumple(contrato))
				.andExpect(jsonPath("$.username", is("vende01")));
	}

	@Test
	void exigeTokenEnLasOperacionesProtegidas() throws Exception {
		mvc.perform(get("/api/v1/usuarios"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code", is("NO_AUTENTICADO")));
		mvc.perform(get("/api/v1/usuarios").header("Authorization", "Bearer token-invalido"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code", is("NO_AUTENTICADO")));
	}

	@Test
	void soloElAdministradorGestionaUsuarios() throws Exception {
		mvc.perform(get("/api/v1/usuarios").header("Authorization", bearer("vende01")))
				.andExpect(status().isForbidden())
				.andExpect(ValidadorContrato.cumple(contrato))
				.andExpect(jsonPath("$.code", is("PERMISO_INSUFICIENTE")));
	}

	@Test
	void listaUsuariosPaginadosConFiltros() throws Exception {
		mvc.perform(get("/api/v1/usuarios").param("rol", "vendedor").param("pageSize", "5")
						.header("Authorization", bearer("admin01")))
				.andExpect(status().isOk())
				.andExpect(ValidadorContrato.cumple(contrato))
				.andExpect(jsonPath("$.items", hasSize(1)))
				.andExpect(jsonPath("$.total", is(1)))
				.andExpect(jsonPath("$.page", is(1)))
				.andExpect(jsonPath("$.pageSize", is(5)));
	}

	@Test
	void rechazaParametrosDePaginacionFueraDeRango() throws Exception {
		mvc.perform(get("/api/v1/usuarios").param("pageSize", "500").header("Authorization", bearer("admin01")))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(ValidadorContrato.cumple(contratoSoloRespuesta))
				.andExpect(jsonPath("$.code", is("VALIDACION_FALLIDA")));
	}

	@Test
	void registraConsultaYActualizaUnUsuario() throws Exception {
		String autorizacion = bearer("admin01");
		MvcResult creado = mvc.perform(post("/api/v1/usuarios").header("Authorization", autorizacion).contentType(JSON)
						.content("{\"nombres\":\"Ana\",\"apellidos\":\"Torres\",\"username\":\"atorres\","
								+ "\"claveTemporal\":\"Temporal2026\",\"rol\":\"almacenero\"}"))
				.andExpect(status().isCreated())
				.andExpect(ValidadorContrato.cumple(contrato))
				.andExpect(jsonPath("$.rol", is("almacenero")))
				.andExpect(jsonPath("$.claveTemporal").doesNotExist())
				.andReturn();
		String id = JsonPath.read(creado.getResponse().getContentAsString(), "$.id");

		mvc.perform(get("/api/v1/usuarios/" + id).header("Authorization", autorizacion))
				.andExpect(status().isOk())
				.andExpect(ValidadorContrato.cumple(contrato))
				.andExpect(jsonPath("$.username", is("atorres")));

		mvc.perform(put("/api/v1/usuarios/" + id).header("Authorization", autorizacion).contentType(JSON)
						.content("{\"nombres\":\"Ana María\",\"apellidos\":\"Torres\",\"rol\":\"reponedor\"}"))
				.andExpect(status().isOk())
				.andExpect(ValidadorContrato.cumple(contrato))
				.andExpect(jsonPath("$.rol", is("reponedor")))
				.andExpect(jsonPath("$.fechaActualizacion", notNullValue()));
	}

	@Test
	void rechazaUnNombreDeUsuarioDuplicado() throws Exception {
		mvc.perform(post("/api/v1/usuarios").header("Authorization", bearer("admin01")).contentType(JSON)
						.content("{\"nombres\":\"Ana\",\"apellidos\":\"Torres\",\"username\":\"vende01\","
								+ "\"claveTemporal\":\"Temporal2026\",\"rol\":\"vendedor\"}"))
				.andExpect(status().isConflict())
				.andExpect(ValidadorContrato.cumple(contrato))
				.andExpect(jsonPath("$.code", is("USUARIO_DUPLICADO")));
	}

	@Test
	void consultarUnUsuarioInexistenteResponde404() throws Exception {
		mvc.perform(get("/api/v1/usuarios/999999").header("Authorization", bearer("admin01")))
				.andExpect(status().isNotFound())
				.andExpect(ValidadorContrato.cumple(contrato))
				.andExpect(jsonPath("$.code", is("RECURSO_NO_ENCONTRADO")));
	}

	@Test
	void unUsuarioDesactivadoPierdeElAccesoDeInmediatoAunConTokenVigente() throws Exception {
		String tokenDeVendedor = bearer("vende01");
		long idVendedor = usuarios.findByUsername("vende01").orElseThrow().getId();

		mvc.perform(put("/api/v1/usuarios/" + idVendedor + "/estado").header("Authorization", bearer("admin01"))
						.contentType(JSON).content("{\"estadoActivo\":false}"))
				.andExpect(status().isOk())
				.andExpect(ValidadorContrato.cumple(contrato))
				.andExpect(jsonPath("$.estadoActivo", is(false)));

		mvc.perform(get("/api/v1/sesiones/actual").header("Authorization", tokenDeVendedor))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code", is("NO_AUTENTICADO")));
	}

	@Test
	void noPermiteDesactivarElUnicoAdministradorNiLaPropiaCuenta() throws Exception {
		long idAdmin = usuarios.findByUsername("admin01").orElseThrow().getId();

		mvc.perform(put("/api/v1/usuarios/" + idAdmin + "/estado").header("Authorization", bearer("admin01"))
						.contentType(JSON).content("{\"estadoActivo\":false}"))
				.andExpect(status().isConflict())
				.andExpect(ValidadorContrato.cumple(contrato))
				.andExpect(jsonPath("$.code", is("ADMINISTRADOR_REQUERIDO")));
	}

	@Test
	void rechazaUnCuerpoQueNoEsJson() throws Exception {
		mvc.perform(post("/api/v1/usuarios").header("Authorization", bearer("admin01")).contentType(JSON)
						.content("no es json"))
				.andExpect(status().isBadRequest())
				.andExpect(ValidadorContrato.cumple(contratoSoloRespuesta))
				.andExpect(jsonPath("$.code", is("SOLICITUD_MAL_FORMADA")));
	}

	@Test
	void unIdentificadorNoNumericoResponde404() throws Exception {
		mvc.perform(get("/api/v1/usuarios/abc").header("Authorization", bearer("admin01")))
				.andExpect(status().isNotFound())
				.andExpect(ValidadorContrato.cumple(contrato))
				.andExpect(jsonPath("$.code", is("RECURSO_NO_ENCONTRADO")));
	}
}
