package com.migal.smartops.catalogo;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.jayway.jsonpath.JsonPath;
import com.migal.smartops.TestcontainersConfiguration;
import com.migal.smartops.catalogo.model.Almacen;
import com.migal.smartops.catalogo.model.Categoria;
import com.migal.smartops.catalogo.model.Producto;
import com.migal.smartops.catalogo.repository.AlmacenRepository;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Prueba de contrato del módulo de catálogo: cada solicitud y cada respuesta se valida contra
 * {@code backend/api/modulos/catalogo.yaml} (ADR-010) y se comprueban los roles, los errores y la paginación.
 * Las filas que crea cada prueba se eliminan al final, solo las propias: SKU con prefijo APT-, almacenes con
 * nombre APT y usuarios apt-.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class CatalogoApiTest {

	private static final String CLAVE = "Clave12345";
	private static final String JSON = MediaType.APPLICATION_JSON_VALUE;
	private static final String PREFIJO_SKU = "APT-";
	private static final String PREFIJO_ALMACEN = "APT ";
	private static final String NOMBRE_PRUEBA = "Prueba API";
	private static final String CEMENTO = "Cemento y aglomerantes";
	private static final String AGREGADOS = "Agregados";

	private final OpenApiInteractionValidator contrato = ValidadorContrato.paraModulo("catalogo");
	private final OpenApiInteractionValidator contratoSoloRespuesta = ValidadorContrato.soloRespuesta("catalogo");

	@Autowired
	private MockMvc mvc;

	@Autowired
	private UsuarioRepository usuarios;

	@Autowired
	private PasswordEncoder codificador;

	@Autowired
	private ProductoRepository productos;

	@Autowired
	private AlmacenRepository almacenes;

	@Autowired
	private CategoriaRepository categorias;

	private final List<Usuario> usuariosDePrueba = new ArrayList<>();
	private String idCemento;
	private String idAgregados;
	private Long idTienda;

	@BeforeEach
	void prepararDatos() {
		crearUsuario("apt-admin", RolUsuario.ADMINISTRADOR);
		crearUsuario("apt-almacenero", RolUsuario.ALMACENERO);
		crearUsuario("apt-vendedor", RolUsuario.VENDEDOR);
		crearUsuario("apt-reponedor", RolUsuario.REPONEDOR);
		idCemento = categoriaPorNombre(CEMENTO).getId().toString();
		idAgregados = categoriaPorNombre(AGREGADOS).getId().toString();
		idTienda = almacenes.saveAndFlush(new Almacen("APT Tienda", "tienda", "Jr. de prueba 100")).getId();
		almacenes.saveAndFlush(new Almacen("APT Almacen 1", "almacen", null));
	}

	@AfterEach
	void limpiarDatos() {
		productos.deleteAll(productos.findAll().stream()
				.filter(p -> p.getSku().startsWith(PREFIJO_SKU)).toList());
		almacenes.deleteAll(almacenes.findAll().stream()
				.filter(a -> a.getNombre().startsWith(PREFIJO_ALMACEN)).toList());
		usuarios.deleteAll(usuariosDePrueba);
		usuariosDePrueba.clear();
	}

	private void crearUsuario(String username, RolUsuario rol) {
		Usuario usuario = new Usuario("Rosa", "Quispe", username, codificador.encode(CLAVE), rol);
		usuario.cambiarEstado(true, Instant.now());
		usuariosDePrueba.add(usuarios.saveAndFlush(usuario));
	}

	private Categoria categoriaPorNombre(String nombre) {
		return categorias.findByNombre(nombre).orElseThrow();
	}

	private String bearer(String username) throws Exception {
		MvcResult resultado = mvc.perform(post("/api/v1/sesiones").contentType(JSON)
						.content("{\"username\":\"" + username + "\",\"clave\":\"" + CLAVE + "\"}"))
				.andExpect(status().isOk())
				.andReturn();
		return "Bearer " + JsonPath.read(resultado.getResponse().getContentAsString(), "$.token");
	}

	private String almacenero() throws Exception {
		return bearer("apt-almacenero");
	}

	private String administrador() throws Exception {
		return bearer("apt-admin");
	}

	private static String cuerpoRegistro(String sku, String nombre, String idCategoria, String estadoActivo) {
		String vigencia = estadoActivo == null ? "" : ",\"estadoActivo\":" + estadoActivo;
		return "{\"sku\":\"" + sku + "\",\"nombreOficial\":\"" + nombre + "\",\"descripcion\":\"Prueba\","
				+ "\"unidadMedida\":\"bolsa\",\"precioReferencial\":28.5,\"stockMinimo\":20,"
				+ "\"idCategoria\":\"" + idCategoria + "\",\"marca\":\"Sol\",\"presentacion\":null" + vigencia + "}";
	}

	private static String cuerpoActualizacion(String nombre, String precio, String idCategoria) {
		return "{\"nombreOficial\":\"" + nombre + "\",\"descripcion\":null,\"unidadMedida\":\"bolsa\","
				+ "\"precioReferencial\":" + precio + ",\"stockMinimo\":25,\"idCategoria\":\"" + idCategoria
				+ "\",\"marca\":\"Sol\",\"presentacion\":\"Bolsa 42.5 kg\"}";
	}

	/** Crea un producto directamente con el repositorio, para preparar datos de las pruebas de listado. */
	private Producto producto(String sku, String nombre, Categoria categoria, boolean activo) {
		return productos.saveAndFlush(new Producto(sku, nombre, null, "bolsa", new BigDecimal("28.50"),
				new BigDecimal("20.000"), categoria, "Sol", null, activo));
	}

	private String registrarProducto(String bearer, String sku) throws Exception {
		MvcResult creado = mvc.perform(post("/api/v1/productos").header("Authorization", bearer).contentType(JSON)
						.content(cuerpoRegistro(sku, NOMBRE_PRUEBA, idCemento, null)))
				.andExpect(status().isCreated())
				.andReturn();
		return JsonPath.read(creado.getResponse().getContentAsString(), "$.id");
	}

	@Test
	void registraUnProductoConVigenciaActivaPorDefecto() throws Exception {
		mvc.perform(post("/api/v1/productos").header("Authorization", almacenero()).contentType(JSON)
						.content(cuerpoRegistro(PREFIJO_SKU + "001", NOMBRE_PRUEBA, idCemento, null)))
				.andExpect(status().isCreated())
				.andExpect(ValidadorContrato.cumple(contrato))
				.andExpect(jsonPath("$.sku", is("APT-001")))
				.andExpect(jsonPath("$.estadoActivo", is(true)))
				.andExpect(jsonPath("$.idCategoria", is(idCemento)))
				.andExpect(jsonPath("$.fechaRegistro", notNullValue()))
				.andExpect(jsonPath("$.fechaActualizacion").doesNotExist());
	}

	@Test
	void registraUnProductoInactivoCuandoSeIndicaLaVigencia() throws Exception {
		mvc.perform(post("/api/v1/productos").header("Authorization", administrador()).contentType(JSON)
						.content(cuerpoRegistro(PREFIJO_SKU + "002", NOMBRE_PRUEBA, idCemento, "false")))
				.andExpect(status().isCreated())
				.andExpect(ValidadorContrato.cumple(contrato))
				.andExpect(jsonPath("$.estadoActivo", is(false)));
	}

	@Test
	void rechazaUnSkuDuplicadoConConflicto() throws Exception {
		String bearer = almacenero();
		registrarProducto(bearer, PREFIJO_SKU + "003");

		mvc.perform(post("/api/v1/productos").header("Authorization", bearer).contentType(JSON)
						.content(cuerpoRegistro(PREFIJO_SKU + "003", "Otro nombre", idCemento, null)))
				.andExpect(status().isConflict())
				.andExpect(ValidadorContrato.cumple(contrato))
				.andExpect(jsonPath("$.code", is("SKU_DUPLICADO")));
	}

	@Test
	void rechazaUnaCategoriaInexistenteConValidacionFallida() throws Exception {
		mvc.perform(post("/api/v1/productos").header("Authorization", almacenero()).contentType(JSON)
						.content(cuerpoRegistro(PREFIJO_SKU + "004", NOMBRE_PRUEBA, "999999999", null)))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(ValidadorContrato.cumple(contrato))
				.andExpect(jsonPath("$.code", is("VALIDACION_FALLIDA")));
	}

	@Test
	void validaLasReglasDelCuerpoDelRegistro() throws Exception {
		String cuerpo = "{\"sku\":\"\",\"nombreOficial\":\"\",\"unidadMedida\":\"bolsa\","
				+ "\"precioReferencial\":-1,\"stockMinimo\":20,\"idCategoria\":\"abc\"}";

		mvc.perform(post("/api/v1/productos").header("Authorization", almacenero()).contentType(JSON).content(cuerpo))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(ValidadorContrato.cumple(contratoSoloRespuesta))
				.andExpect(jsonPath("$.code", is("VALIDACION_FALLIDA")))
				.andExpect(jsonPath("$.errores", hasSize(4)));
	}

	@Test
	void rechazaUnCuerpoQueNoEsJson() throws Exception {
		mvc.perform(post("/api/v1/productos").header("Authorization", almacenero()).contentType(JSON)
						.content("no es json"))
				.andExpect(status().isBadRequest())
				.andExpect(ValidadorContrato.cumple(contratoSoloRespuesta))
				.andExpect(jsonPath("$.code", is("SOLICITUD_MAL_FORMADA")));
	}

	@Test
	void exigeTokenEnLasOperacionesDelCatalogo() throws Exception {
		for (String ruta : List.of("/api/v1/productos", "/api/v1/almacenes", "/api/v1/categorias")) {
			mvc.perform(get(ruta))
					.andExpect(status().isUnauthorized())
					.andExpect(ValidadorContrato.cumple(contratoSoloRespuesta))
					.andExpect(jsonPath("$.code", is("NO_AUTENTICADO")));
		}
		mvc.perform(get("/api/v1/productos").header("Authorization", "Bearer token-invalido"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code", is("NO_AUTENTICADO")));
	}

	@Test
	void denegaElAccesoDeRolesNoAutorizadosAlProducto() throws Exception {
		mvc.perform(get("/api/v1/productos").header("Authorization", bearer("apt-vendedor")))
				.andExpect(status().isForbidden())
				.andExpect(ValidadorContrato.cumple(contrato))
				.andExpect(jsonPath("$.code", is("PERMISO_INSUFICIENTE")));
		mvc.perform(post("/api/v1/productos").header("Authorization", bearer("apt-reponedor")).contentType(JSON)
						.content(cuerpoRegistro(PREFIJO_SKU + "005", NOMBRE_PRUEBA, idCemento, null)))
				.andExpect(status().isForbidden())
				.andExpect(ValidadorContrato.cumple(contrato))
				.andExpect(jsonPath("$.code", is("PERMISO_INSUFICIENTE")));
		mvc.perform(get("/api/v1/categorias").header("Authorization", bearer("apt-vendedor")))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code", is("PERMISO_INSUFICIENTE")));
	}

	@Test
	void listaProductosPaginadosConFiltros() throws Exception {
		Categoria cemento = categoriaPorNombre(CEMENTO);
		producto(PREFIJO_SKU + "010", NOMBRE_PRUEBA + " uno", cemento, true);
		producto(PREFIJO_SKU + "011", NOMBRE_PRUEBA + " dos", cemento, true);
		producto(PREFIJO_SKU + "012", NOMBRE_PRUEBA + " tres", categoriaPorNombre(AGREGADOS), true);
		producto(PREFIJO_SKU + "013", NOMBRE_PRUEBA + " cuatro", cemento, false);
		String bearer = almacenero();

		mvc.perform(get("/api/v1/productos").header("Authorization", bearer)
						.param("q", NOMBRE_PRUEBA).param("pageSize", "2"))
				.andExpect(status().isOk())
				.andExpect(ValidadorContrato.cumple(contrato))
				.andExpect(jsonPath("$.total", is(4)))
				.andExpect(jsonPath("$.items", hasSize(2)))
				.andExpect(jsonPath("$.page", is(1)))
				.andExpect(jsonPath("$.pageSize", is(2)));

		mvc.perform(get("/api/v1/productos").header("Authorization", bearer)
						.param("q", NOMBRE_PRUEBA).param("idCategoria", idAgregados))
				.andExpect(status().isOk())
				.andExpect(ValidadorContrato.cumple(contrato))
				.andExpect(jsonPath("$.total", is(1)))
				.andExpect(jsonPath("$.items[0].sku", is("APT-012")));

		mvc.perform(get("/api/v1/productos").header("Authorization", bearer)
						.param("q", NOMBRE_PRUEBA).param("estadoActivo", "false"))
				.andExpect(status().isOk())
				.andExpect(ValidadorContrato.cumple(contrato))
				.andExpect(jsonPath("$.total", is(1)))
				.andExpect(jsonPath("$.items[0].sku", is("APT-013")));

		mvc.perform(get("/api/v1/productos").header("Authorization", bearer).param("sku", "APT-010"))
				.andExpect(status().isOk())
				.andExpect(ValidadorContrato.cumple(contrato))
				.andExpect(jsonPath("$.total", is(1)))
				.andExpect(jsonPath("$.items[0].nombreOficial", is(NOMBRE_PRUEBA + " uno")));
	}

	@Test
	void ordenaLosProductosYRechazaCamposNoOrdenablesOFueraDeRango() throws Exception {
		Categoria cemento = categoriaPorNombre(CEMENTO);
		producto(PREFIJO_SKU + "020", NOMBRE_PRUEBA + " orden", cemento, true);
		producto(PREFIJO_SKU + "021", NOMBRE_PRUEBA + " orden", cemento, true);
		String bearer = almacenero();

		mvc.perform(get("/api/v1/productos").header("Authorization", bearer)
						.param("q", NOMBRE_PRUEBA + " orden").param("orden", "sku:desc"))
				.andExpect(status().isOk())
				.andExpect(ValidadorContrato.cumple(contrato))
				.andExpect(jsonPath("$.items[0].sku", is("APT-021")));

		mvc.perform(get("/api/v1/productos").header("Authorization", bearer).param("orden", "sku:asc,"))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(ValidadorContrato.cumple(contratoSoloRespuesta))
				.andExpect(jsonPath("$.code", is("VALIDACION_FALLIDA")));

		mvc.perform(get("/api/v1/productos").header("Authorization", bearer).param("orden", "precioReferencial:asc"))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(ValidadorContrato.cumple(contrato))
				.andExpect(jsonPath("$.code", is("VALIDACION_FALLIDA")));

		mvc.perform(get("/api/v1/productos").header("Authorization", bearer).param("pageSize", "500"))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(ValidadorContrato.cumple(contratoSoloRespuesta))
				.andExpect(jsonPath("$.code", is("VALIDACION_FALLIDA")));
	}

	@Test
	void consultaUnProductoPorSuIdentificador() throws Exception {
		String id = producto(PREFIJO_SKU + "030", NOMBRE_PRUEBA, categoriaPorNombre(CEMENTO), true).getId().toString();
		String bearer = almacenero();

		mvc.perform(get("/api/v1/productos/" + id).header("Authorization", bearer))
				.andExpect(status().isOk())
				.andExpect(ValidadorContrato.cumple(contrato))
				.andExpect(jsonPath("$.id", is(id)))
				.andExpect(jsonPath("$.sku", is("APT-030")));

		mvc.perform(get("/api/v1/productos/999999").header("Authorization", bearer))
				.andExpect(status().isNotFound())
				.andExpect(ValidadorContrato.cumple(contrato))
				.andExpect(jsonPath("$.code", is("RECURSO_NO_ENCONTRADO")));

		mvc.perform(get("/api/v1/productos/abc").header("Authorization", bearer))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code", is("RECURSO_NO_ENCONTRADO")));
	}

	@Test
	void actualizaUnProductoSinModificarSuSku() throws Exception {
		String id = producto(PREFIJO_SKU + "040", NOMBRE_PRUEBA, categoriaPorNombre(CEMENTO), true).getId().toString();
		String bearer = almacenero();

		mvc.perform(put("/api/v1/productos/" + id).header("Authorization", bearer).contentType(JSON)
						.content(cuerpoActualizacion("Prueba API editada", "29.90", idAgregados)))
				.andExpect(status().isOk())
				.andExpect(ValidadorContrato.cumple(contrato))
				.andExpect(jsonPath("$.sku", is("APT-040")))
				.andExpect(jsonPath("$.nombreOficial", is("Prueba API editada")))
				.andExpect(jsonPath("$.idCategoria", is(idAgregados)))
				.andExpect(jsonPath("$.fechaActualizacion", notNullValue()));

		mvc.perform(put("/api/v1/productos/999999").header("Authorization", bearer).contentType(JSON)
						.content(cuerpoActualizacion("Inexistente", "10", idAgregados)))
				.andExpect(status().isNotFound())
				.andExpect(ValidadorContrato.cumple(contrato))
				.andExpect(jsonPath("$.code", is("RECURSO_NO_ENCONTRADO")));

		mvc.perform(put("/api/v1/productos/" + id).header("Authorization", bearer).contentType(JSON)
						.content(cuerpoActualizacion("Precio invalido", "-5", idAgregados)))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(ValidadorContrato.cumple(contratoSoloRespuesta))
				.andExpect(jsonPath("$.code", is("VALIDACION_FALLIDA")));
	}

	@Test
	void activaYDesactivaUnProductoSinEliminarlo() throws Exception {
		String id = producto(PREFIJO_SKU + "050", NOMBRE_PRUEBA, categoriaPorNombre(CEMENTO), true).getId().toString();
		String bearer = almacenero();

		mvc.perform(put("/api/v1/productos/" + id + "/estado").header("Authorization", bearer).contentType(JSON)
						.content("{\"estadoActivo\":false}"))
				.andExpect(status().isOk())
				.andExpect(ValidadorContrato.cumple(contrato))
				.andExpect(jsonPath("$.estadoActivo", is(false)))
				.andExpect(jsonPath("$.fechaActualizacion", notNullValue()));

		mvc.perform(put("/api/v1/productos/" + id + "/estado").header("Authorization", bearer).contentType(JSON)
						.content("{}"))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(ValidadorContrato.cumple(contratoSoloRespuesta))
				.andExpect(jsonPath("$.code", is("VALIDACION_FALLIDA")));

		mvc.perform(put("/api/v1/productos/999999/estado").header("Authorization", bearer).contentType(JSON)
						.content("{\"estadoActivo\":true}"))
				.andExpect(status().isNotFound())
				.andExpect(ValidadorContrato.cumple(contrato))
				.andExpect(jsonPath("$.code", is("RECURSO_NO_ENCONTRADO")));
	}

	@Test
	void listaLasUbicacionesParaTodosLosRolesAutorizados() throws Exception {
		for (String usuario : List.of("apt-almacenero", "apt-vendedor", "apt-reponedor", "apt-admin")) {
			mvc.perform(get("/api/v1/almacenes").header("Authorization", bearer(usuario)))
					.andExpect(status().isOk())
					.andExpect(ValidadorContrato.cumple(contrato))
					.andExpect(jsonPath("$[*].nombre", hasItem("APT Tienda")));
		}
	}

	@Test
	void soloElAdministradorCambiaElEstadoDeUnAlmacen() throws Exception {
		String id = idTienda.toString();

		mvc.perform(put("/api/v1/almacenes/" + id + "/estado").header("Authorization", almacenero())
						.contentType(JSON).content("{\"estadoActivo\":false}"))
				.andExpect(status().isForbidden())
				.andExpect(ValidadorContrato.cumple(contrato))
				.andExpect(jsonPath("$.code", is("PERMISO_INSUFICIENTE")));

		mvc.perform(put("/api/v1/almacenes/" + id + "/estado").header("Authorization", administrador())
						.contentType(JSON).content("{\"estadoActivo\":false}"))
				.andExpect(status().isOk())
				.andExpect(ValidadorContrato.cumple(contrato))
				.andExpect(jsonPath("$.nombre", is("APT Tienda")))
				.andExpect(jsonPath("$.estadoActivo", is(false)));

		mvc.perform(put("/api/v1/almacenes/999999/estado").header("Authorization", administrador())
						.contentType(JSON).content("{\"estadoActivo\":false}"))
				.andExpect(status().isNotFound())
				.andExpect(ValidadorContrato.cumple(contrato))
				.andExpect(jsonPath("$.code", is("RECURSO_NO_ENCONTRADO")));

		mvc.perform(put("/api/v1/almacenes/" + id + "/estado").header("Authorization", administrador())
						.contentType(JSON).content("{}"))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(ValidadorContrato.cumple(contratoSoloRespuesta))
				.andExpect(jsonPath("$.code", is("VALIDACION_FALLIDA")));
	}

	@Test
	void listaLasCategoriasParaAlmacenYAdministrador() throws Exception {
		mvc.perform(get("/api/v1/categorias").header("Authorization", almacenero()))
				.andExpect(status().isOk())
				.andExpect(ValidadorContrato.cumple(contrato))
				.andExpect(jsonPath("$[*].nombre", hasItem(CEMENTO)))
				.andExpect(jsonPath("$[?(@.nombre == 'Cemento y aglomerantes')].aprobacionAutomatica", hasItem(true)))
				.andExpect(jsonPath("$[?(@.nombre == 'Cemento y aglomerantes')].idCategoriaPadre", hasItem(notNullValue())));

		mvc.perform(get("/api/v1/categorias").header("Authorization", administrador()))
				.andExpect(status().isOk())
				.andExpect(ValidadorContrato.cumple(contrato))
				.andExpect(jsonPath("$[?(@.nombre == 'Herramientas')].idCategoriaPadre", hasItem(nullValue())));
	}
}
