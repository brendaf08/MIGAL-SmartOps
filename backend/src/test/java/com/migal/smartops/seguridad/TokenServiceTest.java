package com.migal.smartops.seguridad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import java.util.List;
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
	// El decodificador valida la vigencia con el reloj real, por lo que el instante de emisión es la hora actual.
	private static final Instant AHORA = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);

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
	void emiteElTokenDeResultadosDelOcrParaUnDocumento() {
		TokenService.Token token = servicio.emitirServicio(812L, Duration.ofMinutes(6));
		Jwt jwt = configuracion.decodificadorJwt(clave).decode(token.valor());

		assertEquals("servicio-ocr", jwt.getSubject());
		assertEquals("servicio", jwt.getClaimAsString("tipo"));
		assertEquals(List.of("ocr:resultados:escribir"), jwt.getClaimAsStringList("alcances"));
		assertEquals(812L, ((Number) jwt.getClaim("idDocumento")).longValue());
		assertNull(jwt.getClaim("rol"));
		assertEquals(360L, token.expiraEn());
		assertEquals(AHORA.plus(Duration.ofMinutes(6)), jwt.getExpiresAt());
	}

	@Test
	void exigeUnaVigenciaPositivaParaElTokenDeServicio() {
		assertThrows(IllegalArgumentException.class, () -> servicio.emitirServicio(1L, Duration.ZERO));
		assertThrows(IllegalArgumentException.class, () -> servicio.emitirServicio(1L, Duration.ofSeconds(-1)));
		assertThrows(IllegalArgumentException.class, () -> servicio.emitirServicio(1L, null));
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
