package com.migal.smartops.comercial;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
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
import com.migal.smartops.comercial.repository.CotizacionRepository;
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

/**
 * Prueba de contrato del módulo comercial: cada solicitud y cada respuesta se valida contra
 * {@code backend/api/modulos/comercial.yaml} (ADR-010), y se comprueban los roles, los errores, la idempotencia y la
 * paginación. Las filas que crea cada prueba se eliminan al final: clientes con nombre APC, productos con SKU APC-
 * y usuarios apc-.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class ComercialApiTest {

	private static final String CLAVE = "Clave12345";
	private static final String JSON = MediaType.APPLICATION_JSON_VALUE;
	private static final String PREFIJO = "APC";
	private static final String CEMENTO = "Cemento y aglomerantes";

	private final OpenApiInteractionValidator contrato = ValidadorContrato.paraModulo("comercial");
	private final OpenApiInteractionValidator contratoSoloRespuesta = ValidadorContrato.soloRespuesta("comercial");

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
	private CotizacionRepository cotizaciones;
	@Autowired
	private JdbcClient jdbc;

	private final List<Usuario> usuariosDePrueba = new ArrayList<>();
	private String idProducto;

	@BeforeEach
	void prepararDatos() {
		crearUsuario("apc-admin", RolUsuario.ADMINISTRADOR);
		crearUsuario("apc-vendedor", RolUsuario.VENDEDOR);
		crearUsuario("apc-almacenero", RolUsuario.ALMACENERO);
		Producto producto = productos.saveAndFlush(new Producto("APC-" + UUID.randomUUID().toString().substring(0, 6),
				"Cemento de prueba", null, "bolsa", new BigDecimal("28.50"), new BigDecimal("20"),
				categorias.findByNombre(CEMENTO).orElseThrow(), "Sol", null, true));
		idProducto = String.valueOf(producto.getId());
	}

	@AfterEach
	void limpiarDatos() {
		cotizaciones.deleteAll(cotizaciones.findAll().stream()
				.filter(c -> c.getCliente().getNombreRazonSocial().startsWith(PREFIJO)).toList());
		jdbc.sql("DELETE FROM cliente WHERE nombre_razon_social LIKE 'APC%'").update();
		productos.deleteAll(productos.findAll().stream().filter(p -> p.getSku().startsWith("APC-")).toList());
		jdbc.sql("DELETE FROM idempotencia WHERE id_usuario IN (SELECT id_usuario FROM usuario WHERE username LIKE 'apc-%')")
				.update();
		usuarios.deleteAll(usuariosDePrueba);
		usuariosDePrueba.clear();
	}

	private void crearUsuario(String username, RolUsuario rol) {
		Usuario usuario = new Usuario("Rosa", "Quispe", username, codificador.encode(CLAVE), rol);
		usuario.cambiarEstado(true, Instant.now());
		usuariosDePrueba.add(usuarios.saveAndFlush(usuario));
	}

	private String bearer(String username) throws Exception {
		MvcResult resultado = mvc.perform(post("/api/v1/sesiones").contentType(JSON)
						.content("{\"username\":\"" + username + "\",\"clave\":\"" + CLAVE + "\"}"))
				.andExpect(status().isOk())
				.andReturn();
		return "Bearer " + JsonPath.read(resultado.getResponse().getContentAsString(), "$.token");
	}

	private static String clave() {
		return UUID.randomUUID().toString();
	}

	/** RUC aleatorio de once dígitos con el prefijo 20, válido según el formato del contrato. */
	private static String rucAleatorio() {
		return "20" + ThreadLocalRandom.current().nextLong(100_000_000L, 999_999_999L);
	}

	private static String cuerpoCliente(String tipo, String numero, String nombre) {
		String documento = tipo == null ? "\"tipoDocumento\":null,\"numeroDocumento\":null"
				: "\"tipoDocumento\":\"" + tipo + "\",\"numeroDocumento\":\"" + numero + "\"";
		return "{" + documento + ",\"nombreRazonSocial\":\"" + nombre + "\",\"telefonoContacto\":\"987654321\","
				+ "\"direccionReferencia\":null}";
	}

	private String registrarCliente(String bearer, String numero) throws Exception {
		MvcResult creado = mvc.perform(post("/api/v1/clientes").header("Authorization", bearer).contentType(JSON)
						.content(cuerpoCliente("ruc", numero, PREFIJO + " Constructora de prueba")))
				.andExpect(status().isCreated())
				.andReturn();
		return JsonPath.read(creado.getResponse().getContentAsString(), "$.id");
	}

	private String cuerpoCotizacion(String idCliente, String cantidad) {
		String cliente = idCliente == null ? "null" : "\"" + idCliente + "\"";
		return "{\"clienteId\":" + cliente + ",\"lineas\":[{\"productoId\":\"" + idProducto
				+ "\",\"cantidad\":" + cantidad + ",\"precioUnitario\":28.5}],\"descuento\":25,\"validezDias\":7,"
				+ "\"condicionesComerciales\":null}";
	}

	private String crearCotizacion(String bearer, String idCliente, String cantidad) throws Exception {
		MvcResult creada = mvc.perform(post("/api/v1/cotizaciones").header("Authorization", bearer).contentType(JSON)
						.content(cuerpoCotizacion(idCliente, cantidad)))
				.andExpect(status().isCreated())
				.andReturn();
		return JsonPath.read(creada.getResponse().getContentAsString(), "$.id");
	}

	@Test
	void registraUnClienteConDocumentoYLoConsulta() throws Exception {
		String bearer = bearer("apc-vendedor");
		String numero = rucAleatorio();
		mvc.perform(post("/api/v1/clientes").header("Authorization", bearer).contentType(JSON)
						.content(cuerpoCliente("ruc", numero, PREFIJO + " Constructora")))
				.andExpect(status().isCreated())
				.andExpect(ValidadorContrato.cumple(contrato))
				.andExpect(jsonPath("$.tipoDocumento", is("ruc")))
				.andExpect(jsonPath("$.estadoActivo", is(true)))
				.andExpect(jsonPath("$.fechaRegistro", notNullValue()));
	}

	@Test
	void rechazaUnDocumentoDuplicadoConElClienteTitular() throws Exception {
		String bearer = bearer("apc-vendedor");
		String numero = rucAleatorio();
		String titular = registrarCliente(bearer, numero);
		mvc.perform(post("/api/v1/clientes").header("Authorization", bearer).contentType(JSON)
						.content(cuerpoCliente("ruc", numero, PREFIJO + " Otro")))
				.andExpect(status().isConflict())
				.andExpect(ValidadorContrato.cumple(contrato))
				.andExpect(jsonPath("$.code", is("DOCUMENTO_CLIENTE_DUPLICADO")))
				.andExpect(jsonPath("$.clienteId", is(titular)));
	}

	@Test
	void restringeLaGestionDeClientesAlRolVendedorYAdministrador() throws Exception {
		mvc.perform(get("/api/v1/clientes").header("Authorization", bearer("apc-almacenero")))
				.andExpect(status().isForbidden())
				.andExpect(ValidadorContrato.cumple(contratoSoloRespuesta))
				.andExpect(jsonPath("$.code", is("PERMISO_INSUFICIENTE")));
	}

	@Test
	void responde404AUnClienteInexistente() throws Exception {
		mvc.perform(get("/api/v1/clientes/999999999").header("Authorization", bearer("apc-vendedor")))
				.andExpect(status().isNotFound())
				.andExpect(ValidadorContrato.cumple(contratoSoloRespuesta))
				.andExpect(jsonPath("$.code", is("RECURSO_NO_ENCONTRADO")));
	}

	@Test
	void desactivaUnClienteYLoListaConPaginacion() throws Exception {
		String bearer = bearer("apc-vendedor");
		String idCliente = registrarCliente(bearer, rucAleatorio());
		mvc.perform(put("/api/v1/clientes/" + idCliente + "/estado").header("Authorization", bearer).contentType(JSON)
						.content("{\"estadoActivo\":false}"))
				.andExpect(status().isOk())
				.andExpect(ValidadorContrato.cumple(contrato))
				.andExpect(jsonPath("$.estadoActivo", is(false)));
		mvc.perform(get("/api/v1/clientes").param("page", "1").param("pageSize", "5").param("q", PREFIJO)
						.header("Authorization", bearer))
				.andExpect(status().isOk())
				.andExpect(ValidadorContrato.cumple(contrato))
				.andExpect(jsonPath("$.page", is(1)))
				.andExpect(jsonPath("$.pageSize", is(5)))
				.andExpect(jsonPath("$.items", hasSize(1)));
	}

	@Test
	void creaUnaCotizacionSinClienteConCodigoRequeridoYConUnClienteValido() throws Exception {
		String bearer = bearer("apc-vendedor");
		mvc.perform(post("/api/v1/cotizaciones").header("Authorization", bearer).contentType(JSON)
						.content(cuerpoCotizacion(null, "40")))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(ValidadorContrato.cumple(contratoSoloRespuesta))
				.andExpect(jsonPath("$.code", is("CLIENTE_REQUERIDO")));

		String idCliente = registrarCliente(bearer, rucAleatorio());
		mvc.perform(post("/api/v1/cotizaciones").header("Authorization", bearer).contentType(JSON)
						.content(cuerpoCotizacion(idCliente, "40")))
				.andExpect(status().isCreated())
				.andExpect(ValidadorContrato.cumple(contrato))
				.andExpect(jsonPath("$.estado", is("emitida")))
				.andExpect(jsonPath("$.total", is(1115.0)))
				.andExpect(jsonPath("$.lineas[0].sku", notNullValue()));
	}

	@Test
	void recorreElCicloDeAceptacionYValidacionConIdempotencia() throws Exception {
		String vendedor = bearer("apc-vendedor");
		String admin = bearer("apc-admin");
		String idCotizacion = crearCotizacion(vendedor, registrarCliente(vendedor, rucAleatorio()), "40");
		String clave = clave();

		mvc.perform(post("/api/v1/cotizaciones/" + idCotizacion + "/aceptacion").header("Authorization", vendedor)
						.header("Idempotency-Key", clave))
				.andExpect(status().isOk())
				.andExpect(ValidadorContrato.cumple(contrato))
				.andExpect(jsonPath("$.estado", is("aceptada")));

		mvc.perform(post("/api/v1/cotizaciones/" + idCotizacion + "/aceptacion").header("Authorization", vendedor)
						.header("Idempotency-Key", clave))
				.andExpect(status().isOk())
				.andExpect(header().string("Idempotent-Replayed", "true"));

		mvc.perform(post("/api/v1/cotizaciones/" + idCotizacion + "/validacion").header("Authorization", vendedor)
						.header("Idempotency-Key", clave()))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code", is("PERMISO_INSUFICIENTE")));

		mvc.perform(post("/api/v1/cotizaciones/" + idCotizacion + "/validacion").header("Authorization", admin)
						.header("Idempotency-Key", clave()))
				.andExpect(status().isOk())
				.andExpect(ValidadorContrato.cumple(contrato))
				.andExpect(jsonPath("$.estado", is("validada")));
	}

	@Test
	void exigeLaCabeceraIdempotencyKeyEnLasTransiciones() throws Exception {
		String vendedor = bearer("apc-vendedor");
		String idCotizacion = crearCotizacion(vendedor, registrarCliente(vendedor, rucAleatorio()), "40");
		mvc.perform(post("/api/v1/cotizaciones/" + idCotizacion + "/aceptacion").header("Authorization", vendedor))
				.andExpect(status().isBadRequest())
				.andExpect(ValidadorContrato.cumple(contratoSoloRespuesta))
				.andExpect(jsonPath("$.code", is("SOLICITUD_MAL_FORMADA")));
	}

	@Test
	void rechazaElRechazoDeMigalAUnVendedorYRegistraElDelClienteComoRechazada() throws Exception {
		String vendedor = bearer("apc-vendedor");
		String idCotizacion = crearCotizacion(vendedor, registrarCliente(vendedor, rucAleatorio()), "40");
		mvc.perform(post("/api/v1/cotizaciones/" + idCotizacion + "/rechazo").header("Authorization", vendedor)
						.header("Idempotency-Key", clave()).contentType(JSON)
						.content("{\"origen\":\"migal\",\"motivo\":\"Sin abastecimiento\"}"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code", is("PERMISO_INSUFICIENTE")));

		mvc.perform(post("/api/v1/cotizaciones/" + idCotizacion + "/rechazo").header("Authorization", vendedor)
						.header("Idempotency-Key", clave()).contentType(JSON)
						.content("{\"origen\":\"cliente\",\"motivo\":\"El cliente cambió de planes\"}"))
				.andExpect(status().isOk())
				.andExpect(ValidadorContrato.cumple(contrato))
				.andExpect(jsonPath("$.estado", is("rechazada")))
				.andExpect(jsonPath("$.rechazo.origen", is("cliente")));
	}

	@Test
	void divideUnaCotizacionConRespuestaCreada() throws Exception {
		String vendedor = bearer("apc-vendedor");
		String idCotizacion = crearCotizacion(vendedor, registrarCliente(vendedor, rucAleatorio()), "40");
		mvc.perform(post("/api/v1/cotizaciones/" + idCotizacion + "/division").header("Authorization", vendedor)
						.header("Idempotency-Key", clave()).contentType(JSON)
						.content("{\"lineas\":[{\"productoId\":\"" + idProducto + "\",\"cantidad\":20}]}"))
				.andExpect(status().isCreated())
				.andExpect(ValidadorContrato.cumple(contrato))
				.andExpect(jsonPath("$.cotizacionOriginal.lineas[0].cantidad", is(20.0)))
				.andExpect(jsonPath("$.cotizacionNueva.descuento", is(0.0)));
	}

	@Test
	void listaCotizacionesPorEstadoYRechazaUnEstadoInvalido() throws Exception {
		String vendedor = bearer("apc-vendedor");
		String idCliente = registrarCliente(vendedor, rucAleatorio());
		crearCotizacion(vendedor, idCliente, "40");
		mvc.perform(get("/api/v1/cotizaciones").param("estado", "emitida,aceptada").param("clienteId", idCliente)
						.header("Authorization", vendedor))
				.andExpect(status().isOk())
				.andExpect(ValidadorContrato.cumple(contrato))
				.andExpect(jsonPath("$.items", hasSize(1)));
		mvc.perform(get("/api/v1/cotizaciones").param("estado", "desconocida").header("Authorization", vendedor))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(ValidadorContrato.cumple(contratoSoloRespuesta))
				.andExpect(jsonPath("$.code", is("VALIDACION_FALLIDA")));
	}
}
