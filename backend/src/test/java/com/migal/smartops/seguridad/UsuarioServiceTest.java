package com.migal.smartops.seguridad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.migal.smartops.comun.errores.CodigoError;
import com.migal.smartops.comun.errores.ErrorDeNegocio;
import com.migal.smartops.seguridad.model.RolUsuario;
import com.migal.smartops.seguridad.model.Usuario;
import com.migal.smartops.seguridad.repository.UsuarioRepository;
import com.migal.smartops.seguridad.service.UsuarioService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

/** Verifica las reglas de administración de usuarios (CU-02) sin acceso a la base de datos. */
class UsuarioServiceTest {

	private static final Instant AHORA = Instant.parse("2026-11-10T08:00:00Z");

	private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
	private final PasswordEncoder codificador = new BCryptPasswordEncoder(4);
	private final UsuarioService servicio =
			new UsuarioService(usuarios, codificador, Clock.fixed(AHORA, ZoneOffset.UTC));

	private Usuario usuario(long id, RolUsuario rol, boolean activo) {
		Usuario usuario = new Usuario("Rosa", "Quispe", "u" + id, "{hash}", rol);
		ReflectionTestUtils.setField(usuario, "id", id);
		ReflectionTestUtils.setField(usuario, "estadoActivo", activo);
		return usuario;
	}

	private void existe(Usuario usuario) {
		when(usuarios.findById(usuario.getId())).thenReturn(Optional.of(usuario));
		when(usuarios.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
	}

	@Test
	void registraUnUsuarioConLaClaveCodificada() {
		when(usuarios.existsByUsername("nuevo")).thenReturn(false);
		when(usuarios.saveAndFlush(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

		Usuario registrado = servicio.registrar("Ana", "Torres", "nuevo", "Temporal123", RolUsuario.VENDEDOR);

		assertNotEquals("Temporal123", registrado.getPasswordHash());
		assertTrue(codificador.matches("Temporal123", registrado.getPasswordHash()));
		assertEquals(RolUsuario.VENDEDOR, registrado.getRol());
	}

	@Test
	void rechazaUnNombreDeUsuarioExistente() {
		when(usuarios.existsByUsername("repetido")).thenReturn(true);

		ErrorDeNegocio error = assertThrows(ErrorDeNegocio.class,
				() -> servicio.registrar("Ana", "Torres", "repetido", "Temporal123", RolUsuario.VENDEDOR));

		assertEquals(CodigoError.USUARIO_DUPLICADO, error.codigo());
	}

	@Test
	void traduceLaCarreraDeDosRegistrosSimultaneosAUsuarioDuplicado() {
		when(usuarios.existsByUsername("carrera")).thenReturn(false);
		when(usuarios.saveAndFlush(any(Usuario.class))).thenThrow(new DataIntegrityViolationException("uq_usuario_username"));

		ErrorDeNegocio error = assertThrows(ErrorDeNegocio.class,
				() -> servicio.registrar("Ana", "Torres", "carrera", "Temporal123", RolUsuario.VENDEDOR));

		assertEquals(CodigoError.USUARIO_DUPLICADO, error.codigo());
	}

	@Test
	void exigeUnaClaveTemporalConLetraNumeroYOchoCaracteres() {
		for (String clave : new String[] {"corta1", "sinnumeros", "12345678", null}) {
			ErrorDeNegocio error = assertThrows(ErrorDeNegocio.class,
					() -> servicio.registrar("Ana", "Torres", "nuevo", clave, RolUsuario.VENDEDOR));
			assertEquals(CodigoError.VALIDACION_FALLIDA, error.codigo());
		}
		verify(usuarios, never()).saveAndFlush(any());
	}

	@Test
	void consultarUnUsuarioInexistenteResponde404() {
		when(usuarios.findById(99L)).thenReturn(Optional.empty());

		assertEquals(CodigoError.RECURSO_NO_ENCONTRADO,
				assertThrows(ErrorDeNegocio.class, () -> servicio.consultar(99L)).codigo());
	}

	@Test
	void desactivaAUnUsuarioYRegistraLaFecha() {
		Usuario objetivo = usuario(2, RolUsuario.VENDEDOR, true);
		existe(objetivo);

		Usuario resultado = servicio.cambiarEstado(2, false, 1);

		assertFalse(resultado.isEstadoActivo());
		assertEquals(AHORA, resultado.getFechaActualizacion());
	}

	@Test
	void noPermiteDesactivarLaPropiaCuenta() {
		existe(usuario(1, RolUsuario.ADMINISTRADOR, true));
		when(usuarios.countByRolAndEstadoActivoTrue(RolUsuario.ADMINISTRADOR)).thenReturn(3L);

		assertEquals(CodigoError.ADMINISTRADOR_REQUERIDO,
				assertThrows(ErrorDeNegocio.class, () -> servicio.cambiarEstado(1, false, 1)).codigo());
	}

	@Test
	void noPermiteDesactivarAlUnicoAdministradorActivo() {
		existe(usuario(2, RolUsuario.ADMINISTRADOR, true));
		when(usuarios.countByRolAndEstadoActivoTrue(RolUsuario.ADMINISTRADOR)).thenReturn(1L);

		assertEquals(CodigoError.ADMINISTRADOR_REQUERIDO,
				assertThrows(ErrorDeNegocio.class, () -> servicio.cambiarEstado(2, false, 1)).codigo());
	}

	@Test
	void permiteDesactivarAUnAdministradorSiQuedaOtroActivo() {
		existe(usuario(2, RolUsuario.ADMINISTRADOR, true));
		when(usuarios.countByRolAndEstadoActivoTrue(RolUsuario.ADMINISTRADOR)).thenReturn(2L);

		assertFalse(servicio.cambiarEstado(2, false, 1).isEstadoActivo());
	}

	@Test
	void permiteReactivarSinVerificarAdministradores() {
		existe(usuario(2, RolUsuario.ADMINISTRADOR, false));

		assertTrue(servicio.cambiarEstado(2, true, 1).isEstadoActivo());
		verify(usuarios, never()).countByRolAndEstadoActivoTrue(any());
	}

	@Test
	void noPermiteQuitarElRolAlUnicoAdministradorActivo() {
		existe(usuario(2, RolUsuario.ADMINISTRADOR, true));
		when(usuarios.countByRolAndEstadoActivoTrue(RolUsuario.ADMINISTRADOR)).thenReturn(1L);

		assertEquals(CodigoError.ADMINISTRADOR_REQUERIDO,
				assertThrows(ErrorDeNegocio.class,
						() -> servicio.actualizar(2, "Rosa", "Quispe", RolUsuario.VENDEDOR)).codigo());
	}

	@Test
	void actualizaNombresYRolDeUnUsuarioComun() {
		existe(usuario(3, RolUsuario.VENDEDOR, true));

		Usuario resultado = servicio.actualizar(3, "Rosa María", "Quispe Huamán", RolUsuario.ALMACENERO);

		assertEquals("Rosa María", resultado.getNombres());
		assertEquals(RolUsuario.ALMACENERO, resultado.getRol());
		assertEquals(AHORA, resultado.getFechaActualizacion());
	}
}
