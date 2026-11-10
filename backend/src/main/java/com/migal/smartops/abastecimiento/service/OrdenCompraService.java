package com.migal.smartops.abastecimiento.service;

import com.migal.smartops.abastecimiento.model.DetalleCompra;
import com.migal.smartops.abastecimiento.model.EstadoOrdenCompra;
import com.migal.smartops.abastecimiento.model.OrdenCompra;
import com.migal.smartops.abastecimiento.model.Proveedor;
import com.migal.smartops.abastecimiento.repository.OrdenCompraRepository;
import com.migal.smartops.abastecimiento.repository.ProveedorRepository;
import com.migal.smartops.catalogo.model.Producto;
import com.migal.smartops.catalogo.repository.ProductoRepository;
import com.migal.smartops.comun.errores.CodigoError;
import com.migal.smartops.comun.errores.ErrorDeNegocio;
import com.migal.smartops.comun.web.RespuestaProblema.ErrorCampo;
import com.migal.smartops.seguridad.model.Usuario;
import com.migal.smartops.seguridad.service.UsuarioService;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Órdenes de compra (CU-12). El registro y la modificación no modifican el inventario ni generan movimientos de Kardex.
 * El estado de cumplimiento lo recalculan las recepciones validadas; el cierre manual pasa una orden parcial a
 * completada. Las operaciones bloquean la orden antes de modificarla (ADR-010).
 */
@Service
public class OrdenCompraService {

	/** Zona horaria con la que se interpretan los rangos de fechas de los listados. */
	private static final ZoneId ZONA = ZoneId.of("America/Lima");
	private static final int LONGITUD_MAXIMA_MOTIVO = 500;

	/** Línea de una orden, identificada por el SKU del producto. */
	public record LineaOrden(String sku, BigDecimal cantidadComprada, BigDecimal costoUnitario) {
	}

	/** Datos para registrar una orden en estado pendiente. */
	public record DatosOrden(Long proveedorId, LocalDate fechaEntregaEstimada, String observaciones, String modalidadPago,
			List<LineaOrden> lineas) {
	}

	/** Datos para reemplazar las líneas y los datos generales de una orden pendiente. */
	public record DatosActualizacionOrden(LocalDate fechaEntregaEstimada, String observaciones, String modalidadPago,
			List<LineaOrden> lineas) {
	}

	/** Orden con la cantidad recibida de cada producto, indexada por identificador de producto. */
	public record OrdenDetalle(OrdenCompra orden, Map<Long, BigDecimal> recibidoPorProducto) {
	}

	private final OrdenCompraRepository ordenes;
	private final ProveedorRepository proveedores;
	private final ProductoRepository productos;
	private final UsuarioService usuarios;

	public OrdenCompraService(OrdenCompraRepository ordenes, ProveedorRepository proveedores, ProductoRepository productos,
			UsuarioService usuarios) {
		this.ordenes = ordenes;
		this.proveedores = proveedores;
		this.productos = productos;
		this.usuarios = usuarios;
	}

	/**
	 * Lista paginada de órdenes. {@code texto} busca por código o por razón social del proveedor; {@code estados}, por estado
	 * de cumplimiento; y {@code desde} y {@code hasta} delimitan la fecha de emisión de forma inclusiva.
	 */
	@Transactional(readOnly = true)
	public Page<OrdenDetalle> listar(String texto, Long proveedorId, Collection<EstadoOrdenCompra> estados, LocalDate desde,
			LocalDate hasta, Pageable paginacion) {
		String patron = texto == null || texto.isBlank() ? null : "%" + texto.trim().toLowerCase() + "%";
		Instant inicio = desde == null ? null : desde.atStartOfDay(ZONA).toInstant();
		Instant fin = hasta == null ? null : hasta.plusDays(1).atStartOfDay(ZONA).toInstant();
		Specification<OrdenCompra> filtros = (raiz, consulta, cb) -> {
			List<Predicate> condiciones = new ArrayList<>();
			if (patron != null) {
				condiciones.add(cb.or(cb.like(cb.lower(raiz.get("codigo")), patron),
						cb.like(cb.lower(raiz.get("proveedor").get("razonSocial")), patron)));
			}
			if (proveedorId != null) {
				condiciones.add(cb.equal(raiz.get("proveedor").get("id"), proveedorId));
			}
			if (estados != null && !estados.isEmpty()) {
				condiciones.add(raiz.get("estado").in(estados.stream().map(EstadoOrdenCompra::valor).toList()));
			}
			if (inicio != null) {
				condiciones.add(cb.greaterThanOrEqualTo(raiz.<Instant>get("fechaEmision"), inicio));
			}
			if (fin != null) {
				condiciones.add(cb.lessThan(raiz.<Instant>get("fechaEmision"), fin));
			}
			return cb.and(condiciones.toArray(Predicate[]::new));
		};
		return ordenes.findAll(filtros, paginacion).map(this::detalle);
	}

