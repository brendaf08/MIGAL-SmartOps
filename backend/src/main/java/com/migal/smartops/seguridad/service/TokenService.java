package com.migal.smartops.seguridad.service;

import com.migal.smartops.seguridad.config.PropiedadesJwt;
import com.migal.smartops.seguridad.model.Usuario;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/**
 * Emite los JWT de acceso. El token contiene el identificador del usuario ({@code sub}), su nombre de
 * usuario y su rol en minúsculas. No hay renovación: al expirar, el usuario inicia sesión de nuevo (ADR-009). También
 * emite los tokens de servicio con los que el servicio de OCR registra resultados (ADR-012).
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

	/** Sujeto de los tokens que el backend emite para el servicio de OCR. */
	public static final String SUJETO_SERVICIO_OCR = "servicio-ocr";
	/** Alcance que permite registrar el resultado del OCR de un documento. */
	public static final String ALCANCE_RESULTADOS_OCR = "ocr:resultados:escribir";
	/** Reclamación con el documento al que se limita un token de resultados del OCR (ADR-012, punto 5). */
	public static final String RECLAMACION_ID_DOCUMENTO = "idDocumento";

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
		return new Token(firmar(claims), propiedades.duracion().toSeconds());
	}

	/**
	 * Emite el token de servicio con el que el servicio de OCR registra el resultado de un documento (ADR-012, punto 5).
	 * No representa a un usuario: lleva {@code tipo: servicio}, el alcance {@code ocr:resultados:escribir} y el
	 * identificador del documento, de modo que solo sirve para ese documento y durante la vigencia indicada. El servicio
	 * de OCR lo usa sin modificarlo y no conoce la clave de firma.
	 *
	 * @param idDocumento documento cuyo resultado autoriza a registrar
	 * @param vigencia    tiempo durante el cual el token es válido; debe ser positivo
	 */
	public Token emitirServicio(long idDocumento, Duration vigencia) {
		if (vigencia == null || vigencia.isNegative() || vigencia.isZero()) {
			throw new IllegalArgumentException("La vigencia del token de servicio debe ser positiva.");
		}
		Instant ahora = reloj.instant();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.subject(SUJETO_SERVICIO_OCR)
				.issuedAt(ahora)
				.expiresAt(ahora.plus(vigencia))
				.claim("tipo", "servicio")
				.claim("alcances", List.of(ALCANCE_RESULTADOS_OCR))
				.claim(RECLAMACION_ID_DOCUMENTO, idDocumento)
				.build();
		return new Token(firmar(claims), vigencia.toSeconds());
	}

	private String firmar(JwtClaimsSet claims) {
		JwsHeader cabecera = JwsHeader.with(MacAlgorithm.HS256).build();
		return codificador.encode(JwtEncoderParameters.from(cabecera, claims)).getTokenValue();
	}
}
