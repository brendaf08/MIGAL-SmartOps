package com.migal.smartops.inventario.controller;

import com.migal.smartops.inventario.service.ConsultaInventarioService.AlertaStock;
import com.migal.smartops.inventario.service.ConsultaInventarioService.ExistenciaUbicacion;
import com.migal.smartops.inventario.service.ConsultaInventarioService.ProductoExistencias;
import com.migal.smartops.inventario.service.MovimientoDetalle;
import com.migal.smartops.inventario.service.MovimientoInventarioService.CambioExistencia;
import com.migal.smartops.inventario.service.MovimientoInventarioService.ResultadoMovimiento;
import com.migal.smartops.inventario.service.MovimientoInventarioService.ResultadoTraslado;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Cuerpos de respuesta del módulo de inventario, según los esquemas del contrato {@code inventario.yaml}. */
public final class InventarioDtos {

	private InventarioDtos() {
	}

	private static String texto(Object valor) {
		return valor == null ? null : valor.toString();
	}

	/** {@code ExistenciaUbicacion}. */
	public record ExistenciaUbicacionDto(String ubicacionId, String nombre, BigDecimal fisica, BigDecimal reservada,
			BigDecimal libre) {

		static ExistenciaUbicacionDto de(ExistenciaUbicacion e) {
			return new ExistenciaUbicacionDto(texto(e.ubicacionId()), e.nombre(), e.fisica(), e.reservada(), e.libre());
		}
	}

	/** {@code ProductoExistencias}. */
	public record ProductoExistenciasDto(String productoId, String sku, String nombre, String categoria, String unidad,
			BigDecimal stockMinimo, BigDecimal fisico, BigDecimal reservado, BigDecimal libre, String nivelAlerta,
			List<ExistenciaUbicacionDto> ubicaciones) {

		static ProductoExistenciasDto de(ProductoExistencias p) {
			return new ProductoExistenciasDto(texto(p.productoId()), p.sku(), p.nombre(), p.categoria(), p.unidad(),
					p.stockMinimo(), p.fisico(), p.reservado(), p.libre(), p.nivelAlerta(),
					p.ubicaciones().stream().map(ExistenciaUbicacionDto::de).toList());
		}
	}

	/** {@code DisponibilidadPagina}: el sobre de paginación más los totales del catálogo. */
	public record DisponibilidadPaginaDto(List<ProductoExistenciasDto> items, long total, int page, int pageSize,
			long enAlerta, long totalGeneral) {
	}

	/** {@code AlertaStock}. */
	public record AlertaStockDto(String productoId, String sku, String nombre, String unidad, BigDecimal stockMinimo,
			BigDecimal libre, BigDecimal faltante, String nivelAlerta) {

		static AlertaStockDto de(AlertaStock a) {
			return new AlertaStockDto(texto(a.productoId()), a.sku(), a.nombre(), a.unidad(), a.stockMinimo(), a.libre(),
					a.faltante(), a.nivelAlerta());
		}
	}

	/** {@code MovimientoKardex}. Los campos de referencia son nulos cuando no corresponden al origen. */
	public record MovimientoKardexDto(String id, Instant fechaHora, String productoId, String sku,
			String nombreProducto, String tipoOperacion, BigDecimal cantidad, String origenMovimiento,
			String ubicacionId, String usuarioId, String motivo, String idGuia, String idRecepcion,
			String referenciaTraslado, String idMovimientoCorregido) {

		static MovimientoKardexDto de(MovimientoDetalle m) {
			return new MovimientoKardexDto(texto(m.id()), m.fechaHora(), texto(m.productoId()), m.sku(),
					m.nombreProducto(), m.tipoOperacion().valor(), m.cantidad(), m.origenMovimiento().valor(),
					texto(m.ubicacionId()), texto(m.usuarioId()), m.motivo(), texto(m.idGuia()),
					texto(m.idRecepcion()), texto(m.referenciaTraslado()), texto(m.idMovimientoCorregido()));
		}
	}

	/** {@code CambioExistencia}. */
	public record CambioExistenciaDto(String productoId, String sku, String ubicacionId, String unidad,
			BigDecimal antes, BigDecimal despues) {

		static CambioExistenciaDto de(CambioExistencia c) {
			return new CambioExistenciaDto(texto(c.productoId()), c.sku(), texto(c.ubicacionId()), c.unidad(),
					c.antes(), c.despues());
		}
	}

	/** {@code ResultadoMovimiento}. */
	public record ResultadoMovimientoDto(List<MovimientoKardexDto> movimientos, List<CambioExistenciaDto> cambios) {

		static ResultadoMovimientoDto de(ResultadoMovimiento r) {
			return new ResultadoMovimientoDto(List.of(MovimientoKardexDto.de(r.movimiento())),
					List.of(CambioExistenciaDto.de(r.cambio())));
		}
	}

	/** {@code ResultadoTraslado}. */
	public record ResultadoTrasladoDto(String referenciaTraslado, List<MovimientoKardexDto> movimientos,
			List<CambioExistenciaDto> cambios) {

		static ResultadoTrasladoDto de(ResultadoTraslado r) {
			return new ResultadoTrasladoDto(texto(r.referenciaTraslado()),
					r.movimientos().stream().map(MovimientoKardexDto::de).toList(),
					r.cambios().stream().map(CambioExistenciaDto::de).toList());
		}
	}
}
