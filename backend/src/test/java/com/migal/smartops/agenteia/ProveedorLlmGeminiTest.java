package com.migal.smartops.agenteia;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.migal.smartops.agenteia.config.PropiedadesAgenteGemini;
import com.migal.smartops.agenteia.service.DefinicionHerramienta;
import com.migal.smartops.agenteia.service.InvocacionHerramienta;
import com.migal.smartops.agenteia.service.LlamadaHerramienta;
import com.migal.smartops.agenteia.service.ParametroHerramienta;
import com.migal.smartops.agenteia.service.ProveedorLlmGemini;
import com.migal.smartops.agenteia.service.RespuestaRedactada;
import com.migal.smartops.agenteia.service.TurnoAgente;
import com.migal.smartops.agenteia.service.ResultadoHerramienta;
import com.migal.smartops.comun.errores.CodigoError;
import com.migal.smartops.comun.errores.ErrorDeNegocio;
import com.migal.smartops.seguridad.model.RolUsuario;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * Verifica el proveedor Gemini con un servidor HTTP simulado (ADR-012, punto 7): las herramientas declaradas, la
 * interpretación de las llamadas, la redacción estructurada y los errores. Ninguna prueba llama a la API real.
 */
class ProveedorLlmGeminiTest {

	private static final String URL = "https://gemini.local/v1beta/models/gemini-3.5-flash-lite:generateContent";

	private final PropiedadesAgenteGemini propiedades = new PropiedadesAgenteGemini("clave-ficticia",
			"gemini-3.5-flash-lite", "https://gemini.local", null, null);
	private MockRestServiceServer servidor;
	private ProveedorLlmGemini proveedor;

	@BeforeEach
	void preparar() {
		RestClient.Builder constructor = RestClient.builder().baseUrl("https://gemini.local");
		servidor = MockRestServiceServer.bindTo(constructor).build();
		proveedor = new ProveedorLlmGemini(constructor.build(), propiedades);
	}

	@Test
	void declaraSoloLasHerramientasDisponiblesYLeeSusLlamadas() {
		servidor.expect(requestTo(URL))
				.andExpect(method(HttpMethod.POST))
				.andExpect(header("x-goog-api-key", "clave-ficticia"))
				.andExpect(content().string(allOf(
						containsString("\"name\":\"listarAlertasStock\""),
						containsString("\"enum\":[\"critico\",\"minimo\"]"),
						containsString("\"name\":\"consultarExistenciasProducto\""),
						containsString("\"required\":[\"id\"]"),
						containsString("\"type\":\"INTEGER\""),
						not(containsString("consultarKardex")))))
				.andRespond(withSuccess(respuesta("""
						{"functionCall":{"name":"listarAlertasStock","args":{"nivel":"critico"}}},
						{"functionCall":{"name":"consultarExistenciasProducto","args":{"id":42.0}}}"""),
						MediaType.APPLICATION_JSON));

		List<LlamadaHerramienta> llamadas = proveedor.planificar("¿Qué productos están en nivel crítico?",
				List.of(herramienta("listarAlertasStock", ParametroHerramienta.enumerado("nivel", "Nivel.", "critico",
						"minimo")), herramienta("consultarExistenciasProducto", ParametroHerramienta.identificador("Id."))));

		servidor.verify();
		assertEquals(List.of(new LlamadaHerramienta("listarAlertasStock", Map.of("nivel", "critico")),
				new LlamadaHerramienta("consultarExistenciasProducto", Map.of("id", "42"))), llamadas);
	}

