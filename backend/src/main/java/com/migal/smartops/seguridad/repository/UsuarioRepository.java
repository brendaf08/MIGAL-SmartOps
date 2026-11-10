package com.migal.smartops.seguridad.repository;

import com.migal.smartops.seguridad.model.RolUsuario;
import com.migal.smartops.seguridad.model.Usuario;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Acceso a datos de las cuentas de usuario. Solo lo utiliza el servicio del módulo {@code seguridad}. */
public interface UsuarioRepository extends JpaRepository<Usuario, Long>, JpaSpecificationExecutor<Usuario> {

	Optional<Usuario> findByUsername(String username);

	boolean existsByUsername(String username);

	long countByRolAndEstadoActivoTrue(RolUsuario rol);
}
