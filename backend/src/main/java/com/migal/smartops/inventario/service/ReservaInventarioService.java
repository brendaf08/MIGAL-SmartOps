package com.migal.smartops.inventario.service;

import com.migal.smartops.inventario.model.Inventario;
import com.migal.smartops.inventario.model.MovimientoKardex;
import com.migal.smartops.inventario.model.ReservaStock;
import com.migal.smartops.inventario.repository.InventarioRepository;
import com.migal.smartops.inventario.repository.MovimientoKardexRepository;
import com.migal.smartops.inventario.repository.ReservaStockRepository;
import com.migal.smartops.seguridad.model.Usuario;
import com.migal.smartops.seguridad.repository.UsuarioRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Operaciones internas de inventario para ventas, despacho y abastecimiento (ADR-003). Se ejecutan dentro de la
 * transacción de la operación que las invoca, de modo que la venta, la reserva y su Kardex se confirman o se
 * revierten juntos. Todas bloquean las existencias en orden ascendente de identificador (ADR-010).
 *
 * <ul>
 *   <li>La venta reserva stock libre y no genera movimientos de Kardex.
 *   <li>La cancelación libera las reservas activas.
 *   <li>El despacho consume las reservas: reduce el saldo físico y registra una salida de Kardex por cada una.
 *   <li>La recepción validada registra entradas de Kardex y aumenta el saldo.
 * </ul>
 */
@Service
public class ReservaInventarioService {

	/** Cantidad reservada de una línea de venta en un inventario. */
	public record Asignacion(long idReserva, long idInventario, long idUbicacion, BigDecimal cantidad) {
	}

	/** Entrada de una línea de recepción. */
	public record EntradaRecepcion(long idProducto, long idUbicacion, BigDecimal cantidad) {
	}

	private final InventarioRepository inventarios;
	private final ReservaStockRepository reservas;
	private final MovimientoKardexRepository kardex;
	private final UsuarioRepository usuarios;
	private final Clock reloj;

	public ReservaInventarioService(InventarioRepository inventarios, ReservaStockRepository reservas,
			MovimientoKardexRepository kardex, UsuarioRepository usuarios, Clock reloj) {
		this.inventarios = inventarios;
		this.reservas = reservas;
		this.kardex = kardex;
		this.usuarios = usuarios;
		this.reloj = reloj;
	}

	/**
	 * Reserva para una línea de venta hasta la cantidad indicada, tomando el stock libre de cada ubicación con
	 * la tienda primero. Si el stock libre no alcanza, reserva lo disponible (reserva parcial) y el resto queda
	 * pendiente de abastecimiento. La suma de reservas activas de la línea no debe exceder su cantidad vendida,
	 * lo que verifica la base de datos al confirmar.
	 *
	 * @return asignaciones creadas; vacía si no hay stock libre
	 */
	@Transactional
	public List<Asignacion> reservar(long idUsuario, long idDetalleVenta, long idProducto, BigDecimal cantidad) {
		if (cantidad == null || cantidad.signum() <= 0) {
			throw new IllegalArgumentException("La cantidad a reservar debe ser positiva.");
		}
		List<Inventario> bloqueados = inventarios.bloquearPorProducto(idProducto);
		List<Inventario> porPrioridad = new ArrayList<>(bloqueados);
		porPrioridad.sort(Comparator.comparing((Inventario i) -> !esTienda(i)).thenComparing(Inventario::getId));

		Usuario responsable = usuarios.getReferenceById(idUsuario);
		BigDecimal pendiente = cantidad;
		List<Asignacion> asignaciones = new ArrayList<>();
		for (Inventario inventario : porPrioridad) {
			if (pendiente.signum() == 0) {
				break;
			}
			BigDecimal libre = inventario.getCantidadFisica().subtract(reservas.sumaActivas(inventario.getId()));
			BigDecimal tomada = libre.min(pendiente);
			if (tomada.signum() <= 0) {
				continue;
			}
			ReservaStock reserva = reservas.saveAndFlush(
					new ReservaStock(idDetalleVenta, inventario, idProducto, tomada, responsable));
			asignaciones.add(new Asignacion(reserva.getId(), inventario.getId(), inventario.getAlmacen().getId(), tomada));
			pendiente = pendiente.subtract(tomada);
		}
		return asignaciones;
	}

