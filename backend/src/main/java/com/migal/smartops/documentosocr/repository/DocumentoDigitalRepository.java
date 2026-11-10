package com.migal.smartops.documentosocr.repository;

import com.migal.smartops.documentosocr.model.DocumentoDigital;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Acceso a los documentos digitales. Las operaciones que cambian el estado de un documento lo bloquean antes de
 * modificarlo (ADR-010), de modo que dos validaciones simultáneas no confirmen el mismo documento dos veces.
 */
public interface DocumentoDigitalRepository extends JpaRepository<DocumentoDigital, Long>,
		JpaSpecificationExecutor<DocumentoDigital> {

	/** Documento vigente, es decir, no eliminado lógicamente. */
	Optional<DocumentoDigital> findByIdAndFechaEliminacionIsNull(Long id);

	/** Identificador del documento que sirve de apoyo a una recepción, si existe. */
	@Query("select d.id from DocumentoDigital d where d.idRecepcion = :idRecepcion")
	Optional<Long> idDocumentoDeRecepcion(@Param("idRecepcion") Long idRecepcion);

	/** Bloquea el documento hasta el fin de la transacción. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select d from DocumentoDigital d where d.id = :id")
	Optional<DocumentoDigital> bloquearPorId(@Param("id") Long id);
}
