package com.migal.smartops.comercial.service;

import com.migal.smartops.catalogo.model.Producto;
import com.migal.smartops.catalogo.service.ProductoService;
import com.migal.smartops.comercial.model.Cliente;
import com.migal.smartops.comercial.model.Cotizacion;
import com.migal.smartops.comercial.model.DetalleCotizacion;
import com.migal.smartops.comercial.model.EstadoCotizacion;
import com.migal.smartops.comercial.model.OrigenRechazoCotizacion;
import com.migal.smartops.comercial.repository.CotizacionRepository;
import com.migal.smartops.comun.errores.CodigoError;
import com.migal.smartops.comun.errores.ErrorDeNegocio;
import com.migal.smartops.inventario.service.ConsultaInventarioService;
import com.migal.smartops.seguridad.model.RolUsuario;
import com.migal.smartops.seguridad.model.Usuario;
import com.migal.smartops.seguridad.service.UsuarioService;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ciclo de vida de las cotizaciones (CU-09). Una cotización no reserva ni modifica existencias. Antes de cada
 * operación, las cotizaciones emitidas, aceptadas o validadas cuyo vencimiento ya pasó se marcan como vencidas
 * (flujo alternativo 3). La validación de viabilidad consulta el stock libre del inventario (ADR-003).
 */
@Service
public class CotizacionService {

	private static final Logger REGISTRO = LoggerFactory.getLogger(CotizacionService.class);

	/** Zona horaria con la que se determinan las fechas de emisión y de vencimiento. */
	private static final ZoneId ZONA = ZoneId.of("America/Lima");
	private static final int VALIDEZ_MAXIMA_DIAS = 60;
	private static final int LONGITUD_MAXIMA_MOTIVO = 500;

	/** Línea solicitada en una cotización nueva o modificada. */
	public record LineaDatos(long productoId, BigDecimal cantidad, BigDecimal precioUnitario) {
	}

	/** Datos de una cotización. El descuento nulo equivale a cero y la validez se expresa en días. */
	public record DatosCotizacion(Long clienteId, List<LineaDatos> lineas, BigDecimal descuento, int validezDias,
			String condicionesComerciales) {
	}

	/** Cantidad de un producto que se traslada a la cotización nueva durante una división. */
	public record LineaDivision(long productoId, BigDecimal cantidad) {
	}

	/** Cotización original y cotización nueva resultantes de una división. */
	public record ResultadoDivision(Cotizacion original, Cotizacion nueva) {
	}

	private final CotizacionRepository cotizaciones;
	private final ClienteService clientes;
	private final ProductoService productos;
	private final UsuarioService usuarios;
	private final ConsultaInventarioService inventario;
	private final Clock reloj;

	public CotizacionService(CotizacionRepository cotizaciones, ClienteService clientes, ProductoService productos,
			UsuarioService usuarios, ConsultaInventarioService inventario, Clock reloj) {
		this.cotizaciones = cotizaciones;
		this.clientes = clientes;
		this.productos = productos;
		this.usuarios = usuarios;
		this.inventario = inventario;
		this.reloj = reloj;
	}

	/**
	 * Lista paginada de cotizaciones con su estado vigente. Los filtros son opcionales: {@code q} busca por nombre o
	 * razón social del cliente, {@code estados} acepta varios valores, y {@code desde} y {@code hasta} delimitan la
	 * fecha de emisión de forma inclusiva.
	 */
	@Transactional
	public Page<Cotizacion> listar(String q, Collection<EstadoCotizacion> estados, Long clienteId, LocalDate desde,
			LocalDate hasta, Pageable paginacion) {
		vencerVencidas();
		Instant inicio = desde == null ? null : desde.atStartOfDay(ZONA).toInstant();
		Instant fin = hasta == null ? null : hasta.plusDays(1).atStartOfDay(ZONA).toInstant();
		Specification<Cotizacion> filtros = (raiz, consulta, cb) -> {
			List<Predicate> condiciones = new ArrayList<>();
			if (q != null && !q.isBlank()) {
				condiciones.add(cb.like(cb.lower(raiz.get("cliente").get("nombreRazonSocial")),
						"%" + q.trim().toLowerCase() + "%"));
			}
			if (estados != null && !estados.isEmpty()) {
				condiciones.add(raiz.get("estado").in(estados.stream().map(EstadoCotizacion::valor).toList()));
			}
			if (clienteId != null) {
				condiciones.add(cb.equal(raiz.get("cliente").get("id"), clienteId));
			}
			if (inicio != null) {
				condiciones.add(cb.greaterThanOrEqualTo(raiz.<Instant>get("fechaEmision"), inicio));
			}
			if (fin != null) {
				condiciones.add(cb.lessThan(raiz.<Instant>get("fechaEmision"), fin));
			}
			return cb.and(condiciones.toArray(Predicate[]::new));
		};
		return cotizaciones.findAll(filtros, paginacion);
	}

