package com.migal.smartops.recepciones.service;

import com.migal.smartops.abastecimiento.model.EstadoOrdenCompra;
import com.migal.smartops.abastecimiento.model.OrdenCompra;
import com.migal.smartops.abastecimiento.service.OrdenCompraService;
import com.migal.smartops.abastecimiento.service.OrdenCompraService.OrdenDetalle;
import com.migal.smartops.catalogo.model.Almacen;
import com.migal.smartops.catalogo.model.Producto;
import com.migal.smartops.catalogo.repository.AlmacenRepository;
import com.migal.smartops.catalogo.service.ProductoService;
import com.migal.smartops.comun.errores.CodigoError;
import com.migal.smartops.comun.errores.ErrorDeNegocio;
import com.migal.smartops.inventario.service.ConsultaInventarioService;
import com.migal.smartops.inventario.service.ConsultaInventarioService.ExistenciaUbicacion;
import com.migal.smartops.inventario.service.MovimientoDetalle;
import com.migal.smartops.inventario.service.ReservaInventarioService;
import com.migal.smartops.inventario.service.ReservaInventarioService.EntradaRecepcion;
import com.migal.smartops.recepciones.model.EstadoRecepcion;
import com.migal.smartops.recepciones.model.Recepcion;
import com.migal.smartops.recepciones.repository.RecepcionRepository;
import com.migal.smartops.seguridad.model.Usuario;
import com.migal.smartops.seguridad.service.UsuarioService;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Recepciones de mercadería contra una orden de compra (CU-13). El registro y la modificación no modifican el inventario.
 * La validación es una sola transacción: registra las entradas de Kardex mediante {@link ReservaInventarioService}, pasa la
 * recepción a validada y recalcula el estado de cumplimiento de la orden. Bloquea la orden antes que la recepción, y la
 * recepción antes que las existencias (ADR-010). Ante interbloqueo o bloqueo fallido reintenta la transacción completa un
 * número limitado de veces.
 */
@Service
public class RecepcionService {

	/** Zona horaria con la que se interpretan los rangos de fechas de los listados. */
	private static final ZoneId ZONA = ZoneId.of("America/Lima");
	/** Número máximo de intentos de la transacción de validación ante bloqueos. */
	private static final int INTENTOS = 3;

	/** Línea solicitada: producto, ubicación de destino y cantidad recibida. */
	public record LineaRecepcion(long productoId, long ubicacionId, BigDecimal cantidad) {
	}

	/** Datos para registrar una recepción pendiente, con precarga opcional desde un documento validado. */
	public record DatosRecepcion(long ordenCompraId, LocalDate fechaRecepcion, String observaciones, Long documentoId,
			Long ubicacionPrecargaId, List<LineaRecepcion> lineas) {
	}

	/** Datos para reemplazar la fecha, las observaciones y las líneas de una recepción pendiente. */
	public record DatosActualizacionRecepcion(LocalDate fechaRecepcion, String observaciones, List<LineaRecepcion> lineas) {
	}

	/** Recepción con su orden de compra en el estado vigente y, opcionalmente, el documento de precarga. */
	public record RecepcionDetalle(Recepcion recepcion, Long documentoId, OrdenDetalle orden) {
	}

	/** Fila del listado de recepciones. */
	public record RecepcionResumen(long id, String codigo, long ordenCompraId, String codigoOrdenCompra, String proveedor,
			EstadoRecepcion estado, LocalDate fechaRecepcion, Instant fechaValidacion, long usuarioResponsableId) {
	}

	/** Existencia antes y después de la validación, por producto y ubicación. */
	public record CambioExistencia(long productoId, String sku, long ubicacionId, String unidad, BigDecimal antes,
			BigDecimal despues) {
	}

	/** Resultado de la validación: la recepción, sus movimientos de Kardex, las existencias y la orden actualizada. */
	public record ResultadoValidacion(RecepcionDetalle recepcion, List<MovimientoDetalle> movimientos,
			List<CambioExistencia> cambios, OrdenDetalle ordenCompra) {
	}

	/** Documento OCR con el que se precarga una recepción. */
	private record DocumentoFila(String estado, Long idCotizacion, Long idRecepcion, boolean eliminado) {
	}

