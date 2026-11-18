package com.migal.smartops.seguridad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.migal.smartops.seguridad.config.ConversorDeAutenticacion;
import com.migal.smartops.seguridad.config.UsuarioDemostracion;
import com.migal.smartops.seguridad.model.RolUsuario;
import com.migal.smartops.seguridad.repository.UsuarioRepository;
import com.migal.smartops.seguridad.service.UsuarioService;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

/** Verifica el rol de demostración (ADR-013) sin acceso a la base de datos. */
class UsuarioDemostracionTest {

	private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
	private final UsuarioService servicio = mock(UsuarioService.class);

	private static Set<String> autoridades(RolUsuario rol) {
		return ConversorDeAutenticacion.autoridades(rol).stream().map(GrantedAuthority::getAuthority)
				.collect(Collectors.toSet());
	}

	@Test
	void elRolDeDemostracionRecibeLasAutoridadesDeLosCuatroRoles() {
		assertEquals(Set.of("ROLE_DEMOSTRACION", "ROLE_ADMINISTRADOR", "ROLE_VENDEDOR", "ROLE_ALMACENERO",
				"ROLE_REPONEDOR"), autoridades(RolUsuario.DEMOSTRACION));
		assertEquals(Set.of("ROLE_ADMINISTRADOR"), autoridades(RolUsuario.ADMINISTRADOR));
		assertEquals(Set.of("ROLE_VENDEDOR"), autoridades(RolUsuario.VENDEDOR));
	}

	@Test
	void incluyeSoloElMismoRolSalvoEnDemostracion() {
		assertTrue(RolUsuario.DEMOSTRACION.incluye(RolUsuario.ADMINISTRADOR));
		assertTrue(RolUsuario.VENDEDOR.incluye(RolUsuario.VENDEDOR));
		assertFalse(RolUsuario.VENDEDOR.incluye(RolUsuario.ADMINISTRADOR));
		assertFalse(RolUsuario.ADMINISTRADOR.incluye(RolUsuario.DEMOSTRACION));
	}

	@Test
	void creaLaCuentaSoloSiElRolEstaHabilitadoYHayCredenciales() {
		when(usuarios.existsByUsername("demo")).thenReturn(false);

		new UsuarioDemostracion(usuarios, servicio, false, "demo", "Demo12345").run(null);
		new UsuarioDemostracion(usuarios, servicio, true, "", "Demo12345").run(null);
		new UsuarioDemostracion(usuarios, servicio, true, "demo", "").run(null);
		verify(servicio, never()).registrar(anyString(), anyString(), anyString(), anyString(), any());

		new UsuarioDemostracion(usuarios, servicio, true, " demo ", "Demo12345").run(null);
		verify(servicio).registrar("Usuario", "Demostración", "demo", "Demo12345", RolUsuario.DEMOSTRACION);
	}

	@Test
	void noSobrescribeUnUsuarioExistente() {
		when(usuarios.existsByUsername("demo")).thenReturn(true);

		new UsuarioDemostracion(usuarios, servicio, true, "demo", "Demo12345").run(null);

		verify(servicio, never()).registrar(anyString(), anyString(), anyString(), anyString(), any());
	}
}