	@Test
	void enviaLosTurnosPreviosYUnLimiteDeTokens() {
		servidor.expect(requestTo(URL))
				.andExpect(content().string(allOf(
						containsString("\"role\":\"model\",\"parts\":[{\"text\":\"Hay 12 bolsas en Tienda.\"}]"),
						containsString("\"text\":\"¿y en el almacén 2?\""),
						containsString("\"maxOutputTokens\":512"))))
				.andRespond(withSuccess(respuesta("{\"text\":\"Sin llamadas\"}"), MediaType.APPLICATION_JSON));

		proveedor.planificar("¿y en el almacén 2?", List.of(herramienta("listarAlertasStock")),
				List.of(new TurnoAgente(true, "¿cuánto cemento hay?"), new TurnoAgente(false, "Hay 12 bolsas en Tienda.")));

		servidor.verify();
	}

	@Test
	void descartaHerramientasNoDeclaradasYLimitaLasLlamadas() {
		servidor.expect(requestTo(URL)).andRespond(withSuccess(respuesta("""
				{"functionCall":{"name":"consultarKardex","args":{}}},
				{"functionCall":{"name":"listarAlertasStock","args":{}}},
				{"functionCall":{"name":"listarAlertasStock","args":{"nivel":"minimo"}}},
				{"functionCall":{"name":"listarAlertasStock","args":{"nivel":"critico"}}},
				{"functionCall":{"name":"listarAlertasStock","args":{}}}"""), MediaType.APPLICATION_JSON));

		List<LlamadaHerramienta> llamadas = proveedor.planificar("alertas", List.of(herramienta("listarAlertasStock")));

		assertEquals(3, llamadas.size());
		assertTrue(llamadas.stream().allMatch(l -> l.nombre().equals("listarAlertasStock")));
	}

	@Test
	void unaRespuestaSinLlamadasNoEjecutaHerramientas() {
		servidor.expect(requestTo(URL)).andRespond(withSuccess(respuesta("{\"text\":\"Hola\"}"),
				MediaType.APPLICATION_JSON));

		assertTrue(proveedor.planificar("hola", List.of(herramienta("listarAlertasStock"))).isEmpty());
	}

	@Test
	void sinHerramientasDisponiblesNoLlamaAlModelo() {
		assertTrue(proveedor.planificar("alertas", List.of()).isEmpty());
		servidor.verify();
	}

	@Test
	void redactaConLosDatosDeLasHerramientasExitosas() {
		servidor.expect(requestTo(URL))
				.andExpect(content().string(allOf(containsString("responseSchema"),
						containsString("Hay 1 producto en alerta"), not(containsString("Sin permiso")))))
				.andRespond(withSuccess(respuesta(
						"{\"text\":\"{\\\"texto\\\":\\\"Hay un producto crítico: Cemento Sol.\\\",\\\"incierta\\\":false}\"}"),
						MediaType.APPLICATION_JSON));

		RespuestaRedactada redactada = proveedor.redactar("alertas", List.of(
				InvocacionHerramienta.exitosa("listarAlertasStock", Map.of(),
						new ResultadoHerramienta("Hay 1 producto en alerta de stock.", List.of())),
				InvocacionHerramienta.fallida("consultarKardex", Map.of(), "Sin permiso",
						CodigoError.PERMISO_INSUFICIENTE)));

		assertEquals("Hay un producto crítico: Cemento Sol.", redactada.texto());
		assertFalse(redactada.incierta());
	}

	@Test
	void sinDatosRespondeInciertoSinLlamarAlModelo() {
		RespuestaRedactada redactada = proveedor.redactar("¿cuánto cemento hay?", List.of());

		servidor.verify();
		assertTrue(redactada.incierta());
		assertFalse(redactada.texto().matches(".*\\d.*"));
	}

	@Test
	void unaApiSaturadaSeReintentaUnaVezAntesDeRendirse() {
		servidor.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
		servidor.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

		ErrorDeNegocio error = assertThrows(ErrorDeNegocio.class,
				() -> proveedor.planificar("alertas", List.of(herramienta("listarAlertasStock"))));
		assertEquals(CodigoError.SERVICIO_NO_DISPONIBLE, error.codigo());
		assertTrue(error.getMessage().contains("saturado"));
		servidor.verify();
	}