	private final RecepcionRepository recepciones;
	private final OrdenCompraService ordenes;
	private final ProductoService productos;
	private final AlmacenRepository almacenes;
	private final UsuarioService usuarios;
	private final ReservaInventarioService reservas;
	private final ConsultaInventarioService existencias;
	private final JdbcClient jdbc;
	private final TransactionTemplate transaccion;
	private final Clock reloj;

	public RecepcionService(RecepcionRepository recepciones, OrdenCompraService ordenes, ProductoService productos,
			AlmacenRepository almacenes, UsuarioService usuarios, ReservaInventarioService reservas,
			ConsultaInventarioService existencias, JdbcClient jdbc, PlatformTransactionManager transacciones, Clock reloj) {
		this.recepciones = recepciones;
		this.ordenes = ordenes;
		this.productos = productos;
		this.almacenes = almacenes;
		this.usuarios = usuarios;
		this.reservas = reservas;
		this.existencias = existencias;
		this.jdbc = jdbc;
		this.transaccion = new TransactionTemplate(transacciones);
		this.reloj = reloj;
	}

	/** Lista paginada de recepciones, filtrada por orden, estado y fecha de recepción de forma inclusiva. */
	@Transactional(readOnly = true)
	public Page<RecepcionResumen> listar(Long idOrden, Collection<EstadoRecepcion> estados, LocalDate desde, LocalDate hasta,
			Pageable paginacion) {
		Instant inicio = desde == null ? null : desde.atStartOfDay(ZONA).toInstant();
		Instant fin = hasta == null ? null : hasta.plusDays(1).atStartOfDay(ZONA).toInstant();
		Specification<Recepcion> filtros = (raiz, consulta, cb) -> {
			List<Predicate> condiciones = new ArrayList<>();
			if (idOrden != null) {
				condiciones.add(cb.equal(raiz.get("orden").get("id"), idOrden));
			}
			if (estados != null && !estados.isEmpty()) {
				condiciones.add(raiz.get("estado").in(estados.stream().map(EstadoRecepcion::valor).toList()));
			}
			if (desde != null) {
				condiciones.add(cb.greaterThanOrEqualTo(raiz.get("fechaRecepcion"), desde));
			}
			if (hasta != null) {
				condiciones.add(cb.lessThanOrEqualTo(raiz.get("fechaRecepcion"), hasta));
			}
			return cb.and(condiciones.toArray(Predicate[]::new));
		};
		return recepciones.findAll(filtros, paginacion).map(RecepcionService::resumen);
	}

	/** Consulta una recepción con sus líneas, su documento de precarga y la orden en su estado vigente. */
	@Transactional(readOnly = true)
	public RecepcionDetalle consultar(long id) {
		return detalle(recepciones.findById(id).orElseThrow(RecepcionService::noEncontrada));
	}

	/**
	 * Registra una recepción pendiente contra una orden pendiente o parcial. Con {@code documentoId}, precarga las líneas
	 * desde el documento validado y las asigna a {@code ubicacionPrecargaId}; las líneas enviadas prevalecen sobre la
	 * precarga para los productos que contengan.
	 */
	@Transactional
	public RecepcionDetalle registrar(long idUsuario, DatosRecepcion datos) {
		OrdenCompra orden = ordenes.bloquear(datos.ordenCompraId());
		if (orden.getEstado() == EstadoOrdenCompra.COMPLETADA) {
			throw new ErrorDeNegocio(CodigoError.ORDEN_COMPRA_CERRADA, "La orden de compra está completada.");
		}
		DocumentoFila documento = datos.documentoId() == null ? null : documentoValidado(datos.documentoId());
		List<LineaRecepcion> solicitadas = datos.lineas() == null ? List.of() : datos.lineas();
		List<LineaRecepcion> lineas = new ArrayList<>(solicitadas);
		if (datos.documentoId() != null) {
			if (datos.ubicacionPrecargaId() == null) {
				throw validacion("La precarga desde documento exige la ubicación de destino.");
			}
			Set<Long> explicitas = new HashSet<>();
			solicitadas.forEach(l -> explicitas.add(l.productoId()));
			precarga(datos.documentoId(), datos.ubicacionPrecargaId()).stream()
					.filter(l -> !explicitas.contains(l.productoId())).forEach(lineas::add);
		}
		if (lineas.isEmpty()) {
			throw validacion("La recepción debe tener al menos una línea.");
		}
		Usuario responsable = usuarios.consultar(idUsuario);
		Recepcion recepcion = new Recepcion(orden, responsable, datos.fechaRecepcion(), datos.observaciones());
		recepcion.reemplazarLineas(nuevasLineas(lineas));
		recepciones.saveAndFlush(recepcion);
		if (documento != null) {
			jdbc.sql("UPDATE documento_digital SET id_recepcion = :recepcion WHERE id_documento = :documento")
					.param("recepcion", recepcion.getId()).param("documento", datos.documentoId()).update();
		}
		return detalle(recepcion);
	}