	/** Consulta una cotización con sus líneas y su estado vigente. */
	@Transactional
	public Cotizacion consultar(long id) {
		vencerVencidas();
		return cotizaciones.findById(id).orElseThrow(CotizacionService::noEncontrada);
	}

	/**
	 * Emite una cotización en estado emitida. El vencimiento es la fecha de emisión más la validez en días.
	 */
	@Transactional
	public Cotizacion crear(DatosCotizacion datos, long idUsuario) {
		Cliente cliente = clienteDe(datos.clienteId());
		BigDecimal descuento = validarCuerpo(datos);
		Instant ahora = reloj.instant();
		Cotizacion nueva = new Cotizacion(cliente, usuarios.consultar(idUsuario), ahora,
				vencimiento(ahora, datos.validezDias()), datos.condicionesComerciales(), descuento);
		for (LineaDatos linea : datos.lineas()) {
			nueva.agregarLinea(productoVigente(linea.productoId()), linea.cantidad(), linea.precioUnitario());
		}
		return cotizaciones.saveAndFlush(nueva);
	}

	/**
	 * Modifica una cotización emitida. La fecha de emisión se conserva y la de vencimiento se recalcula con la validez
	 * indicada. Las líneas existentes se actualizan en su lugar para no repetir la clave del producto.
	 */
	@Transactional
	public Cotizacion actualizar(long id, DatosCotizacion datos) {
		vencerVencidas();
		Cotizacion cotizacion = bloquear(id);
		exigirVigenteYEmitida(cotizacion);
		Cliente cliente = clienteDe(datos.clienteId());
		BigDecimal descuento = validarCuerpo(datos);
		Set<Long> productosSolicitados = new HashSet<>();
		datos.lineas().forEach(linea -> productosSolicitados.add(linea.productoId()));
		for (DetalleCotizacion existente : List.copyOf(cotizacion.getLineas())) {
			if (!productosSolicitados.contains(existente.getProducto().getId())) {
				cotizacion.quitarLinea(existente);
			}
		}
		for (LineaDatos linea : datos.lineas()) {
			cotizacion.getLineas().stream()
					.filter(existente -> existente.getProducto().getId().equals(linea.productoId()))
					.findFirst()
					.ifPresentOrElse(
							existente -> existente.actualizar(linea.cantidad(), linea.precioUnitario()),
							() -> cotizacion.agregarLinea(productoVigente(linea.productoId()), linea.cantidad(),
									linea.precioUnitario()));
		}
		cotizacion.actualizarDatos(cliente, vencimiento(cotizacion.getFechaEmision(), datos.validezDias()),
				datos.condicionesComerciales(), descuento);
		return cotizaciones.saveAndFlush(cotizacion);
	}

	/** Registra la aceptación del cliente: emitida pasa a aceptada. No reserva existencias. */
	@Transactional
	public Cotizacion registrarAceptacion(long id) {
		vencerVencidas();
		Cotizacion cotizacion = bloquear(id);
		exigirVigenteYEmitida(cotizacion);
		cotizacion.registrarAceptacion(reloj.instant());
		return cotizaciones.saveAndFlush(cotizacion);
	}

	/**
	 * Registra la validación de viabilidad por MIGAL: aceptada pasa a validada. La realiza el Administrador, que
	 * representa a la propietaria del abastecimiento (ADR-008). Las líneas con faltante en categorías sin aprobación
	 * automática se registran en el log como confirmadas por esta operación.
	 */
	@Transactional
	public Cotizacion registrarValidacion(long id) {
		vencerVencidas();
		Cotizacion cotizacion = bloquear(id);
		if (cotizacion.getEstado() == EstadoCotizacion.VENCIDA) {
			throw vencida();
		}
		if (cotizacion.getEstado() != EstadoCotizacion.ACEPTADA) {
			throw noConvertible();
		}
		registrarViabilidad(cotizacion);
		cotizacion.registrarValidacion(reloj.instant());
		return cotizaciones.saveAndFlush(cotizacion);
	}

