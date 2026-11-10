package com.migal.smartops.documentosocr.config;

import com.migal.smartops.documentosocr.service.ProveedorOcr;
import com.migal.smartops.documentosocr.service.ProveedorOcrNoConfigurado;
import com.migal.smartops.documentosocr.service.ProveedorOcrSimulado;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Selecciona la implementación de {@link ProveedorOcr} según la propiedad {@code migal.ocr.proveedor}. Si la propiedad
 * no es {@code simulado} ni {@code no-configurado}, el contexto no arranca: ningún proveedor real está definido todavía.
 */
@Configuration
@EnableConfigurationProperties({PropiedadesOcr.class, PropiedadesDocumentos.class})
public class ConfiguracionDocumentosOcr {

	@Bean
	@ConditionalOnProperty(prefix = "migal.ocr", name = "proveedor", havingValue = PropiedadesOcr.PROVEEDOR_SIMULADO,
			matchIfMissing = true)
	ProveedorOcr proveedorOcrSimulado(ApplicationEventPublisher eventos) {
		return new ProveedorOcrSimulado(eventos);
	}

	@Bean
	@ConditionalOnProperty(prefix = "migal.ocr", name = "proveedor", havingValue = PropiedadesOcr.PROVEEDOR_NO_CONFIGURADO)
	ProveedorOcr proveedorOcrNoConfigurado() {
		return new ProveedorOcrNoConfigurado();
	}
}
