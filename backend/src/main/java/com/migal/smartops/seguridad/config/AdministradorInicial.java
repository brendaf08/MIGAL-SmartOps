package com.migal.smartops.seguridad.config;

import com.migal.smartops.seguridad.model.RolUsuario;
import com.migal.smartops.seguridad.repository.UsuarioRepository;
import com.migal.smartops.seguridad.service.UsuarioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Crea el primer Administrador al arrancar cuando todavía no existe ninguno. Sin un Administrador no es posible
 * iniciar sesión ni crear usuarios, por lo que una base de datos nueva queda inaccesible. El nombre de usuario y la
 * clave inicial se leen de las variables de entorno {@code MIGAL_ADMIN_USERNAME} y {@code MIGAL_ADMIN_CLAVE} y
 * nunca se almacenan en el repositorio. Si alguna no está definida, o si ya existe un Administrador activo, no hace
 * nada. La clave debe cumplir la política vigente (mínimo ocho caracteres con letras y números) y conviene
 * reemplazarla tras el primer inicio de sesión.
 */
@Component
public class AdministradorInicial implements ApplicationRunner {

	private static final Logger REGISTRO = LoggerFactory.getLogger(AdministradorInicial.class);

	private final UsuarioRepository usuarios;
	private final UsuarioService servicio;
	private final String username;
	private final String clave;

	public AdministradorInicial(UsuarioRepository usuarios, UsuarioService servicio,
			@Value("${migal.seguridad.administrador-inicial.username:}") String username,
			@Value("${migal.seguridad.administrador-inicial.clave:}") String clave) {
		this.usuarios = usuarios;
		this.servicio = servicio;
		this.username = username;
		this.clave = clave;
	}

	@Override
	public void run(ApplicationArguments argumentos) {
		if (username.isBlank() || clave.isBlank()) {
			return;
		}
		if (usuarios.countByRolAndEstadoActivoTrue(RolUsuario.ADMINISTRADOR) > 0) {
			return;
		}
		if (usuarios.existsByUsername(username.trim())) {
			REGISTRO.warn("No se creó el Administrador inicial: el usuario '{}' ya existe y no es un Administrador activo.",
					username.trim());
			return;
		}
		servicio.registrar("Administrador", "Inicial", username.trim(), clave, RolUsuario.ADMINISTRADOR);
		REGISTRO.info("Se creó el Administrador inicial '{}'. Debe reemplazarse su clave tras el primer inicio de sesión.",
				username.trim());
	}
}