	/**
	 * Registra el rechazo de una cotización, que pasa a rechazada. Con origen cliente, la cotización debe estar emitida
	 * y pueden registrarlo el Vendedor y el Administrador. Con origen MIGAL, debe estar aceptada y solo lo registra el
	 * Administrador.
	 */
	@Transactional
	public Cotizacion registrarRechazo(long id, OrigenRechazoCotizacion origen, String motivo, long idUsuario,
			RolUsuario rol) {
		String texto = motivo == null ? "" : motivo.trim();
		if (texto.isEmpty() || texto.length() > LONGITUD_MAXIMA_MOTIVO) {
			throw validacion("El motivo del rechazo es obligatorio y no puede superar 500 caracteres.");
		}
		vencerVencidas();
		Cotizacion cotizacion = bloquear(id);
		if (origen == OrigenRechazoCotizacion.CLIENTE) {
			exigirEmitida(cotizacion);
		} else {
			if (rol != RolUsuario.ADMINISTRADOR) {
				throw new ErrorDeNegocio(CodigoError.PERMISO_INSUFICIENTE,
						"Solo el Administrador registra el rechazo de MIGAL.");
			}
			if (cotizacion.getEstado() != EstadoCotizacion.ACEPTADA) {
				throw noConvertible();
			}
		}
		cotizacion.registrarRechazo(origen, usuarios.consultar(idUsuario), reloj.instant(), texto);
		return cotizaciones.saveAndFlush(cotizacion);
	}

	/**
	 * Divide una cotización emitida para entregas escalonadas (ADR-003, regla 4). Las cantidades indicadas pasan a una
	 * cotización nueva con el mismo cliente, responsable, condiciones y fechas, sin descuento. La original conserva el
	 * resto y su descuento, que no puede superar el subtotal restante.
	 */
	@Transactional
	public ResultadoDivision dividir(long id, List<LineaDivision> lineas) {
		vencerVencidas();
		Cotizacion original = bloquear(id);
		exigirEmitida(original);
		if (lineas == null || lineas.isEmpty()) {
			throw validacion("La división requiere al menos una línea.");
		}
		Cotizacion nueva = new Cotizacion(original.getCliente(), original.getUsuarioResponsable(),
				original.getFechaEmision(), original.getFechaVencimiento(), original.getCondicionesComerciales(),
				BigDecimal.ZERO);
		Set<Long> vistos = new HashSet<>();
		for (LineaDivision division : lineas) {
			if (!vistos.add(division.productoId())) {
				throw validacion("Un producto no puede repetirse en la división.");
			}
			DetalleCotizacion linea = original.getLineas().stream()
					.filter(existente -> existente.getProducto().getId().equals(division.productoId()))
					.findFirst()
					.orElseThrow(() -> validacion("El producto no pertenece a la cotización."));
			BigDecimal cantidad = division.cantidad();
			if (cantidad == null || cantidad.signum() <= 0 || cantidad.compareTo(linea.getCantidadCotizada()) > 0) {
				throw validacion("La cantidad a dividir debe ser mayor que cero y no superar la cotizada.");
			}
			nueva.agregarLinea(linea.getProducto(), cantidad, linea.getPrecioUnitario());
			BigDecimal restante = linea.getCantidadCotizada().subtract(cantidad);
			if (restante.signum() == 0) {
				original.quitarLinea(linea);
			} else {
				linea.actualizar(restante, linea.getPrecioUnitario());
			}
		}
		if (original.getLineas().isEmpty()) {
			throw validacion("La cotización original debe conservar al menos una línea.");
		}
		if (original.getDescuento().compareTo(original.subtotal()) > 0) {
			throw validacion("El descuento de la cotización original supera su subtotal restante.");
		}
		Cotizacion guardada = cotizaciones.saveAndFlush(original);
		return new ResultadoDivision(guardada, cotizaciones.saveAndFlush(nueva));
	}

	/**
	 * Registra de forma explícita el vencimiento de una cotización cuya fecha ya se superó. Las cotizaciones validadas
	 * que originaron una venta no pueden vencerse.
	 */
	@Transactional
	public Cotizacion registrarVencimiento(long id) {
		Cotizacion cotizacion = bloquear(id);
		if (cotizacion.getEstado() == EstadoCotizacion.RECHAZADA || cotizacion.getEstado() == EstadoCotizacion.VENCIDA) {
			throw noEditable();
		}
		if (!hoy().isAfter(cotizacion.getFechaVencimiento())) {
			throw validacion("La fecha de vencimiento de la cotización aún no se ha superado.");
		}
		if (cotizacion.getEstado() == EstadoCotizacion.VALIDADA && cotizaciones.existeVenta(id)) {
			throw new ErrorDeNegocio(CodigoError.COTIZACION_YA_CONVERTIDA, "La cotización ya originó una venta.");
		}
		cotizacion.vencer();
		return cotizaciones.saveAndFlush(cotizacion);
	}

	/** Marca como vencidas las cotizaciones cuyo vencimiento ya pasó, salvo las que originaron una venta. */
	private void vencerVencidas() {
		cotizaciones.vencerVencidas(hoy());
	}

