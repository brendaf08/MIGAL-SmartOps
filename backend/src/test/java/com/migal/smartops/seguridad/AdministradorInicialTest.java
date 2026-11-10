package com.migal.smartops.seguridad;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.migal.smartops.seguridad.config.AdministradorInicial;
import com.migal.smartops.seguridad.model.RolUsuario;
import com.migal.smartops.seguridad.repository.UsuarioRepository;
import com.migal.smartops.seguridad.service.UsuarioService;
import org.junit.jupiter.api.Test;

/** Verifica cuándo se crea el Administrador inicial, sin acceso a la base de datos. */
class AdministradorInicialTest {

	private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
	private final UsuarioService servicio = mock(UsuarioService.class);

	private AdministradorInicial semilla(String username, String clave) {
		return new AdministradorInicial(usuarios, servicio, username, clave);
	}

	@Test
	void creaElAdministradorCuandoNoExisteNinguno() {
		when(usuarios.countByRolAndEstadoActivoTrue(RolUsuario.ADMINISTRADOR)).thenReturn(0L);
		when(usuarios.existsByUsername("admin")).thenReturn(false);

		semilla(" admin ", "Inicial123").run(null);

		verify(servicio).registrar("Administrador", "Inicial", "admin", "Inicial123", RolUsuario.ADMINISTRADOR);
	}

	@Test
	void noHaceNadaSiYaHayUnAdministradorActivo() {
		when(usuarios.countByRolAndEstadoActivoTrue(RolUsuario.ADMINISTRADOR)).thenReturn(1L);

		semilla("admin", "Inicial123").run(null);

		verify(servicio, never()).registrar(anyString(), anyString(), anyString(), anyString(), any());
	}

	@Test
	void noHaceNadaSiFaltaElUsuarioOLaClave() {
		semilla("", "Inicial123").run(null);
		semilla("admin", "").run(null);

		verify(usuarios, never()).countByRolAndEstadoActivoTrue(any());
		verify(servicio, never()).registrar(anyString(), anyString(), anyString(), anyString(), any());
	}

	@Test
	void noSobrescribeUnUsuarioExistenteQueNoEsAdministrador() {
		when(usuarios.countByRolAndEstadoActivoTrue(RolUsuario.ADMINISTRADOR)).thenReturn(0L);
		when(usuarios.existsByUsername("admin")).thenReturn(true);

		semilla("admin", "Inicial123").run(null);

		verify(servicio, never()).registrar(anyString(), anyString(), anyString(), anyString(), any());
	}
}