	/** Libera todas las reservas activas de las líneas indicadas (cancelación de la venta). Devuelve cuántas liberó. */
	@Transactional
	public int liberar(Collection<Long> idsDetalleVenta) {
		List<ReservaStock> activas = activas(idsDetalleVenta);
		bloquear(activas);
		Instant ahora = reloj.instant();
		activas.forEach(r -> r.liberar(ahora));
		reservas.flush();
		return activas.size();
	}

	/**
	 * Consume las reservas activas de las líneas de un despacho: reduce el saldo físico de cada inventario,
	 * cierra la reserva como consumida y registra una salida de Kardex vinculada a la guía.
	 *
	 * @return movimientos de Kardex creados, uno por reserva consumida
	 */
	@Transactional
	public List<MovimientoDetalle> consumirDespacho(long idUsuario, long idGuia, Collection<Long> idsDetalleVenta) {
		List<ReservaStock> activas = activas(idsDetalleVenta);
		Map<Long, Inventario> bloqueados = bloquear(activas);
		Instant ahora = reloj.instant();
		Usuario responsable = usuarios.getReferenceById(idUsuario);
		List<MovimientoDetalle> movimientos = new ArrayList<>();
		for (ReservaStock reserva : activas) {
			Inventario inventario = bloqueados.get(reserva.getInventario().getId());
			inventario.restar(reserva.getCantidadReservada());
			reserva.consumir(ahora);
			movimientos.add(MovimientoDetalle.de(kardex.saveAndFlush(
					MovimientoKardex.despacho(inventario, reserva.getCantidadReservada(), responsable, idGuia))));
		}
		return movimientos;
	}

	/**
	 * Registra las entradas de una recepción validada: crea la existencia si falta, aumenta el saldo y anota una
	 * entrada de Kardex por línea, vinculada a la recepción.
	 *
	 * @return movimientos de Kardex creados, en el orden de las líneas
	 */
	@Transactional
	public List<MovimientoDetalle> registrarEntradas(long idUsuario, long idRecepcion, List<EntradaRecepcion> lineas) {
		for (EntradaRecepcion linea : lineas) {
			if (linea.cantidad() == null || linea.cantidad().signum() <= 0) {
				throw new IllegalArgumentException("La cantidad recibida debe ser positiva.");
			}
			inventarios.crearSiNoExiste(linea.idProducto(), linea.idUbicacion());
		}
		TreeSet<Long> ids = new TreeSet<>();
		for (EntradaRecepcion linea : lineas) {
			ids.add(inventarios.findByProductoIdAndAlmacenId(linea.idProducto(), linea.idUbicacion()).orElseThrow().getId());
		}
		Map<Long, Inventario> bloqueados = inventarios.bloquearPorIds(ids).stream()
				.collect(Collectors.toMap(Inventario::getId, Function.identity()));
		Usuario responsable = usuarios.getReferenceById(idUsuario);
		List<MovimientoDetalle> movimientos = new ArrayList<>();
		for (EntradaRecepcion linea : lineas) {
			Inventario inventario = bloqueados.values().stream()
					.filter(i -> i.getProducto().getId() == linea.idProducto() && i.getAlmacen().getId() == linea.idUbicacion())
					.findFirst().orElseThrow();
			inventario.sumar(linea.cantidad());
			movimientos.add(MovimientoDetalle.de(kardex.saveAndFlush(
					MovimientoKardex.recepcion(inventario, linea.cantidad(), responsable, idRecepcion))));
		}
		return movimientos;
	}

	private List<ReservaStock> activas(Collection<Long> idsDetalleVenta) {
		return idsDetalleVenta.isEmpty() ? List.of() : reservas.findActivasPorDetallesVenta(idsDetalleVenta);
	}

	/** Bloquea los inventarios de las reservas, en orden ascendente, y los devuelve por identificador. */
	private Map<Long, Inventario> bloquear(List<ReservaStock> activas) {
		TreeSet<Long> ids = activas.stream().map(r -> r.getInventario().getId())
				.collect(Collectors.toCollection(TreeSet::new));
		if (ids.isEmpty()) {
			return Map.of();
		}
		return inventarios.bloquearPorIds(ids).stream().collect(Collectors.toMap(Inventario::getId, Function.identity()));
	}

	private static boolean esTienda(Inventario inventario) {
		return inventario.getAlmacen().getTipoUbicacion().toLowerCase().startsWith("tienda");
	}
}
