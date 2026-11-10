package com.migal.smartops.abastecimiento.repository;

import com.migal.smartops.abastecimiento.model.Proveedor;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Acceso al directorio de proveedores. Los filtros combinados del listado se componen con especificaciones. */
public interface ProveedorRepository extends JpaRepository<Proveedor, Long>, JpaSpecificationExecutor<Proveedor> {

	/** Búsqueda por RUC, que identifica de forma única al proveedor. */
	Optional<Proveedor> findByRuc(String ruc);
}
