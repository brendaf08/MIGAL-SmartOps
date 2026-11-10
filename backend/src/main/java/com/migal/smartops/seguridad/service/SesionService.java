package com.migal.smartops.seguridad.service;

import com.migal.smartops.comun.errores.CodigoError;
import com.migal.smartops.comun.errores.ErrorDeNegocio;
import com.migal.smartops.seguridad.model.Usuario;
import com.migal.smartops.seguridad.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Inicio de sesión (CU-01): valida las credenciales personales y emite el JWT de acceso. */
@Service
public class SesionService {

	/** Hash válido de una clave inexistente, usado para igualar el tiempo de respuesta cuando el usuario no existe. */
	private static final String CLAVE_FICTICIA = "clave-ficticia-para-igualar-tiempos";

	private final UsuarioRepository usuarios;
	private final PasswordEncoder codificador;
	private final TokenService tokens;
	private final String hashFicticio;

	public SesionService(UsuarioRepository usuarios, PasswordEncoder codificador, TokenService tokens) {
		this.usuarios = usuarios;
		this.codificador = codificador;
		this.tokens = tokens;
		this.hashFicticio = codificador.encode(CLAVE_FICTICIA);
	}

	/** Resultado de un inicio de sesión correcto. */
	public record SesionIniciada(TokenService.Token token, Usuario usuario) {
	}

	@Transactional(readOnly = true)
	public SesionIniciada iniciarSesion(String username, String clave) {
		Usuario usuario = usuarios.findByUsername(username).orElse(null);
		String hash = usuario == null ? hashFicticio : usuario.getPasswordHash();
		boolean claveCorrecta = codificador.matches(clave, hash);
		if (usuario == null || !claveCorrecta) {
			throw new ErrorDeNegocio(CodigoError.NO_AUTENTICADO, "El usuario o la contraseña son incorrectos.");
		}
		if (!usuario.isEstadoActivo()) {
			throw new ErrorDeNegocio(CodigoError.CUENTA_INACTIVA,
					"La cuenta está desactivada. Contacte al Administrador.");
		}
		return new SesionIniciada(tokens.emitir(usuario), usuario);
	}
}