	/** Consulta una orden con sus líneas y la cantidad recibida de cada producto. Operación de solo lectura. */
	@Transactional(readOnly = true)
	public OrdenDetalle consultar(long id) {
		OrdenCompra orden = ordenes.findById(id).orElseThrow(OrdenCompraService::noEncontrada);
		return detalle(orden);
	}

	/**
	 * Registra una orden en estado pendiente. El proveedor debe existir y estar activo (PROVEEDOR_INACTIVO). Cada SKU debe
	 * existir en el catálogo maestro y no puede repetirse.
	 */
	@Transactional
	public OrdenDetalle registrar(long idUsuario, DatosOrden datos) {
		Proveedor proveedor = proveedores.findById(datos.proveedorId())
				.orElseThrow(() -> new ErrorDeNegocio(CodigoError.VALIDACION_FALLIDA, "El proveedor no existe."));
		if (!proveedor.isEstadoActivo()) {
			throw new ErrorDeNegocio(CodigoError.PROVEEDOR_INACTIVO, "El proveedor está inactivo y no admite nuevas órdenes.");
		}
		Usuario responsable = usuarios.consultar(idUsuario);
		OrdenCompra orden = new OrdenCompra(proveedor, responsable, datos.fechaEntregaEstimada(), datos.observaciones(),
				datos.modalidadPago());
		orden.reemplazarLineas(lineas(orden, datos.lineas()));
		return detalle(ordenes.saveAndFlush(orden));
	}

	/**
	 * Modifica una orden pendiente y reemplaza sus líneas. Una orden completada responde ORDEN_COMPRA_CERRADA; una orden
	 * parcial o con recepciones responde TRANSICION_NO_PERMITIDA.
	 */
	@Transactional
	public OrdenDetalle actualizar(long id, DatosActualizacionOrden datos) {
		OrdenCompra orden = bloquear(id);
		if (orden.getEstado() == EstadoOrdenCompra.COMPLETADA) {
			throw new ErrorDeNegocio(CodigoError.ORDEN_COMPRA_CERRADA, "La orden está completada y no puede modificarse.");
		}
		if (orden.getEstado() != EstadoOrdenCompra.PENDIENTE || ordenes.contarRecepciones(id) > 0) {
			throw new ErrorDeNegocio(CodigoError.TRANSICION_NO_PERMITIDA,
					"La orden tiene recepciones y no puede modificarse.");
		}
		orden.actualizar(datos.fechaEntregaEstimada(), datos.observaciones(), datos.modalidadPago());
		orden.reemplazarLineas(lineas(orden, datos.lineas()));
		return detalle(ordenes.saveAndFlush(orden));
	}

	/**
	 * Cierra una orden parcial y la registra como completada, aun cuando queden cantidades por recibir. Si quedan cantidades
	 * pendientes, el motivo es obligatorio y se anexa a las observaciones. No modifica el inventario.
	 */
	@Transactional
	public OrdenDetalle cerrar(long id, String motivo) {
		OrdenCompra orden = bloquear(id);
		if (orden.getEstado() == EstadoOrdenCompra.COMPLETADA) {
			throw new ErrorDeNegocio(CodigoError.ORDEN_COMPRA_CERRADA, "La orden ya está completada.");
		}
		if (orden.getEstado() == EstadoOrdenCompra.PENDIENTE) {
			throw new ErrorDeNegocio(CodigoError.TRANSICION_NO_PERMITIDA, "Una orden sin recepciones validadas no puede cerrarse.");
		}
		String texto = motivo == null ? "" : motivo.trim();
		if (texto.length() > LONGITUD_MAXIMA_MOTIVO) {
			throw new ErrorDeNegocio(CodigoError.VALIDACION_FALLIDA, "El motivo no puede superar 500 caracteres.");
		}
		boolean pendiente = cantidadPendiente(orden, recibidoPorProducto(id));
		if (pendiente) {
			if (texto.isEmpty()) {
				throw new ErrorDeNegocio(CodigoError.MOTIVO_REQUERIDO,
						"El motivo es obligatorio cuando quedan cantidades pendientes de recepción.");
			}
			orden.anexarObservacion("Cierre con cantidades pendientes de recepción: " + texto);
		}
		orden.cambiarEstado(EstadoOrdenCompra.COMPLETADA);
		return detalle(ordenes.saveAndFlush(orden));
	}

