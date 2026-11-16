package com.migal.smartops.comun.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Habilita las tareas programadas del backend: la limpieza diaria de la tabla de idempotencia (ADR-010) y, con el
 * proveedor de OCR {@code http}, la vigilancia de documentos pendientes (ADR-012).
 */
@Configuration
@EnableScheduling
public class ConfiguracionTareasProgramadas {
}
