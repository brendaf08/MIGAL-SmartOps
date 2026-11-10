package com.migal.smartops.contrato;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.model.SimpleResponse;
import com.atlassian.oai.validator.report.ValidationReport;
import java.io.File;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Comprueba que cada documento de módulo del contrato se carga y que el validador distingue una
 * interacción válida de una inválida.
 */
class ValidadorContratoTest {

	static Stream<String> modulos() {
		File[] archivos = new File("api/modulos").listFiles((dir, nombre) -> nombre.endsWith(".yaml"));
		return Stream.of(archivos).map(a -> a.getName().replace(".yaml", "")).sorted();
	}

	@ParameterizedTest
	@MethodSource("modulos")
	void cargaElDocumentoDeCadaModulo(String modulo) {
		OpenApiInteractionValidator validador = ValidadorContrato.paraModulo(modulo);
		assertTrue(validador != null);
	}

	@Test
	void aceptaUnaSolicitudDeInicioDeSesionValida() {
		OpenApiInteractionValidator validador = ValidadorContrato.paraModulo("autenticacion-usuarios");
		Request solicitud = SimpleRequest.Builder.post("/api/v1/sesiones")
				.withContentType("application/json")
				.withBody("{\"username\":\"rquispe\",\"clave\":\"Clave12345\"}")
				.build();
		ValidationReport informe = validador.validateRequest(solicitud);
		assertFalse(informe.hasErrors(), informe.getMessages().toString());
	}

	@Test
	void rechazaUnaRespuestaQueNoCumpleElContrato() {
		OpenApiInteractionValidator validador = ValidadorContrato.paraModulo("autenticacion-usuarios");
		SimpleResponse respuesta = SimpleResponse.Builder.status(200)
				.withContentType("application/json")
				.withBody("{\"token\":\"x\"}")
				.build();
		ValidationReport informe = validador.validateResponse("/api/v1/sesiones", Request.Method.POST, respuesta);
		assertTrue(informe.hasErrors());
	}
}
