package com.migal.smartops.seguridad.config;

import com.migal.smartops.seguridad.model.Usuario;
import com.migal.smartops.seguridad.repository.UsuarioRepository;
import java.util.List;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Convierte un JWT válido en la autenticación de la solicitud. Verifica en cada solicitud que el usuario
 * exista y siga activo, y toma su rol vigente de la base de datos y no del token (ADR-009).
 */
@Component
public class ConversorDeAutenticacion implements Converter<Jwt, AbstractAuthenticationToken> {

	private final UsuarioRepository usuarios;

	public ConversorDeAutenticacion(UsuarioRepository usuarios) {
		this.usuarios = usuarios;
	}

	@Override
	public AbstractAuthenticationToken convert(Jwt jwt) {
		Usuario usuario = idDelToken(jwt)
				.flatMap(usuarios::findById)
				.filter(Usuario::isEstadoActivo)
				.orElseThrow(() -> new OAuth2AuthenticationException(
						new OAuth2Error("invalid_token", "El usuario no existe o está desactivado.", null)));
		UsuarioAutenticado principal = new UsuarioAutenticado(usuario.getId(), usuario.getUsername(), usuario.getRol());
		return new UsernamePasswordAuthenticationToken(principal, jwt,
				List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().name())));
	}

	private static java.util.Optional<Long> idDelToken(Jwt jwt) {
		try {
			return java.util.Optional.of(Long.parseLong(jwt.getSubject()));
		} catch (NumberFormatException e) {
			return java.util.Optional.empty();
		}
	}
}
