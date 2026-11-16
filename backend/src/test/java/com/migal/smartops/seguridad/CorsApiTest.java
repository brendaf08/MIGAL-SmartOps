package com.migal.smartops.seguridad;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.migal.smartops.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/** Verifica que solo los orígenes de MIGAL_CORS_ORIGENES puedan llamar a la API desde otro dominio. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "migal.cors.origenes=https://app.migal.test, https://otro.migal.test")
@AutoConfigureMockMvc
class CorsApiTest {

	@Autowired
	private MockMvc mvc;

	@Test
	void unOrigenAutorizadoRecibeLaRespuestaPrevia() throws Exception {
		mvc.perform(options("/api/v1/sesiones").header("Origin", "https://otro.migal.test")
						.header("Access-Control-Request-Method", "POST")
						.header("Access-Control-Request-Headers", "Content-Type"))
				.andExpect(status().isOk())
				.andExpect(header().string("Access-Control-Allow-Origin", "https://otro.migal.test"));
	}

	@Test
	void unOrigenNoAutorizadoEsRechazado() throws Exception {
		mvc.perform(options("/api/v1/sesiones").header("Origin", "https://externo.example")
						.header("Access-Control-Request-Method", "POST"))
				.andExpect(status().isForbidden())
				.andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
	}
}
