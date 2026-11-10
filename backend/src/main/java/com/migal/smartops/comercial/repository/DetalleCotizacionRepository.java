package com.migal.smartops.comercial.repository;

import com.migal.smartops.comercial.model.DetalleCotizacion;
import com.migal.smartops.comercial.model.DetalleCotizacionId;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso a las líneas de cotización. Las líneas se persisten en cascada desde su cotización. */
public interface DetalleCotizacionRepository extends JpaRepository<DetalleCotizacion, DetalleCotizacionId> {
}
