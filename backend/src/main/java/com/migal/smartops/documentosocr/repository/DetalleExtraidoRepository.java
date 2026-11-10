package com.migal.smartops.documentosocr.repository;

import com.migal.smartops.documentosocr.model.DetalleExtraido;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso a las líneas extraídas. Las líneas se consultan siempre dentro de su documento. */
public interface DetalleExtraidoRepository extends JpaRepository<DetalleExtraido, Long> {

	/** Línea de un documento concreto, para impedir que una ruta acceda a líneas de otro documento. */
	Optional<DetalleExtraido> findByIdAndDocumentoId(Long id, Long idDocumento);
}
