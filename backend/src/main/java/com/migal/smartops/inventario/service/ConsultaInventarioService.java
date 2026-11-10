package com.migal.smartops.inventario.service;

import com.migal.smartops.comun.errores.CodigoError;
import com.migal.smartops.comun.errores.ErrorDeNegocio;
import com.migal.smartops.inventario.model.OrigenMovimiento;
import com.migal.smartops.inventario.model.TipoOperacion;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consultas de inventario (CU-04 y CU-07): disponibilidad, existencias por producto, alertas de stock y Kardex.
 * Son operaciones de solo lectura que no generan movimientos. El stock libre es la existencia física menos las
 * reservas activas, y el nivel de alerta se calcula sobre él: crítico si es igual o inferior a cero, mínimo si es
 * igual o inferior al stock mínimo y normal en otro caso.
 */
@Service
public class ConsultaInventarioService {

	/** Zona horaria con la que se interpretan los rangos de fechas del Kardex. */
	static final ZoneId ZONA = ZoneId.of("America/Lima");

	/** Existencias de un producto en una ubicación. */
	public record ExistenciaUbicacion(long ubicacionId, String nombre, BigDecimal fisica, BigDecimal reservada,
			BigDecimal libre) {
	}

	/** Existencias de un producto, consolidadas y por ubicación. */
	public record ProductoExistencias(long productoId, String sku, String nombre, String categoria, String unidad,
			BigDecimal stockMinimo, BigDecimal fisico, BigDecimal reservado, BigDecimal libre, String nivelAlerta,
			List<ExistenciaUbicacion> ubicaciones) {
	}

	/** Producto en nivel crítico o mínimo, con lo que falta para alcanzar el stock mínimo. */
	public record AlertaStock(long productoId, String sku, String nombre, String unidad, BigDecimal stockMinimo,
			BigDecimal libre, BigDecimal faltante, String nivelAlerta) {
	}

	/** Página de disponibilidad con los totales del catálogo sin filtros. */
	public record Disponibilidad(Page<ProductoExistencias> pagina, long enAlerta, long totalGeneral) {
	}

	private static final String BASE = """
			WITH stock AS (
			    SELECT p.id_producto, p.sku, p.nombre_oficial, c.id_categoria, c.id_categoria_padre,
			           c.nombre AS categoria, p.unidad_medida, p.stock_minimo,
			           COALESCE(SUM(v.cantidad_fisica), 0)    AS fisico,
			           COALESCE(SUM(v.cantidad_reservada), 0) AS reservado
			      FROM producto p
			      JOIN categoria c ON c.id_categoria = p.id_categoria
			      LEFT JOIN inventario i ON i.id_producto = p.id_producto
			      LEFT JOIN v_disponibilidad_inventario v ON v.id_inventario = i.id_inventario
			     WHERE %s
			     GROUP BY p.id_producto, c.id_categoria
			), nivelado AS (
			    SELECT s.*, s.fisico - s.reservado AS libre,
			           CASE WHEN s.fisico - s.reservado <= 0 THEN 'critico'
			                WHEN s.fisico - s.reservado <= s.stock_minimo THEN 'minimo'
			                ELSE 'normal' END AS nivel
			      FROM stock s
			)
			""";

	private static final String BASE_ACTIVOS = BASE.formatted("p.estado_activo");
	private static final String BASE_UN_PRODUCTO = BASE.formatted("p.id_producto = :id");

	private final JdbcClient jdbc;