	/** Reemplaza la fecha, las observaciones y las líneas de una recepción pendiente. Una validada responde TRANSICION_NO_PERMITIDA. */
	@Transactional
	public RecepcionDetalle actualizar(long id, DatosActualizacionRecepcion datos) {
		Recepcion recepcion = recepciones.bloquearPorId(id).orElseThrow(RecepcionService::noEncontrada);
		if (recepcion.getEstado() != EstadoRecepcion.PENDIENTE) {
			throw new ErrorDeNegocio(CodigoError.TRANSICION_NO_PERMITIDA, "La recepción validada no puede modificarse.");
		}
		recepcion.actualizar(datos.fechaRecepcion(), datos.observaciones());
		recepcion.reemplazarLineas(nuevasLineas(datos.lineas()));
		recepciones.saveAndFlush(recepcion);
		return detalle(recepcion);
	}

	/**
	 * Valida una recepción pendiente. Si {@code reemplazarObservaciones} es verdadero, las observaciones enviadas sustituyen
	 * a las de la recepción antes de validar. Los errores de bloqueo se reintentan con la transacción completa.
	 */
	public ResultadoValidacion validar(long idUsuario, long id, String observaciones, boolean reemplazarObservaciones) {
		return conReintento(() -> transaccion.execute(estado ->
				validarEnTransaccion(idUsuario, id, observaciones, reemplazarObservaciones)));
	}

	private ResultadoValidacion validarEnTransaccion(long idUsuario, long id, String observaciones,
			boolean reemplazarObservaciones) {
		// Se lee solo el identificador de la orden: cargar la recepción antes de bloquearla dejaría una copia desactualizada.
		long idOrden = recepciones.idOrdenDeRecepcion(id).orElseThrow(RecepcionService::noEncontrada);
		OrdenCompra orden = ordenes.bloquear(idOrden);
		Recepcion recepcion = recepciones.bloquearPorId(id).orElseThrow(RecepcionService::noEncontrada);
		if (recepcion.getEstado() != EstadoRecepcion.PENDIENTE) {
			throw new ErrorDeNegocio(CodigoError.RECEPCION_NO_CONFIRMABLE, "La recepción no está pendiente y no puede validarse.");
		}
		if (orden.getEstado() == EstadoOrdenCompra.COMPLETADA) {
			throw new ErrorDeNegocio(CodigoError.ORDEN_COMPRA_CERRADA, "La orden de compra está completada.");
		}
		if (reemplazarObservaciones) {
			recepcion.actualizar(recepcion.getFechaRecepcion(), observaciones == null || observaciones.isBlank() ? null
					: observaciones.trim());
		}
		List<LineaRecepcion> lineas = lineasDe(recepcion);
		exigirSinSobrante(orden, lineas, recepcion.getObservaciones());
		List<EntradaRecepcion> entradas = lineas.stream().filter(l -> l.cantidad().signum() > 0)
				.map(l -> new EntradaRecepcion(l.productoId(), l.ubicacionId(), l.cantidad())).toList();
		if (entradas.isEmpty()) {
			throw validacion("Ninguna línea tiene una cantidad recibida mayor que cero.");
		}
		List<MovimientoDetalle> movimientos = reservas.registrarEntradas(idUsuario, recepcion.getId(), entradas);
		List<CambioExistencia> cambios = cambios(entradas, movimientos);
		recepcion.validar(reloj.instant());
		recepciones.saveAndFlush(recepcion);
		ordenes.recalcularCumplimiento(orden);
		return new ResultadoValidacion(detalle(recepcion), movimientos, cambios, ordenes.consultar(idOrden));
	}

