package com.migal.smartops.comercial.repository;

import com.migal.smartops.comercial.model.Cliente;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Acceso a datos de los clientes. Los filtros combinados del listado se componen con especificaciones. */
public interface ClienteRepository extends JpaRepository<Cliente, Long>, JpaSpecificationExecutor<Cliente> {

	/** Búsqueda por documento de identificación, usada para verificar la unicidad de los documentos informados. */
	Optional<Cliente> findByTipoDocumentoAndNumeroDocumento(String tipoDocumento, String numeroDocumento);
}
