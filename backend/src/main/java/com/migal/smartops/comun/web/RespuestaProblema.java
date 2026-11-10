package com.migal.smartops.comun.web;

import com.migal.smartops.comun.errores.CodigoError;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import tools.jackson.databind.ObjectMapper;

/**
 * Construye las respuestas de error {@code application/problem+json} (RFC 9457) del contrato, con el campo
 * {@code code} del catálogo y, para {@code VALIDACION_FALLIDA}, la lista {@code errores}.
 */
public final class RespuestaProblema {

	/** Error de un campo de la solicitud. */
	public record ErrorCampo(String campo, String codigo, String mensaje) {
	}

	private RespuestaProblema() {
	}

	public static ProblemDetail problema(CodigoError codigo, String detalle, String instancia, List<ErrorCampo> errores) {
		HttpStatus estado = codigo.estado();
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(estado, detalle);
		problema.setType(URI.create("urn:migal:problema:" + codigo.name().toLowerCase().replace('_', '-')));
		problema.setTitle(titulo(estado));
		problema.setInstance(URI.create(instancia));
		problema.setProperty("code", codigo.name());
		if (errores != null && !errores.isEmpty()) {
			problema.setProperty("errores", errores);
		}
		return problema;
	}

	public static ResponseEntity<ProblemDetail> respuesta(CodigoError codigo, String detalle, String instancia,
			List<ErrorCampo> errores) {
		return ResponseEntity.status(codigo.estado())
				.contentType(MediaType.APPLICATION_PROBLEM_JSON)
				.body(problema(codigo, detalle, instancia, errores));
	}

	/** Escribe el problema directamente en la respuesta, para los filtros de seguridad que no pasan por un controlador. */
	public static void escribir(HttpServletResponse respuesta, ObjectMapper mapeador, CodigoError codigo, String detalle,
			String instancia) throws IOException {
		respuesta.setStatus(codigo.estado().value());
		respuesta.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		respuesta.setCharacterEncoding("UTF-8");
		mapeador.writeValue(respuesta.getOutputStream(), problema(codigo, detalle, instancia, null));
	}

	private static String titulo(HttpStatus estado) {
		return switch (estado) {
			case BAD_REQUEST -> "Solicitud mal formada";
			case UNAUTHORIZED -> "No autenticado";
			case FORBIDDEN -> "Acceso denegado";
			case NOT_FOUND -> "Recurso no encontrado";
			case CONFLICT -> "Conflicto";
			case UNPROCESSABLE_ENTITY -> "Regla de negocio no cumplida";
			case SERVICE_UNAVAILABLE -> "Servicio no disponible";
			default -> "Error interno";
		};
	}
}
