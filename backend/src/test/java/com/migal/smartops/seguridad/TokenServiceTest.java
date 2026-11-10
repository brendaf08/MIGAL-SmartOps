package com.migal.smartops.seguridad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.migal.smartops.seguridad.config.PropiedadesJwt;
import com.migal.smartops.seguridad.config.SeguridadConfig;
import com.migal.smartops.seguridad.model.RolUsuario;
import com.migal.smartops.seguridad.model.Usuario;
import com.migal.smartops.seguridad.service.TokenService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.util.ReflectionTestUtils;

/** Verifica el contenido, la firma y la vigencia de los JWT emitidos. */
class TokenServiceTest {

	private static final String SECRETO = "secreto-ficticio-de-pruebas-0123456789-abcdef";
	private static final Instant AHORA = Instant.parse("2026-11-10T08:00:00Z");

	private final PropiedadesJwt propiedades = new PropiedadesJwt(SECRETO, Duration.ofHours(8));
	private final SeguridadConfig configuracion = new SeguridadConfig();
	private final SecretKey clave = configuracion.claveJwt(propiedades);
	private final TokenService servicio = new TokenService(
			configuracion.codificadorJwt(clave), propiedades, Clock.fixed(AHORA, ZoneOffset.UTC));

	private Usuario usuario() {
		Usuario usuario = new Usuario("Rosa", "Quispe", "rquispe", "{hash}", RolUsuario.ALMACENERO);
		ReflectionTestUtils.setField(usuario, "id", 42L);
		return usuario;
	}

	@Test
	void incluyeElUsuarioElRolYLaVigenciaDeOchoHoras() {
		TokenService.Token token = servicio.emitir(usuario());
		Jwt jwt = configuracion.decodificadorJwt(clave).decode(token.valor());

		assertEquals("42", jwt.getSubject());
		assertEquals("rquispe", jwt.getClaimAsString("username"));
		assertEquals("almacenero", jwt.getClaimAsString("rol"));
		assertEquals(28800L, token.expiraEn());
		assertEquals(AHORA, jwt.getIssuedAt());
		assertEquals(AHORA.plus(Duration.ofHours(8)), jwt.getExpiresAt());
	}

	@Test
	void rechazaUnTokenFirmadoConOtraClave() {
		String token = servicio.emitir(usuario()).valor();
		SecretKey otra = configuracion.claveJwt(new PropiedadesJwt("otra-clave-ficticia-0123456789-abcdefghij", null));
		JwtDecoder decodificador = NimbusJwtDecoder.withSecretKey(otra).build();

		assertThrows(JwtException.class, () -> decodificador.decode(token));
	}

	@Test
	void usaOchoHorasCuandoNoSeIndicaLaDuracion() {
		assertEquals(Duration.ofHours(8), new PropiedadesJwt(SECRETO, null).duracion());
	}

	@Test
	void exigeUnSecretoDeAlMenosTreintaYDosCaracteres() {
		assertThrows(IllegalStateException.class, () -> new PropiedadesJwt("corto", null));
		assertThrows(IllegalStateException.class, () -> new PropiedadesJwt("", null));
	}
}
