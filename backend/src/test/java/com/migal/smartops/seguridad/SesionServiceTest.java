package com.migal.smartops.seguridad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.migal.smartops.comun.errores.CodigoError;
import com.migal.smartops.comun.errores.ErrorDeNegocio;
import com.migal.smartops.seguridad.model.RolUsuario;
import com.migal.smartops.seguridad.model.Usuario;
import com.migal.smartops.seguridad.repository.UsuarioRepository;
import com.migal.smartops.seguridad.service.SesionService;
import com.migal.smartops.seguridad.service.TokenService;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Verifica las reglas del inicio de sesión (CU-01) sin acceso a la base de datos. */
class SesionServiceTest {

	private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
	private final PasswordEncoder codificador = new BCryptPasswordEncoder(4);
	private final TokenService tokens = mock(TokenService.class);
	private final SesionService servicio = new SesionService(usuarios, codificador, tokens);

	private Usuario usuario(boolean activo) {
		Usuario usuario = new Usuario("Rosa", "Quispe", "rquispe", codificador.encode("Clave12345"), RolUsuario.VENDEDOR);
		usuario.cambiarEstado(activo, Instant.parse("2026-11-10T08:00:00Z"));
		return usuario;
	}

	private ErrorDeNegocio intentar(String username, String clave) {
		return assertThrows(ErrorDeNegocio.class, () -> servicio.iniciarSesion(username, clave));
	}

	@Test
	void iniciaSesionConCredencialesCorrectas() {
		Usuario usuario = usuario(true);
		when(usuarios.findByUsername("rquispe")).thenReturn(Optional.of(usuario));
		when(tokens.emitir(usuario)).thenReturn(new TokenService.Token("jwt", 28800));

		SesionService.SesionIniciada sesion = servicio.iniciarSesion("rquispe", "Clave12345");

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
}
