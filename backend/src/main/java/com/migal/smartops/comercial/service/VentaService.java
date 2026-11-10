package com.migal.smartops.comercial.service;

import com.migal.smartops.catalogo.model.Producto;
import com.migal.smartops.catalogo.service.ProductoService;
import com.migal.smartops.comercial.model.Cliente;
import com.migal.smartops.comercial.model.Cotizacion;
import com.migal.smartops.comercial.model.DetalleVenta;
import com.migal.smartops.comercial.model.EstadoCotizacion;
import com.migal.smartops.comercial.model.EstadoVenta;
import com.migal.smartops.comercial.model.Venta;
import com.migal.smartops.comercial.repository.CotizacionRepository;
import com.migal.smartops.comercial.repository.VentaRepository;
import com.migal.smartops.comun.errores.CodigoError;
import com.migal.smartops.comun.errores.ErrorDeNegocio;
import com.migal.smartops.despacho.service.DespachoService;
import com.migal.smartops.inventario.model.ReservaStock;
import com.migal.smartops.inventario.service.ReservaInventarioService;
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
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ciclo de vida de las ventas (CU-10, ADR-003). Una venta solo reserva existencias: no modifica el stock físico ni
 * genera movimientos de Kardex. Cada operación que cambia reservas o estado se ejecuta en una única transacción, de
 * modo que la venta, sus líneas y sus reservas se confirman o se revierten juntas. Las líneas se bloquean en orden
 * ascendente de producto para evitar interbloqueos entre conversiones concurrentes (ADR-010).
 */
@Service
public class VentaService {

	/** Zona horaria con la que se interpretan los rangos de fechas del listado. */
	private static final ZoneId ZONA = ZoneId.of("America/Lima");
	private static final int LONGITUD_MAXIMA_MOTIVO = 500;

	/** Cobertura de una línea: cantidad reservada actual y cantidad pendiente de abastecimiento. */
	public record Cobertura(BigDecimal reservada, BigDecimal pendiente) {
	}

	/** Reserva de una línea, con los datos de la ubicación necesarios para la respuesta. */
	public record ReservaVista(long id, long idLinea, long idProducto, long idUbicacion, String nombreUbicacion,
			BigDecimal cantidad, String estado, Instant fechaReserva, Instant fechaCierre) {
	}

	/** Venta con la cobertura de cada línea (por identificador de línea) y sus reservas. */
	public record VentaDetalle(Venta venta, Map<Long, Cobertura> coberturaPorLinea, List<ReservaVista> reservas) {
	}

	/** Producto y cantidad de una venta directa de mostrador. */
	public record LineaDirecta(long productoId, BigDecimal cantidad, BigDecimal precioUnitario) {
	}

	/** Datos de una venta directa. Sin fecha de vencimiento, la cotización vence en la fecha de la operación. */
	public record DatosVentaDirecta(Long clienteId, BigDecimal descuento, String condicionesComerciales,
			LocalDate fechaVencimiento, List<LineaDirecta> lineas) {
	}

	private final VentaRepository ventas;
	private final CotizacionRepository cotizaciones;
	private final ClienteService clientes;
	private final ProductoService productos;
	private final UsuarioService usuarios;
	private final ReservaInventarioService reservasInventario;
	private final DespachoService despacho;
	private final Clock reloj;

	public VentaService(VentaRepository ventas, CotizacionRepository cotizaciones, ClienteService clientes,
			ProductoService productos, UsuarioService usuarios, ReservaInventarioService reservasInventario,
			DespachoService despacho, Clock reloj) {
		this.ventas = ventas;
		this.cotizaciones = cotizaciones;
		this.clientes = clientes;
		this.productos = productos;
		this.usuarios = usuarios;
		this.reservasInventario = reservasInventario;
		this.despacho = despacho;
		this.reloj = reloj;
	}

	/**
	 * Lista paginada de ventas. Los filtros son opcionales y combinables: {@code estados} acepta varios valores,
	 * {@code clienteId} filtra por cliente, y {@code desde} y {@code hasta} delimitan la fecha de venta de forma inclusiva.
	 */
	@Transactional(readOnly = true)
	public Page<Venta> listar(Collection<EstadoVenta> estados, Long clienteId, LocalDate desde, LocalDate hasta,
			Pageable paginacion) {
		Instant inicio = desde == null ? null : desde.atStartOfDay(ZONA).toInstant();
		Instant fin = hasta == null ? null : hasta.plusDays(1).atStartOfDay(ZONA).toInstant();
		Specification<Venta> filtros = (raiz, consulta, cb) -> {
			List<Predicate> condiciones = new ArrayList<>();
			if (estados != null && !estados.isEmpty()) {
				condiciones.add(raiz.get("estado").in(estados.stream().map(EstadoVenta::valor).toList()));
			}
			if (clienteId != null) {
				condiciones.add(cb.equal(raiz.get("cliente").get("id"), clienteId));
			}
			if (inicio != null) {
				condiciones.add(cb.greaterThanOrEqualTo(raiz.<Instant>get("fechaVenta"), inicio));
			}
			if (fin != null) {
				condiciones.add(cb.lessThan(raiz.<Instant>get("fechaVenta"), fin));
			}
			return cb.and(condiciones.toArray(Predicate[]::new));
		};
		return ventas.findAll(filtros, paginacion);
	}

