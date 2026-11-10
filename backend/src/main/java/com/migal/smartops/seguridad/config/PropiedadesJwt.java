package com.migal.smartops.seguridad.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Parámetros del JWT de acceso. El secreto se lee de la variable de entorno {@code MIGAL_JWT_SECRETO}
 * y nunca se almacena en el repositorio. La duración por defecto es de 8 horas (ADR-009).
 *
 * @param secreto  clave HS256, de al menos 32 caracteres
 * @param duracion vigencia del token
 */
@ConfigurationProperties(prefix = "migal.seguridad.jwt")
public record PropiedadesJwt(String secreto, Duration duracion) {

	public PropiedadesJwt {
		if (secreto == null || secreto.length() < 32) {
			throw new IllegalStateException(
					"Debe definirse migal.seguridad.jwt.secreto (variable MIGAL_JWT_SECRETO) con al menos 32 caracteres");
		}
		if (duracion == null) {
			duracion = Duration.ofHours(8);
		}
	}
}
