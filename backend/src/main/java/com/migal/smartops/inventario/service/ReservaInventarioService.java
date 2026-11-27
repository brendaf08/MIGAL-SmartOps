package com.migal.smartops.inventario.service;

import com.migal.smartops.comun.errores.CodigoError;
import com.migal.smartops.comun.errores.ErrorDeNegocio;
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
import java.util.HashMap;
import java.util.HashSet;
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

	/** Cantidad de una línea que debe salir de una ubicación concreta. */
	public record SalidaLinea(long idUbicacion, BigDecimal cantidad) {
	}

	/** Ubicación desde la que puede salir una línea: lo disponible (libre más lo ya reservado por la línea) y lo asignado. */
	public record OpcionUbicacion(long idUbicacion, String nombre, BigDecimal disponible, BigDecimal asignada) {
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

	/**
	 * Ubicaciones desde las que puede salir una línea de venta, con la cantidad que cada una puede aportar: su stock
	 * libre más lo que la propia línea ya tiene reservado allí. Solo lectura; la tienda aparece primero.
	 */
	@Transactional(readOnly = true)
	public List<OpcionUbicacion> opcionesSalida(long idDetalleVenta, long idProducto) {
		Map<Long, BigDecimal> asignada = new HashMap<>();
		for (ReservaStock r : reservas.findActivasPorDetalleVenta(idDetalleVenta)) {
			asignada.merge(r.getInventario().getAlmacen().getId(), r.getCantidadReservada(), BigDecimal::add);
		}
		List<Inventario> existencias = new ArrayList<>(inventarios.findByProductoId(idProducto));
		existencias.sort(Comparator.comparing((Inventario i) -> !esTienda(i)).thenComparing(Inventario::getId));
		List<OpcionUbicacion> opciones = new ArrayList<>();
		for (Inventario i : existencias) {
			BigDecimal propia = asignada.getOrDefault(i.getAlmacen().getId(), BigDecimal.ZERO);
			BigDecimal disponible = i.getCantidadFisica().subtract(reservas.sumaActivas(i.getId())).add(propia);
			if (disponible.signum() > 0 || propia.signum() > 0) {
				opciones.add(new OpcionUbicacion(i.getAlmacen().getId(), i.getAlmacen().getNombre(), disponible, propia));
			}
		}
		return opciones;
	}

	/**
	 * Cambia las ubicaciones de las que sale una línea de venta ya reservada: libera sus reservas activas y reserva de
	 * nuevo en las ubicaciones indicadas, de modo que la salida de Kardex quede en el lugar que el almacenero eligió.
	 * La suma debe ser la cantidad vendida de la línea y cada ubicación debe tener stock libre suficiente (contando lo
	 * que la propia línea ya tenía allí). Si ya coincide con las reservas actuales no cambia nada.
	 */
	@Transactional
	public void reasignarSalida(long idUsuario, long idDetalleVenta, long idProducto, BigDecimal cantidadVendida,
			List<SalidaLinea> salidas) {
		BigDecimal suma = BigDecimal.ZERO;
		HashSet<Long> vistas = new HashSet<>();
		for (SalidaLinea s : salidas) {
			if (s.cantidad() == null || s.cantidad().signum() <= 0) {
				throw new ErrorDeNegocio(CodigoError.VALIDACION_FALLIDA, "Cada cantidad de salida debe ser mayor que 0.");
			}
			if (!vistas.add(s.idUbicacion())) {
				throw new ErrorDeNegocio(CodigoError.VALIDACION_FALLIDA, "Una ubicación se repite en la salida de un producto.");
			}
			suma = suma.add(s.cantidad());
		}
		if (suma.compareTo(cantidadVendida) != 0) {
			throw new ErrorDeNegocio(CodigoError.VALIDACION_FALLIDA,
					"La suma de las salidas de un producto debe ser igual a su cantidad vendida.");
		}
		List<Inventario> bloqueados = inventarios.bloquearPorProducto(idProducto);
		List<ReservaStock> actuales = reservas.findActivasPorDetalleVenta(idDetalleVenta);
		Map<Long, BigDecimal> actual = new HashMap<>();
		actuales.forEach(r -> actual.merge(r.getInventario().getAlmacen().getId(), r.getCantidadReservada(), BigDecimal::add));
		boolean igual = actual.size() == salidas.size() && salidas.stream()
				.allMatch(s -> actual.containsKey(s.idUbicacion()) && actual.get(s.idUbicacion()).compareTo(s.cantidad()) == 0);
		if (igual) {
			return;
		}
		Instant ahora = reloj.instant();
		actuales.forEach(r -> r.liberar(ahora));
		reservas.flush();
		Usuario responsable = usuarios.getReferenceById(idUsuario);
		for (SalidaLinea s : salidas) {
			Inventario inventario = bloqueados.stream().filter(i -> i.getAlmacen().getId() == s.idUbicacion()).findFirst()
					.orElseThrow(() -> new ErrorDeNegocio(CodigoError.STOCK_INSUFICIENTE,
							"El producto no tiene existencias en una de las ubicaciones elegidas."));
			BigDecimal libre = inventario.getCantidadFisica().subtract(reservas.sumaActivas(inventario.getId()));
			if (libre.compareTo(s.cantidad()) < 0) {
				throw new ErrorDeNegocio(CodigoError.STOCK_INSUFICIENTE, "No hay stock libre suficiente en "
						+ inventario.getAlmacen().getNombre() + ": disponible " + libre.stripTrailingZeros().toPlainString() + ".");
			}
			reservas.saveAndFlush(new ReservaStock(idDetalleVenta, inventario, idProducto, s.cantidad(), responsable));
		}
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

	/** Reservas de las líneas indicadas, activas o cerradas, para consultar la cobertura de una venta. */
	@Transactional(readOnly = true)
	public List<ReservaStock> reservasDe(Collection<Long> idsDetalleVenta) {
		return idsDetalleVenta.isEmpty() ? List.of() : reservas.findByIdDetalleVentaInOrderByIdAsc(idsDetalleVenta);
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
