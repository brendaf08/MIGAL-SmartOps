package com.migal.smartops.comercial;

import static com.migal.smartops.contrato.ValidadorContrato.cumple;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Prueba de contrato del módulo de ventas: cada solicitud y respuesta se valida contra
 * {@code backend/api/modulos/ventas.yaml} (ADR-010), y se comprueban los roles, los códigos de error, la idempotencia
 * y la paginación. Los datos de cada prueba se eliminan al final: clientes APC, productos APC- y usuarios apc-.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class VentasApiTest {

	private static final String CLAVE = "Clave12345";
	private static final String JSON = MediaType.APPLICATION_JSON_VALUE;
	private static final String PREFIJO = "APC";
	private static final String CEMENTO = "Cemento y aglomerantes";

	private final OpenApiInteractionValidator contrato = ValidadorContrato.paraModulo("ventas");
	private final OpenApiInteractionValidator contratoSoloRespuesta = ValidadorContrato.soloRespuesta("ventas");

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
	private String idProducto;
	private String idCliente;
	private long idAlmacen;

	@BeforeEach
	void prepararDatos() {
		crearUsuario("apc-admin", RolUsuario.ADMINISTRADOR);
		crearUsuario("apc-vendedor", RolUsuario.VENDEDOR);
		crearUsuario("apc-almacenero", RolUsuario.ALMACENERO);
		Producto producto = productos.saveAndFlush(new Producto("APC-" + UUID.randomUUID().toString().substring(0, 6),
				"Cemento de prueba", null, "bolsa", new BigDecimal("28.50"), new BigDecimal("20"),
				categorias.findByNombre(CEMENTO).orElseThrow(), "Sol", null, true));
		idProducto = String.valueOf(producto.getId());
		idCliente = String.valueOf(jdbc.sql("INSERT INTO cliente (nombre_razon_social) VALUES ('APC Cliente de prueba') "
				+ "RETURNING id_cliente").query(Long.class).single());
		idAlmacen = jdbc.sql("INSERT INTO almacen (nombre, tipo_ubicacion) VALUES (:nombre, 'tienda') RETURNING id_almacen")
				.param("nombre", "APC Tienda " + UUID.randomUUID()).query(Long.class).single();
		jdbc.sql("INSERT INTO inventario (id_producto, id_almacen, cantidad_fisica) VALUES (:producto, :almacen, 100)")
				.param("producto", producto.getId()).param("almacen", idAlmacen).update();
	}

	@AfterEach
	void limpiarDatos() {
		jdbc.sql("DELETE FROM reserva_stock WHERE id_producto IN (SELECT id_producto FROM producto WHERE sku LIKE 'APC-%')")
				.update();
		jdbc.sql("DELETE FROM detalle_venta WHERE id_venta IN (SELECT v.id_venta FROM venta v JOIN cliente c "
				+ "ON c.id_cliente = v.id_cliente WHERE c.nombre_razon_social LIKE 'APC%')").update();
		jdbc.sql("DELETE FROM venta WHERE id_cliente IN (SELECT id_cliente FROM cliente WHERE nombre_razon_social LIKE 'APC%')")
				.update();
		jdbc.sql("DELETE FROM detalle_cotizacion WHERE id_cotizacion IN (SELECT co.id_cotizacion FROM cotizacion co "
				+ "JOIN cliente c ON c.id_cliente = co.id_cliente WHERE c.nombre_razon_social LIKE 'APC%')").update();
		jdbc.sql("DELETE FROM cotizacion WHERE id_cliente IN (SELECT id_cliente FROM cliente WHERE nombre_razon_social LIKE 'APC%')")
				.update();
		jdbc.sql("DELETE FROM cliente WHERE nombre_razon_social LIKE 'APC%'").update();
		jdbc.sql("DELETE FROM inventario WHERE id_almacen = :almacen").param("almacen", idAlmacen).update();
		jdbc.sql("DELETE FROM almacen WHERE id_almacen = :almacen").param("almacen", idAlmacen).update();
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

	/** Cotización de una línea (10 unidades) del producto de prueba, en el estado indicado, para el cliente de prueba. */
	private String cotizacion(String estado) {
		Long id = jdbc.sql("INSERT INTO cotizacion (id_cliente, id_usuario_responsable, fecha_vencimiento, descuento, estado) "
						+ "VALUES (:cliente, (SELECT id_usuario FROM usuario WHERE username = 'apc-vendedor'), "
						+ "CURRENT_DATE + 30, 0, :estado) RETURNING id_cotizacion")
				.param("cliente", Long.valueOf(idCliente)).param("estado", estado).query(Long.class).single();
		jdbc.sql("INSERT INTO detalle_cotizacion (id_cotizacion, id_producto, cantidad_cotizada, precio_unitario) "
						+ "VALUES (:cotizacion, :producto, 10, 28.50)")
				.param("cotizacion", id).param("producto", Long.valueOf(idProducto)).update();
		return String.valueOf(id);
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

	private static String clave() {
		return UUID.randomUUID().toString();
	}

	private static String desdeCotizacion(String cotizacion) {
		return "{\"idCotizacion\":\"" + cotizacion + "\"}";
	}

	private String registrarVentaDesdeCotizacion(String token, String cotizacion) throws Exception {
		MvcResult creada = alta("/api/v1/ventas", token, clave(), desdeCotizacion(cotizacion))
				.andExpect(status().isCreated())
				.andExpect(cumple(contrato))
				.andReturn();
		return JsonPath.read(creada.getResponse().getContentAsString(), "$.id");
	}

	@Test
	void registraVentaDesdeCotizacionValidadaConCobertura() throws Exception {
		String token = bearer("apc-vendedor");
		alta("/api/v1/ventas", token, clave(), desdeCotizacion(cotizacion("validada")))
				.andExpect(status().isCreated())
				.andExpect(cumple(contrato))
				.andExpect(jsonPath("$.estado", is("pendiente_despacho")))
				.andExpect(jsonPath("$.lineas[0].cantidadReservadaActual", is(10.0)))
				.andExpect(jsonPath("$.reservas", hasSize(1)));
	}

	@Test
	void repiteLaMismaClaveDevuelveLaRespuestaOriginal() throws Exception {
		String token = bearer("apc-vendedor");
		String clave = clave();
		String cuerpo = desdeCotizacion(cotizacion("validada"));
		MvcResult primera = alta("/api/v1/ventas", token, clave, cuerpo).andExpect(status().isCreated()).andReturn();
		String id = JsonPath.read(primera.getResponse().getContentAsString(), "$.id");

		alta("/api/v1/ventas", token, clave, cuerpo)
				.andExpect(status().isCreated())
				.andExpect(header().string("Idempotent-Replayed", "true"))
				.andExpect(jsonPath("$.id", is(id)))
				.andExpect(cumple(contrato));
	}

	@Test
	void rechazaUnaClaveReusadaConOtroCuerpo() throws Exception {
		String token = bearer("apc-vendedor");
		String clave = clave();
		alta("/api/v1/ventas", token, clave, desdeCotizacion(cotizacion("validada"))).andExpect(status().isCreated());

		alta("/api/v1/ventas", token, clave, desdeCotizacion(cotizacion("validada")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code", is("IDEMPOTENCIA_CONFLICTO")))
				.andExpect(cumple(contratoSoloRespuesta));
	}

	@Test
	void exigeLaCabeceraIdempotencyKey() throws Exception {
		alta("/api/v1/ventas", bearer("apc-vendedor"), null, desdeCotizacion(cotizacion("validada")))
				.andExpect(status().isBadRequest())
				.andExpect(cumple(contratoSoloRespuesta));
	}

	@Test
	void limitaElRegistroDesdeCotizacionAlVendedor() throws Exception {
		alta("/api/v1/ventas", bearer("apc-almacenero"), clave(), desdeCotizacion(cotizacion("validada")))
				.andExpect(status().isForbidden())
				.andExpect(cumple(contratoSoloRespuesta));
	}

	@Test
	void respondeCotizacionNoConvertibleCuandoNoEstaValidada() throws Exception {
		alta("/api/v1/ventas", bearer("apc-vendedor"), clave(), desdeCotizacion(cotizacion("aceptada")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code", is("COTIZACION_NO_CONVERTIBLE")))
				.andExpect(cumple(contratoSoloRespuesta));
	}

	@Test
	void consultaUnaVentaYRespondeNoEncontradaParaUnIdInexistente() throws Exception {
		String token = bearer("apc-almacenero");
		String id = registrarVentaDesdeCotizacion(bearer("apc-vendedor"), cotizacion("validada"));

		mvc.perform(get("/api/v1/ventas/" + id).header("Authorization", token))
				.andExpect(status().isOk())
				.andExpect(cumple(contrato))
				.andExpect(jsonPath("$.id", is(id)));
		mvc.perform(get("/api/v1/ventas/999999999").header("Authorization", token))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code", is("RECURSO_NO_ENCONTRADO")))
				.andExpect(cumple(contratoSoloRespuesta));
	}

	@Test
	void listaVentasPaginadasConFiltroDeEstado() throws Exception {
		String token = bearer("apc-vendedor");
		registrarVentaDesdeCotizacion(token, cotizacion("validada"));
		registrarVentaDesdeCotizacion(token, cotizacion("validada"));

		mvc.perform(get("/api/v1/ventas").header("Authorization", token)
						.param("estado", "pendiente_despacho").param("page", "1").param("pageSize", "1"))
				.andExpect(status().isOk())
				.andExpect(cumple(contrato))
				.andExpect(jsonPath("$.items", hasSize(1)))
				.andExpect(jsonPath("$.pageSize", is(1)));
		mvc.perform(get("/api/v1/ventas").header("Authorization", token).param("orden", "campoInexistente:asc"))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(cumple(contratoSoloRespuesta));
	}

	@Test
	void reintentaLaCoberturaParaElAlmacenero() throws Exception {
		String id = registrarVentaDesdeCotizacion(bearer("apc-vendedor"), cotizacion("validada"));

		alta("/api/v1/ventas/" + id + "/cobertura", bearer("apc-almacenero"), clave(), null)
				.andExpect(status().isOk())
				.andExpect(cumple(contrato))
				.andExpect(jsonPath("$.estado", is("pendiente_despacho")));
	}

	@Test
	void cancelaLaVentaYRechazaUnMotivoVacio() throws Exception {
		String token = bearer("apc-vendedor");
		String id = registrarVentaDesdeCotizacion(token, cotizacion("validada"));

		alta("/api/v1/ventas/" + id + "/cancelacion", token, clave(), "{\"motivo\":\"   \"}")
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.code", is("MOTIVO_REQUERIDO")))
				.andExpect(cumple(contratoSoloRespuesta));
		alta("/api/v1/ventas/" + id + "/cancelacion", token, clave(), "{\"motivo\":\"Cliente desistió de la obra\"}")
				.andExpect(status().isOk())
				.andExpect(cumple(contrato))
				.andExpect(jsonPath("$.estado", is("cancelada")))
				.andExpect(jsonPath("$.reservas[0].estado", is("liberada")));
	}

	@Test
	void registraVentaDirectaComoAdministrador() throws Exception {
		String cuerpo = "{\"clienteId\":\"" + idCliente + "\",\"descuento\":0,\"condicionesComerciales\":null,"
				+ "\"lineas\":[{\"productoId\":\"" + idProducto + "\",\"cantidad\":5,\"precioUnitario\":28.50}]}";

		alta("/api/v1/ventas/directas", bearer("apc-admin"), clave(), cuerpo)
				.andExpect(status().isCreated())
				.andExpect(cumple(contrato))
				.andExpect(jsonPath("$.estado", is("pendiente_despacho")));
	}

	@Test
	void rechazaUnaVentaDirectaSinCliente() throws Exception {
		String cuerpo = "{\"clienteId\":null,\"descuento\":0,"
				+ "\"lineas\":[{\"productoId\":\"" + idProducto + "\",\"cantidad\":5,\"precioUnitario\":28.50}]}";

		alta("/api/v1/ventas/directas", bearer("apc-vendedor"), clave(), cuerpo)
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.code", is("CLIENTE_REQUERIDO")))
				.andExpect(cumple(contratoSoloRespuesta));
	}
}
