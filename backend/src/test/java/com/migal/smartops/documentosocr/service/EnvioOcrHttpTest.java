package com.migal.smartops.documentosocr.service;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import com.migal.smartops.documentosocr.DatosOcr;
import com.migal.smartops.documentosocr.config.PropiedadesOcr;
import com.migal.smartops.documentosocr.model.TipoDocumento;
import com.migal.smartops.documentosocr.service.DocumentoOcrService.ProductoCatalogoOcr;
import com.migal.smartops.seguridad.service.TokenService;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.yaml.snakeyaml.Yaml;

/**
 * Verifica el envío de documentos al servicio de OCR (ADR-012): el contenido de la solicitud según el contrato
 * {@code servicio-ocr/api/procesar.yaml} y el registro de SERVICIO_NO_DISPONIBLE cuando el envío falla.
 */
class EnvioOcrHttpTest {

	private static final Path CONTRATO = Path.of("..", "servicio-ocr", "api", "procesar.yaml");

	@TempDir
	Path carpeta;

	private final PropiedadesOcr propiedades = new PropiedadesOcr(PropiedadesOcr.PROVEEDOR_HTTP,
			new PropiedadesOcr.Umbrales(new BigDecimal("0.85"), new BigDecimal("0.60")),
			new PropiedadesOcr.Servicio("http://ocr.local", Duration.ofSeconds(60), null, null, null));
	private final TokenService tokens = mock(TokenService.class);
	private final DocumentoOcrService documentos = mock(DocumentoOcrService.class);
	private MockRestServiceServer servidor;
	private EnvioOcrHttp envio;
	private Path archivo;

	@BeforeEach
	void preparar() throws IOException {
		RestClient.Builder constructor = RestClient.builder().baseUrl("http://ocr.local");
		servidor = MockRestServiceServer.bindTo(constructor).build();
		envio = new EnvioOcrHttp(constructor.build(), propiedades, tokens, documentos);
		archivo = Files.write(carpeta.resolve("3f2a.png"), DatosOcr.PNG);
		when(tokens.emitirServicio(eq(812L), any())).thenReturn(new TokenService.Token("token-812", 360));
		when(documentos.catalogoActivo()).thenReturn(List.of(
				new ProductoCatalogoOcr("CEM-001", "Cemento Sol tipo I", "Sol", "Bolsa 42.5 kg", "bolsa"),
				new ProductoCatalogoOcr("FIE-012", "Fierro corrugado 1/2\"", null, null, "varilla")));
	}

	@Test
	void enviaElArchivoYLaSolicitudConElTokenDelDocumento() {
		servidor.expect(requestTo("http://ocr.local/procesar"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(header("Content-Type", startsWith("multipart/form-data")))
				.andExpect(content().string(allOf(
						containsString("name=\"archivo\""),
						containsString("Content-Type: image/png"),
						containsString("name=\"solicitud\""),
						containsString("\"idDocumento\":\"812\""),
						containsString("\"tipo\":\"solicitud_cliente\""),
						containsString("\"umbralBaja\":0.60"),
						containsString("\"tokenResultados\":\"token-812\""),
						containsString("\"sku\":\"FIE-012\""),
						containsString("\"marca\":null"))))
				.andRespond(withStatus(HttpStatus.ACCEPTED));

		envio.enviar(solicitud(archivo));

		servidor.verify();
		verify(tokens).emitirServicio(812L, Duration.ofSeconds(60).plusMinutes(5));
		verify(documentos, never()).registrarServicioNoDisponible(812L);
	}

	@Test
	void enviaElTokenInternoCuandoEstaConfigurado() {
		PropiedadesOcr conToken = new PropiedadesOcr(PropiedadesOcr.PROVEEDOR_HTTP, propiedades.umbrales(),
				new PropiedadesOcr.Servicio("http://ocr.local", Duration.ofSeconds(60), null, null, null, "secreto"));
		RestClient.Builder constructor = RestClient.builder().baseUrl("http://ocr.local");
		MockRestServiceServer otro = MockRestServiceServer.bindTo(constructor).build();
		otro.expect(requestTo("http://ocr.local/procesar"))
				.andExpect(header("X-Token-Interno", "secreto"))
				.andRespond(withStatus(HttpStatus.ACCEPTED));

		new EnvioOcrHttp(constructor.build(), conToken, tokens, documentos).enviar(solicitud(archivo));

		otro.verify();
	}

	@Test
	void unaRespuestaDeErrorRegistraServicioNoDisponible() {
		servidor.expect(requestTo("http://ocr.local/procesar")).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

		envio.enviar(solicitud(archivo));

		verify(documentos).registrarServicioNoDisponible(812L);
	}

	@Test
	void unArchivoAusenteRegistraServicioNoDisponibleSinLlamarAlServicio() {
		envio.enviar(solicitud(carpeta.resolve("inexistente.png")));

		servidor.verify();
		verify(documentos).registrarServicioNoDisponible(812L);
	}

	@Test
	@SuppressWarnings("unchecked")
	void laSolicitudUsaLosCamposDelContratoDelServicio() throws IOException {
		Map<String, Object> esquemas;
		try (InputStream entrada = Files.newInputStream(CONTRATO)) {
			Map<String, Object> contrato = new Yaml().load(entrada);
			esquemas = (Map<String, Object>) ((Map<String, Object>) contrato.get("components")).get("schemas");
		}
		HttpEntity<?> parte = (HttpEntity<?>) envio.cuerpo(solicitud(archivo)).getFirst("solicitud");

		assertCampos((Map<String, Object>) esquemas.get("SolicitudProcesamiento"), parte.getBody().getClass());
		assertCampos((Map<String, Object>) esquemas.get("ProductoCatalogo"), ProductoCatalogoOcr.class);
	}

	@SuppressWarnings("unchecked")
	private static void assertCampos(Map<String, Object> esquema, Class<?> registro) {
		Set<String> campos = new TreeSet<>(Arrays.stream(registro.getRecordComponents())
				.map(RecordComponent::getName).toList());
		assertEquals(new TreeSet<>(((Map<String, Object>) esquema.get("properties")).keySet()), campos);
		assertEquals(new TreeSet<>((List<String>) esquema.get("required")), campos);
	}

	private static SolicitudOcr solicitud(Path ruta) {
		return new SolicitudOcr(812L, "nota-pedido.png", ruta, TipoDocumento.SOLICITUD_CLIENTE);
	}
}
