package com.migal.smartops.seguridad.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/** Componentes de seguridad compartidos: reloj, codificador de contraseñas y firma del JWT. */
@Configuration
@EnableConfigurationProperties(PropiedadesJwt.class)
public class SeguridadConfig {

	@Bean
	Clock reloj() {
		return Clock.systemUTC();
	}

	@Bean
	PasswordEncoder codificadorDeClaves() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public SecretKey claveJwt(PropiedadesJwt propiedades) {
		return new SecretKeySpec(propiedades.secreto().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
	}

	@Bean
	public JwtEncoder codificadorJwt(SecretKey claveJwt) {
		return new NimbusJwtEncoder(new ImmutableSecret<>(claveJwt));
	}

	@Bean
	public JwtDecoder decodificadorJwt(SecretKey claveJwt) {
		return NimbusJwtDecoder.withSecretKey(claveJwt).macAlgorithm(MacAlgorithm.HS256).build();
	}
}
