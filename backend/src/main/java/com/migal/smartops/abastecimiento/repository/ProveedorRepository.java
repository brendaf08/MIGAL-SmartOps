package com.migal.smartops.abastecimiento.repository;

import com.migal.smartops.abastecimiento.model.Proveedor;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Acceso al directorio de proveedores. Los filtros combinados del listado se componen con especificaciones. */
public interface ProveedorRepository extends JpaRepository<Proveedor, Long>, JpaSpecificationExecutor<Proveedor> {

	/** Búsqueda por RUC, que identifica de forma única al proveedor. */
	Optional<Proveedor> findByRuc(String ruc);

	/** Bloquea el proveedor hasta el fin de la transacción, para reemplazar su catálogo sin carreras. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select p from Proveedor p where p.id = :id")
	Optional<Proveedor> bloquearPorId(@Param("id") Long id);
}