	public ConsultaInventarioService(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	/**
	 * Lista los productos activos con su disponibilidad, ordenados por SKU. El filtro de categoría incluye a sus
	 * subcategorías. Los totales {@code enAlerta} y {@code totalGeneral} no dependen de los filtros.
	 */
	@Transactional(readOnly = true)
	public Disponibilidad disponibilidad(String q, String sku, Long idCategoria, boolean soloAlerta, int page,
			int pageSize) {
		String filtros = """
				WHERE (CAST(:q AS text) IS NULL OR sku ILIKE '%' || CAST(:q AS text) || '%'
				       OR nombre_oficial ILIKE '%' || CAST(:q AS text) || '%')
				  AND (CAST(:sku AS text) IS NULL OR sku = CAST(:sku AS text))
				  AND (CAST(:categoria AS bigint) IS NULL OR id_categoria = CAST(:categoria AS bigint)
				       OR id_categoria_padre = CAST(:categoria AS bigint))
				  AND (NOT :soloAlerta OR nivel <> 'normal')
				""";
		String texto = q == null || q.isBlank() ? null : q.trim();
		String clave = sku == null || sku.isBlank() ? null : sku.trim();

		long total = jdbc.sql(BASE_ACTIVOS + "SELECT count(*) FROM nivelado " + filtros)
				.param("q", texto).param("sku", clave).param("categoria", idCategoria).param("soloAlerta", soloAlerta)
				.query(Long.class).single();
		List<ProductoExistencias> items = jdbc.sql(BASE_ACTIVOS + "SELECT * FROM nivelado " + filtros
						+ " ORDER BY sku LIMIT :limite OFFSET :desplazamiento")
				.param("q", texto).param("sku", clave).param("categoria", idCategoria).param("soloAlerta", soloAlerta)
				.param("limite", pageSize).param("desplazamiento", (long) (page - 1) * pageSize)
				.query((rs, n) -> new ProductoExistencias(rs.getLong("id_producto"), rs.getString("sku"),
						rs.getString("nombre_oficial"), rs.getString("categoria"), rs.getString("unidad_medida"),
						rs.getBigDecimal("stock_minimo"), rs.getBigDecimal("fisico"), rs.getBigDecimal("reservado"),
						rs.getBigDecimal("libre"), rs.getString("nivel"), List.of()))
				.list();
		Map<Long, List<ExistenciaUbicacion>> ubicaciones = ubicaciones(items.stream().map(ProductoExistencias::productoId).toList());
		List<ProductoExistencias> completos = items.stream().map(p -> conUbicaciones(p, ubicaciones)).toList();

		long[] totales = jdbc.sql(BASE_ACTIVOS + "SELECT count(*) AS total, count(*) FILTER (WHERE nivel <> 'normal') AS alerta FROM nivelado")
				.query((rs, n) -> new long[] {rs.getLong("total"), rs.getLong("alerta")}).single();
		return new Disponibilidad(new PageImpl<>(completos, PageRequest.of(page - 1, pageSize), total), totales[1],
				totales[0]);
	}

	/** Existencias de un producto, activo o no, consolidadas y por ubicación. */
	@Transactional(readOnly = true)
	public ProductoExistencias existencias(long idProducto) {
		ProductoExistencias producto = jdbc.sql(BASE_UN_PRODUCTO + "SELECT * FROM nivelado").param("id", idProducto)
				.query((rs, n) -> new ProductoExistencias(rs.getLong("id_producto"), rs.getString("sku"),
						rs.getString("nombre_oficial"), rs.getString("categoria"), rs.getString("unidad_medida"),
						rs.getBigDecimal("stock_minimo"), rs.getBigDecimal("fisico"), rs.getBigDecimal("reservado"),
						rs.getBigDecimal("libre"), rs.getString("nivel"), List.of()))
				.optional()
				.orElseThrow(() -> new ErrorDeNegocio(CodigoError.RECURSO_NO_ENCONTRADO, "El producto no existe."));
		return conUbicaciones(producto, ubicaciones(List.of(idProducto)));
	}

	/**
	 * Lista los productos activos en alerta. El orden es fijo: crítico antes que mínimo, mayor faltante primero y
	 * SKU ascendente.
	 *
	 * @param nivel {@code critico}, {@code minimo} o {@code null} para ambos
	 */
	@Transactional(readOnly = true)
	public Page<AlertaStock> alertas(String nivel, int page, int pageSize) {
		if (nivel != null && !nivel.equals("critico") && !nivel.equals("minimo")) {
			throw new ErrorDeNegocio(CodigoError.VALIDACION_FALLIDA, "El nivel debe ser critico o minimo.");
		}
		String filtro = "WHERE nivel <> 'normal' AND (CAST(:nivel AS text) IS NULL OR nivel = CAST(:nivel AS text)) ";
		long total = jdbc.sql(BASE_ACTIVOS + "SELECT count(*) FROM nivelado " + filtro).param("nivel", nivel)
				.query(Long.class).single();
		List<AlertaStock> items = jdbc.sql(BASE_ACTIVOS + "SELECT * FROM nivelado " + filtro
						+ " ORDER BY (nivel = 'critico') DESC, stock_minimo - libre DESC, sku LIMIT :limite OFFSET :desplazamiento")
				.param("nivel", nivel).param("limite", pageSize).param("desplazamiento", (long) (page - 1) * pageSize)
				.query((rs, n) -> new AlertaStock(rs.getLong("id_producto"), rs.getString("sku"),
						rs.getString("nombre_oficial"), rs.getString("unidad_medida"), rs.getBigDecimal("stock_minimo"),
						rs.getBigDecimal("libre"),
						rs.getBigDecimal("stock_minimo").subtract(rs.getBigDecimal("libre")).max(BigDecimal.ZERO),
						rs.getString("nivel")))
				.list();
		return new PageImpl<>(items, PageRequest.of(page - 1, pageSize), total);
	}

	/**
	 * Consulta el Kardex del más reciente al más antiguo. Todos los filtros son opcionales y combinables; el
	 * rango de fechas es inclusivo y se interpreta en la zona horaria de Lima.
	 */
	@Transactional(readOnly = true)
	public Page<MovimientoDetalle> kardex(String q, String sku, Long idUbicacion, String tipoOperacion, String origen,
			LocalDate desde, LocalDate hasta, int page, int pageSize) {
		String tipo = tipoOperacion == null ? null : normalizar(TipoOperacion.class, tipoOperacion).valor();
		String origenValor = origen == null ? null : normalizar(OrigenMovimiento.class, origen).valor();
		if (desde != null && hasta != null && hasta.isBefore(desde)) {
			throw new ErrorDeNegocio(CodigoError.VALIDACION_FALLIDA, "La fecha hasta no puede ser anterior a desde.");
		}
		OffsetDateTime inicio = desde == null ? null : desde.atStartOfDay(ZONA).toOffsetDateTime();
		OffsetDateTime fin = hasta == null ? null : hasta.plusDays(1).atStartOfDay(ZONA).toOffsetDateTime();
		String filtros = """
				WHERE (CAST(:q AS text) IS NULL OR p.sku ILIKE '%' || CAST(:q AS text) || '%'
				       OR p.nombre_oficial ILIKE '%' || CAST(:q AS text) || '%')
				  AND (CAST(:sku AS text) IS NULL OR p.sku = CAST(:sku AS text))
				  AND (CAST(:ubicacion AS bigint) IS NULL OR i.id_almacen = CAST(:ubicacion AS bigint))
				  AND (CAST(:tipo AS text) IS NULL OR m.tipo_operacion = CAST(:tipo AS text))
				  AND (CAST(:origen AS text) IS NULL OR m.origen_movimiento = CAST(:origen AS text))
				  AND (CAST(:desde AS timestamptz) IS NULL OR m.fecha_hora >= CAST(:desde AS timestamptz))
				  AND (CAST(:hasta AS timestamptz) IS NULL OR m.fecha_hora < CAST(:hasta AS timestamptz))
				""";
		String union = """
				  FROM movimiento_kardex m
				  JOIN inventario i ON i.id_inventario = m.id_inventario
				  JOIN producto p ON p.id_producto = i.id_producto
				""";
		String texto = q == null || q.isBlank() ? null : q.trim();
		String clave = sku == null || sku.isBlank() ? null : sku.trim();
		long total = jdbc.sql("SELECT count(*) " + union + filtros)
				.param("q", texto).param("sku", clave).param("ubicacion", idUbicacion).param("tipo", tipo)
				.param("origen", origenValor).param("desde", inicio).param("hasta", fin)
				.query(Long.class).single();
		List<MovimientoDetalle> items = jdbc.sql("""
				SELECT m.id_movimiento, m.fecha_hora, p.id_producto, p.sku, p.nombre_oficial, m.tipo_operacion,
				       m.cantidad, m.origen_movimiento, i.id_almacen, m.id_usuario_responsable, m.motivo,
				       m.id_guia, m.id_recepcion, m.referencia_traslado, m.id_movimiento_corregido
				""" + union + filtros + " ORDER BY m.fecha_hora DESC, m.id_movimiento DESC LIMIT :limite OFFSET :desplazamiento")
				.param("q", texto).param("sku", clave).param("ubicacion", idUbicacion).param("tipo", tipo)
				.param("origen", origenValor).param("desde", inicio).param("hasta", fin)
				.param("limite", pageSize).param("desplazamiento", (long) (page - 1) * pageSize)
				.query((rs, n) -> new MovimientoDetalle(rs.getLong("id_movimiento"),
						rs.getObject("fecha_hora", OffsetDateTime.class).toInstant(), rs.getLong("id_producto"),
						rs.getString("sku"), rs.getString("nombre_oficial"),
						TipoOperacion.de(rs.getString("tipo_operacion")), rs.getBigDecimal("cantidad"),
						OrigenMovimiento.de(rs.getString("origen_movimiento")), rs.getLong("id_almacen"),
						rs.getLong("id_usuario_responsable"), rs.getString("motivo"), nulable(rs.getObject("id_guia")),
						nulable(rs.getObject("id_recepcion")), (UUID) rs.getObject("referencia_traslado"),
						nulable(rs.getObject("id_movimiento_corregido"))))
				.list();
		return new PageImpl<>(items, PageRequest.of(page - 1, pageSize), total);
	}

	private static Long nulable(Object valor) {
		return valor == null ? null : ((Number) valor).longValue();
	}

	private static <E extends Enum<E>> E normalizar(Class<E> tipo, String valor) {
		try {
			return Enum.valueOf(tipo, valor.toUpperCase());
		} catch (IllegalArgumentException e) {
			throw new ErrorDeNegocio(CodigoError.VALIDACION_FALLIDA, "El valor '" + valor + "' no es válido.");
		}
	}

	private Map<Long, List<ExistenciaUbicacion>> ubicaciones(List<Long> idsProducto) {
		Map<Long, List<ExistenciaUbicacion>> porProducto = new LinkedHashMap<>();
		if (idsProducto.isEmpty()) {
			return porProducto;
		}
		jdbc.sql("""
				SELECT i.id_producto, a.id_almacen, a.nombre, v.cantidad_fisica, v.cantidad_reservada, v.cantidad_libre
				  FROM inventario i
				  JOIN almacen a ON a.id_almacen = i.id_almacen
				  JOIN v_disponibilidad_inventario v ON v.id_inventario = i.id_inventario
				 WHERE i.id_producto IN (:ids)
				 ORDER BY i.id_producto, (lower(a.tipo_ubicacion) NOT LIKE 'tienda%'), a.id_almacen
				""").param("ids", idsProducto)
				.query((rs, n) -> {
					porProducto.computeIfAbsent(rs.getLong("id_producto"), k -> new ArrayList<>())
							.add(new ExistenciaUbicacion(rs.getLong("id_almacen"), rs.getString("nombre"),
									rs.getBigDecimal("cantidad_fisica"), rs.getBigDecimal("cantidad_reservada"),
									rs.getBigDecimal("cantidad_libre")));
					return null;
				}).list();
		return porProducto;
	}

	private static ProductoExistencias conUbicaciones(ProductoExistencias p, Map<Long, List<ExistenciaUbicacion>> mapa) {
		return new ProductoExistencias(p.productoId(), p.sku(), p.nombre(), p.categoria(), p.unidad(), p.stockMinimo(),
				p.fisico(), p.reservado(), p.libre(), p.nivelAlerta(), mapa.getOrDefault(p.productoId(), List.of()));
	}
}
