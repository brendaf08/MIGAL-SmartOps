package com.migal.smartops.despacho.service;

import com.migal.smartops.comercial.model.DetalleVenta;
import com.migal.smartops.comercial.model.EstadoVenta;
import com.migal.smartops.comercial.model.Venta;
import com.migal.smartops.comercial.repository.VentaRepository;
import com.migal.smartops.comun.errores.CodigoError;
import com.migal.smartops.comun.errores.ErrorDeNegocio;
import com.migal.smartops.despacho.model.EstadoGuia;
import com.migal.smartops.despacho.model.GuiaDespacho;
import com.migal.smartops.despacho.model.ReimpresionGuia;
import com.migal.smartops.despacho.repository.GuiaDespachoRepository;
import com.migal.smartops.despacho.repository.ReimpresionGuiaRepository;
import com.migal.smartops.inventario.model.EstadoReserva;
import com.migal.smartops.inventario.model.Inventario;
import com.migal.smartops.inventario.model.ReservaStock;
import com.migal.smartops.inventario.repository.InventarioRepository;
import com.migal.smartops.inventario.service.MovimientoDetalle;
import com.migal.smartops.inventario.service.ReservaInventarioService;
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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ciclo de vida de las guías de despacho (CU-06, ADR-003). La emisión no modifica el inventario. La confirmación de la
 * salida consume las reservas de la venta, reduce el saldo y registra las salidas de Kardex en la misma transacción que
 * el cambio de estado de la guía y de la venta. Las operaciones bloquean la venta, después la guía y por último las
 * existencias, en ese orden, para que todas las transacciones adquieran los bloqueos en un mismo orden (ADR-010).
 */
@Service
public class DespachoService {

	/** Zona horaria con la que se interpretan los rangos de fechas de los listados. */
	private static final ZoneId ZONA = ZoneId.of("America/Lima");
	private static final int LONGITUD_MAXIMA_MOTIVO = 500;

	/** Ubicación de la que sale una parte de una línea, con la cantidad reservada desde ella. */
	public record UbicacionSalida(long ubicacionId, String nombre, BigDecimal cantidad) {
	}

	/** Línea de la guía con sus ubicaciones de salida, tomadas de las reservas no liberadas de la venta. */
	public record LineaGuia(DetalleVenta linea, List<UbicacionSalida> ubicaciones) {
	}

	/** Guía con sus líneas, lista para presentarse. */
	public record GuiaDetalle(GuiaDespacho guia, List<LineaGuia> lineas) {
	}

	/** Existencia antes y después de la salida, por producto y ubicación. */
	public record Cambio(long productoId, String sku, long ubicacionId, String unidad, BigDecimal antes,
			BigDecimal despues) {
	}

	/** Resultado de la confirmación de la salida: la guía, sus movimientos de Kardex y las existencias afectadas. */
	public record ResultadoSalida(GuiaDetalle guia, List<MovimientoDetalle> movimientos, List<Cambio> cambios) {
	}

	private final GuiaDespachoRepository guias;
	private final ReimpresionGuiaRepository reimpresiones;
	private final VentaRepository ventas;
	private final InventarioRepository inventarios;
	private final ReservaInventarioService reservas;
	private final UsuarioService usuarios;
	private final Clock reloj;

	public DespachoService(GuiaDespachoRepository guias, ReimpresionGuiaRepository reimpresiones, VentaRepository ventas,
			InventarioRepository inventarios, ReservaInventarioService reservas, UsuarioService usuarios, Clock reloj) {
		this.guias = guias;
		this.reimpresiones = reimpresiones;
		this.ventas = ventas;
		this.inventarios = inventarios;
		this.reservas = reservas;
		this.usuarios = usuarios;
		this.reloj = reloj;
	}

