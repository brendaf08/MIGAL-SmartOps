package com.migal.smartops.contrato;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.mockmvc.OpenApiValidationMatchers;
import com.atlassian.oai.validator.report.LevelResolver;
import com.atlassian.oai.validator.report.ValidationReport;
import java.io.File;
import org.springframework.test.web.servlet.ResultMatcher;

/**
 * Verifica las solicitudes y respuestas de las pruebas de los controladores contra el documento
 * OpenAPI de cada módulo (ADR-010). Los documentos se leen de {@code backend/api/modulos/}, relativos a la carpeta {@code backend/} desde la que Maven ejecuta las pruebas.
 */
public final class ValidadorContrato {

	private static final String RUTA_MODULOS = "api/modulos/";

	private ValidadorContrato() {
	}

	/**
	 * Crea un validador para el documento del módulo indicado, por ejemplo {@code "autenticacion-usuarios"}.
	 *
	 * <p>El validador trata como cerrado cada esquema que forma parte de un {@code allOf} (los listados paginados
	 * amplían el sobre común con {@code allOf}), por lo que rechazaría las propiedades del sobre. Se ignora
	 * únicamente la regla de propiedades adicionales de las respuestas; el resto de las reglas, incluidas las de
	 * las solicitudes, se mantienen.
	 */
	public static OpenApiInteractionValidator paraModulo(String modulo) {
		LevelResolver niveles = LevelResolver.create()
				.withLevel("validation.response.body.schema.additionalProperties", ValidationReport.Level.IGNORE)
				.build();
		return constructor(modulo).withLevelResolver(niveles).build();
	}

	/**
	 * Igual que {@link #paraModulo(String)}, pero sin validar la solicitud. Se usa en las pruebas que envían a
	 * propósito una solicitud inválida para comprobar que la respuesta de error cumple el contrato.
	 */
	public static OpenApiInteractionValidator soloRespuesta(String modulo) {
		LevelResolver niveles = LevelResolver.create()
				.withLevel("validation.response.body.schema.additionalProperties", ValidationReport.Level.IGNORE)
				.withLevel("validation.request", ValidationReport.Level.IGNORE)
				.build();
		return constructor(modulo).withLevelResolver(niveles).build();
	}

	private static OpenApiInteractionValidator.Builder constructor(String modulo) {
		return OpenApiInteractionValidator
				.createForSpecificationUrl(new File(RUTA_MODULOS + modulo + ".yaml").toURI().toString())
				.withBasePathOverride("/api/v1");
	}

	/** Comprobación de MockMvc: la solicitud y la respuesta deben cumplir el contrato del módulo. */
	public static ResultMatcher cumple(OpenApiInteractionValidator validador) {
		return OpenApiValidationMatchers.openApi().isValid(validador);
	}
}
