package com.migal.smartops.agenteia.repository;

import com.migal.smartops.agenteia.model.MensajeAgente;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso a los mensajes de las conversaciones del agente. */
public interface MensajeAgenteRepository extends JpaRepository<MensajeAgente, Long> {

	/** Mensajes de una conversación en orden cronológico ascendente. */
	List<MensajeAgente> findByConversacionIdOrderByFechaAscIdAsc(Long idConversacion);

	/** Número de mensajes de una conversación. */
	long countByConversacionId(Long idConversacion);
}