	/**
	 * Lista paginada de guías. Los filtros son opcionales y combinables: {@code texto} busca por código de la guía o por
	 * cliente, {@code idVenta} filtra por venta de origen, y {@code desde} y {@code hasta} delimitan la fecha de emisión.
	 */
	@Transactional(readOnly = true)
	public Page<GuiaDespacho> listar(Collection<EstadoGuia> estados, String texto, Long idVenta, LocalDate desde,
			LocalDate hasta, Pageable paginacion) {
		String patron = patron(texto);
		Instant inicio = inicio(desde);
		Instant fin = fin(hasta);
		Specification<GuiaDespacho> filtros = (raiz, consulta, cb) -> {
			List<Predicate> condiciones = new ArrayList<>();
			if (estados != null && !estados.isEmpty()) {
				condiciones.add(raiz.get("estado").in(estados.stream().map(EstadoGuia::valor).toList()));
			}
			if (patron != null) {
				condiciones.add(cb.or(cb.like(cb.lower(raiz.get("codigo")), patron),
						cb.like(cb.lower(raiz.get("venta").get("cliente").get("nombreRazonSocial")), patron)));
			}
			if (idVenta != null) {
				condiciones.add(cb.equal(raiz.get("venta").get("id"), idVenta));
			}
			if (inicio != null) {
				condiciones.add(cb.greaterThanOrEqualTo(raiz.<Instant>get("fechaEmision"), inicio));
			}
			if (fin != null) {
				condiciones.add(cb.lessThan(raiz.<Instant>get("fechaEmision"), fin));
			}
			return cb.and(condiciones.toArray(Predicate[]::new));
		};
		return guias.findAll(filtros, paginacion);
	}