	/**
	 * Verifica que ninguna cantidad supere la pendiente de su producto, salvo que la recepción tenga observaciones que
	 * expliquen el sobrante. Un producto ajeno a la orden tiene pendiente cero.
	 */
	private void exigirSinSobrante(OrdenCompra orden, List<LineaRecepcion> lineas, String observaciones) {
		Map<Long, BigDecimal> recibido = ordenes.recibidoPorProducto(orden.getId());
		Map<Long, BigDecimal> recibidoEnRecepcion = new HashMap<>();
		lineas.forEach(l -> recibidoEnRecepcion.merge(l.productoId(), l.cantidad(), BigDecimal::add));
		boolean sinNota = observaciones == null || observaciones.isBlank();
		for (var entrada : recibidoEnRecepcion.entrySet()) {
			BigDecimal pendiente = orden.getLineas().stream()
					.filter(l -> l.getProducto().getId().equals(entrada.getKey()))
					.findFirst()
					.map(l -> l.getCantidadComprada().subtract(recibido.getOrDefault(entrada.getKey(), BigDecimal.ZERO))
							.max(BigDecimal.ZERO))
					.orElse(BigDecimal.ZERO);
			if (entrada.getValue().compareTo(pendiente) > 0 && sinNota) {
				throw new ErrorDeNegocio(CodigoError.SOBRANTE_SIN_NOTA,
						"La cantidad recibida supera la pendiente de la orden y la recepción no tiene observaciones.");
			}
		}
	}

	/** Existencia antes y después por producto y ubicación. Las existencias ya incluyen las entradas registradas. */
	private List<CambioExistencia> cambios(List<EntradaRecepcion> entradas, List<MovimientoDetalle> movimientos) {
		Map<String, BigDecimal> sumas = new LinkedHashMap<>();
		for (MovimientoDetalle movimiento : movimientos) {
			sumas.merge(movimiento.productoId() + ":" + movimiento.ubicacionId(), movimiento.cantidad(), BigDecimal::add);
		}
		List<CambioExistencia> cambios = new ArrayList<>();
		for (EntradaRecepcion entrada : entradas) {
			String clave = entrada.idProducto() + ":" + entrada.idUbicacion();
			if (!sumas.containsKey(clave)) {
				continue;
			}
			var producto = existencias.existencias(entrada.idProducto());
			ExistenciaUbicacion ubicacion = producto.ubicaciones().stream()
					.filter(u -> u.ubicacionId() == entrada.idUbicacion()).findFirst().orElseThrow();
			cambios.add(new CambioExistencia(entrada.idProducto(), producto.sku(), entrada.idUbicacion(), producto.unidad(),
					ubicacion.fisica().subtract(sumas.remove(clave)), ubicacion.fisica()));
		}
		return cambios;
	}

	/** Líneas de la recepción en forma de solicitud, para recalcular su contenido. */
	private static List<LineaRecepcion> lineasDe(Recepcion recepcion) {
		return recepcion.getLineas().stream()
				.map(l -> new LineaRecepcion(l.getProducto().getId(), l.getAlmacen().getId(), l.getCantidadRecibida())).toList();
	}

	/** Convierte las líneas solicitadas en entidades validadas: producto y ubicación activos y sin combinaciones repetidas. */
	private List<Recepcion.NuevaLinea> nuevasLineas(List<LineaRecepcion> lineas) {
		if (lineas == null || lineas.isEmpty()) {
			throw validacion("La recepción debe tener al menos una línea.");
		}
		Set<String> vistas = new HashSet<>();
		List<Recepcion.NuevaLinea> resultado = new ArrayList<>();
		for (LineaRecepcion linea : lineas) {
			if (linea.cantidad() == null || linea.cantidad().signum() < 0) {
				throw validacion("La cantidad recibida no puede ser negativa.");
			}
			if (!vistas.add(linea.productoId() + ":" + linea.ubicacionId())) {
				throw validacion("Un producto no puede repetirse en la misma ubicación.");
			}
			Producto producto = productos.consultar(linea.productoId());
			if (!producto.isEstadoActivo()) {
				throw new ErrorDeNegocio(CodigoError.PRODUCTO_INACTIVO, "El producto está inactivo.");
			}
			Almacen almacen = almacenes.findById(linea.ubicacionId())
					.orElseThrow(() -> new ErrorDeNegocio(CodigoError.RECURSO_NO_ENCONTRADO, "La ubicación no existe."));
			if (!almacen.isEstadoActivo()) {
				throw new ErrorDeNegocio(CodigoError.ALMACEN_INACTIVO, "La ubicación está inactiva.");
			}
			resultado.add(new Recepcion.NuevaLinea(producto, almacen, linea.cantidad()));
		}
		return resultado;
	}