	@Test
	void unaSaturacionPasajeraSeRecuperaConElReintento() {
		servidor.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
		servidor.expect(requestTo(URL)).andRespond(withSuccess(respuesta("{\"text\":\"Sin llamadas\"}"),
				MediaType.APPLICATION_JSON));

		assertTrue(proveedor.planificar("alertas", List.of(herramienta("listarAlertasStock"))).isEmpty());
		servidor.verify();
	}

	@Test
	void conLaCuotaAgotadaPasaAlSiguienteModeloYLoOmiteDespues() {
		PropiedadesAgenteGemini dos = new PropiedadesAgenteGemini("clave-ficticia",
				"gemini-3.5-flash-lite, gemini-3.1-flash-lite", "https://gemini.local", null, null);
		RestClient.Builder constructor = RestClient.builder().baseUrl("https://gemini.local");
		MockRestServiceServer simulado = MockRestServiceServer.bindTo(constructor).build();
		ProveedorLlmGemini conRespaldo = new ProveedorLlmGemini(constructor.build(), dos);
		String respaldo = "https://gemini.local/v1beta/models/gemini-3.1-flash-lite:generateContent";
		simulado.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
		simulado.expect(requestTo(respaldo)).andRespond(withSuccess(respuesta("{\"text\":\"Sin llamadas\"}"),
				MediaType.APPLICATION_JSON));
		// La segunda consulta ya no toca el modelo agotado.
		simulado.expect(requestTo(respaldo)).andRespond(withSuccess(respuesta("{\"text\":\"Sin llamadas\"}"),
				MediaType.APPLICATION_JSON));

		assertTrue(conRespaldo.planificar("alertas", List.of(herramienta("listarAlertasStock"))).isEmpty());
		assertTrue(conRespaldo.planificar("alertas", List.of(herramienta("listarAlertasStock"))).isEmpty());
		simulado.verify();
	}

	@Test
	void unaApiNoDisponibleRespondeServicioNoDisponible() {
		servidor.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.BAD_GATEWAY));

		ErrorDeNegocio error = assertThrows(ErrorDeNegocio.class,
				() -> proveedor.planificar("alertas", List.of(herramienta("listarAlertasStock"))));
		assertEquals(CodigoError.SERVICIO_NO_DISPONIBLE, error.codigo());
	}

	@Test
	void unaRedaccionMalFormadaRespondeServicioNoDisponible() {
		servidor.expect(requestTo(URL)).andRespond(withSuccess(respuesta("{\"text\":\"no es json\"}"),
				MediaType.APPLICATION_JSON));

		ErrorDeNegocio error = assertThrows(ErrorDeNegocio.class, () -> proveedor.redactar("alertas", List.of(
				InvocacionHerramienta.exitosa("listarAlertasStock", Map.of(),
						new ResultadoHerramienta("Hay 1 producto en alerta.", List.of())))));
		assertEquals(CodigoError.SERVICIO_NO_DISPONIBLE, error.codigo());
	}

	@Test
	void rechazaModelosPreviewYExigeLaClave() {
		assertThrows(IllegalStateException.class,
				() -> new PropiedadesAgenteGemini("x", "gemini-3-flash-preview", null, null, null));
		assertThrows(IllegalStateException.class,
				() -> new PropiedadesAgenteGemini("", null, null, null, null).exigirClave());
	}

	private static String respuesta(String partes) {
		return "{\"candidates\":[{\"content\":{\"role\":\"model\",\"parts\":[" + partes + "]}}]}";
	}

	private static DefinicionHerramienta herramienta(String nombre, ParametroHerramienta... parametros) {
		return new DefinicionHerramienta(nombre, "Prueba", "GET /prueba", Set.of(RolUsuario.ALMACENERO),
				List.of(parametros), argumentos -> new ResultadoHerramienta("ok", List.of()));
	}
}
