package com.migal.smartops.agenteia.repository;

import com.migal.smartops.agenteia.model.ConversacionAgente;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a las conversaciones del agente. Todas las consultas de usuario filtran por el propietario, de modo que una
 * conversación ajena nunca se distingue de una inexistente.
 */
public interface ConversacionAgenteRepository extends JpaRepository<ConversacionAgente, Long> {

	/** Conversación del usuario indicado. Vacío si no existe o pertenece a otro usuario. */
	Optional<ConversacionAgente> findByIdAndUsuarioId(Long id, Long idUsuario);

	/** Conversaciones del usuario indicado, paginadas según el ordenamiento recibido. */
	Page<ConversacionAgente> findByUsuarioId(Long idUsuario, Pageable paginacion);
}
