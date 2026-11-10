package com.migal.smartops.seguridad.config;

import com.migal.smartops.comun.errores.CodigoError;
import com.migal.smartops.comun.web.RespuestaProblema;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import tools.jackson.databind.ObjectMapper;

/**
 * Cadena de seguridad HTTP: API sin estado protegida con JWT. Solo el inicio de sesión y la comprobación de
 * salud son públicos. Los fallos de autenticación y de autorización responden {@code application/problem+json}
 * con los códigos {@code NO_AUTENTICADO} y {@code PERMISO_INSUFICIENTE}.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SeguridadWeb {

	@Bean
	SecurityFilterChain cadenaDeSeguridad(HttpSecurity http, JwtDecoder decodificador,
			ConversorDeAutenticacion conversor, ObjectMapper mapeador) throws Exception {
		AuthenticationEntryPoint noAutenticado = (solicitud, respuesta, e) -> RespuestaProblema.escribir(respuesta,
				mapeador, CodigoError.NO_AUTENTICADO, "Falta el token de acceso o no es válido.",
				solicitud.getRequestURI());
		AccessDeniedHandler denegado = (solicitud, respuesta, e) -> RespuestaProblema.escribir(respuesta, mapeador,
				CodigoError.PERMISO_INSUFICIENTE, "El rol del usuario no permite esta operación.",
				solicitud.getRequestURI());

		http.csrf(csrf -> csrf.disable())
				.sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(acceso -> acceso
						.requestMatchers(HttpMethod.POST, "/api/v1/sesiones").permitAll()
						.requestMatchers("/actuator/health/**").permitAll()
						.anyRequest().authenticated())
				.oauth2ResourceServer(oauth -> oauth
						.jwt(jwt -> jwt.decoder(decodificador).jwtAuthenticationConverter(conversor))
						.authenticationEntryPoint(noAutenticado)
						.accessDeniedHandler(denegado))
				.exceptionHandling(e -> e.authenticationEntryPoint(noAutenticado).accessDeniedHandler(denegado));
		return http.build();
	}
}
