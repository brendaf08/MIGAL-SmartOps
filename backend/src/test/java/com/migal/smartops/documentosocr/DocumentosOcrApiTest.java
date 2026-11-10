package com.migal.smartops.documentosocr;

import static com.migal.smartops.contrato.ValidadorContrato.cumple;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Prueba de contrato del módulo de documentos OCR: cada solicitud y respuesta se valida contra
 * {@code backend/api/modulos/documentos-ocr.yaml} (ADR-010). Se comprueban los roles, la carga multipart, el listado
 * paginado, los códigos de error declarados y el alcance del servicio de OCR. El proveedor es el simulado: un nombre de
 * archivo que contiene "ilegible" deja el documento pendiente con DOCUMENTO_ILEGIBLE; cualquier otro se procesa de inmediato.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class DocumentosOcrApiTest {

	private static final String CLAVE = "Clave12345";
	private static final String JSON = MediaType.APPLICATION_JSON_VALUE;
	private static final String RUTA = "/api/v1/documentos";

	private final OpenApiInteractionValidator contrato = ValidadorContrato.paraModulo("documentos-ocr");
	private final OpenApiInteractionValidator contratoSoloRespuesta = ValidadorContrato.soloRespuesta("documentos-ocr");

	@Autowired
	private MockMvc mvc;
	@Autowired
	private UsuarioRepository usuarios;
	@Autowired
	private PasswordEncoder codificador;
	@Autowired
	private JwtEncoder emisor;
	@Autowired
	private JdbcTemplate jdbc;

	private DatosOcr datos;
	private String sku;
	private long producto;

	@BeforeEach
	void prepararDatos() {
		datos = new DatosOcr(jdbc);
		crearUsuario("ocr-vendedor", RolUsuario.VENDEDOR);
		crearUsuario("ocr-almacenero", RolUsuario.ALMACENERO);
		crearUsuario("ocr-administrador", RolUsuario.ADMINISTRADOR);
		crearUsuario("ocr-reponedor", RolUsuario.REPONEDOR);
		sku = DatosOcr.sku();
		producto = datos.producto(sku, true);
	}

	@AfterEach
	void limpiarDatos() {
		datos.limpiar();
	}

	@Test
	void vendedorCargaUnaSolicitudDeClienteYLaRespuestaCumpleElContrato() throws Exception {
		mvc.perform(multipart(RUTA).file(archivo("pedido.pdf", "application/pdf", DatosOcr.PDF))
						.param("tipoDocumento", "solicitud_cliente").header("Authorization", bearer("ocr-vendedor")))
				.andExpect(status().isAccepted())
				.andExpect(cumple(contrato))
				.andExpect(jsonPath("$.estadoProcesamiento", is("pendiente")));
	}

	@Test
	void vendedorNoCargaDocumentosDeRecepcionResponsePermisoInsuficiente() throws Exception {
		mvc.perform(multipart(RUTA).file(archivo("guia.jpg", "image/jpeg", DatosOcr.JPG))
						.param("tipoDocumento", "documento_recepcion").header("Authorization", bearer("ocr-vendedor")))
				.andExpect(status().isForbidden())
				.andExpect(cumple(contratoSoloRespuesta))
				.andExpect(jsonPath("$.code", is("PERMISO_INSUFICIENTE")));
	}

	@Test
	void formatoNoAdmitidoResponseValidacionFallidaConElCampoArchivo() throws Exception {
		mvc.perform(multipart(RUTA).file(archivo("pedido.txt", "text/plain", "cemento".getBytes()))
						.param("tipoDocumento", "solicitud_cliente").header("Authorization", bearer("ocr-vendedor")))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(cumple(contratoSoloRespuesta))
				.andExpect(jsonPath("$.code", is("VALIDACION_FALLIDA")))
				.andExpect(jsonPath("$.errores[0].campo", is("archivo")));
	}

	@Test
	void faltaTipoDocumentoResponseSolicitudMalFormada() throws Exception {
		mvc.perform(multipart(RUTA).file(archivo("pedido.pdf", "application/pdf", DatosOcr.PDF))
						.header("Authorization", bearer("ocr-vendedor")))
				.andExpect(status().isBadRequest())
				.andExpect(cumple(contratoSoloRespuesta))
				.andExpect(jsonPath("$.code", is("SOLICITUD_MAL_FORMADA")));
	}

	@Test
	void documentoProcesadoSeConsultaConSusLineasYLosUmbrales() throws Exception {
		String token = bearer("ocr-almacenero");
		String id = subir(token, "guia-0812.jpg");

		mvc.perform(get(RUTA + "/" + id).header("Authorization", token))
				.andExpect(status().isOk())
				.andExpect(cumple(contrato))
				.andExpect(jsonPath("$.estadoProcesamiento", is("procesado_ocr")))
				.andExpect(jsonPath("$.detalles", hasSize(1)))
				.andExpect(jsonPath("$.detalles[0].requiereRevision", is(true)))
				.andExpect(jsonPath("$.umbrales.alta", is(0.85)));
	}

	@Test
	void listadoPaginadoCumpleElContratoYRechazaFiltrosInvalidos() throws Exception {
		String token = bearer("ocr-almacenero");
		subir(token, "guia-a.jpg");
		subir(token, "guia-b.jpg");

		mvc.perform(get(RUTA).param("page", "1").param("pageSize", "1").param("estadoProcesamiento", "procesado_ocr")
						.param("orden", "fechaSubida:desc").header("Authorization", token))
				.andExpect(status().isOk())
				.andExpect(cumple(contrato))
				.andExpect(jsonPath("$.pageSize", is(1)))
				.andExpect(jsonPath("$.total", greaterThanOrEqualTo(2)));

		mvc.perform(get(RUTA).param("estadoProcesamiento", "bloqueado").header("Authorization", token))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(cumple(contratoSoloRespuesta))
				.andExpect(jsonPath("$.code", is("VALIDACION_FALLIDA")));
	}

	@Test
	void reponedorNoOperaDocumentosYDocumentoInexistenteResponde404() throws Exception {
		mvc.perform(get(RUTA).header("Authorization", bearer("ocr-reponedor")))
				.andExpect(status().isForbidden())
				.andExpect(cumple(contratoSoloRespuesta));

		mvc.perform(get(RUTA + "/999999999").header("Authorization", bearer("ocr-administrador")))
				.andExpect(status().isNotFound())
				.andExpect(cumple(contratoSoloRespuesta))
				.andExpect(jsonPath("$.code", is("RECURSO_NO_ENCONTRADO")));
	}

	@Test
	void correccionDeLineaYValidacionCumplenElContrato() throws Exception {
		String token = bearer("ocr-almacenero");
		String id = subir(token, "guia-0812.jpg");
		String linea = idDetalle(token, id);

		mvc.perform(put(RUTA + "/" + id + "/detalles/" + linea).contentType(JSON)
						.content("{\"idProducto\":\"" + producto + "\",\"cantidad\":12.5}").header("Authorization", token))
				.andExpect(status().isOk())
				.andExpect(cumple(contrato))
				.andExpect(jsonPath("$.sku", is(sku)))
				.andExpect(jsonPath("$.cantidadCorregida", is(12.5)));

		mvc.perform(post(RUTA + "/" + id + "/validacion").header("Authorization", token))
				.andExpect(status().isOk())
				.andExpect(cumple(contrato))
				.andExpect(jsonPath("$.estadoProcesamiento", is("validado")));

		mvc.perform(post(RUTA + "/" + id + "/validacion").header("Authorization", token))
				.andExpect(status().isConflict())
				.andExpect(cumple(contratoSoloRespuesta))
				.andExpect(jsonPath("$.code", is("DOCUMENTO_NO_VALIDABLE")));
	}

	@Test
	void validacionSinProductoConfirmadoResponseValidacionFallidaConLaListaDeErrores() throws Exception {
		String token = bearer("ocr-almacenero");
		String id = subir(token, "guia-0812.jpg");

		mvc.perform(post(RUTA + "/" + id + "/validacion").header("Authorization", token))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(cumple(contratoSoloRespuesta))
				.andExpect(jsonPath("$.errores", hasSize(1)));
	}

	@Test
	void correccionInvalidaResponderSegunElCodigoDelCatalogo() throws Exception {
		String token = bearer("ocr-almacenero");
		String id = subir(token, "guia-0812.jpg");
		String linea = idDetalle(token, id);
		long inactivo = datos.producto(DatosOcr.sku(), false);

		mvc.perform(put(RUTA + "/" + id + "/detalles/" + linea).contentType(JSON).content("{}")
						.header("Authorization", token))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(cumple(contratoSoloRespuesta))
				.andExpect(jsonPath("$.code", is("VALIDACION_FALLIDA")));

		mvc.perform(put(RUTA + "/" + id + "/detalles/" + linea).contentType(JSON)
						.content("{\"idProducto\":\"" + inactivo + "\"}").header("Authorization", token))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(cumple(contratoSoloRespuesta))
				.andExpect(jsonPath("$.code", is("PRODUCTO_INACTIVO")));
	}

	@Test
	void resultadosDelOcrRequierenElAlcanceDelServicio() throws Exception {
		String token = bearer("ocr-almacenero");
		String id = subir(token, "ilegible-0812.jpg");
		String cuerpo = "{\"resultado\":\"procesado\",\"detalles\":[{\"textoCrudo\":\"CEMENTO SOL 42.5KG\","
				+ "\"cantidadLeida\":100,\"coincidencias\":[{\"sku\":\"" + sku + "\",\"confianza\":0.92}]}]}";

		mvc.perform(put(RUTA + "/" + id + "/resultados").contentType(JSON).content(cuerpo).header("Authorization", token))
				.andExpect(status().isForbidden())
				.andExpect(cumple(contratoSoloRespuesta))
				.andExpect(jsonPath("$.code", is("PERMISO_INSUFICIENTE")));

		mvc.perform(put(RUTA + "/" + id + "/resultados").contentType(JSON).content(cuerpo)
						.header("Authorization", tokenServicio()))
				.andExpect(status().isOk())
				.andExpect(cumple(contrato))
				.andExpect(jsonPath("$.estadoProcesamiento", is("procesado_ocr")))
				.andExpect(jsonPath("$.detalles[0].sku", nullValue()))
				.andExpect(jsonPath("$.detalles[0].coincidencias[0].sku", is(sku)));

		mvc.perform(put(RUTA + "/" + id + "/resultados").contentType(JSON).content(cuerpo)
						.header("Authorization", tokenServicio()))
				.andExpect(status().isConflict())
				.andExpect(cumple(contratoSoloRespuesta))
				.andExpect(jsonPath("$.code", is("TRANSICION_NO_PERMITIDA")));
	}

	@Test
	void reprocesarUnDocumentoIlegibleLoDevuelvePendiente() throws Exception {
		String token = bearer("ocr-administrador");
		String id = subir(token, "ilegible-0812.jpg");

		mvc.perform(post(RUTA + "/" + id + "/procesamiento").header("Authorization", token))
				.andExpect(status().isAccepted())
				.andExpect(cumple(contrato))
				.andExpect(jsonPath("$.estadoProcesamiento", is("pendiente")));

		String procesado = subir(token, "guia-0813.jpg");
		mvc.perform(post(RUTA + "/" + procesado + "/procesamiento").header("Authorization", token))
				.andExpect(status().isConflict())
				.andExpect(cumple(contratoSoloRespuesta))
				.andExpect(jsonPath("$.code", is("TRANSICION_NO_PERMITIDA")));
	}

	@Test
	void asociacionAmbiguaResponseValidacionFallidaYDescarteOculta() throws Exception {
		String token = bearer("ocr-almacenero");
		String id = subir(token, "guia-0812.jpg");

		mvc.perform(post(RUTA + "/" + id + "/asociacion").contentType(JSON)
						.content("{\"idCotizacion\":\"1\",\"idRecepcion\":\"2\"}").header("Authorization", token))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(cumple(contratoSoloRespuesta))
				.andExpect(jsonPath("$.code", is("VALIDACION_FALLIDA")));

		mvc.perform(delete(RUTA + "/" + id).header("Authorization", token))
				.andExpect(status().isNoContent());
		mvc.perform(get(RUTA + "/" + id).header("Authorization", token))
				.andExpect(status().isNotFound())
				.andExpect(cumple(contratoSoloRespuesta));
	}

	/** Carga un documento como almacenero y devuelve su identificador. */
	private String subir(String token, String nombre) throws Exception {
		MvcResult creado = mvc.perform(multipart(RUTA).file(archivo(nombre, "image/jpeg", DatosOcr.JPG))
						.param("tipoDocumento", "documento_recepcion").header("Authorization", token))
				.andExpect(status().isAccepted())
				.andExpect(cumple(contrato))
				.andReturn();
		return JsonPath.read(creado.getResponse().getContentAsString(), "$.id");
	}

	/** Identificador de la primera línea de un documento. */
	private String idDetalle(String token, String id) throws Exception {
		MvcResult consulta = mvc.perform(get(RUTA + "/" + id).header("Authorization", token)).andExpect(status().isOk())
				.andReturn();
		return JsonPath.read(consulta.getResponse().getContentAsString(), "$.detalles[0].id");
	}

	private static MockMultipartFile archivo(String nombre, String tipo, byte[] contenido) {
		return new MockMultipartFile("archivo", nombre, tipo, contenido);
	}

	private void crearUsuario(String username, RolUsuario rol) {
		Usuario usuario = new Usuario("Prueba", "OCR", username, codificador.encode(CLAVE), rol);
		usuario.cambiarEstado(true, Instant.now());
		usuarios.saveAndFlush(usuario);
	}

	private String bearer(String username) throws Exception {
		ResultActions sesion = mvc.perform(post("/api/v1/sesiones").contentType(JSON)
				.content("{\"username\":\"" + username + "\",\"clave\":\"" + CLAVE + "\"}"));
		MvcResult resultado = sesion.andExpect(status().isOk()).andReturn();
		return "Bearer " + JsonPath.read(resultado.getResponse().getContentAsString(), "$.token");
	}

	/** Token de servicio del servicio de OCR, con el alcance de resultados. */
	private String tokenServicio() {
		Instant ahora = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder().subject("servicio-ocr").claim("tipo", "servicio")
				.claim("alcances", List.of("ocr:resultados:escribir")).issuedAt(ahora)
				.expiresAt(ahora.plusSeconds(300)).build();
		return "Bearer " + emisor.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
				.getTokenValue();
	}
}
