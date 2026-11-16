package com.migal.smartops.documentosocr.config;

import com.migal.smartops.documentosocr.service.DocumentoOcrService;
import com.migal.smartops.documentosocr.service.EnvioOcrHttp;
import com.migal.smartops.documentosocr.service.ProveedorOcr;
import com.migal.smartops.documentosocr.service.ProveedorOcrHttp;
import com.migal.smartops.documentosocr.service.ProveedorOcrNoConfigurado;
import com.migal.smartops.documentosocr.service.ProveedorOcrSimulado;
import com.migal.smartops.documentosocr.service.VigilanciaOcr;
import com.migal.smartops.seguridad.service.TokenService;
import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Selecciona la implementación de {@link ProveedorOcr} según la propiedad {@code migal.ocr.proveedor}: simulado,
 * no-configurado o http. Con http registra además el envío al servicio de OCR y la vigilancia de pendientes (ADR-012).
 * Con cualquier otro valor el contexto no arranca.
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

	@Bean
	@ConditionalOnProperty(prefix = "migal.ocr", name = "proveedor", havingValue = PropiedadesOcr.PROVEEDOR_HTTP)
	ProveedorOcr proveedorOcrHttp(ApplicationEventPublisher eventos) {
		return new ProveedorOcrHttp(eventos);
	}

	@Bean
	@ConditionalOnProperty(prefix = "migal.ocr", name = "proveedor", havingValue = PropiedadesOcr.PROVEEDOR_HTTP)
	EnvioOcrHttp envioOcrHttp(PropiedadesOcr propiedades, TokenService tokens, DocumentoOcrService documentos) {
		return new EnvioOcrHttp(EnvioOcrHttp.cliente(propiedades.servicio()), propiedades, tokens, documentos);
	}

	@Bean
	@ConditionalOnProperty(prefix = "migal.ocr", name = "proveedor", havingValue = PropiedadesOcr.PROVEEDOR_HTTP)
	VigilanciaOcr vigilanciaOcr(DocumentoOcrService documentos, PropiedadesOcr propiedades, Clock reloj) {
		return new VigilanciaOcr(documentos, propiedades, reloj);
	}
}
