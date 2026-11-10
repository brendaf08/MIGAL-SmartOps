package com.migal.smartops.seguridad.service;

import com.migal.smartops.comun.errores.CodigoError;
import com.migal.smartops.comun.errores.ErrorDeNegocio;
import com.migal.smartops.seguridad.model.RolUsuario;
import com.migal.smartops.seguridad.model.Usuario;
import com.migal.smartops.seguridad.repository.UsuarioRepository;
import jakarta.persistence.criteria.Predicate;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Administración de usuarios, roles y estado de acceso (CU-02). Aplica la protección del último
 * Administrador activo (ADR-009): no se puede desactivar ni dejar de ser Administrador al único que queda,
 * ni desactivarse a uno mismo.
 */
@Service
public class UsuarioService {

	private static final Pattern POLITICA_CLAVE = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).{8,}$");

	private final UsuarioRepository usuarios;
	private final PasswordEncoder codificador;
	private final Clock reloj;

	public UsuarioService(UsuarioRepository usuarios, PasswordEncoder codificador, Clock reloj) {
		this.usuarios = usuarios;
		this.codificador = codificador;
		this.reloj = reloj;
	}

	@Transactional(readOnly = true)
	public Page<Usuario> listar(String q, RolUsuario rol, Boolean estadoActivo, Pageable paginacion) {
		Specification<Usuario> filtros = (raiz, consulta, cb) -> {
			List<Predicate> condiciones = new ArrayList<>();
			if (q != null && !q.isBlank()) {
				String patron = "%" + q.trim().toLowerCase() + "%";
				condiciones.add(cb.or(
						cb.like(cb.lower(raiz.get("nombres")), patron),
						cb.like(cb.lower(raiz.get("apellidos")), patron),
						cb.like(cb.lower(raiz.get("username")), patron)));
			}
			if (rol != null) {
				condiciones.add(cb.equal(raiz.get("rol"), rol));
			}
			if (estadoActivo != null) {
				condiciones.add(cb.equal(raiz.get("estadoActivo"), estadoActivo));
			}
			return cb.and(condiciones.toArray(Predicate[]::new));
		};
		return usuarios.findAll(filtros, paginacion);
	}

	@Transactional(readOnly = true)
	public Usuario consultar(long id) {
		return usuarios.findById(id).orElseThrow(UsuarioService::noEncontrado);
	}

	@Transactional
	public Usuario registrar(String nombres, String apellidos, String username, String claveTemporal, RolUsuario rol) {
		validarClave(claveTemporal);
		if (usuarios.existsByUsername(username)) {
			throw usuarioDuplicado();
		}
		Usuario nuevo = new Usuario(nombres, apellidos, username, codificador.encode(claveTemporal), rol);
		try {
			return usuarios.saveAndFlush(nuevo);
		} catch (DataIntegrityViolationException e) {
			throw usuarioDuplicado();
		}
	}

	@Transactional
	public Usuario actualizar(long id, String nombres, String apellidos, RolUsuario rol) {
		Usuario usuario = consultar(id);
		if (usuario.getRol() == RolUsuario.ADMINISTRADOR && rol != RolUsuario.ADMINISTRADOR) {
			asegurarOtroAdministradorActivo(usuario);
		}
		usuario.actualizar(nombres, apellidos, rol, reloj.instant());
		return usuarios.save(usuario);
	}

	/**
	 * Activa o desactiva el acceso de un usuario.
	 *
	 * @param idActor identificador del usuario autenticado que realiza la operación
	 */
	@Transactional
	public Usuario cambiarEstado(long id, boolean activo, long idActor) {
		Usuario usuario = consultar(id);
		if (!activo) {
			if (id == idActor) {
				throw new ErrorDeNegocio(CodigoError.ADMINISTRADOR_REQUERIDO,
						"Un usuario no puede desactivar su propia cuenta.");
			}
			asegurarOtroAdministradorActivo(usuario);
		}
		usuario.cambiarEstado(activo, reloj.instant());
		return usuarios.save(usuario);
	}

	private void asegurarOtroAdministradorActivo(Usuario usuario) {
		boolean esAdministradorActivo = usuario.getRol() == RolUsuario.ADMINISTRADOR && usuario.isEstadoActivo();
		if (esAdministradorActivo && usuarios.countByRolAndEstadoActivoTrue(RolUsuario.ADMINISTRADOR) <= 1) {
			throw new ErrorDeNegocio(CodigoError.ADMINISTRADOR_REQUERIDO,
					"Debe existir al menos un Administrador activo.");
		}
	}

	private static void validarClave(String clave) {
		if (clave == null || !POLITICA_CLAVE.matcher(clave).matches()) {
			throw new ErrorDeNegocio(CodigoError.VALIDACION_FALLIDA,
					"La contraseña temporal debe tener al menos 8 caracteres, con una letra y un número.");
		}
	}

	private static ErrorDeNegocio usuarioDuplicado() {
		return new ErrorDeNegocio(CodigoError.USUARIO_DUPLICADO, "El nombre de usuario ya existe.");
	}

	private static ErrorDeNegocio noEncontrado() {
		return new ErrorDeNegocio(CodigoError.RECURSO_NO_ENCONTRADO, "El usuario no existe.");
	}
}
