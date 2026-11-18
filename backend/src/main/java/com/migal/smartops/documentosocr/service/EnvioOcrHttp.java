package com.migal.smartops.documentosocr.service;

import com.migal.smartops.documentosocr.config.PropiedadesOcr;
import com.migal.smartops.documentosocr.service.DocumentoOcrService.ProductoCatalogoOcr;
import com.migal.smartops.seguridad.service.TokenService;
import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * Envía al servicio de OCR los documentos encolados por {@link ProveedorOcrHttp} (ADR-012, puntos 2, 3 y 5). El envío
 * ocurre después de confirmar la transacción de carga o de reprocesamiento y transporta, según el contrato
 * {@code servicio-ocr/api/procesar.yaml}, el archivo, el catálogo activo, el umbral bajo y un token de resultados
 * limitado al documento. El servicio responde 202 y procesa en segundo plano. Si el envío falla, el documento registra
 * SERVICIO_NO_DISPONIBLE y queda disponible para reprocesarse.
 */
public class EnvioOcrHttp {

	private static final Logger REGISTRO = LoggerFactory.getLogger(EnvioOcrHttp.class);
	/** Margen de vigencia del token de resultados sobre el plazo de vigilancia. */
	static final Duration MARGEN_TOKEN = Duration.ofMinutes(5);
	/** Cabecera con el secreto compartido que el servicio de OCR exige si lo tiene configurado. */
	static final String CABECERA_TOKEN_INTERNO = "X-Token-Interno";

	private final RestClient cliente;
	private final PropiedadesOcr propiedades;
	private final TokenService tokens;
	private final DocumentoOcrService documentos;
	private final String tokenInterno;

	public EnvioOcrHttp(RestClient cliente, PropiedadesOcr propiedades, TokenService tokens,
			DocumentoOcrService documentos) {
		this.cliente = cliente;
		this.propiedades = propiedades;
		this.tokens = tokens;
		this.documentos = documentos;
		this.tokenInterno = propiedades.servicio().tokenInterno();
	}

	/** Cliente HTTP del servicio de OCR con los tiempos de espera configurados. */
	public static RestClient cliente(PropiedadesOcr.Servicio servicio) {
		JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(
				HttpClient.newBuilder().connectTimeout(servicio.tiempoConexion()).build());
		fabrica.setReadTimeout(servicio.tiempoLectura());
		return RestClient.builder().baseUrl(servicio.url()).requestFactory(fabrica).build();
	}

	/** Solicitud de procesamiento, con los nombres de SolicitudProcesamiento del contrato del servicio. */
	record SolicitudProcesamiento(String idDocumento, String tipo, String nombreArchivo, BigDecimal umbralBaja,
			String tokenResultados, List<ProductoCatalogoOcr> catalogo) {
	}

	/** Envía la solicitud después de confirmar la transacción que encoló el documento. */
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void enviar(SolicitudOcr solicitud) {
		try {
			var peticion = cliente.post().uri("/procesar").contentType(MediaType.MULTIPART_FORM_DATA);
			if (!tokenInterno.isEmpty()) {
				peticion = peticion.header(CABECERA_TOKEN_INTERNO, tokenInterno);
			}
			peticion.body(cuerpo(solicitud)).retrieve().toBodilessEntity();
		} catch (RuntimeException e) {
			REGISTRO.warn("No fue posible enviar el documento {} al servicio de OCR: {}", solicitud.idDocumento(),
					e.getMessage());
			documentos.registrarServicioNoDisponible(solicitud.idDocumento());
		}
	}

	/** Cuerpo multipart: el archivo tal como se cargó y la solicitud en JSON. */
	MultiValueMap<String, Object> cuerpo(SolicitudOcr solicitud) {
		if (!Files.isReadable(solicitud.ruta())) {
			throw new IllegalStateException("El archivo del documento no está disponible en el almacenamiento.");
		}
		Duration vigencia = propiedades.servicio().plazoResultado().plus(MARGEN_TOKEN);
		SolicitudProcesamiento datos = new SolicitudProcesamiento(String.valueOf(solicitud.idDocumento()),
				solicitud.tipo().valor(), solicitud.nombreArchivo(), propiedades.umbrales().baja(),
				tokens.emitirServicio(solicitud.idDocumento(), vigencia).valor(), documentos.catalogoActivo());
		MultiValueMap<String, Object> partes = new LinkedMultiValueMap<>();
		partes.add("archivo", parte(new FileSystemResource(solicitud.ruta()), tipoDeContenido(solicitud)));
		partes.add("solicitud", parte(datos, MediaType.APPLICATION_JSON));
		return partes;
	}

	private static HttpEntity<Object> parte(Object contenido, MediaType tipo) {
		HttpHeaders cabeceras = new HttpHeaders();
		cabeceras.setContentType(tipo);
		return new HttpEntity<>(contenido, cabeceras);
	}

	private static MediaType tipoDeContenido(SolicitudOcr solicitud) {
		String nombre = solicitud.ruta().getFileName().toString().toLowerCase(Locale.ROOT);
		if (nombre.endsWith(".png")) {
			return MediaType.IMAGE_PNG;
		}
		if (nombre.endsWith(".pdf")) {
			return MediaType.APPLICATION_PDF;
		}
		return MediaType.IMAGE_JPEG;
	}
}