	/** Registra en el log las líneas cuyo stock libre no cubre la cantidad y requieren confirmación de abastecimiento. */
	private void registrarViabilidad(Cotizacion cotizacion) {
		for (DetalleCotizacion linea : cotizacion.getLineas()) {
			Producto producto = linea.getProducto();
			BigDecimal faltante = linea.getCantidadCotizada().subtract(inventario.existencias(producto.getId()).libre());
			if (faltante.signum() > 0 && !producto.getCategoria().isAprobacionAutomatica()) {
				REGISTRO.info("Cotización {}: el producto {} tiene faltante {} y su abastecimiento fue confirmado por el Administrador.",
						cotizacion.getId(), producto.getSku(), faltante);
			}
		}
	}

	/** Valida las líneas, la validez y el descuento. Devuelve el descuento, con cero cuando se omite. */
	private static BigDecimal validarCuerpo(DatosCotizacion datos) {
		if (datos.lineas() == null || datos.lineas().isEmpty()) {
			throw validacion("La cotización requiere al menos una línea.");
		}
		Set<Long> vistos = new HashSet<>();
		BigDecimal subtotal = BigDecimal.ZERO;
		for (LineaDatos linea : datos.lineas()) {
			if (linea.cantidad() == null || linea.cantidad().signum() <= 0) {
				throw validacion("La cantidad debe ser mayor que cero.");
			}
			if (linea.precioUnitario() == null || linea.precioUnitario().signum() < 0) {
				throw validacion("El precio unitario no puede ser negativo.");
			}
			if (!vistos.add(linea.productoId())) {
				throw validacion("Un producto no puede repetirse en la cotización.");
			}
			subtotal = subtotal.add(linea.cantidad().multiply(linea.precioUnitario()));
		}
		if (datos.validezDias() < 1 || datos.validezDias() > VALIDEZ_MAXIMA_DIAS) {
			throw validacion("La validez debe estar entre 1 y 60 días.");
		}
		BigDecimal descuento = datos.descuento() == null ? BigDecimal.ZERO : datos.descuento();
		if (descuento.signum() < 0 || descuento.compareTo(subtotal.setScale(2, RoundingMode.HALF_UP)) > 0) {
			throw validacion("El descuento no puede ser negativo ni superar el subtotal de las líneas.");
		}
		return descuento.setScale(2, RoundingMode.HALF_UP);
	}

	/** Verifica que el cliente exista y esté activo. Un cliente nulo responde {@code CLIENTE_REQUERIDO}. */
	private Cliente clienteDe(Long idCliente) {
		if (idCliente == null) {
			throw new ErrorDeNegocio(CodigoError.CLIENTE_REQUERIDO, "La cotización requiere un cliente.");
		}
		return clientes.validarVigente(idCliente);
	}

	private Producto productoVigente(long idProducto) {
		try {
			return productos.consultar(idProducto);
		} catch (ErrorDeNegocio e) {
			throw validacion("El producto indicado no existe.");
		}
	}

	private Cotizacion bloquear(long id) {
		return cotizaciones.bloquearPorId(id).orElseThrow(CotizacionService::noEncontrada);
	}

	/** Exige el estado emitida. Cualquier otro estado responde no editable. */
	private static void exigirEmitida(Cotizacion cotizacion) {
		if (cotizacion.getEstado() != EstadoCotizacion.EMITIDA) {
			throw noEditable();
		}
	}

	/** Exige el estado emitida, distinguiendo una cotización vencida, que responde {@code COTIZACION_VENCIDA}. */
	private static void exigirVigenteYEmitida(Cotizacion cotizacion) {
		if (cotizacion.getEstado() == EstadoCotizacion.VENCIDA) {
			throw vencida();
		}
		exigirEmitida(cotizacion);
	}

	private static ErrorDeNegocio vencida() {
		return new ErrorDeNegocio(CodigoError.COTIZACION_VENCIDA, "La cotización venció y ya no admite esta operación.");
	}

	private LocalDate hoy() {
		return reloj.instant().atZone(ZONA).toLocalDate();
	}

	private static LocalDate vencimiento(Instant emision, int validezDias) {
		return emision.atZone(ZONA).toLocalDate().plusDays(validezDias);
	}

	private static ErrorDeNegocio validacion(String detalle) {
		return new ErrorDeNegocio(CodigoError.VALIDACION_FALLIDA, detalle);
	}

	private static ErrorDeNegocio noConvertible() {
		return new ErrorDeNegocio(CodigoError.COTIZACION_NO_CONVERTIBLE,
				"La cotización no está en el estado requerido para esta operación.");
	}

	private static ErrorDeNegocio noEncontrada() {
		return new ErrorDeNegocio(CodigoError.RECURSO_NO_ENCONTRADO, "La cotización no existe.");
	}

	private static ErrorDeNegocio noEditable() {
		return new ErrorDeNegocio(CodigoError.COTIZACION_NO_EDITABLE, "La cotización no admite esta operación.");
	}
}
