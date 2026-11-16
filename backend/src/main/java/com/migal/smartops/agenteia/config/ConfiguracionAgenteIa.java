package com.migal.smartops.agenteia.config;

import com.migal.smartops.agenteia.service.ProveedorLlm;
import com.migal.smartops.agenteia.service.ProveedorLlmGemini;
import com.migal.smartops.agenteia.service.ProveedorLlmNoConfigurado;
import com.migal.smartops.agenteia.service.ProveedorLlmSimulado;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Selecciona la implementación de {@link ProveedorLlm} según la propiedad {@code migal.agente.proveedor}. El valor por
 * defecto es {@code simulado}; {@code gemini} usa la API de Gemini con la clave de GEMINI_API_KEY (ADR-012).
 */
@Configuration
@EnableConfigurationProperties(PropiedadesAgenteGemini.class)
public class ConfiguracionAgenteIa {

	@Bean
	@ConditionalOnProperty(prefix = "migal.agente", name = "proveedor", havingValue = "simulado", matchIfMissing = true)
	ProveedorLlm proveedorLlmSimulado() {
		return new ProveedorLlmSimulado();
	}

	@Bean
	@ConditionalOnProperty(prefix = "migal.agente", name = "proveedor", havingValue = "no-configurado")
	ProveedorLlm proveedorLlmNoConfigurado() {
		return new ProveedorLlmNoConfigurado();
	}

	/** Proveedor real sobre la API de Gemini (ADR-012). Sin GEMINI_API_KEY el contexto no arranca. */
	@Bean
	@ConditionalOnProperty(prefix = "migal.agente", name = "proveedor", havingValue = "gemini")
	ProveedorLlm proveedorLlmGemini(PropiedadesAgenteGemini propiedades) {
		PropiedadesAgenteGemini conClave = propiedades.exigirClave();
		return new ProveedorLlmGemini(ProveedorLlmGemini.cliente(conClave), conClave);
	}
}
