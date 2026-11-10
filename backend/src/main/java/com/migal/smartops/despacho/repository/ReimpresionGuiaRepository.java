package com.migal.smartops.despacho.repository;

import com.migal.smartops.despacho.model.ReimpresionGuia;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso a las reimpresiones de guías. Solo se anexan registros; la base de datos rechaza modificaciones y eliminaciones. */
public interface ReimpresionGuiaRepository extends JpaRepository<ReimpresionGuia, Long> {

	/** Reimpresiones de una guía, en orden de registro. */
	List<ReimpresionGuia> findByGuiaIdOrderByIdAsc(Long idGuia);
}
