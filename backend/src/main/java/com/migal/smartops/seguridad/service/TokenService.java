package com.migal.smartops.seguridad.service;

import com.migal.smartops.seguridad.config.PropiedadesJwt;
import com.migal.smartops.seguridad.model.Usuario;
import java.time.Clock;
import java.time.Instant;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/**
 * Emite los JWT de acceso. El token contiene el identificador del usuario ({@code sub}), su nombre de
 * usuario y su rol en minúsculas. No hay renovación: al expirar, el usuario inicia sesión de nuevo (ADR-009).
 */
@Service
public class TokenService {

	private final JwtEncoder codificador;
	private final PropiedadesJwt propiedades;
	private final Clock reloj;

	public TokenService(JwtEncoder codificador, PropiedadesJwt propiedades, Clock reloj) {
		this.codificador = codificador;
		this.propiedades = propiedades;
		this.reloj = reloj;
	}

	/** Token emitido y su vigencia en segundos. */
	public record Token(String valor, long expiraEn) {
	}

	public Token emitir(Usuario usuario) {
		Instant ahora = reloj.instant();
		Instant expira = ahora.plus(propiedades.duracion());
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.subject(String.valueOf(usuario.getId()))
				.issuedAt(ahora)
				.expiresAt(expira)
				.claim("username", usuario.getUsername())
				.claim("rol", usuario.getRol().valor())
				.build();
		JwsHeader cabecera = JwsHeader.with(MacAlgorithm.HS256).build();
		String valor = codificador.encode(JwtEncoderParameters.from(cabecera, claims)).getTokenValue();
		return new Token(valor, propiedades.duracion().toSeconds());
	}
}
