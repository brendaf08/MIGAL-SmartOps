package com.migal.smartops.seguridad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.migal.smartops.TestcontainersConfiguration;
import com.migal.smartops.seguridad.model.RolUsuario;
import com.migal.smartops.seguridad.model.Usuario;
import com.migal.smartops.seguridad.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** Verifica el mapeo de la entidad {@link Usuario} y las consultas del repositorio con PostgreSQL real. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class UsuarioRepositoryTest {

	@Autowired
	private UsuarioRepository repositorio;

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private JdbcTemplate jdbc;

	private Usuario nuevo(String username, RolUsuario rol) {
		return new Usuario("Rosa", "Quispe", username, "{hash}", rol);
	}

	@Test
	void guardaUnUsuarioYLeeLosValoresGeneradosPorLaBaseDeDatos() {
		Usuario guardado = repositorio.saveAndFlush(nuevo("rquispe", RolUsuario.ALMACENERO));

		assertNotNull(guardado.getId());
		assertNotNull(guardado.getFechaRegistro());
		assertTrue(guardado.isEstadoActivo());
	}

	@Test
	void persisteElRolComoTextoEnMinusculas() {
		Usuario guardado = repositorio.saveAndFlush(nuevo("avendedor", RolUsuario.VENDEDOR));

		String rol = jdbc.queryForObject("SELECT rol FROM usuario WHERE id_usuario = ?", String.class, guardado.getId());
		assertEquals("vendedor", rol);
	}

	@Test
	void recuperaElRolComoEnumeracion() {
		Usuario guardado = repositorio.saveAndFlush(nuevo("rrepo", RolUsuario.REPONEDOR));
		entityManager.clear();

		assertEquals(RolUsuario.REPONEDOR, repositorio.findById(guardado.getId()).orElseThrow().getRol());
	}

	@Test
	void buscaPorNombreDeUsuario() {
		repositorio.saveAndFlush(nuevo("buscable", RolUsuario.ADMINISTRADOR));

		assertTrue(repositorio.findByUsername("buscable").isPresent());
		assertTrue(repositorio.existsByUsername("buscable"));
		assertFalse(repositorio.existsByUsername("inexistente"));
	}

	@Test
	void rechazaUnNombreDeUsuarioDuplicado() {
		repositorio.saveAndFlush(nuevo("duplicado", RolUsuario.VENDEDOR));

		assertThrows(DataIntegrityViolationException.class,
				() -> repositorio.saveAndFlush(nuevo("duplicado", RolUsuario.ALMACENERO)));
	}

	@Test
	void cuentaLosAdministradoresActivos() {
		long antes = repositorio.countByRolAndEstadoActivoTrue(RolUsuario.ADMINISTRADOR);
		repositorio.saveAndFlush(nuevo("admin1", RolUsuario.ADMINISTRADOR));
		Usuario inactivo = repositorio.saveAndFlush(nuevo("admin2", RolUsuario.ADMINISTRADOR));
		inactivo.cambiarEstado(false, Instant.now());
		repositorio.saveAndFlush(inactivo);

		assertEquals(antes + 1, repositorio.countByRolAndEstadoActivoTrue(RolUsuario.ADMINISTRADOR));
	}

	@Test
	void actualizaDatosYRegistraLaFechaDeModificacion() {
		Usuario guardado = repositorio.saveAndFlush(nuevo("modificable", RolUsuario.VENDEDOR));
		Instant ahora = Instant.parse("2026-11-10T12:00:00Z");

		guardado.actualizar("Rosa María", "Quispe Huamán", RolUsuario.ALMACENERO, ahora);
		repositorio.saveAndFlush(guardado);
		entityManager.clear();

		Usuario leido = repositorio.findById(guardado.getId()).orElseThrow();
		assertEquals("Rosa María", leido.getNombres());
		assertEquals(RolUsuario.ALMACENERO, leido.getRol());
		assertEquals(ahora, leido.getFechaActualizacion());
	}
}
