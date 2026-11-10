package com.migal.smartops.inventario;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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
 * Prueba de contrato del módulo de inventario: cada solicitud y cada respuesta se valida contra
 * {@code backend/api/modulos/inventario.yaml} (ADR-010), y se comprueban los roles, los errores y la
 * idempotencia de las operaciones de escritura.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class InventarioApiTest {

	private static final String CLAVE = "Clave12345";
	private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

	private final OpenApiInteractionValidator contrato = ValidadorContrato.paraModulo("inventario");
	private final OpenApiInteractionValidator contratoSoloRespuesta = ValidadorContrato.soloRespuesta("inventario");

	@Autowired
	private MockMvc mvc;
	@Autowired
	private UsuarioRepository usuarios;
	@Autowired
	private PasswordEncoder codificador;
	@Autowired
	private JdbcTemplate jdbc;

	private final List<Usuario> usuariosDePrueba = new ArrayList<>();
	private EscenarioInventario escenario;
	private long producto;
	private long tienda;
	private long deposito;
	private String sku;

	@BeforeEach
	void preparar() {
		crearUsuario("inv-almacenero", RolUsuario.ALMACENERO);
		crearUsuario("inv-admin", RolUsuario.ADMINISTRADOR);
		crearUsuario("inv-vendedor", RolUsuario.VENDEDOR);
		crearUsuario("inv-reponedor", RolUsuario.REPONEDOR);
		escenario = new EscenarioInventario(jdbc);
		producto = escenario.producto("10");
		tienda = escenario.almacen("tienda");
		deposito = escenario.almacen("almacen");
		escenario.inventario(producto, tienda, "40");
		sku = jdbc.queryForObject("SELECT sku FROM producto WHERE id_producto = ?", String.class, producto);
	}

	@AfterEach
	void limpiar() {
		escenario.limpiar();
		jdbc.update("DELETE FROM idempotencia WHERE id_usuario IN (SELECT id_usuario FROM usuario WHERE username LIKE 'inv-%')");
		usuarios.deleteAll(usuariosDePrueba);
		usuariosDePrueba.clear();
	}

	private void crearUsuario(String username, RolUsuario rol) {
		Usuario usuario = new Usuario("Rosa", "Quispe", username, codificador.encode(CLAVE), rol);
		usuario.cambiarEstado(true, Instant.now());
		usuariosDePrueba.add(usuarios.saveAndFlush(usuario));
	}

	private String bearer(String username) throws Exception {
		MvcResult r = mvc.perform(post("/api/v1/sesiones").contentType(JSON)
						.content("{\"username\":\"" + username + "\",\"clave\":\"" + CLAVE + "\"}"))
				.andExpect(status().isOk()).andReturn();
		return "Bearer " + JsonPath.read(r.getResponse().getContentAsString(), "$.token");
	}

	private String movimiento(String tipo, long ubicacion, String cantidad, String motivo) {
		return "{\"tipoOperacion\":\"" + tipo + "\",\"productoId\":\"" + producto + "\",\"ubicacionId\":\"" + ubicacion
				+ "\",\"cantidad\":" + cantidad + ",\"motivo\":\"" + motivo + "\",\"idMovimientoCorregido\":null}";
	}

	private String traslado(long origen, long destino, String cantidad) {
		return "{\"productoId\":\"" + producto + "\",\"ubicacionOrigenId\":\"" + origen + "\",\"ubicacionDestinoId\":\""
				+ destino + "\",\"cantidad\":" + cantidad + ",\"motivo\":\"Reposición\"}";
	}

	private static String clave() {
		return UUID.randomUUID().toString();
	}

	@Test
	void consultaLaDisponibilidadConFiltros() throws Exception {
		mvc.perform(get("/api/v1/inventario/disponibilidad").header("Authorization", bearer("inv-vendedor"))
						.param("sku", sku))
				.andExpect(status().isOk())
				.andExpect(ValidadorContrato.cumple(contrato))
				.andExpect(jsonPath("$.items", hasSize(1)))
				.andExpect(jsonPath("$.items[0].productoId", is(String.valueOf(producto))))
				.andExpect(jsonPath("$.items[0].fisico", is(40.0)))
				.andExpect(jsonPath("$.items[0].ubicaciones", hasSize(1)))
				.andExpect(jsonPath("$.totalGeneral", greaterThanOrEqualTo(1)))
				.andExpect(jsonPath("$.page", is(1)));
	}

	@Test
	void consultaLasExistenciasDeUnProductoYResponde404SiNoExiste() throws Exception {
		mvc.perform(get("/api/v1/productos/{id}/existencias", producto).header("Authorization", bearer("inv-almacenero")))
				.andExpect(status().isOk())
				.andExpect(ValidadorContrato.cumple(contrato))
				.andExpect(jsonPath("$.libre", is(40.0)))
				.andExpect(jsonPath("$.nivelAlerta", is("normal")));
		mvc.perform(get("/api/v1/productos/{id}/existencias", "999999999").header("Authorization", bearer("inv-almacenero")))
				.andExpect(status().isNotFound())
				.andExpect(ValidadorContrato.cumple(contrato))
				.andExpect(jsonPath("$.code", is("RECURSO_NO_ENCONTRADO")));
	}

	@Test
	void listaLasAlertasSoloParaLosRolesAutorizados() throws Exception {
		mvc.perform(get("/api/v1/inventario/alertas").header("Authorization", bearer("inv-reponedor")).param("nivel", "critico"))
				.andExpect(status().isOk())
				.andExpect(ValidadorContrato.cumple(contrato));
		mvc.perform(get("/api/v1/inventario/alertas").header("Authorization", bearer("inv-vendedor")))
				.andExpect(status().isForbidden())
				.andExpect(ValidadorContrato.cumple(contrato))
				.andExpect(jsonPath("$.code", is("PERMISO_INSUFICIENTE")));
		mvc.perform(get("/api/v1/inventario/alertas").header("Authorization", bearer("inv-admin")).param("nivel", "grave"))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(ValidadorContrato.cumple(contratoSoloRespuesta));
	}

	@Test
	void sinCredencialesResponde401() throws Exception {
		mvc.perform(get("/api/v1/inventario/disponibilidad"))
				.andExpect(status().isUnauthorized())
				.andExpect(ValidadorContrato.cumple(contratoSoloRespuesta));
	}

	@Test
	void registraUnMovimientoYLoListaEnElKardex() throws Exception {
		String bearer = bearer("inv-almacenero");

		mvc.perform(post("/api/v1/inventario/movimientos").header("Authorization", bearer).header("Idempotency-Key", clave())
						.contentType(JSON).content(movimiento("salida", tienda, "5", "Bolsas dañadas")))
				.andExpect(status().isCreated())
				.andExpect(ValidadorContrato.cumple(contrato))
				.andExpect(jsonPath("$.movimientos", hasSize(1)))
				.andExpect(jsonPath("$.movimientos[0].origenMovimiento", is("ajuste")))
				.andExpect(jsonPath("$.movimientos[0].referenciaTraslado", nullValue()))
				.andExpect(jsonPath("$.cambios[0].antes", is(40.0)))
				.andExpect(jsonPath("$.cambios[0].despues", is(35.0)));

		mvc.perform(get("/api/v1/inventario/movimientos").header("Authorization", bearer).param("sku", sku)
						.param("origen", "ajuste").param("tipoOperacion", "salida"))
				.andExpect(status().isOk())
				.andExpect(ValidadorContrato.cumple(contrato))
				.andExpect(jsonPath("$.items", hasSize(1)))
				.andExpect(jsonPath("$.items[0].motivo", is("Bolsas dañadas")))
				.andExpect(jsonPath("$.items[0].fechaHora", notNullValue()));
	}

	@Test
	void repiteLaRespuestaConLaMismaClaveYRechazaOtraSolicitud() throws Exception {
		String bearer = bearer("inv-almacenero");
		String clave = clave();
		String cuerpo = movimiento("salida", tienda, "5", "Merma");

		MvcResult primera = mvc.perform(post("/api/v1/inventario/movimientos").header("Authorization", bearer)
						.header("Idempotency-Key", clave).contentType(JSON).content(cuerpo))
				.andExpect(status().isCreated()).andReturn();
		MvcResult repetida = mvc.perform(post("/api/v1/inventario/movimientos").header("Authorization", bearer)
						.header("Idempotency-Key", clave).contentType(JSON).content(cuerpo))
				.andExpect(status().isCreated())
				.andExpect(ValidadorContrato.cumple(contrato))
				.andExpect(header().string("Idempotent-Replayed", "true"))
				.andReturn();

		String idPrimera = JsonPath.read(primera.getResponse().getContentAsString(), "$.movimientos[0].id");
		String idRepetida = JsonPath.read(repetida.getResponse().getContentAsString(), "$.movimientos[0].id");
		org.junit.jupiter.api.Assertions.assertEquals(idPrimera, idRepetida);
		org.junit.jupiter.api.Assertions.assertEquals(1, escenario.movimientos(producto));
		mvc.perform(post("/api/v1/inventario/movimientos").header("Authorization", bearer).header("Idempotency-Key", clave)
						.contentType(JSON).content(movimiento("salida", tienda, "6", "Merma")))
				.andExpect(status().isConflict())
				.andExpect(ValidadorContrato.cumple(contrato))
				.andExpect(jsonPath("$.code", is("IDEMPOTENCIA_CONFLICTO")));
	}

	@Test
	void rechazaUnaSalidaMayorAlStockConConflicto() throws Exception {
		mvc.perform(post("/api/v1/inventario/movimientos").header("Authorization", bearer("inv-almacenero"))
						.header("Idempotency-Key", clave()).contentType(JSON).content(movimiento("salida", tienda, "41", "Merma")))
				.andExpect(status().isConflict())
				.andExpect(ValidadorContrato.cumple(contrato))
				.andExpect(jsonPath("$.code", is("STOCK_INSUFICIENTE")));
	}

	@Test
	void exigeLaClaveDeIdempotenciaYUnCuerpoValido() throws Exception {
		String bearer = bearer("inv-almacenero");
		mvc.perform(post("/api/v1/inventario/movimientos").header("Authorization", bearer).contentType(JSON)
						.content(movimiento("salida", tienda, "1", "Merma")))
				.andExpect(status().isBadRequest())
				.andExpect(ValidadorContrato.cumple(contratoSoloRespuesta))
				.andExpect(jsonPath("$.code", is("SOLICITUD_MAL_FORMADA")));
		mvc.perform(post("/api/v1/inventario/movimientos").header("Authorization", bearer).header("Idempotency-Key", clave())
						.contentType(JSON).content(movimiento("salida", tienda, "-1", "Merma")))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(ValidadorContrato.cumple(contratoSoloRespuesta))
				.andExpect(jsonPath("$.code", is("VALIDACION_FALLIDA")));
		mvc.perform(post("/api/v1/inventario/movimientos").header("Authorization", bearer).header("Idempotency-Key", clave())
						.contentType(JSON).content(movimiento("salida", tienda, "1", "   ")))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(ValidadorContrato.cumple(contratoSoloRespuesta))
				.andExpect(jsonPath("$.code", is("MOTIVO_REQUERIDO")));
	}

	@Test
	void soloElAlmaceneroRegistraMovimientosManuales() throws Exception {
		mvc.perform(post("/api/v1/inventario/movimientos").header("Authorization", bearer("inv-admin"))
						.header("Idempotency-Key", clave()).contentType(JSON).content(movimiento("salida", tienda, "1", "Merma")))
				.andExpect(status().isForbidden())
				.andExpect(ValidadorContrato.cumple(contrato));
	}

	@Test
	void registraUnTrasladoYDevuelveSusDosMovimientos() throws Exception {
		mvc.perform(post("/api/v1/inventario/traslados").header("Authorization", bearer("inv-admin"))
						.header("Idempotency-Key", clave()).contentType(JSON).content(traslado(tienda, deposito, "15")))
				.andExpect(status().isCreated())
				.andExpect(ValidadorContrato.cumple(contrato))
				.andExpect(jsonPath("$.movimientos", hasSize(2)))
				.andExpect(jsonPath("$.movimientos[0].tipoOperacion", is("salida")))
				.andExpect(jsonPath("$.movimientos[1].tipoOperacion", is("entrada")))
				.andExpect(jsonPath("$.cambios", hasSize(2)))
				.andExpect(jsonPath("$.cambios[0].despues", is(25.0)))
				.andExpect(jsonPath("$.cambios[1].despues", is(15.0)));
	}

	@Test
	void rechazaTrasladosInvalidos() throws Exception {
		String bearer = bearer("inv-almacenero");
		mvc.perform(post("/api/v1/inventario/traslados").header("Authorization", bearer).header("Idempotency-Key", clave())
						.contentType(JSON).content(traslado(tienda, deposito, "41")))
				.andExpect(status().isConflict())
				.andExpect(ValidadorContrato.cumple(contrato))
				.andExpect(jsonPath("$.code", is("STOCK_INSUFICIENTE")));
		mvc.perform(post("/api/v1/inventario/traslados").header("Authorization", bearer).header("Idempotency-Key", clave())
						.contentType(JSON).content(traslado(tienda, tienda, "1")))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(ValidadorContrato.cumple(contrato))
				.andExpect(jsonPath("$.code", is("VALIDACION_FALLIDA")));
		mvc.perform(post("/api/v1/inventario/traslados").header("Authorization", bearer("inv-vendedor"))
						.header("Idempotency-Key", clave()).contentType(JSON).content(traslado(tienda, deposito, "1")))
				.andExpect(status().isForbidden())
				.andExpect(ValidadorContrato.cumple(contrato));
	}
}
