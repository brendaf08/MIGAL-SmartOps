package com.migal.smartops.seguridad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.migal.smartops.comun.errores.CodigoError;
import com.migal.smartops.comun.errores.ErrorDeNegocio;
import com.migal.smartops.seguridad.model.RolUsuario;
import com.migal.smartops.seguridad.model.Usuario;
import com.migal.smartops.seguridad.repository.UsuarioRepository;
import com.migal.smartops.seguridad.service.LimitadorIntentosSesion;
import com.migal.smartops.seguridad.service.SesionService;
import com.migal.smartops.seguridad.service.TokenService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Verifica las reglas del inicio de sesión (CU-01) sin acceso a la base de datos. */
class SesionServiceTest {

	private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
	private final PasswordEncoder codificador = new BCryptPasswordEncoder(4);
	private final TokenService tokens = mock(TokenService.class);
	private final Clock reloj = Clock.fixed(Instant.parse("2026-11-18T15:00:00Z"), ZoneOffset.UTC);
	private final SesionService servicio = new SesionService(usuarios, codificador, tokens,
			new LimitadorIntentosSesion(reloj), reloj, false);
	private static final String ORIGEN = "10.0.0.5";

	private Usuario usuario(boolean activo) {
		Usuario usuario = new Usuario("Rosa", "Quispe", "rquispe", codificador.encode("Clave12345"), RolUsuario.VENDEDOR);
		usuario.cambiarEstado(activo, Instant.parse("2026-11-10T08:00:00Z"));
		return usuario;
	}

	private ErrorDeNegocio intentar(String username, String clave) {
		return assertThrows(ErrorDeNegocio.class, () -> servicio.iniciarSesion(username, clave, ORIGEN));
	}

	@Test
	void iniciaSesionConCredencialesCorrectas() {
		Usuario usuario = usuario(true);
		when(usuarios.findByUsername("rquispe")).thenReturn(Optional.of(usuario));
		when(tokens.emitir(usuario)).thenReturn(new TokenService.Token("jwt", 28800));

		SesionService.SesionIniciada sesion = servicio.iniciarSesion("rquispe", "Clave12345", ORIGEN);

		assertEquals("jwt", sesion.token().valor());
		assertEquals(usuario, sesion.usuario());
	}

	@Test
	void rechazaUnaClaveIncorrecta() {
		when(usuarios.findByUsername("rquispe")).thenReturn(Optional.of(usuario(true)));

		assertEquals(CodigoError.NO_AUTENTICADO, intentar("rquispe", "otra-clave").codigo());
	}

	@Test
	void respondeIgualAnteUnUsuarioInexistenteQueAnteUnaClaveIncorrecta() {
		when(usuarios.findByUsername("nadie")).thenReturn(Optional.empty());
		when(usuarios.findByUsername("rquispe")).thenReturn(Optional.of(usuario(true)));

		ErrorDeNegocio inexistente = intentar("nadie", "Clave12345");
		ErrorDeNegocio incorrecta = intentar("rquispe", "otra-clave");

		assertEquals(inexistente.codigo(), incorrecta.codigo());
		assertEquals(inexistente.getMessage(), incorrecta.getMessage());
	}

	@Test
	void rechazaUnaCuentaDesactivadaSoloSiLasCredencialesSonCorrectas() {
		when(usuarios.findByUsername("rquispe")).thenReturn(Optional.of(usuario(false)));

		assertEquals(CodigoError.CUENTA_INACTIVA, intentar("rquispe", "Clave12345").codigo());
		assertEquals(CodigoError.NO_AUTENTICADO, intentar("rquispe", "otra-clave").codigo());
		org.mockito.Mockito.verify(tokens, org.mockito.Mockito.never()).emitir(any());
	}

	@Test
	void bloqueaTrasCincoIntentosFallidosDelMismoUsuarioYOrigen() {
		Usuario usuario = usuario(true);
		when(usuarios.findByUsername("rquispe")).thenReturn(Optional.of(usuario));
		when(tokens.emitir(usuario)).thenReturn(new TokenService.Token("jwt", 28800));
		for (int i = 0; i < 5; i++) {
			assertEquals(CodigoError.NO_AUTENTICADO, intentar("rquispe", "otra-clave").codigo());
		}

		// Aun con la clave correcta, el par usuario y origen queda bloqueado; otro origen no se ve afectado.
		assertEquals(CodigoError.DEMASIADOS_INTENTOS, intentar("rquispe", "Clave12345").codigo());
		assertEquals("jwt", servicio.iniciarSesion("rquispe", "Clave12345", "10.0.0.9").token().valor());
	}

	@Test
	void unInicioCorrectoReiniciaLosIntentos() {
		Usuario usuario = usuario(true);
		when(usuarios.findByUsername("rquispe")).thenReturn(Optional.of(usuario));
		when(tokens.emitir(usuario)).thenReturn(new TokenService.Token("jwt", 28800));
		for (int i = 0; i < 4; i++) {
			intentar("rquispe", "otra-clave");
		}
		servicio.iniciarSesion("rquispe", "Clave12345", ORIGEN);

		assertEquals(CodigoError.NO_AUTENTICADO, intentar("rquispe", "otra-clave").codigo());
	}

	@Test
	void cambiaLaClaveSoloConLaClaveActualYUnaNuevaDistinta() {
		Usuario usuario = usuario(true);
		when(usuarios.findById(7L)).thenReturn(Optional.of(usuario));

		ErrorDeNegocio incorrecta = assertThrows(ErrorDeNegocio.class,
				() -> servicio.cambiarClave(7L, "otra-clave", "Nueva2026"));
		ErrorDeNegocio igual = assertThrows(ErrorDeNegocio.class,
				() -> servicio.cambiarClave(7L, "Clave12345", "Clave12345"));
		servicio.cambiarClave(7L, "Clave12345", "Nueva2026");

		assertEquals("claveActual", incorrecta.campo());
		assertEquals("claveNueva", igual.campo());
		assertTrue(codificador.matches("Nueva2026", usuario.getPasswordHash()));
	}

	@Test
	void rechazaLaCuentaDeDemostracionSiElRolEstaDeshabilitado() {
		Usuario demo = new Usuario("Usuario", "Demostración", "demo", codificador.encode("Clave12345"),
				RolUsuario.DEMOSTRACION);
		when(usuarios.findByUsername("demo")).thenReturn(Optional.of(demo));
		when(tokens.emitir(any())).thenReturn(new TokenService.Token("jwt", 28800));

		assertEquals(CodigoError.CUENTA_INACTIVA, intentar("demo", "Clave12345").codigo());

		SesionService habilitado = new SesionService(usuarios, codificador, tokens, new LimitadorIntentosSesion(reloj),
				reloj, true);
		assertEquals("jwt", habilitado.iniciarSesion("demo", "Clave12345", ORIGEN).token().valor());
	}
}
