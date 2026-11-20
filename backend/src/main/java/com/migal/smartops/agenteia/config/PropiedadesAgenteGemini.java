package com.migal.smartops.agenteia.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Conexión del agente con la API de Gemini (ADR-012, puntos 1, 7 y 9). Solo se usa con
 * {@code migal.agente.proveedor=gemini}. La clave se define únicamente por variable de entorno.
 *
 * @param apiKey         clave de la API de Gemini, de un proyecto con facturación habilitada
 * @param modelo         identificadores de modelos Gemini estables separados por coma, por orden de preferencia: si
 * el primero agota su cuota o no responde, se usa el siguiente; no se admiten versiones preview ni alias latest
 * @param urlBase        dirección base de la API
 * @param tiempoConexion tiempo máximo para conectar con la API
 * @param tiempoLectura  tiempo máximo de cada respuesta del modelo
 */
@ConfigurationProperties(prefix = "migal.agente.gemini")
public record PropiedadesAgenteGemini(String apiKey, String modelo, String urlBase, Duration tiempoConexion,
		Duration tiempoLectura) {

	/** Modelos por defecto, por orden: dos Flash-Lite (cuota diaria amplia) y, como respaldo, Flash. */
	public static final String MODELO_POR_DEFECTO = "gemini-3.5-flash-lite,gemini-3.1-flash-lite,gemini-3.6-flash";

	public PropiedadesAgenteGemini {
		apiKey = apiKey == null ? "" : apiKey.trim();
		modelo = modelo == null || modelo.isBlank() ? MODELO_POR_DEFECTO : modelo.trim();
		urlBase = urlBase == null || urlBase.isBlank() ? "https://generativelanguage.googleapis.com" : urlBase.trim();
		tiempoConexion = tiempoConexion == null ? Duration.ofSeconds(3) : tiempoConexion;
		tiempoLectura = tiempoLectura == null ? Duration.ofSeconds(10) : tiempoLectura;
		for (String m : modelo.split(",")) {
			if (m.isBlank() || m.contains("preview") || m.contains("-exp") || m.endsWith("latest")) {
				throw new IllegalStateException("migal.agente.gemini.modelo debe listar modelos estables");
			}
		}
	}

	/** Modelos en orden de preferencia. */
	public java.util.List<String> modelos() {
		return java.util.Arrays.stream(modelo.split(",")).map(String::trim).toList();
	}

	/** Exige la clave antes de crear el proveedor Gemini. */
	public PropiedadesAgenteGemini exigirClave() {
		if (apiKey.isEmpty()) {
			throw new IllegalStateException("El proveedor gemini exige la variable de entorno GEMINI_API_KEY");
		}
		return this;
	}
}
