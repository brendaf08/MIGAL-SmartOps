package com.migal.smartops.recepciones;

import static com.migal.smartops.contrato.ValidadorContrato.cumple;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.jayway.jsonpath.JsonPath;
import com.migal.smartops.TestcontainersConfiguration;
import com.migal.smartops.catalogo.model.Producto;
import com.migal.smartops.catalogo.repository.CategoriaRepository;
import com.migal.smartops.catalogo.repository.ProductoRepository;
import com.migal.smartops.contrato.ValidadorContrato;
import com.migal.smartops.seguridad.model.RolUsuario;
import com.migal.smartops.seguridad.model.Usuario;
import com.migal.smartops.seguridad.repository.UsuarioRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Prueba de contrato del módulo de recepciones: cada solicitud y respuesta se valida contra
 * {@code backend/api/modulos/recepciones.yaml} (ADR-010). Se comprueban los roles, la validación con cabecera
 * idempotente, la repetición de la respuesta y los códigos de error de la validación. Los datos se eliminan al final:
 * proveedores RCP, productos RCP-, almacenes RCP y usuarios rcp-.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class RecepcionesApiTest {

	private static final String CLAVE = "Clave12345";
	private static final String JSON = MediaType.APPLICATION_JSON_VALUE;
	private static final String CEMENTO = "Cemento y aglomerantes";

	private final OpenApiInteractionValidator contrato = ValidadorContrato.paraModulo("recepciones");
	private final OpenApiInteractionValidator contratoSoloRespuesta = ValidadorContrato.soloRespuesta("recepciones");

	@Autowired
	private MockMvc mvc;
	@Autowired
	private UsuarioRepository usuarios;
	@Autowired
	private PasswordEncoder codificador;
	@Autowired
	private ProductoRepository productos;
	@Autowired
	private CategoriaRepository categorias;
	@Autowired
	private JdbcClient jdbc;

	private final List<Usuario> usuariosDePrueba = new ArrayList<>();
	private long idProducto;
	private long idAlmacen;
	private long idOrden;

	@BeforeEach
	void prepararDatos() {
		crearUsuario("rcp-almacenero", RolUsuario.ALMACENERO);
		crearUsuario("rcp-reponedor", RolUsuario.REPONEDOR);
		Producto producto = productos.saveAndFlush(new Producto("RCP-" + sufijo(), "Cemento de recepcion", null, "bolsa",
				new BigDecimal("28.50"), new BigDecimal("20"), categorias.findByNombre(CEMENTO).orElseThrow(), "Sol", null, true));
		idProducto = producto.getId();
		idAlmacen = jdbc.sql("INSERT INTO almacen (nombre, tipo_ubicacion) VALUES (:nombre, 'tienda') RETURNING id_almacen")
				.param("nombre", "RCP Tienda " + sufijo()).query(Long.class).single();
		long proveedor = jdbc.sql("INSERT INTO proveedor (ruc, razon_social, contacto, estado_activo) VALUES (:ruc, "
						+ "'RCP Proveedor de prueba', 'ventas@prueba.example', true) RETURNING id_proveedor")
				.param("ruc", ruc()).query(Long.class).single();
		long usuario = idUsuario("rcp-almacenero");
		idOrden = jdbc.sql("INSERT INTO orden_compra (id_proveedor, id_usuario_responsable, estado) "
						+ "VALUES (:proveedor, :usuario, 'pendiente') RETURNING id_orden")
				.param("proveedor", proveedor).param("usuario", usuario).query(Long.class).single();
		jdbc.sql("INSERT INTO detalle_compra (id_orden, id_producto, cantidad_comprada, costo_unitario) "
						+ "VALUES (:orden, :producto, 100, 26.50)")
				.param("orden", idOrden).param("producto", idProducto).update();
	}

	@AfterEach
	void limpiarDatos() {
		jdbc.sql("ALTER TABLE movimiento_kardex DISABLE TRIGGER tg_movimiento_kardex_solo_anexion").update();
		try {
			jdbc.sql("DELETE FROM movimiento_kardex WHERE id_inventario IN (SELECT id_inventario FROM inventario "
					+ "WHERE id_producto IN (SELECT id_producto FROM producto WHERE sku LIKE 'RCP-%'))").update();
		} finally {
			jdbc.sql("ALTER TABLE movimiento_kardex ENABLE TRIGGER tg_movimiento_kardex_solo_anexion").update();
		}
		jdbc.sql("DELETE FROM detalle_recepcion WHERE id_producto IN (SELECT id_producto FROM producto WHERE sku LIKE 'RCP-%')")
				.update();
		jdbc.sql("DELETE FROM orden_recepcion WHERE id_orden IN (SELECT o.id_orden FROM orden_compra o JOIN proveedor p "
				+ "ON p.id_proveedor = o.id_proveedor WHERE p.razon_social LIKE 'RCP%')").update();
		jdbc.sql("DELETE FROM detalle_compra WHERE id_orden IN (SELECT o.id_orden FROM orden_compra o JOIN proveedor p "
				+ "ON p.id_proveedor = o.id_proveedor WHERE p.razon_social LIKE 'RCP%')").update();
		jdbc.sql("DELETE FROM orden_compra WHERE id_proveedor IN (SELECT id_proveedor FROM proveedor "
				+ "WHERE razon_social LIKE 'RCP%')").update();
		jdbc.sql("DELETE FROM proveedor WHERE razon_social LIKE 'RCP%'").update();
		jdbc.sql("DELETE FROM inventario WHERE id_almacen = :almacen").param("almacen", idAlmacen).update();
		jdbc.sql("DELETE FROM almacen WHERE id_almacen = :almacen").param("almacen", idAlmacen).update();
		jdbc.sql("DELETE FROM producto WHERE sku LIKE 'RCP-%'").update();
		jdbc.sql("DELETE FROM idempotencia WHERE id_usuario IN (SELECT id_usuario FROM usuario WHERE username LIKE 'rcp-%')")
				.update();
		usuarios.deleteAll(usuariosDePrueba);
		usuariosDePrueba.clear();
	}

	@Test
	void almaceneroRegistraYValidaUnaRecepcionQueDejaLaOrdenParcial() throws Exception {
		String token = bearer("rcp-almacenero");
		String idRecepcion = registrar(token, 40);

		alta("/api/v1/recepciones/" + idRecepcion + "/validacion", token, UUID.randomUUID().toString(), null)
				.andExpect(status().isOk())
				.andExpect(cumple(contrato))
				.andExpect(jsonPath("$.recepcion.estado", is("validada")))
				.andExpect(jsonPath("$.movimientos", hasSize(1)))
				.andExpect(jsonPath("$.movimientos[0].origenMovimiento", is("recepcion")))
				.andExpect(jsonPath("$.ordenCompra.estado", is("parcial")));
	}

	@Test
	void reintentoConLaMismaClaveDevuelveLaRespuestaOriginal() throws Exception {
		String token = bearer("rcp-almacenero");
		String idRecepcion = registrar(token, 40);
		String clave = UUID.randomUUID().toString();
		alta("/api/v1/recepciones/" + idRecepcion + "/validacion", token, clave, null).andExpect(status().isOk());

		alta("/api/v1/recepciones/" + idRecepcion + "/validacion", token, clave, null)
				.andExpect(status().isOk())
				.andExpect(header().string("Idempotent-Replayed", "true"))
				.andExpect(cumple(contrato));

		alta("/api/v1/recepciones/" + idRecepcion + "/validacion", token, clave, "{\"observaciones\":\"Otra nota\"}")
				.andExpect(status().isConflict())
				.andExpect(cumple(contratoSoloRespuesta))
				.andExpect(jsonPath("$.code", is("IDEMPOTENCIA_CONFLICTO")));
	}

	@Test
	void validarSinClaveIdempotenteResponseSolicitudMalFormada() throws Exception {
		String token = bearer("rcp-almacenero");
		String idRecepcion = registrar(token, 40);

		alta("/api/v1/recepciones/" + idRecepcion + "/validacion", token, null, null)
				.andExpect(status().isBadRequest())
				.andExpect(cumple(contratoSoloRespuesta))
				.andExpect(jsonPath("$.code", is("SOLICITUD_MAL_FORMADA")));
	}

	@Test
	void segundaValidacionConOtraClaveResponseRecepcionNoConfirmable() throws Exception {
		String token = bearer("rcp-almacenero");
		String idRecepcion = registrar(token, 40);
		alta("/api/v1/recepciones/" + idRecepcion + "/validacion", token, UUID.randomUUID().toString(), null)
				.andExpect(status().isOk());

		alta("/api/v1/recepciones/" + idRecepcion + "/validacion", token, UUID.randomUUID().toString(), null)
				.andExpect(status().isConflict())
				.andExpect(cumple(contratoSoloRespuesta))
				.andExpect(jsonPath("$.code", is("RECEPCION_NO_CONFIRMABLE")));
	}

	@Test
	void sobranteSinNotaResponseSobranteSinNota() throws Exception {
		String token = bearer("rcp-almacenero");
		String idRecepcion = registrar(token, 120);

		alta("/api/v1/recepciones/" + idRecepcion + "/validacion", token, UUID.randomUUID().toString(), null)
				.andExpect(status().isUnprocessableEntity())
				.andExpect(cumple(contratoSoloRespuesta))
				.andExpect(jsonPath("$.code", is("SOBRANTE_SIN_NOTA")));
	}

	@Test
	void reponedorNoRegistraRecepciones() throws Exception {
		mvc.perform(post("/api/v1/recepciones").contentType(JSON).content(cuerpoRegistro(40))
						.header("Authorization", bearer("rcp-reponedor")))
				.andExpect(status().isForbidden())
				.andExpect(cumple(contratoSoloRespuesta));
	}

	@Test
	void consultaYListadoSeAbrenAlAlmacenero() throws Exception {
		String token = bearer("rcp-almacenero");
		String idRecepcion = registrar(token, 40);

		mvc.perform(get("/api/v1/recepciones").param("ordenCompraId", String.valueOf(idOrden))
						.header("Authorization", token))
				.andExpect(status().isOk())
				.andExpect(cumple(contrato))
				.andExpect(jsonPath("$.items", hasSize(1)));

		mvc.perform(get("/api/v1/recepciones/" + idRecepcion).header("Authorization", token))
				.andExpect(status().isOk())
				.andExpect(cumple(contrato))
				.andExpect(jsonPath("$.ordenCompra.lineas[0].cantidadPendiente", is(100.0)));
	}

	@Test
	void validadaNoSeModificaYResponseTransicionNoPermitida() throws Exception {
		String token = bearer("rcp-almacenero");
		String idRecepcion = registrar(token, 40);
		alta("/api/v1/recepciones/" + idRecepcion + "/validacion", token, UUID.randomUUID().toString(), null)
				.andExpect(status().isOk());

		mvc.perform(put("/api/v1/recepciones/" + idRecepcion).contentType(JSON)
						.content("{\"fechaRecepcion\":\"2026-11-08\",\"lineas\":[{\"productoId\":\"" + idProducto
								+ "\",\"ubicacionId\":\"" + idAlmacen + "\",\"cantidadRecibida\":10}]}")
						.header("Authorization", token))
				.andExpect(status().isConflict())
				.andExpect(cumple(contratoSoloRespuesta))
				.andExpect(jsonPath("$.code", is("TRANSICION_NO_PERMITIDA")));
	}

	/** Registra una recepción pendiente con la cantidad indicada y devuelve su identificador. */
	private String registrar(String token, int cantidad) throws Exception {
		MvcResult creada = alta("/api/v1/recepciones", token, null, cuerpoRegistro(cantidad))
				.andExpect(status().isCreated())
				.andExpect(cumple(contrato))
				.andExpect(jsonPath("$.estado", is("pendiente")))
				.andReturn();
		return JsonPath.read(creada.getResponse().getContentAsString(), "$.id");
	}

	private String cuerpoRegistro(int cantidad) {
		return "{\"ordenCompraId\":\"" + idOrden + "\",\"fechaRecepcion\":\"2026-11-07\",\"lineas\":[{\"productoId\":\""
				+ idProducto + "\",\"ubicacionId\":\"" + idAlmacen + "\",\"cantidadRecibida\":" + cantidad + "}]}";
	}

	private void crearUsuario(String username, RolUsuario rol) {
		Usuario usuario = new Usuario("Luis", "Mamani", username, codificador.encode(CLAVE), rol);
		usuario.cambiarEstado(true, Instant.now());
		usuariosDePrueba.add(usuarios.saveAndFlush(usuario));
	}

	private long idUsuario(String username) {
		return jdbc.sql("SELECT id_usuario FROM usuario WHERE username = :username").param("username", username)
				.query(Long.class).single();
	}

	private ResultActions alta(String ruta, String token, String clave, String cuerpo) throws Exception {
		var peticion = post(ruta).header("Authorization", token);
		// El validador de OpenAPI exige un Content-Type cuando la operación declara cuerpo, aunque sea opcional.
		String contenido = cuerpo == null && ruta.endsWith("/validacion") ? "{}" : cuerpo;
		if (contenido != null) {
			peticion.contentType(JSON).content(contenido);
		}
		if (clave != null) {
			peticion.header("Idempotency-Key", clave);
		}
		return mvc.perform(peticion);
	}

	private String bearer(String username) throws Exception {
		MvcResult resultado = mvc.perform(post("/api/v1/sesiones").contentType(JSON)
						.content("{\"username\":\"" + username + "\",\"clave\":\"" + CLAVE + "\"}"))
				.andExpect(status().isOk())
				.andReturn();
		return "Bearer " + JsonPath.read(resultado.getResponse().getContentAsString(), "$.token");
	}

	private static String ruc() {
		return "20" + String.format("%09d", ThreadLocalRandom.current().nextInt(1_000_000_000));
	}

	private static String sufijo() {
		return UUID.randomUUID().toString().substring(0, 6);
	}
}
