package com.migal.smartops.seguridad.config;

import com.migal.smartops.seguridad.model.RolUsuario;
import com.migal.smartops.seguridad.model.Usuario;
import com.migal.smartops.seguridad.repository.UsuarioRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Convierte un JWT válido en la autenticación de la solicitud. Verifica en cada solicitud que el usuario
 * exista y siga activo, y toma su rol vigente de la base de datos y no del token (ADR-009).
 */
@Component
public class ConversorDeAutenticacion implements Converter<Jwt, AbstractAuthenticationToken> {

	/** Reclamación que distingue un token de servicio de un token de acceso de usuario. */
	private static final String RECLAMACION_TIPO = "tipo";
	private static final String TIPO_SERVICIO = "servicio";
	/** Reclamación con los alcances del token de servicio, por ejemplo {@code ocr:resultados:escribir}. */
	private static final String RECLAMACION_ALCANCES = "alcances";

	private final UsuarioRepository usuarios;
	private final boolean demostracionHabilitada;

	public ConversorDeAutenticacion(UsuarioRepository usuarios,
			@Value("${migal.seguridad.demostracion.habilitada:false}") boolean demostracionHabilitada) {
		this.usuarios = usuarios;
		this.demostracionHabilitada = demostracionHabilitada;
	}

	@Override
	public AbstractAuthenticationToken convert(Jwt jwt) {
		if (esTokenDeServicio(jwt)) {
			return convertirServicio(jwt);
		}
		Usuario usuario = idDelToken(jwt)
				.flatMap(usuarios::findById)
				.filter(Usuario::isEstadoActivo)
				.orElseThrow(() -> new OAuth2AuthenticationException(
						new OAuth2Error("invalid_token", "El usuario no existe o está desactivado.", null)));
		if (usuario.getRol() == RolUsuario.DEMOSTRACION && !demostracionHabilitada) {
			throw new OAuth2AuthenticationException(
					new OAuth2Error("invalid_token", "El rol de demostración está deshabilitado.", null));
		}
		UsuarioAutenticado principal = new UsuarioAutenticado(usuario.getId(), usuario.getUsername(), usuario.getRol());
		return new UsernamePasswordAuthenticationToken(principal, jwt, autoridades(usuario.getRol()));
	}

	/**
	 * Autoridades del rol. El de demostración recibe las de los cuatro roles del negocio, de modo que cada
	 * {@code @PreAuthorize} existente lo admite sin enumerarlo (ADR-013).
	 */
	public static List<SimpleGrantedAuthority> autoridades(RolUsuario rol) {
		if (rol != RolUsuario.DEMOSTRACION) {
			return List.of(new SimpleGrantedAuthority("ROLE_" + rol.name()));
		}
		List<SimpleGrantedAuthority> autoridades = new ArrayList<>();
		autoridades.add(new SimpleGrantedAuthority("ROLE_" + RolUsuario.DEMOSTRACION.name()));
		RolUsuario.ROLES_DEL_NEGOCIO.forEach(r -> autoridades.add(new SimpleGrantedAuthority("ROLE_" + r.name())));
		return autoridades;
	}

	/**
	 * Un token de servicio lleva la reclamación {@code tipo} con el valor {@code servicio} y la lista {@code alcances}. No
	 * representa a un usuario ni a un rol (ADR-008): sus alcances se convierten en autoridades {@code SCOPE_}.
	 */
	private static boolean esTokenDeServicio(Jwt jwt) {
		return TIPO_SERVICIO.equals(jwt.getClaimAsString(RECLAMACION_TIPO));
	}

	private static AbstractAuthenticationToken convertirServicio(Jwt jwt) {
		List<SimpleGrantedAuthority> autoridades = jwt.getClaimAsStringList(RECLAMACION_ALCANCES) == null
				? List.of()
				: jwt.getClaimAsStringList(RECLAMACION_ALCANCES).stream()
						.map(alcance -> new SimpleGrantedAuthority("SCOPE_" + alcance)).toList();
		// JwtAuthenticationToken conserva el token tras la autenticación; las operaciones de servicio leen sus
		// reclamaciones, por ejemplo idDocumento (ADR-012).
		return new JwtAuthenticationToken(jwt, autoridades, jwt.getSubject());
	}

	private static java.util.Optional<Long> idDelToken(Jwt jwt) {
		try {
			return java.util.Optional.of(Long.parseLong(jwt.getSubject()));
		} catch (NumberFormatException e) {
			return java.util.Optional.empty();
		}
	}
}
