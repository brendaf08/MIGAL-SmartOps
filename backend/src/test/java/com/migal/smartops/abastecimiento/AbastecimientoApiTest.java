package com.migal.smartops.abastecimiento;

import static com.migal.smartops.contrato.ValidadorContrato.cumple;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
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
 * Prueba de contrato del módulo de abastecimiento: cada solicitud y respuesta se valida contra
 * {@code backend/api/modulos/abastecimiento.yaml} (ADR-010). Se comprueban los roles, los códigos de error, el cierre
 * idempotente y la validación del catálogo. Los datos de cada prueba se eliminan al final: proveedores ABS, productos
 * ABS- y usuarios abs-.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class AbastecimientoApiTest {

	private static final String CLAVE = "Clave12345";
	private static final String JSON = MediaType.APPLICATION_JSON_VALUE;
	private static final String CEMENTO = "Cemento y aglomerantes";

	private final OpenApiInteractionValidator contrato = ValidadorContrato.paraModulo("abastecimiento");
	private final OpenApiInteractionValidator contratoSoloRespuesta = ValidadorContrato.soloRespuesta("abastecimiento");

	@Autowired
	private MockMvc mvc;
	@Autowired
	private UsuarioRepository usuarios;
	@Autowired
	private PasswordEncoder codificador;
	@Autowired
	private CategoriaRepository categorias;
	@Autowired
	private ProductoRepository productos;
	@Autowired
	private JdbcClient jdbc;

	private final List<Usuario> usuariosDePrueba = new ArrayList<>();
	private String skuProducto;

	@BeforeEach
	void prepararDatos() {
		crearUsuario("abs-reponedor", RolUsuario.REPONEDOR);
		crearUsuario("abs-almacenero", RolUsuario.ALMACENERO);
		crearUsuario("abs-admin", RolUsuario.ADMINISTRADOR);
		skuProducto = "ABS-" + sufijo();
		productos.saveAndFlush(new Producto(skuProducto, "Cemento de prueba", null, "bolsa", new BigDecimal("28.50"),
				new BigDecimal("20"), categorias.findByNombre(CEMENTO).orElseThrow(), "Sol", null, true));
	}

	@AfterEach
	void limpiarDatos() {
		jdbc.sql("DELETE FROM detalle_compra WHERE id_orden IN (SELECT o.id_orden FROM orden_compra o JOIN proveedor p "
				+ "ON p.id_proveedor = o.id_proveedor WHERE p.razon_social LIKE 'ABS%')").update();
		jdbc.sql("DELETE FROM orden_compra WHERE id_proveedor IN (SELECT id_proveedor FROM proveedor "
				+ "WHERE razon_social LIKE 'ABS%')").update();
		jdbc.sql("DELETE FROM catalogo_proveedor WHERE id_proveedor IN (SELECT id_proveedor FROM proveedor "
				+ "WHERE razon_social LIKE 'ABS%')").update();
		jdbc.sql("DELETE FROM proveedor WHERE razon_social LIKE 'ABS%'").update();
		jdbc.sql("DELETE FROM catalogo_proveedor WHERE id_producto IN (SELECT id_producto FROM producto WHERE sku LIKE 'ABS-%')")
				.update();
		jdbc.sql("DELETE FROM producto WHERE sku LIKE 'ABS-%'").update();
		jdbc.sql("DELETE FROM idempotencia WHERE id_usuario IN (SELECT id_usuario FROM usuario WHERE username LIKE 'abs-%')")
				.update();
		usuarios.deleteAll(usuariosDePrueba);
		usuariosDePrueba.clear();
	}

	@Test
	void reponedorConsultaElDirectorioDeProveedores() throws Exception {
		String token = bearer("abs-reponedor");
		String nombre = "ABS Directorio " + sufijo();
		long idProveedor = jdbc.sql("INSERT INTO proveedor (ruc, razon_social, contacto, estado_activo) VALUES (:ruc, :nombre, "
						+ "'ventas@prueba.example', true) RETURNING id_proveedor")
				.param("ruc", ruc()).param("nombre", nombre).query(Long.class).single();

		mvc.perform(get("/api/v1/proveedores").param("q", nombre).header("Authorization", token))
				.andExpect(status().isOk())
				.andExpect(cumple(contrato))
				.andExpect(jsonPath("$.items", hasSize(1)))
				.andExpect(jsonPath("$.items[0].id", is(String.valueOf(idProveedor))));
	}

	@Test
	void almaceneroNoAccedeAlDirectorioDeProveedores() throws Exception {
		mvc.perform(get("/api/v1/proveedores").header("Authorization", bearer("abs-almacenero")))
				.andExpect(status().isForbidden())
				.andExpect(cumple(contratoSoloRespuesta));
	}

	@Test
	void registraUnProveedorYRechazaSuRucDuplicado() throws Exception {
		String ruc = ruc();
		MvcResult creado = alta("/api/v1/proveedores", bearer("abs-reponedor"), null,
				"{\"ruc\":\"" + ruc + "\",\"razonSocial\":\"ABS Proveedor de prueba\",\"contacto\":\"ventas@prueba.example\"}")
				.andExpect(status().isCreated())
				.andExpect(cumple(contrato))
				.andExpect(jsonPath("$.estadoActivo", is(true)))
				.andReturn();
		String proveedorId = JsonPath.read(creado.getResponse().getContentAsString(), "$.id");

		alta("/api/v1/proveedores", bearer("abs-reponedor"), null,
				"{\"ruc\":\"" + ruc + "\",\"razonSocial\":\"ABS Otro\",\"contacto\":\"otro@prueba.example\"}")
				.andExpect(status().isConflict())
				.andExpect(cumple(contratoSoloRespuesta))
				.andExpect(jsonPath("$.code", is("RUC_DUPLICADO")))
				.andExpect(jsonPath("$.proveedorId", is(proveedorId)));
	}

	@Test
	void rechazaUnCatalogoConSkuRepetidoConLaListaDeErrores() throws Exception {
		long idProveedor = proveedorActivo();
		String cuerpo = "{\"productos\":[{\"sku\":\"" + skuProducto + "\",\"precioCompra\":26.5},"
				+ "{\"sku\":\"" + skuProducto + "\",\"precioCompra\":27}]}";

		mvc.perform(put("/api/v1/proveedores/" + idProveedor + "/catalogo").contentType(JSON).content(cuerpo)
						.header("Authorization", bearer("abs-reponedor")))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(cumple(contratoSoloRespuesta))
				.andExpect(jsonPath("$.code", is("VALIDACION_FALLIDA")))
				.andExpect(jsonPath("$.errores", hasSize(1)));
	}

	@Test
	void actualizaElCatalogoYDevuelveElResultado() throws Exception {
		long idProveedor = proveedorActivo();
		String cuerpo = "{\"productos\":[{\"sku\":\"" + skuProducto + "\",\"precioCompra\":26.5,"
				+ "\"plazoEntregaReferencialDias\":3}]}";

		mvc.perform(put("/api/v1/proveedores/" + idProveedor + "/catalogo").contentType(JSON).content(cuerpo)
						.header("Authorization", bearer("abs-reponedor")))
				.andExpect(status().isOk())
				.andExpect(cumple(contrato))
				.andExpect(jsonPath("$[0].sku", is(skuProducto)))
				.andExpect(jsonPath("$[0].nombre", is("Cemento de prueba")))
				.andExpect(jsonPath("$[0].fechaActualizacionPrecio", notNullValue()));
	}

	@Test
	void registraUnaOrdenPendienteYLaConsultaElAlmacenero() throws Exception {
		long idProveedor = proveedorActivo();
		String orden = ordenCuerpo(idProveedor, 100, "26.50");

		MvcResult creada = alta("/api/v1/ordenes-compra", bearer("abs-reponedor"), null, orden)
				.andExpect(status().isCreated())
				.andExpect(cumple(contrato))
				.andExpect(jsonPath("$.estado", is("pendiente")))
				.andExpect(jsonPath("$.codigo", startsWith("OC-")))
				.andExpect(jsonPath("$.total", is(2650.0)))
				.andReturn();
		String idOrden = JsonPath.read(creada.getResponse().getContentAsString(), "$.id");

		mvc.perform(get("/api/v1/ordenes-compra/" + idOrden).header("Authorization", bearer("abs-almacenero")))
				.andExpect(status().isOk())
				.andExpect(cumple(contrato))
				.andExpect(jsonPath("$.lineas[0].cantidadPendienteRecepcion", is(100.0)));
	}

	@Test
	void rechazaUnaOrdenDeUnProveedorInactivo() throws Exception {
		long idProveedor = proveedorActivo();
		mvc.perform(put("/api/v1/proveedores/" + idProveedor + "/estado").contentType(JSON)
						.content("{\"estadoActivo\":false}").header("Authorization", bearer("abs-reponedor")))
				.andExpect(status().isOk())
				.andExpect(cumple(contrato));

		alta("/api/v1/ordenes-compra", bearer("abs-reponedor"), null, ordenCuerpo(idProveedor, 10, "26.50"))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(cumple(contratoSoloRespuesta))
				.andExpect(jsonPath("$.code", is("PROVEEDOR_INACTIVO")));
	}

	@Test
	void cierreRequiereLaCabeceraIdempotencyKeyYRepiteLaRespuestaOriginal() throws Exception {
		long idOrden = ordenParcial();
		String cierre = "{\"motivo\":\"Proveedor sin stock del resto\"}";

		alta("/api/v1/ordenes-compra/" + idOrden + "/cierre", bearer("abs-reponedor"), null, cierre)
				.andExpect(status().isBadRequest())
				.andExpect(cumple(contratoSoloRespuesta))
				.andExpect(jsonPath("$.code", is("SOLICITUD_MAL_FORMADA")));

		String clave = UUID.randomUUID().toString();
		alta("/api/v1/ordenes-compra/" + idOrden + "/cierre", bearer("abs-reponedor"), clave, cierre)
				.andExpect(status().isOk())
				.andExpect(cumple(contrato))
				.andExpect(jsonPath("$.estado", is("completada")));

		alta("/api/v1/ordenes-compra/" + idOrden + "/cierre", bearer("abs-reponedor"), clave, cierre)
				.andExpect(status().isOk())
				.andExpect(header().string("Idempotent-Replayed", "true"))
				.andExpect(cumple(contrato));

		alta("/api/v1/ordenes-compra/" + idOrden + "/cierre", bearer("abs-reponedor"), clave,
				"{\"motivo\":\"Otro motivo\"}")
				.andExpect(status().isConflict())
				.andExpect(cumple(contratoSoloRespuesta))
				.andExpect(jsonPath("$.code", is("IDEMPOTENCIA_CONFLICTO")));
	}

	@Test
	void cierreSinMotivoCuandoQuedanCantidadesResponseMotivoRequerido() throws Exception {
		long idOrden = ordenParcial();

		alta("/api/v1/ordenes-compra/" + idOrden + "/cierre", bearer("abs-reponedor"), UUID.randomUUID().toString(), "{}")
				.andExpect(status().isUnprocessableEntity())
				.andExpect(cumple(contratoSoloRespuesta))
				.andExpect(jsonPath("$.code", is("MOTIVO_REQUERIDO")));
	}

	@Test
	void cierreDeUnaOrdenPendienteResponseTransicionNoPermitida() throws Exception {
		long idProveedor = proveedorActivo();
		MvcResult creada = alta("/api/v1/ordenes-compra", bearer("abs-reponedor"), null,
				ordenCuerpo(idProveedor, 10, "26.50")).andReturn();
		String idOrden = JsonPath.read(creada.getResponse().getContentAsString(), "$.id");

		alta("/api/v1/ordenes-compra/" + idOrden + "/cierre", bearer("abs-reponedor"), UUID.randomUUID().toString(),
				"{\"motivo\":\"Prueba\"}")
				.andExpect(status().isConflict())
				.andExpect(cumple(contratoSoloRespuesta))
				.andExpect(jsonPath("$.code", is("TRANSICION_NO_PERMITIDA")));
	}

	private long proveedorActivo() {
		return jdbc.sql("INSERT INTO proveedor (ruc, razon_social, contacto, estado_activo) VALUES (:ruc, 'ABS Proveedor "
						+ "de prueba', 'ventas@prueba.example', true) RETURNING id_proveedor")
				.param("ruc", ruc()).query(Long.class).single();
	}

	/** Orden en estado parcial sin recepciones validadas, con el estado fijado directamente en la base de datos. */
	private long ordenParcial() throws Exception {
		long idProveedor = proveedorActivo();
		MvcResult creada = alta("/api/v1/ordenes-compra", bearer("abs-reponedor"), null,
				ordenCuerpo(idProveedor, 100, "26.50")).andExpect(status().isCreated()).andReturn();
		long idOrden = Long.parseLong(JsonPath.read(creada.getResponse().getContentAsString(), "$.id"));
		jdbc.sql("UPDATE orden_compra SET estado = 'parcial' WHERE id_orden = :orden").param("orden", idOrden).update();
		return idOrden;
	}

	private String ordenCuerpo(long idProveedor, int cantidad, String costo) {
		return "{\"proveedorId\":\"" + idProveedor + "\",\"fechaEntregaEstimada\":\"2026-11-06\","
				+ "\"modalidadPago\":\"Credito de 30 dias\",\"lineas\":[{\"sku\":\"" + skuProducto
				+ "\",\"cantidadComprada\":" + cantidad + ",\"costoUnitario\":" + costo + "}]}";
	}

	private void crearUsuario(String username, RolUsuario rol) {
		Usuario usuario = new Usuario("Rosa", "Quispe", username, codificador.encode(CLAVE), rol);
		usuario.cambiarEstado(true, Instant.now());
		usuariosDePrueba.add(usuarios.saveAndFlush(usuario));
	}

	private ResultActions alta(String ruta, String token, String clave, String cuerpo) throws Exception {
		var peticion = post(ruta).header("Authorization", token);
		if (cuerpo != null) {
			peticion.contentType(JSON).content(cuerpo);
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