	/** Consulta una venta con sus líneas, su cobertura y sus reservas, incluidas las cerradas. */
	@Transactional(readOnly = true)
	public VentaDetalle consultar(long id) {
		return detalle(ventas.findById(id).orElseThrow(VentaService::noEncontrada));
	}

	/**
	 * Convierte una cotización aceptada y validada en venta. Las líneas, los precios y el descuento se toman de la
	 * cotización. Reserva el stock libre por ubicación y registra el resto como pendiente de abastecimiento.
	 */
	@Transactional
	public VentaDetalle registrarDesdeCotizacion(long idUsuario, long idCotizacion) {
		Cotizacion cotizacion = cotizaciones.bloquearPorId(idCotizacion)
				.orElseThrow(() -> ErrorDeNegocio.validacionDeCampo("idCotizacion", "La cotización indicada no existe."));
		validarConvertible(cotizacion);
		return materializar(idUsuario, cotizacion);
	}

	/**
	 * Registra en un solo paso una venta directa de mostrador: crea la cotización, registra su aceptación y su validación,
	 * y crea la venta con las mismas reglas de reserva. Si no todas las categorías tienen aprobación automática, solo
	 * el Administrador puede confirmar la viabilidad (VIABILIDAD_NO_CONFIRMADA).
	 */
	@Transactional
	public VentaDetalle registrarDirecta(long idUsuario, RolUsuario rol, DatosVentaDirecta datos) {
		if (datos.clienteId() == null) {
			throw new ErrorDeNegocio(CodigoError.CLIENTE_REQUERIDO, "La venta requiere un cliente.");
		}
		Cliente cliente = clientes.validarVigente(datos.clienteId());
		List<Producto> productosVenta = new ArrayList<>();
		BigDecimal subtotal = BigDecimal.ZERO;
		Set<Long> vistos = new HashSet<>();
		for (LineaDirecta linea : lineasOValidas(datos.lineas())) {
			if (!vistos.add(linea.productoId())) {
				throw validacion("Un producto no puede repetirse en la venta.");
			}
			productosVenta.add(productoVigente(linea.productoId()));
			subtotal = subtotal.add(linea.cantidad().multiply(linea.precioUnitario()));
		}
		BigDecimal descuento = datos.descuento() == null ? BigDecimal.ZERO : datos.descuento();
		if (descuento.signum() < 0 || descuento.compareTo(subtotal.setScale(2, RoundingMode.HALF_UP)) > 0) {
			throw validacion("El descuento no puede ser negativo ni superar el subtotal de las líneas.");
		}
		LocalDate hoy = hoy();
		LocalDate vencimiento = datos.fechaVencimiento() == null ? hoy : datos.fechaVencimiento();
		if (vencimiento.isBefore(hoy)) {
			throw validacion("La fecha de vencimiento no puede ser anterior a la fecha de la operación.");
		}
		boolean aprobacionAutomatica = productosVenta.stream().allMatch(p -> p.getCategoria().isAprobacionAutomatica());
		if (!aprobacionAutomatica && rol != RolUsuario.ADMINISTRADOR) {
			throw new ErrorDeNegocio(CodigoError.VIABILIDAD_NO_CONFIRMADA,
					"La viabilidad de abastecimiento requiere la confirmación del Administrador.");
		}
		Instant ahora = reloj.instant();
		Cotizacion cotizacion = new Cotizacion(cliente, usuarios.consultar(idUsuario), ahora, vencimiento,
				datos.condicionesComerciales(), descuento);
		for (int i = 0; i < productosVenta.size(); i++) {
			LineaDirecta linea = datos.lineas().get(i);
			cotizacion.agregarLinea(productosVenta.get(i), linea.cantidad(), linea.precioUnitario());
		}
		cotizacion.registrarAceptacion(ahora);
		cotizacion.registrarValidacion(ahora);
		cotizaciones.saveAndFlush(cotizacion);
		return materializar(idUsuario, cotizacion);
	}

