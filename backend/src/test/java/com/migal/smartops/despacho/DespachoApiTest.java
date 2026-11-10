package com.migal.smartops.despacho;

import static com.migal.smartops.contrato.ValidadorContrato.cumple;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
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
import com.migal.smartops.inventario.service.ReservaInventarioService;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Prueba de contrato del módulo de despacho: cada solicitud y respuesta se valida contra
 * {@code backend/api/modulos/despacho.yaml} (ADR-010). Se comprueban los roles, los códigos de error, la confirmación
 * idempotente de la salida y la paginación. Los datos de cada prueba se eliminan al final: clientes DSP, productos DSP-
 * y usuarios dsp-.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class DespachoApiTest {

	private static final String CLAVE = "Clave12345";
	private static final String JSON = MediaType.APPLICATION_JSON_VALUE;
	private static final String CEMENTO = "Cemento y aglomerantes";
	private static final String FILTRO = "?q=DSP";

	private final OpenApiInteractionValidator contrato = ValidadorContrato.paraModulo("despacho");
	private final OpenApiInteractionValidator contratoSoloRespuesta = ValidadorContrato.soloRespuesta("despacho");

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
	private ReservaInventarioService reservas;
	@Autowired
	private JdbcClient jdbc;

	private final List<Usuario> usuariosDePrueba = new ArrayList<>();
	private long idProducto;
	private long idCliente;
	private long idAlmacen;

	@BeforeEach
	void prepararDatos() {
		crearUsuario("dsp-admin", RolUsuario.ADMINISTRADOR);
		crearUsuario("dsp-vendedor", RolUsuario.VENDEDOR);
		crearUsuario("dsp-almacenero", RolUsuario.ALMACENERO);
		Producto producto = productos.saveAndFlush(new Producto("DSP-" + UUID.randomUUID().toString().substring(0, 6),
				"Cemento de prueba", null, "bolsa", new BigDecimal("28.50"), new BigDecimal("20"),
				categorias.findByNombre(CEMENTO).orElseThrow(), "Sol", null, true));
		idProducto = producto.getId();
		idCliente = jdbc.sql("INSERT INTO cliente (nombre_razon_social, direccion_referencia) "
				+ "VALUES ('DSP Cliente de prueba', 'Av. DSP 100') RETURNING id_cliente").query(Long.class).single();
		idAlmacen = jdbc.sql("INSERT INTO almacen (nombre, tipo_ubicacion) VALUES (:nombre, 'tienda') RETURNING id_almacen")
				.param("nombre", "DSP Tienda " + UUID.randomUUID()).query(Long.class).single();
		jdbc.sql("INSERT INTO inventario (id_producto, id_almacen, cantidad_fisica) VALUES (:producto, :almacen, 100)")
				.param("producto", idProducto).param("almacen", idAlmacen).update();
	}

	@AfterEach
	void limpiarDatos() {
		jdbc.sql("ALTER TABLE movimiento_kardex DISABLE TRIGGER tg_movimiento_kardex_solo_anexion").update();
		jdbc.sql("ALTER TABLE reimpresion_guia DISABLE TRIGGER tg_reimpresion_guia_solo_anexion").update();
		try {
			jdbc.sql("DELETE FROM movimiento_kardex WHERE id_inventario IN (SELECT id_inventario FROM inventario "
					+ "WHERE id_producto IN (SELECT id_producto FROM producto WHERE sku LIKE 'DSP-%'))").update();
			jdbc.sql("DELETE FROM reimpresion_guia WHERE id_guia IN (SELECT g.id_guia FROM guia_despacho g "
					+ "JOIN venta v ON v.id_venta = g.id_venta JOIN cliente c ON c.id_cliente = v.id_cliente "
					+ "WHERE c.nombre_razon_social LIKE 'DSP%')").update();
		} finally {
			jdbc.sql("ALTER TABLE reimpresion_guia ENABLE TRIGGER tg_reimpresion_guia_solo_anexion").update();
			jdbc.sql("ALTER TABLE movimiento_kardex ENABLE TRIGGER tg_movimiento_kardex_solo_anexion").update();
		}
		jdbc.sql("DELETE FROM guia_despacho WHERE id_venta IN (SELECT v.id_venta FROM venta v JOIN cliente c "
				+ "ON c.id_cliente = v.id_cliente WHERE c.nombre_razon_social LIKE 'DSP%')").update();
		jdbc.sql("DELETE FROM reserva_stock WHERE id_producto IN (SELECT id_producto FROM producto WHERE sku LIKE 'DSP-%')")
				.update();
		jdbc.sql("DELETE FROM detalle_venta WHERE id_venta IN (SELECT v.id_venta FROM venta v JOIN cliente c "
				+ "ON c.id_cliente = v.id_cliente WHERE c.nombre_razon_social LIKE 'DSP%')").update();
		jdbc.sql("DELETE FROM venta WHERE id_cliente IN (SELECT id_cliente FROM cliente WHERE nombre_razon_social LIKE 'DSP%')")
				.update();
		jdbc.sql("DELETE FROM cotizacion WHERE id_cliente IN (SELECT id_cliente FROM cliente WHERE nombre_razon_social LIKE 'DSP%')")
				.update();
		jdbc.sql("DELETE FROM cliente WHERE nombre_razon_social LIKE 'DSP%'").update();
		jdbc.sql("DELETE FROM inventario WHERE id_almacen = :almacen").param("almacen", idAlmacen).update();
		jdbc.sql("DELETE FROM almacen WHERE id_almacen = :almacen").param("almacen", idAlmacen).update();
		jdbc.sql("DELETE FROM producto WHERE sku LIKE 'DSP-%'").update();
		jdbc.sql("DELETE FROM idempotencia WHERE id_usuario IN (SELECT id_usuario FROM usuario WHERE username LIKE 'dsp-%')")
				.update();
		usuarios.deleteAll(usuariosDePrueba);
		usuariosDePrueba.clear();
	}

	private void crearUsuario(String username, RolUsuario rol) {
		Usuario usuario = new Usuario("Rosa", "Quispe", username, codificador.encode(CLAVE), rol);
		usuario.cambiarEstado(true, Instant.now());
		usuariosDePrueba.add(usuarios.saveAndFlush(usuario));
	}

	private long idUsuario(String username) {
		return jdbc.sql("SELECT id_usuario FROM usuario WHERE username = :username").param("username", username)
				.query(Long.class).single();
	}

	private String bearer(String username) throws Exception {
		MvcResult resultado = mvc.perform(post("/api/v1/sesiones").contentType(JSON)
						.content("{\"username\":\"" + username + "\",\"clave\":\"" + CLAVE + "\"}"))
				.andExpect(status().isOk())
				.andReturn();
		return "Bearer " + JsonPath.read(resultado.getResponse().getContentAsString(), "$.token");
	}

	/** Venta de 10 unidades en el estado indicado; con {@code reservar} se reserva el stock de la línea. */
	private long venta(String estado, boolean reservar) {
		Long cotizacion = jdbc.sql("INSERT INTO cotizacion (id_cliente, id_usuario_responsable, fecha_vencimiento, descuento, estado) "
						+ "VALUES (:cliente, :usuario, CURRENT_DATE + 30, 0, 'validada') RETURNING id_cotizacion")
				.param("cliente", idCliente).param("usuario", idUsuario("dsp-vendedor")).query(Long.class).single();
		Long venta = jdbc.sql("INSERT INTO venta (id_cotizacion, id_cliente, id_usuario_responsable, descuento, estado) "
						+ "VALUES (:cotizacion, :cliente, :usuario, 0, :estado) RETURNING id_venta")
				.param("cotizacion", cotizacion).param("cliente", idCliente).param("usuario", idUsuario("dsp-vendedor"))
				.param("estado", estado).query(Long.class).single();
		Long detalle = jdbc.sql("INSERT INTO detalle_venta (id_venta, id_producto, cantidad_vendida, precio_unitario) "
						+ "VALUES (:venta, :producto, 10, 28.50) RETURNING id_detalle_venta")
				.param("venta", venta).param("producto", idProducto).query(Long.class).single();
		if (reservar) {
			reservas.reservar(idUsuario("dsp-vendedor"), detalle, idProducto, new BigDecimal("10"));
		}
		return venta;
	}

	/** Venta pendiente de despacho con todo su stock reservado. */
	private long ventaPendiente() {
		return venta("pendiente_despacho", true);
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

	private String emitirGuia(String token, long idVenta) throws Exception {
		MvcResult creada = alta("/api/v1/guias-despacho", token, null, "{\"idVenta\":\"" + idVenta + "\"}")
				.andExpect(status().isCreated())
				.andExpect(cumple(contrato))
				.andReturn();
		return JsonPath.read(creada.getResponse().getContentAsString(), "$.id");
	}

	private ResultActions confirmarSalida(String token, String idGuia, String clave) throws Exception {
		return alta("/api/v1/guias-despacho/" + idGuia + "/salida", token, clave, null);
	}

	@Test
	void listaLasGuiasSoloParaAlmaceneroYAdministrador() throws Exception {
		String almacenero = bearer("dsp-almacenero");
		emitirGuia(almacenero, ventaPendiente());

		mvc.perform(get("/api/v1/guias-despacho" + FILTRO).header("Authorization", almacenero))
				.andExpect(status().isOk())
				.andExpect(cumple(contrato))
				.andExpect(jsonPath("$.items", hasSize(1)))
				.andExpect(jsonPath("$.page", is(1)))
				.andExpect(jsonPath("$.pageSize", is(20)));
		mvc.perform(get("/api/v1/guias-despacho" + FILTRO).header("Authorization", bearer("dsp-admin")))
				.andExpect(status().isOk())
				.andExpect(cumple(contrato));
		mvc.perform(get("/api/v1/guias-despacho" + FILTRO).header("Authorization", bearer("dsp-vendedor")))
				.andExpect(status().isForbidden())
				.andExpect(cumple(contratoSoloRespuesta));
	}

	@Test
	void emiteLaGuiaConRolAlmaceneroYLaConsultaSinCambiarla() throws Exception {
		String almacenero = bearer("dsp-almacenero");
		String cuerpo = "{\"idVenta\":\"" + ventaPendiente() + "\"}";

		MvcResult creada = alta("/api/v1/guias-despacho", almacenero, null, cuerpo)
				.andExpect(status().isCreated())
				.andExpect(cumple(contrato))
				.andExpect(jsonPath("$.estado", is("emitida")))
				.andExpect(jsonPath("$.codigo", startsWith("D-")))
				.andExpect(jsonPath("$.direccionEntrega", is("Av. DSP 100")))
				.andExpect(jsonPath("$.lineas[0].ubicacionesSalida", hasSize(1)))
				.andReturn();
		String id = JsonPath.read(creada.getResponse().getContentAsString(), "$.id");
		mvc.perform(get("/api/v1/guias-despacho/" + id).header("Authorization", almacenero))
				.andExpect(status().isOk())
				.andExpect(cumple(contrato))
				.andExpect(jsonPath("$.estado", is("emitida")));
		mvc.perform(get("/api/v1/guias-despacho/" + id + "/documento").header("Authorization", almacenero))
				.andExpect(status().isOk())
				.andExpect(cumple(contrato));
	}

	@Test
	void rechazaUnaSegundaGuiaConGuiaExistente() throws Exception {
		String almacenero = bearer("dsp-almacenero");
		long venta = ventaPendiente();
		emitirGuia(almacenero, venta);

		alta("/api/v1/guias-despacho", almacenero, null, "{\"idVenta\":\"" + venta + "\"}")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code", is("GUIA_EXISTENTE")))
				.andExpect(cumple(contratoSoloRespuesta));
	}

	@Test
	void rechazaLaEmisionDeUnaVentaNoDespachable() throws Exception {
		long venta = venta("pendiente_abastecimiento", false);

		alta("/api/v1/guias-despacho", bearer("dsp-almacenero"), null, "{\"idVenta\":\"" + venta + "\"}")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code", is("VENTA_NO_DESPACHABLE")))
				.andExpect(cumple(contratoSoloRespuesta));
	}

	@Test
	void rechazaUnaEmisionSinIdentificarLaVenta() throws Exception {
		alta("/api/v1/guias-despacho", bearer("dsp-almacenero"), null, "{}")
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.code", is("VALIDACION_FALLIDA")))
				.andExpect(cumple(contratoSoloRespuesta));
	}

	@Test
	void responde404ParaUnaGuiaInexistente() throws Exception {
		mvc.perform(get("/api/v1/guias-despacho/999999999").header("Authorization", bearer("dsp-almacenero")))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code", is("RECURSO_NO_ENCONTRADO")))
				.andExpect(cumple(contratoSoloRespuesta));
	}

	@Test
	void confirmaLaSalidaYReproduceLaRespuestaConLaMismaClave() throws Exception {
		String almacenero = bearer("dsp-almacenero");
		String guia = emitirGuia(almacenero, ventaPendiente());
		String clave = clave();

		confirmarSalida(almacenero, guia, clave)
				.andExpect(status().isOk())
				.andExpect(cumple(contrato))
				.andExpect(jsonPath("$.guia.estado", is("salida_confirmada")))
				.andExpect(jsonPath("$.movimientos", hasSize(1)))
				.andExpect(jsonPath("$.movimientos[0].origenMovimiento", is("despacho")))
				.andExpect(jsonPath("$.movimientos[0].tipoOperacion", is("salida")))
				.andExpect(jsonPath("$.cambios", hasSize(1)));

		confirmarSalida(almacenero, guia, clave)
				.andExpect(status().isOk())
				.andExpect(header().string("Idempotent-Replayed", "true"))
				.andExpect(cumple(contrato))
				.andExpect(jsonPath("$.guia.id", is(guia)));
	}

	@Test
	void exigeLaCabeceraIdempotencyKeyYRechazaClavesReutilizadas() throws Exception {
		String almacenero = bearer("dsp-almacenero");
		String primera = emitirGuia(almacenero, ventaPendiente());
		String segunda = emitirGuia(almacenero, ventaPendiente());
		String clave = clave();

		confirmarSalida(almacenero, primera, null)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code", is("SOLICITUD_MAL_FORMADA")))
				.andExpect(cumple(contratoSoloRespuesta));
		confirmarSalida(almacenero, primera, clave).andExpect(status().isOk());
		confirmarSalida(almacenero, primera, clave()).andExpect(status().isConflict())
				.andExpect(jsonPath("$.code", is("SALIDA_NO_CONFIRMABLE")))
				.andExpect(cumple(contratoSoloRespuesta));
		confirmarSalida(almacenero, segunda, clave)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code", is("IDEMPOTENCIA_CONFLICTO")))
				.andExpect(cumple(contratoSoloRespuesta));
	}

	@Test
	void registraLaEntregaYRechazaRepetirla() throws Exception {
		String almacenero = bearer("dsp-almacenero");
		String guia = emitirGuia(almacenero, ventaPendiente());
		confirmarSalida(almacenero, guia, clave()).andExpect(status().isOk());

		alta("/api/v1/guias-despacho/" + guia + "/entrega", almacenero, null, null)
				.andExpect(status().isOk())
				.andExpect(cumple(contrato))
				.andExpect(jsonPath("$.estado", is("entregada")))
				.andExpect(jsonPath("$.fechaEntrega", notNullValue()));
		alta("/api/v1/guias-despacho/" + guia + "/entrega", almacenero, null, null)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code", is("TRANSICION_NO_PERMITIDA")))
				.andExpect(cumple(contratoSoloRespuesta));
	}

	@Test
	void reimprimeConMotivoYRechazaUnMotivoVacio() throws Exception {
		String almacenero = bearer("dsp-almacenero");
		String guia = emitirGuia(almacenero, ventaPendiente());

		alta("/api/v1/guias-despacho/" + guia + "/reimpresiones", almacenero, null, "{\"motivo\":\"   \"}")
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.code", is("VALIDACION_FALLIDA")))
				.andExpect(cumple(contratoSoloRespuesta));
		alta("/api/v1/guias-despacho/" + guia + "/reimpresiones", almacenero, null,
						"{\"motivo\":\"Documento dañado durante el traslado\"}")
				.andExpect(status().isCreated())
				.andExpect(cumple(contrato))
				.andExpect(jsonPath("$.motivo", is("Documento dañado durante el traslado")));
	}

	@Test
	void listaLasVentasPendientesDeDespachoPaginadas() throws Exception {
		String almacenero = bearer("dsp-almacenero");
		ventaPendiente();
		ventaPendiente();

		mvc.perform(get("/api/v1/ventas-pendientes-despacho?q=DSP&page=1&pageSize=1").header("Authorization", almacenero))
				.andExpect(status().isOk())
				.andExpect(cumple(contrato))
				.andExpect(jsonPath("$.items", hasSize(1)))
				.andExpect(jsonPath("$.total", is(2)))
				.andExpect(jsonPath("$.pageSize", is(1)))
				.andExpect(jsonPath("$.items[0].estado", is("pendiente_despacho")));
	}

	@Test
	void filtraLasGuiasPorEstado() throws Exception {
		String almacenero = bearer("dsp-almacenero");
		emitirGuia(almacenero, ventaPendiente());

		mvc.perform(get("/api/v1/guias-despacho" + FILTRO + "&estado=emitida").header("Authorization", almacenero))
				.andExpect(status().isOk())
				.andExpect(cumple(contratoSoloRespuesta))
				.andExpect(jsonPath("$.items", hasSize(1)))
				.andExpect(jsonPath("$.items[0].estado", is("emitida")));
		mvc.perform(get("/api/v1/guias-despacho" + FILTRO + "&estado=entregada").header("Authorization", almacenero))
				.andExpect(status().isOk())
				.andExpect(cumple(contratoSoloRespuesta))
				.andExpect(jsonPath("$.total", is(0)));
	}
}
