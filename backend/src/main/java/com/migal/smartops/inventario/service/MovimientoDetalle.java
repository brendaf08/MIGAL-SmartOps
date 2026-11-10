package com.migal.smartops.inventario.service;

import com.migal.smartops.inventario.model.MovimientoKardex;
import com.migal.smartops.inventario.model.OrigenMovimiento;
import com.migal.smartops.inventario.model.TipoOperacion;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Movimiento del Kardex con los datos del producto y la ubicación ya resueltos, para que las capas superiores
 * lo presenten sin acceder a la persistencia.
 */
public record MovimientoDetalle(long id, Instant fechaHora, long productoId, String sku, String nombreProducto,
		TipoOperacion tipoOperacion, BigDecimal cantidad, OrigenMovimiento origenMovimiento, long ubicacionId,
		long usuarioId, String motivo, Long idGuia, Long idRecepcion, UUID referenciaTraslado,
		Long idMovimientoCorregido) {

	/** Convierte un movimiento persistido. Debe invocarse dentro de la transacción que lo cargó. */
	static MovimientoDetalle de(MovimientoKardex m) {
		var inventario = m.getInventario();
		var producto = inventario.getProducto();
		return new MovimientoDetalle(m.getId(), m.getFechaHora(), producto.getId(), producto.getSku(),
				producto.getNombreOficial(), m.getTipoOperacion(), m.getCantidad(), m.getOrigenMovimiento(),
				inventario.getAlmacen().getId(), m.getUsuarioResponsable().getId(), m.getMotivo(), m.getIdGuia(),
				m.getIdRecepcion(), m.getReferenciaTraslado(), m.getIdMovimientoCorregido());
	}
}