	/**
	 * Vuelve a consultar el stock libre y reserva las cantidades pendientes que puedan atenderse. Una venta cancelada o
	 * despachada no reserva nada y responde con su estado actual.
	 */
	@Transactional
	public VentaDetalle reintentarCobertura(long idUsuario, long idVenta) {
		Venta venta = bloquear(idVenta);
		if (venta.getEstado() == EstadoVenta.CANCELADA || venta.getEstado() == EstadoVenta.DESPACHADA) {
			return detalle(venta);
		}
		for (VentaDetalleLinea pendiente : pendientes(venta)) {
			reservasInventario.reservar(idUsuario, pendiente.linea().getId(), pendiente.linea().getProducto().getId(),
					pendiente.cantidad());
		}
		actualizarEstadoPorCobertura(venta);
		return detalle(venta);
	}

	/**
	 * Cancela la venta antes del despacho. Anula su guía de despacho si la tiene, libera sus reservas activas, registra el
	 * motivo y el usuario autorizante, y no genera movimientos de Kardex. Las ventas despachadas o canceladas no admiten
	 * cancelación.
	 */
	@Transactional
	public VentaDetalle cancelar(long idUsuario, long idVenta, String motivo) {
		String texto = motivo == null ? "" : motivo.trim();
		if (texto.isEmpty()) {
			throw new ErrorDeNegocio(CodigoError.MOTIVO_REQUERIDO, "El motivo de la cancelación es obligatorio.");
		}
		if (texto.length() > LONGITUD_MAXIMA_MOTIVO) {
			throw validacion("El motivo de la cancelación no puede superar 500 caracteres.");
		}
		Venta venta = bloquear(idVenta);
		if (venta.getEstado() == EstadoVenta.DESPACHADA || venta.getEstado() == EstadoVenta.CANCELADA) {
			throw new ErrorDeNegocio(CodigoError.VENTA_NO_CANCELABLE, "La venta no admite cancelación en su estado actual.");
		}
		despacho.anularGuiaDeVenta(idVenta);
		reservasInventario.liberar(venta.getLineas().stream().map(DetalleVenta::getId).toList());
		venta.cancelar(usuarios.consultar(idUsuario), reloj.instant(), texto);
		return detalle(venta);
	}

	/** Línea con cantidad pendiente de abastecimiento. */
	private record VentaDetalleLinea(DetalleVenta linea, BigDecimal cantidad) {
	}

	/** Crea la venta a partir de una cotización validada, reserva cada línea y fija el estado según la cobertura. */
	private VentaDetalle materializar(long idUsuario, Cotizacion cotizacion) {
		Usuario responsable = usuarios.consultar(idUsuario);
		Venta venta = new Venta(cotizacion, responsable, reloj.instant(), cotizacion.getDescuento());
		cotizacion.getLineas().stream().sorted(Comparator.comparing(l -> l.getProducto().getId()))
				.forEach(l -> venta.agregarLinea(l.getProducto(), l.getCantidadCotizada(), l.getPrecioUnitario()));
		ventas.saveAndFlush(venta);
		for (VentaDetalleLinea pendiente : pendientes(venta)) {
			reservasInventario.reservar(idUsuario, pendiente.linea().getId(), pendiente.linea().getProducto().getId(),
					pendiente.cantidad());
		}
		actualizarEstadoPorCobertura(venta);
		return detalle(venta);
	}

	/**
	 * Lista las líneas con cantidad pendiente, en orden ascendente de producto. La cantidad pendiente es la vendida
	 * menos la reservada activa, de modo que la reserva nunca supera la cantidad vendida.
	 */
	private List<VentaDetalleLinea> pendientes(Venta venta) {
		Map<Long, BigDecimal> reservado = reservadoPorLinea(venta);
		List<VentaDetalleLinea> resultado = new ArrayList<>();
		venta.getLineas().stream().sorted(Comparator.comparing(l -> l.getProducto().getId())).forEach(linea -> {
			BigDecimal pendiente = linea.getCantidadVendida().subtract(reservado.getOrDefault(linea.getId(), BigDecimal.ZERO));
			if (pendiente.signum() > 0) {
				resultado.add(new VentaDetalleLinea(linea, pendiente));
			}
		});
		return resultado;
	}

	/** Pasa la venta a pendiente de despacho si todas las líneas están cubiertas, o la conserva pendiente de abastecimiento. */
	private void actualizarEstadoPorCobertura(Venta venta) {
		if (venta.getEstado() != EstadoVenta.PENDIENTE_ABASTECIMIENTO && venta.getEstado() != EstadoVenta.PENDIENTE_DESPACHO) {
			return;
		}
		boolean cubierta = detalle(venta).coberturaPorLinea().values().stream()
				.allMatch(c -> c.pendiente().signum() == 0);
		venta.cambiarEstado(cubierta ? EstadoVenta.PENDIENTE_DESPACHO : EstadoVenta.PENDIENTE_ABASTECIMIENTO);
	}