	/** Bloquea la orden hasta el fin de la transacción. Lo invocan las operaciones que cambian su estado. */
	@Transactional
	public OrdenCompra bloquear(long id) {
		return ordenes.bloquearPorId(id).orElseThrow(OrdenCompraService::noEncontrada);
	}

	/**
	 * Recalcula el estado de cumplimiento de la orden a partir de sus recepciones validadas: completada si todas sus líneas
	 * están recibidas en su totalidad; parcial si hay recepciones validadas; pendiente en otro caso. Debe invocarse con la
	 * orden bloqueada, después de registrar las recepciones.
	 */
	@Transactional
	public EstadoOrdenCompra recalcularCumplimiento(OrdenCompra orden) {
		Map<Long, BigDecimal> recibido = recibidoPorProducto(orden.getId());
		EstadoOrdenCompra estado;
		if (!cantidadPendiente(orden, recibido)) {
			estado = EstadoOrdenCompra.COMPLETADA;
		} else if (ordenes.contarRecepcionesValidadas(orden.getId()) > 0) {
			estado = EstadoOrdenCompra.PARCIAL;
		} else {
			estado = EstadoOrdenCompra.PENDIENTE;
		}
		orden.cambiarEstado(estado);
		return estado;
	}

	/** Cantidad recibida por producto en las recepciones validadas de la orden. */
	@Transactional(readOnly = true)
	public Map<Long, BigDecimal> recibidoPorProducto(long idOrden) {
		Map<Long, BigDecimal> recibido = new HashMap<>();
		for (Object[] fila : ordenes.recibidoPorProducto(idOrden)) {
			recibido.put((Long) fila[0], (BigDecimal) fila[1]);
		}
		return recibido;
	}

	/** Indica si alguna línea de la orden tiene cantidad pendiente de recepción. */
	private static boolean cantidadPendiente(OrdenCompra orden, Map<Long, BigDecimal> recibido) {
		return orden.getLineas().stream().anyMatch(l -> recibido.getOrDefault(l.getProducto().getId(), BigDecimal.ZERO)
				.compareTo(l.getCantidadComprada()) < 0);
	}

	private OrdenDetalle detalle(OrdenCompra orden) {
		return new OrdenDetalle(orden, recibidoPorProducto(orden.getId()));
	}

	/**
	 * Convierte las líneas enviadas en líneas de la orden. Valida que haya al menos una, que cada SKU exista en el catálogo
	 * maestro y que no se repita. Los errores se devuelven en la lista del problema.
	 */
	private List<DetalleCompra> lineas(OrdenCompra orden, List<LineaOrden> entradas) {
		if (entradas == null || entradas.isEmpty()) {
			throw new ErrorDeNegocio(CodigoError.VALIDACION_FALLIDA, "La orden debe tener al menos una línea.");
		}
		List<ErrorCampo> errores = new ArrayList<>();
		Set<String> vistos = new HashSet<>();
		List<DetalleCompra> resultado = new ArrayList<>();
		for (int i = 0; i < entradas.size(); i++) {
			LineaOrden entrada = entradas.get(i);
			String campo = "lineas[" + i + "].sku";
			if (!vistos.add(entrada.sku())) {
				errores.add(new ErrorCampo(campo, "SKU_DUPLICADO", "El SKU se repite en la orden."));
				continue;
			}
			Optional<Producto> producto = productos.findBySku(entrada.sku());
			if (producto.isEmpty()) {
				errores.add(new ErrorCampo(campo, "RECURSO_NO_ENCONTRADO", "El SKU no existe en el catálogo maestro."));
			} else {
				resultado.add(orden.nuevaLinea(producto.get(), entrada.cantidadComprada(), entrada.costoUnitario()));
			}
		}
		if (!errores.isEmpty()) {
			throw new ErrorDeNegocio(CodigoError.VALIDACION_FALLIDA, "La orden contiene líneas no válidas.",
					Map.of("errores", errores));
		}
		return resultado;
	}

	private static ErrorDeNegocio noEncontrada() {
		return new ErrorDeNegocio(CodigoError.RECURSO_NO_ENCONTRADO, "La orden de compra no existe.");
	}
}
