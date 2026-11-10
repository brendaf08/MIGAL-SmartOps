package com.migal.smartops.recepciones.repository;

import com.migal.smartops.recepciones.model.Recepcion;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Acceso a las recepciones. Las operaciones que cambian una recepción la bloquean antes de modificarla (ADR-010), de modo
 * que dos validaciones simultáneas de la misma recepción no registren las entradas dos veces.
 */
public interface RecepcionRepository extends JpaRepository<Recepcion, Long>, JpaSpecificationExecutor<Recepcion> {

	/** Identificador de la orden de una recepción, sin cargar la recepción en el contexto de persistencia. */
	@Query("select r.orden.id from Recepcion r where r.id = :id")
	Optional<Long> idOrdenDeRecepcion(@Param("id") Long id);

	/** Bloquea la recepción hasta el fin de la transacción. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select r from Recepcion r where r.id = :id")
	Optional<Recepcion> bloquearPorId(@Param("id") Long id);
}