	private Map<Long, BigDecimal> reservadoPorLinea(Venta venta) {
		Map<Long, BigDecimal> reservado = new LinkedHashMap<>();
		reservasInventario.reservasDe(venta.getLineas().stream().map(DetalleVenta::getId).toList()).stream()
				.filter(r -> r.getEstado().valor().equals("activa"))
				.forEach(r -> reservado.merge(r.getIdDetalleVenta(), r.getCantidadReservada(), BigDecimal::add));
		return reservado;
	}

	/** Compone la vista de la venta: cobertura por línea y reservas. Las ventas cerradas no tienen pendientes. */
	private VentaDetalle detalle(Venta venta) {
		List<ReservaStock> reservas = reservasInventario.reservasDe(venta.getLineas().stream().map(DetalleVenta::getId).toList());
		Map<Long, BigDecimal> reservado = reservadoPorLinea(venta);
		boolean cerrada = venta.getEstado() == EstadoVenta.DESPACHADA || venta.getEstado() == EstadoVenta.CANCELADA;
		Map<Long, Cobertura> cobertura = new LinkedHashMap<>();
		for (DetalleVenta linea : venta.getLineas()) {
			BigDecimal reservadoLinea = reservado.getOrDefault(linea.getId(), BigDecimal.ZERO);
			BigDecimal pendiente = cerrada ? BigDecimal.ZERO
					: linea.getCantidadVendida().subtract(reservadoLinea).max(BigDecimal.ZERO);
			cobertura.put(linea.getId(), new Cobertura(reservadoLinea, pendiente));
		}
		return new VentaDetalle(venta, cobertura, reservas.stream().map(VentaService::reservaVista).toList());
	}

	private static ReservaVista reservaVista(ReservaStock reserva) {
		return new ReservaVista(reserva.getId(), reserva.getIdDetalleVenta(), reserva.getIdProducto(),
				reserva.getInventario().getAlmacen().getId(), reserva.getInventario().getAlmacen().getNombre(),
				reserva.getCantidadReservada(), reserva.getEstado().valor(), reserva.getFechaReserva(),
				reserva.getFechaCierre());
	}

	/**
	 * Verifica que la cotización esté aceptada y validada, sin venta previa, no vencida y con cliente activo. El orden
	 * de las comprobaciones determina el código de error (ADR-003 y CU-10).
	 */
	private void validarConvertible(Cotizacion cotizacion) {
		if (ventas.existsByCotizacionId(cotizacion.getId())) {
			throw new ErrorDeNegocio(CodigoError.COTIZACION_YA_CONVERTIDA, "La cotización ya originó una venta.");
		}
		boolean vencida = cotizacion.getEstado() == EstadoCotizacion.VENCIDA
				|| (cotizacion.getEstado() == EstadoCotizacion.VALIDADA && hoy().isAfter(cotizacion.getFechaVencimiento()));
		if (vencida) {
			throw new ErrorDeNegocio(CodigoError.COTIZACION_VENCIDA, "La cotización venció y ya no puede convertirse.");
		}
		if (cotizacion.getEstado() != EstadoCotizacion.VALIDADA) {
			throw new ErrorDeNegocio(CodigoError.COTIZACION_NO_CONVERTIBLE,
					"La cotización debe estar aceptada y validada para convertirse en venta.");
		}
		clientes.validarVigente(cotizacion.getCliente().getId());
	}

	/** Valida las líneas de una venta directa: al menos una, con cantidad positiva y precio no negativo. */
	private static List<LineaDirecta> lineasOValidas(List<LineaDirecta> lineas) {
		if (lineas == null || lineas.isEmpty()) {
			throw validacion("La venta requiere al menos una línea.");
		}
		for (LineaDirecta linea : lineas) {
			if (linea.cantidad() == null || linea.cantidad().signum() <= 0) {
				throw validacion("La cantidad debe ser mayor que cero.");
			}
			if (linea.precioUnitario() == null || linea.precioUnitario().signum() < 0) {
				throw validacion("El precio unitario no puede ser negativo.");
			}
		}
		return lineas;
	}

	private Producto productoVigente(long idProducto) {
		try {
			return productos.consultar(idProducto);
		} catch (ErrorDeNegocio e) {
			throw validacion("El producto indicado no existe.");
		}
	}

	private Venta bloquear(long id) {
		return ventas.bloquearPorId(id).orElseThrow(VentaService::noEncontrada);
	}

	private LocalDate hoy() {
		return reloj.instant().atZone(ZONA).toLocalDate();
	}

	private static ErrorDeNegocio validacion(String detalle) {
		return new ErrorDeNegocio(CodigoError.VALIDACION_FALLIDA, detalle);
	}

	private static ErrorDeNegocio noEncontrada() {
		return new ErrorDeNegocio(CodigoError.RECURSO_NO_ENCONTRADO, "La venta no existe.");
	}
}
