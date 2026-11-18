package com.migal.smartops.seguridad.service;

import com.migal.smartops.comun.errores.CodigoError;
import com.migal.smartops.comun.errores.ErrorDeNegocio;
import com.migal.smartops.seguridad.model.RolUsuario;
import com.migal.smartops.seguridad.model.Usuario;
import com.migal.smartops.seguridad.repository.UsuarioRepository;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
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
	private final LimitadorIntentosSesion limitador;
	private final Clock reloj;
	private final boolean demostracionHabilitada;
	private final String hashFicticio;

	public SesionService(UsuarioRepository usuarios, PasswordEncoder codificador, TokenService tokens,
			LimitadorIntentosSesion limitador, Clock reloj,
			@Value("${migal.seguridad.demostracion.habilitada:false}") boolean demostracionHabilitada) {
		this.demostracionHabilitada = demostracionHabilitada;
		this.usuarios = usuarios;
		this.codificador = codificador;
		this.tokens = tokens;
		this.limitador = limitador;
		this.reloj = reloj;
		this.hashFicticio = codificador.encode(CLAVE_FICTICIA);
	}

	/** Resultado de un inicio de sesión correcto. */
	public record SesionIniciada(TokenService.Token token, Usuario usuario) {
	}

	/**
	 * Inicia sesión. {@code origen} identifica de dónde llega la solicitud (dirección remota) para limitar los intentos
	 * fallidos; tras cinco fallos en 15 minutos responde DEMASIADOS_INTENTOS.
	 */
	@Transactional(readOnly = true)
	public SesionIniciada iniciarSesion(String username, String clave, String origen) {
		limitador.verificar(username, origen);
		Usuario usuario = usuarios.findByUsername(username).orElse(null);
		String hash = usuario == null ? hashFicticio : usuario.getPasswordHash();
		boolean claveCorrecta = codificador.matches(clave, hash);
		if (usuario == null || !claveCorrecta) {
			limitador.registrarFallo(username, origen);
			throw new ErrorDeNegocio(CodigoError.NO_AUTENTICADO, "El usuario o la contraseña son incorrectos.");
		}
		limitador.registrarExito(username, origen);
		if (!usuario.isEstadoActivo()) {
			throw new ErrorDeNegocio(CodigoError.CUENTA_INACTIVA,
					"La cuenta está desactivada. Contacte al Administrador.");
		}
		if (usuario.getRol() == RolUsuario.DEMOSTRACION && !demostracionHabilitada) {
			throw new ErrorDeNegocio(CodigoError.CUENTA_INACTIVA,
					"El rol de demostración está deshabilitado en este entorno (MIGAL_ROL_DEMOSTRACION).");
		}
		return new SesionIniciada(tokens.emitir(usuario), usuario);
	}

	/**
	 * Cambia la clave del usuario autenticado. Exige la clave actual; la nueva debe tener al menos 8 caracteres con una
	 * letra y un número (validada en la solicitud) y ser distinta de la actual. El Administrador inicial la usa para
	 * reemplazar la clave de MIGAL_ADMIN_CLAVE tras el primer ingreso.
	 */
	@Transactional
	public void cambiarClave(long idUsuario, String claveActual, String claveNueva) {
		Usuario usuario = usuarios.findById(idUsuario)
				.orElseThrow(() -> new ErrorDeNegocio(CodigoError.NO_AUTENTICADO, "La sesión no es válida."));
		if (!codificador.matches(claveActual, usuario.getPasswordHash())) {
			throw ErrorDeNegocio.validacionDeCampo("claveActual", "La contraseña actual no es correcta.");
		}
		if (claveActual.equals(claveNueva)) {
			throw ErrorDeNegocio.validacionDeCampo("claveNueva", "La nueva contraseña debe ser distinta de la actual.");
		}
		usuario.cambiarPasswordHash(codificador.encode(claveNueva), reloj.instant());
	}
}
