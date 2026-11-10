package com.migal.smartops.contrato;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.mockmvc.OpenApiValidationMatchers;
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

	/** Crea un validador para el documento del módulo indicado, por ejemplo {@code "autenticacion-usuarios"}. */
	public static OpenApiInteractionValidator paraModulo(String modulo) {
		return OpenApiInteractionValidator.createForSpecificationUrl(new File(RUTA_MODULOS + modulo + ".yaml").toURI().toString())
				.withBasePathOverride("/api/v1")
				.build();
	}

	/** Comprobación de MockMvc: la solicitud y la respuesta deben cumplir el contrato del módulo. */
	public static ResultMatcher cumple(OpenApiInteractionValidator validador) {
		return OpenApiValidationMatchers.openApi().isValid(validador);
	}
}
