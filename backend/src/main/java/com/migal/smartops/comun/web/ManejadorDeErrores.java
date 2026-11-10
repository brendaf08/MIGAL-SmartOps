package com.migal.smartops.comun.web;

import com.migal.smartops.comun.errores.CodigoError;
import com.migal.smartops.comun.errores.ErrorDeNegocio;
import com.migal.smartops.comun.web.RespuestaProblema.ErrorCampo;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Convierte las excepciones en respuestas {@code application/problem+json} con el código de error del
 * contrato (ADR-010). Las excepciones de dominio usan el estado HTTP de su código.
 */
@RestControllerAdvice
public class ManejadorDeErrores {

	private static final Logger REGISTRO = LoggerFactory.getLogger(ManejadorDeErrores.class);

	@ExceptionHandler(ErrorDeNegocio.class)
	ResponseEntity<ProblemDetail> negocio(ErrorDeNegocio e, HttpServletRequest solicitud) {
		return RespuestaProblema.respuesta(e.codigo(), e.getMessage(), solicitud.getRequestURI(), null, e.propiedades());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ProblemDetail> cuerpoInvalido(MethodArgumentNotValidException e, HttpServletRequest solicitud) {
		List<ErrorCampo> errores = e.getBindingResult().getFieldErrors().stream()
				.map(f -> new ErrorCampo(f.getField(), f.getCode(), f.getDefaultMessage()))
				.toList();
		return validacion(errores, solicitud);
	}

	@ExceptionHandler(HandlerMethodValidationException.class)
	ResponseEntity<ProblemDetail> parametrosInvalidos(HandlerMethodValidationException e, HttpServletRequest solicitud) {
		List<ErrorCampo> errores = e.getParameterValidationResults().stream()
				.flatMap(r -> r.getResolvableErrors().stream()
						.map(m -> new ErrorCampo(r.getMethodParameter().getParameterName(), m.getCodes() == null
								? "invalido" : m.getCodes()[m.getCodes().length - 1], m.getDefaultMessage())))
				.toList();
		return validacion(errores, solicitud);
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	ResponseEntity<ProblemDetail> tipoIncorrecto(MethodArgumentTypeMismatchException e, HttpServletRequest solicitud) {
		return validacion(List.of(new ErrorCampo(e.getName(), "tipo", "El valor no tiene el formato esperado.")), solicitud);
	}

	/** Falta un parámetro o una parte obligatoria de la solicitud, por ejemplo el archivo de una carga multipart. */
	@ExceptionHandler({MissingServletRequestParameterException.class, MissingServletRequestPartException.class})
	ResponseEntity<ProblemDetail> campoAusente(HttpServletRequest solicitud) {
		return RespuestaProblema.respuesta(CodigoError.SOLICITUD_MAL_FORMADA,
				"Falta un campo obligatorio de la solicitud.", solicitud.getRequestURI(), null);
	}

	/** El archivo de una carga multipart supera el tamaño máximo configurado en el servidor. */
	@ExceptionHandler(MaxUploadSizeExceededException.class)
	ResponseEntity<ProblemDetail> archivoExcesivo(HttpServletRequest solicitud) {
		return validacion(List.of(new ErrorCampo("archivo", "tamano_excedido",
				"El archivo supera el tamaño máximo permitido.")), solicitud);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ResponseEntity<ProblemDetail> cuerpoIlegible(HttpServletRequest solicitud) {
		return RespuestaProblema.respuesta(CodigoError.SOLICITUD_MAL_FORMADA,
				"El cuerpo de la solicitud no es un JSON válido.", solicitud.getRequestURI(), null);
	}

	@ExceptionHandler(NoResourceFoundException.class)
	ResponseEntity<ProblemDetail> rutaInexistente(HttpServletRequest solicitud) {
		return RespuestaProblema.respuesta(CodigoError.RECURSO_NO_ENCONTRADO,
				"La ruta solicitada no existe.", solicitud.getRequestURI(), null);
	}

	@ExceptionHandler(AccessDeniedException.class)
	ResponseEntity<ProblemDetail> accesoDenegado(HttpServletRequest solicitud) {
		return RespuestaProblema.respuesta(CodigoError.PERMISO_INSUFICIENTE,
				"El rol del usuario no permite esta operación.", solicitud.getRequestURI(), null);
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ProblemDetail> inesperado(Exception e, HttpServletRequest solicitud) {
		REGISTRO.error("Error no controlado en {}", solicitud.getRequestURI(), e);
		return RespuestaProblema.respuesta(CodigoError.ERROR_INTERNO,
				"Ocurrió un error interno. Intente nuevamente.", solicitud.getRequestURI(), null);
	}

	private static ResponseEntity<ProblemDetail> validacion(List<ErrorCampo> errores, HttpServletRequest solicitud) {
		return RespuestaProblema.respuesta(CodigoError.VALIDACION_FALLIDA,
				"Uno o más campos no cumplen las reglas.", solicitud.getRequestURI(), errores);
	}
}
