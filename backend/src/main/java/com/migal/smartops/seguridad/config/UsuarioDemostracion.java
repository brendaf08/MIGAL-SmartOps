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
 * Crea al arrancar el usuario de demostración (ADR-013) cuando {@code MIGAL_ROL_DEMOSTRACION} está activo y se definen
 * {@code MIGAL_DEMO_USERNAME} y {@code MIGAL_DEMO_CLAVE}. El rol no se asigna por la API: esta es la única forma de
 * crearlo. Si el usuario ya existe, no hace nada.
 */
@Component
public class UsuarioDemostracion implements ApplicationRunner {

	private static final Logger REGISTRO = LoggerFactory.getLogger(UsuarioDemostracion.class);

	private final UsuarioRepository usuarios;
	private final UsuarioService servicio;
	private final boolean habilitada;
	private final String username;
	private final String clave;

	public UsuarioDemostracion(UsuarioRepository usuarios, UsuarioService servicio,
			@Value("${migal.seguridad.demostracion.habilitada:false}") boolean habilitada,
			@Value("${migal.seguridad.demostracion.username:}") String username,
			@Value("${migal.seguridad.demostracion.clave:}") String clave) {
		this.usuarios = usuarios;
		this.servicio = servicio;
		this.habilitada = habilitada;
		this.username = username;
		this.clave = clave;
	}

	@Override
	public void run(ApplicationArguments argumentos) {
		if (!habilitada || username.isBlank() || clave.isBlank() || usuarios.existsByUsername(username.trim())) {
			return;
		}
		servicio.registrar("Usuario", "Demostración", username.trim(), clave, RolUsuario.DEMOSTRACION);
		REGISTRO.info("Se creó el usuario de demostración '{}' con los permisos de los cuatro roles.", username.trim());
	}
}