	/**
	 * Lista paginada de ventas pendientes de despacho, es decir, con todas sus cantidades reservadas y sin guía. Los
	 * filtros son los del listado de guías, aplicados sobre la venta: {@code texto} busca por código o por cliente, y el
	 * rango se aplica sobre la fecha de la venta.
	 */
	@Transactional(readOnly = true)
	public Page<Venta> ventasPendientes(String texto, LocalDate desde, LocalDate hasta, Pageable paginacion) {
		String patron = patron(texto);
		Instant inicio = inicio(desde);
		Instant fin = fin(hasta);
		Specification<Venta> filtros = (raiz, consulta, cb) -> {
			List<Predicate> condiciones = new ArrayList<>();
			condiciones.add(cb.equal(raiz.get("estado"), EstadoVenta.PENDIENTE_DESPACHO.valor()));
			if (patron != null) {
				condiciones.add(cb.or(cb.like(cb.lower(raiz.get("codigo")), patron),
						cb.like(cb.lower(raiz.get("cliente").get("nombreRazonSocial")), patron)));
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

	/** Consulta una guía con sus líneas y sus ubicaciones de salida. Operación de solo lectura. */
	@Transactional(readOnly = true)
	public GuiaDetalle consultar(long id) {
		return detalle(guias.findById(id).orElseThrow(DespachoService::guiaNoEncontrada));
	}

	/**
	 * Emite la guía de una venta con todas sus cantidades reservadas. Verifica, en este orden, que la venta no tenga guía
	 * (GUIA_EXISTENTE), que esté pendiente de despacho (VENTA_NO_DESPACHABLE) y que cada línea tenga reservada la cantidad
	 * completa (RESERVA_INCOMPLETA). La venta pasa a lista para despacho. La emisión no genera movimientos de Kardex.
	 */
	@Transactional
	public GuiaDetalle emitir(long idVenta) {
		Venta venta = ventas.bloquearPorId(idVenta).orElseThrow(DespachoService::ventaNoEncontrada);
		if (guias.existsByVentaId(idVenta)) {
			throw new ErrorDeNegocio(CodigoError.GUIA_EXISTENTE, "La venta ya tiene una guía de despacho.");
		}
		if (venta.getEstado() != EstadoVenta.PENDIENTE_DESPACHO) {
			throw new ErrorDeNegocio(CodigoError.VENTA_NO_DESPACHABLE, "La venta no está pendiente de despacho.");
		}
		exigirReservaCompleta(venta);
		String direccion = venta.getCliente().getDireccionReferencia();
		if (direccion == null || direccion.isBlank()) {
			throw validacion("El cliente de la venta no tiene una dirección de entrega registrada.");
		}
		GuiaDespacho guia = guias.saveAndFlush(new GuiaDespacho(venta, direccion));
		venta.cambiarEstado(EstadoVenta.LISTA_PARA_DESPACHO);
		return detalle(guia);
	}

	/**
	 * Confirma la salida física completa de una guía emitida. Consume las reservas activas de la venta, reduce el saldo de
	 * cada existencia y registra una salida de Kardex por cada reserva. La guía pasa a salida confirmada y la venta a
	 * despachada. Todo ocurre en una sola transacción: si el consumo falla, ningún cambio se confirma.
	 */
	@Transactional
	public ResultadoSalida confirmarSalida(long idUsuario, long idGuia) {
		Long idVenta = guias.idVentaDeGuia(idGuia).orElseThrow(DespachoService::guiaNoEncontrada);
		Venta venta = ventas.bloquearPorId(idVenta).orElseThrow(DespachoService::ventaNoEncontrada);
		GuiaDespacho guia = guias.bloquearPorId(idGuia).orElseThrow(DespachoService::guiaNoEncontrada);
		if (guia.getEstado() != EstadoGuia.EMITIDA) {
			throw new ErrorDeNegocio(CodigoError.SALIDA_NO_CONFIRMABLE, "La guía no está emitida y su salida no puede confirmarse.");
		}
		exigirReservaCompleta(venta);

		List<Long> lineas = idsLineas(venta);
		List<Long> idsInventario = reservas.reservasDe(lineas).stream()
				.filter(r -> r.getEstado() == EstadoReserva.ACTIVA)
				.map(r -> r.getInventario().getId()).distinct().toList();
		List<Inventario> afectados = inventarios.bloquearPorIds(idsInventario);
		Map<Long, BigDecimal> antes = afectados.stream()
				.collect(Collectors.toMap(Inventario::getId, Inventario::getCantidadFisica));

		List<MovimientoDetalle> movimientos = reservas.consumirDespacho(idUsuario, idGuia, lineas);
		List<Cambio> cambios = afectados.stream()
				.map(i -> new Cambio(i.getProducto().getId(), i.getProducto().getSku(), i.getAlmacen().getId(),
						i.getProducto().getUnidadMedida(), antes.get(i.getId()), i.getCantidadFisica()))
				.toList();

		Usuario responsable = usuarios.consultar(idUsuario);
		guia.confirmarSalida(responsable, reloj.instant());
		venta.cambiarEstado(EstadoVenta.DESPACHADA);
		return new ResultadoSalida(detalle(guia), movimientos, cambios);
	}

	/** Registra la entrega al cliente de una guía con la salida confirmada. No modifica el inventario ni el Kardex. */
	@Transactional
	public GuiaDetalle registrarEntrega(long idGuia) {
		GuiaDespacho guia = guias.bloquearPorId(idGuia).orElseThrow(DespachoService::guiaNoEncontrada);
		if (guia.getEstado() != EstadoGuia.SALIDA_CONFIRMADA) {
			throw new ErrorDeNegocio(CodigoError.TRANSICION_NO_PERMITIDA,
					"La guía debe tener la salida confirmada para registrar la entrega.");
		}
		guia.registrarEntrega(reloj.instant());
		return detalle(guia);
	}

	/**
	 * Registra una reimpresión de la guía con su motivo obligatorio. No modifica el inventario ni el estado de la guía.
	 */
	@Transactional
	public ReimpresionGuia registrarReimpresion(long idUsuario, long idGuia, String motivo) {
		String texto = motivo == null ? "" : motivo.trim();
		if (texto.isEmpty()) {
			throw new ErrorDeNegocio(CodigoError.MOTIVO_REQUERIDO, "El motivo de la reimpresión es obligatorio.");
		}
		if (texto.length() > LONGITUD_MAXIMA_MOTIVO) {
			throw validacion("El motivo de la reimpresión no puede superar 500 caracteres.");
		}
		GuiaDespacho guia = guias.findById(idGuia).orElseThrow(DespachoService::guiaNoEncontrada);
		Usuario usuario = usuarios.consultar(idUsuario);
		return reimpresiones.saveAndFlush(new ReimpresionGuia(guia, usuario, texto));
	}

	/**
	 * Anula la guía de una venta cancelada. Se invoca dentro de la transacción de la cancelación, después de bloquear la
	 * venta. Una venta sin guía no produce cambios.
	 */
	@Transactional
	public void anularGuiaDeVenta(long idVenta) {
		guias.bloquearPorVenta(idVenta).ifPresent(guia -> {
			if (guia.getEstado() != EstadoGuia.EMITIDA && guia.getEstado() != EstadoGuia.PREPARACION) {
				throw new ErrorDeNegocio(CodigoError.TRANSICION_NO_PERMITIDA,
						"La guía de la venta no puede anularse en su estado actual.");
			}
			guia.anular();
		});
	}

	/** Verifica que cada línea de la venta tenga reservada su cantidad vendida completa (reservas activas). */
	private void exigirReservaCompleta(Venta venta) {
		Map<Long, BigDecimal> reservado = reservadoPorLinea(venta);
		boolean incompleta = venta.getLineas().stream()
				.anyMatch(l -> reservado.getOrDefault(l.getId(), BigDecimal.ZERO).compareTo(l.getCantidadVendida()) != 0);
		if (incompleta) {
			throw new ErrorDeNegocio(CodigoError.RESERVA_INCOMPLETA, "Alguna línea no tiene reservada la cantidad completa.");
		}
	}

	private Map<Long, BigDecimal> reservadoPorLinea(Venta venta) {
		Map<Long, BigDecimal> reservado = new HashMap<>();
		reservas.reservasDe(idsLineas(venta)).stream()
				.filter(r -> r.getEstado() == EstadoReserva.ACTIVA)
				.forEach(r -> reservado.merge(r.getIdDetalleVenta(), r.getCantidadReservada(), BigDecimal::add));
		return reservado;
	}

	/** Compone la guía con sus líneas. Incluye las reservas activas y consumidas; las liberadas no forman parte de la guía. */
	private GuiaDetalle detalle(GuiaDespacho guia) {
		Venta venta = guia.getVenta();
		Map<Long, List<UbicacionSalida>> porLinea = new HashMap<>();
		for (ReservaStock reserva : reservas.reservasDe(idsLineas(venta))) {
			if (reserva.getEstado() == EstadoReserva.LIBERADA) {
				continue;
			}
			var almacen = reserva.getInventario().getAlmacen();
			porLinea.computeIfAbsent(reserva.getIdDetalleVenta(), k -> new ArrayList<>())
					.add(new UbicacionSalida(almacen.getId(), almacen.getNombre(), reserva.getCantidadReservada()));
		}
		List<LineaGuia> lineas = venta.getLineas().stream()
				.map(l -> new LineaGuia(l, porLinea.getOrDefault(l.getId(), List.of())))
				.toList();
		return new GuiaDetalle(guia, lineas);
	}

	private static List<Long> idsLineas(Venta venta) {
		return venta.getLineas().stream().map(DetalleVenta::getId).toList();
	}

	private static String patron(String texto) {
		return texto == null || texto.isBlank() ? null : "%" + texto.trim().toLowerCase() + "%";
	}

	private static Instant inicio(LocalDate desde) {
		return desde == null ? null : desde.atStartOfDay(ZONA).toInstant();
	}

	private static Instant fin(LocalDate hasta) {
		return hasta == null ? null : hasta.plusDays(1).atStartOfDay(ZONA).toInstant();
	}

	private static ErrorDeNegocio guiaNoEncontrada() {
		return new ErrorDeNegocio(CodigoError.RECURSO_NO_ENCONTRADO, "La guía de despacho no existe.");
	}

	private static ErrorDeNegocio ventaNoEncontrada() {
		return new ErrorDeNegocio(CodigoError.RECURSO_NO_ENCONTRADO, "La venta no existe.");
	}

	private static ErrorDeNegocio validacion(String detalle) {
		return new ErrorDeNegocio(CodigoError.VALIDACION_FALLIDA, detalle);
	}
}