	/** Lee las líneas del documento validado: productos confirmados y cantidad corregida o leída, sin elementos descartados. */
	private List<LineaRecepcion> precarga(long idDocumento, long ubicacion) {
		List<Object[]> filas = jdbc.sql("""
				SELECT id_producto, COALESCE(cantidad_corregida, cantidad_leida) AS cantidad
				  FROM detalle_extraido
				 WHERE id_documento = :documento AND descartado = false
				 ORDER BY id_detalle_extraido
				""").param("documento", idDocumento).query((rs, n) -> new Object[] {rs.getObject("id_producto"),
				rs.getBigDecimal("cantidad")}).list();
		List<LineaRecepcion> lineas = new ArrayList<>();
		for (Object[] fila : filas) {
			if (fila[0] == null || fila[1] == null) {
				throw validacion("El documento contiene elementos sin producto confirmado o sin cantidad.");
			}
			lineas.add(new LineaRecepcion(((Number) fila[0]).longValue(), ubicacion, (BigDecimal) fila[1]));
		}
		return lineas;
	}

	private DocumentoFila documentoValidado(long idDocumento) {
		DocumentoFila documento = jdbc.sql("""
				SELECT estado_procesamiento, id_cotizacion, id_recepcion, fecha_eliminacion
				  FROM documento_digital WHERE id_documento = :documento
				""").param("documento", idDocumento)
				.query((rs, n) -> new DocumentoFila(rs.getString("estado_procesamiento"), rs.getObject("id_cotizacion") == null
						? null : rs.getLong("id_cotizacion"), rs.getObject("id_recepcion") == null ? null
						: rs.getLong("id_recepcion"), rs.getObject("fecha_eliminacion") != null))
				.optional()
				.orElseThrow(() -> new ErrorDeNegocio(CodigoError.RECURSO_NO_ENCONTRADO, "El documento no existe."));
		boolean utilizable = "validado".equals(documento.estado()) && documento.idCotizacion() == null
				&& documento.idRecepcion() == null && !documento.eliminado();
		if (!utilizable) {
			throw new ErrorDeNegocio(CodigoError.DOCUMENTO_NO_VALIDADO,
					"El documento no está validado o ya apoya otra operación.");
		}
		return documento;
	}

	/** Compone la recepción con su documento de precarga y la orden vigente. Inicializa las líneas dentro de la transacción. */
	private RecepcionDetalle detalle(Recepcion recepcion) {
		recepcion.getLineas().size();
		recepcion.getOrden().getProveedor().getRazonSocial();
		Long documento = jdbc.sql("SELECT id_documento FROM documento_digital WHERE id_recepcion = :recepcion")
				.param("recepcion", recepcion.getId()).query(Long.class).optional().orElse(null);
		return new RecepcionDetalle(recepcion, documento, ordenes.consultar(recepcion.getOrden().getId()));
	}

	private static RecepcionResumen resumen(Recepcion recepcion) {
		return new RecepcionResumen(recepcion.getId(), recepcion.getCodigo(), recepcion.getOrden().getId(),
				recepcion.getOrden().getCodigo(), recepcion.getOrden().getProveedor().getRazonSocial(), recepcion.getEstado(),
				recepcion.getFechaRecepcion(), recepcion.getFechaValidacion(), recepcion.getUsuarioResponsable().getId());
	}

	/**
	 * Ejecuta la operación y la repite cuando el motor de base de datos aborta la transacción por interbloqueo o no
	 * puede adquirir un bloqueo. Agotados los intentos, responde PETICION_CONCURRENTE.
	 */
	private static <T> T conReintento(Supplier<T> operacion) {
		for (int intento = 1;; intento++) {
			try {
				return operacion.get();
			} catch (PessimisticLockingFailureException e) {
				if (intento >= INTENTOS) {
					throw new ErrorDeNegocio(CodigoError.PETICION_CONCURRENTE,
							"La operación compite con otra. Intente nuevamente.");
				}
			}
		}
	}

	private static ErrorDeNegocio noEncontrada() {
		return new ErrorDeNegocio(CodigoError.RECURSO_NO_ENCONTRADO, "La recepción no existe.");
	}

	private static ErrorDeNegocio validacion(String detalle) {
		return new ErrorDeNegocio(CodigoError.VALIDACION_FALLIDA, detalle);
	}
}
