package com.migal.smartops.agenteia.config;

import com.migal.smartops.agenteia.service.ProveedorLlm;
import com.migal.smartops.agenteia.service.ProveedorLlmNoConfigurado;
import com.migal.smartops.agenteia.service.ProveedorLlmSimulado;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Selecciona la implementación de {@link ProveedorLlm} según la propiedad {@code migal.agente.proveedor}. El valor por
 * defecto es {@code simulado}. Ningún proveedor real está definido: no hay credenciales en el repositorio.
 */
@Configuration
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
}
