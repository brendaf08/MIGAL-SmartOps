package com.migal.smartops.inventario.service;

import com.migal.smartops.catalogo.model.Almacen;
import com.migal.smartops.catalogo.model.Producto;
import com.migal.smartops.catalogo.repository.AlmacenRepository;
import com.migal.smartops.catalogo.repository.ProductoRepository;
import com.migal.smartops.comun.errores.CodigoError;
import com.migal.smartops.comun.errores.ErrorDeNegocio;
import com.migal.smartops.inventario.model.Inventario;
import com.migal.smartops.inventario.model.MovimientoKardex;
import com.migal.smartops.inventario.model.OrigenMovimiento;
import com.migal.smartops.inventario.model.TipoOperacion;
import com.migal.smartops.inventario.repository.InventarioRepository;
import com.migal.smartops.inventario.repository.MovimientoKardexRepository;
import com.migal.smartops.inventario.repository.ReservaStockRepository;
import com.migal.smartops.seguridad.model.Usuario;
import com.migal.smartops.seguridad.repository.UsuarioRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Movimientos manuales de inventario (CU-05): ajustes de entrada o salida y traslados entre ubicaciones. Cada
 * operación es una sola transacción que bloquea las existencias afectadas en orden ascendente de identificador
 * (ADR-010), valida contra el stock libre y registra los movimientos de Kardex junto con el cambio de saldo.
 * Una salida solo reduce el stock libre: las reservas de las ventas nunca se invaden.
 */
@Service
public class MovimientoInventarioService {

	private static final int MAXIMO_MOTIVO = 500;
	private static final BigDecimal MAXIMO_CANTIDAD = new BigDecimal("99999999999.999");

	/** Ajuste manual. {@code idMovimientoCorregido} es opcional y marca el movimiento como compensatorio. */
	public record SolicitudAjuste(TipoOperacion tipoOperacion, long productoId, long ubicacionId, BigDecimal cantidad,
			String motivo, Long idMovimientoCorregido) {
	}

	/** Traslado de un producto entre dos ubicaciones. */
	public record SolicitudTraslado(long productoId, long ubicacionOrigenId, long ubicacionDestinoId,
			BigDecimal cantidad, String motivo) {
	}

	/** Existencia física de un inventario antes y después de la operación. */
	public record CambioExistencia(long productoId, String sku, long ubicacionId, String unidad, BigDecimal antes,
			BigDecimal despues) {
	}

	/** Resultado de un ajuste: el movimiento creado y el cambio de existencia. */
	public record ResultadoMovimiento(MovimientoDetalle movimiento, CambioExistencia cambio) {
	}

	/** Resultado de un traslado: referencia compartida, salida y entrada, y cambios en origen y destino. */
	public record ResultadoTraslado(UUID referenciaTraslado, List<MovimientoDetalle> movimientos,
			List<CambioExistencia> cambios) {
	}

	private final InventarioRepository inventarios;
	private final MovimientoKardexRepository kardex;
	private final ReservaStockRepository reservas;
	private final ProductoRepository productos;
	private final AlmacenRepository almacenes;
	private final UsuarioRepository usuarios;

	public MovimientoInventarioService(InventarioRepository inventarios, MovimientoKardexRepository kardex,
			ReservaStockRepository reservas, ProductoRepository productos, AlmacenRepository almacenes,
			UsuarioRepository usuarios) {
		this.inventarios = inventarios;
		this.kardex = kardex;
		this.reservas = reservas;
		this.productos = productos;
		this.almacenes = almacenes;
		this.usuarios = usuarios;
	}

	/** Registra un ajuste manual de entrada o salida. */
	@Transactional
	public ResultadoMovimiento registrarAjuste(long idUsuario, SolicitudAjuste solicitud) {
		BigDecimal cantidad = cantidadValida(solicitud.cantidad());
		String motivo = motivoValido(solicitud.motivo());
		Producto producto = productoActivo(solicitud.productoId());
		Almacen ubicacion = almacenActivo(solicitud.ubicacionId());

		Inventario inventario = bloquear(producto, List.of(ubicacion), solicitud.tipoOperacion() == TipoOperacion.ENTRADA)
				.get(0);
		if (solicitud.idMovimientoCorregido() != null) {
			validarCorreccion(solicitud, inventario);
		}
		BigDecimal antes = inventario.getCantidadFisica();
		aplicar(inventario, solicitud.tipoOperacion(), cantidad);

		MovimientoKardex movimiento = kardex.saveAndFlush(MovimientoKardex.ajuste(inventario, solicitud.tipoOperacion(),
				cantidad, motivo, usuarios.getReferenceById(idUsuario), solicitud.idMovimientoCorregido()));
		return new ResultadoMovimiento(MovimientoDetalle.de(movimiento), cambio(inventario, antes));
	}

	/** Registra de forma atómica la salida en el origen y la entrada en el destino de un traslado. */
	@Transactional
	public ResultadoTraslado registrarTraslado(long idUsuario, SolicitudTraslado solicitud) {
		BigDecimal cantidad = cantidadValida(solicitud.cantidad());
		String motivo = motivoValido(solicitud.motivo());
		if (solicitud.ubicacionOrigenId() == solicitud.ubicacionDestinoId()) {
			throw new ErrorDeNegocio(CodigoError.VALIDACION_FALLIDA, "La ubicación de destino debe ser distinta de la de origen.");
		}
		Producto producto = productoActivo(solicitud.productoId());
		Almacen origen = almacenActivo(solicitud.ubicacionOrigenId());
		Almacen destino = almacenActivo(solicitud.ubicacionDestinoId());

		// El origen no se crea si no existe: sin existencia no hay nada que trasladar. El destino sí se crea.
		inventarios.crearSiNoExiste(producto.getId(), destino.getId());
		List<Inventario> bloqueados = inventarios.bloquearPorProductoYAlmacenes(producto.getId(),
				List.of(origen.getId(), destino.getId()));
		Inventario inventarioOrigen = bloqueados.stream().filter(i -> i.getAlmacen().getId().equals(origen.getId()))
				.findFirst().orElseThrow(() -> stockInsuficiente());
		Inventario inventarioDestino = bloqueados.stream().filter(i -> i.getAlmacen().getId().equals(destino.getId()))
				.findFirst().orElseThrow();

		BigDecimal antesOrigen = inventarioOrigen.getCantidadFisica();
		BigDecimal antesDestino = inventarioDestino.getCantidadFisica();
		aplicar(inventarioOrigen, TipoOperacion.SALIDA, cantidad);
		inventarioDestino.sumar(cantidad);

		UUID referencia = UUID.randomUUID();
		Usuario responsable = usuarios.getReferenceById(idUsuario);
		MovimientoKardex salida = kardex.save(MovimientoKardex.traslado(inventarioOrigen, TipoOperacion.SALIDA, cantidad,
				motivo, responsable, referencia));
		MovimientoKardex entrada = kardex.saveAndFlush(MovimientoKardex.traslado(inventarioDestino,
				TipoOperacion.ENTRADA, cantidad, motivo, responsable, referencia));
		return new ResultadoTraslado(referencia,
				List.of(MovimientoDetalle.de(salida), MovimientoDetalle.de(entrada)),
				List.of(cambio(inventarioOrigen, antesOrigen), cambio(inventarioDestino, antesDestino)));
	}

	/** Aplica el movimiento al saldo; las salidas se limitan al stock libre. */
	private void aplicar(Inventario inventario, TipoOperacion tipo, BigDecimal cantidad) {
		if (tipo == TipoOperacion.ENTRADA) {
			inventario.sumar(cantidad);
			return;
		}
		BigDecimal libre = inventario.getCantidadFisica().subtract(reservas.sumaActivas(inventario.getId()));
		if (cantidad.compareTo(libre) > 0) {
			throw new ErrorDeNegocio(CodigoError.STOCK_INSUFICIENTE,
					"El stock libre (" + libre.stripTrailingZeros().toPlainString() + ") no alcanza la cantidad solicitada.");
		}
		inventario.restar(cantidad);
	}

	/**
	 * Bloquea las existencias del producto en las ubicaciones indicadas. Si {@code crear} es verdadero, las que
	 * no existen se crean en cero; de lo contrario, su ausencia equivale a stock insuficiente.
	 */
	private List<Inventario> bloquear(Producto producto, List<Almacen> ubicaciones, boolean crear) {
		if (crear) {
			ubicaciones.forEach(u -> inventarios.crearSiNoExiste(producto.getId(), u.getId()));
		}
		List<Inventario> bloqueados = inventarios.bloquearPorProductoYAlmacenes(producto.getId(),
				ubicaciones.stream().map(Almacen::getId).toList());
		if (bloqueados.size() != ubicaciones.size()) {
			throw stockInsuficiente();
		}
		return bloqueados;
	}

	/** Un movimiento compensatorio es de tipo opuesto y del mismo producto y ubicación, y se aplica una sola vez. */
	private void validarCorreccion(SolicitudAjuste solicitud, Inventario inventario) {
		MovimientoKardex corregido = kardex.findById(solicitud.idMovimientoCorregido()).orElseThrow(
				() -> new ErrorDeNegocio(CodigoError.RECURSO_NO_ENCONTRADO, "El movimiento a corregir no existe."));
		if (corregido.getOrigenMovimiento() == OrigenMovimiento.TRASLADO) {
			throw new ErrorDeNegocio(CodigoError.MOVIMIENTO_NO_CORREGIBLE,
					"Un traslado no se corrige con un ajuste: se registra un traslado en sentido contrario.");
		}
		if (!corregido.getInventario().getId().equals(inventario.getId())) {
			throw new ErrorDeNegocio(CodigoError.MOVIMIENTO_NO_CORREGIBLE,
					"El producto y la ubicación deben coincidir con los del movimiento corregido.");
		}
		if (corregido.getTipoOperacion() != solicitud.tipoOperacion().opuesto()) {
			throw new ErrorDeNegocio(CodigoError.MOVIMIENTO_NO_CORREGIBLE,
					"El movimiento compensatorio debe ser de tipo opuesto al corregido.");
		}
		if (kardex.existsByIdMovimientoCorregido(corregido.getId())) {
			throw new ErrorDeNegocio(CodigoError.MOVIMIENTO_NO_CORREGIBLE, "El movimiento ya fue corregido.");
		}
	}

	private Producto productoActivo(long id) {
		Producto producto = productos.findById(id)
				.orElseThrow(() -> new ErrorDeNegocio(CodigoError.RECURSO_NO_ENCONTRADO, "El producto no existe."));
		if (!producto.isEstadoActivo()) {
			throw new ErrorDeNegocio(CodigoError.PRODUCTO_INACTIVO, "El producto está inactivo.");
		}
		return producto;
	}

	private Almacen almacenActivo(long id) {
		Almacen almacen = almacenes.findById(id)
				.orElseThrow(() -> new ErrorDeNegocio(CodigoError.RECURSO_NO_ENCONTRADO, "La ubicación no existe."));
		if (!almacen.isEstadoActivo()) {
			throw new ErrorDeNegocio(CodigoError.ALMACEN_INACTIVO, "La ubicación está inactiva.");
		}
		return almacen;
	}

	private static BigDecimal cantidadValida(BigDecimal cantidad) {
		if (cantidad == null || cantidad.signum() <= 0) {
			throw new ErrorDeNegocio(CodigoError.VALIDACION_FALLIDA, "La cantidad debe ser positiva.");
		}
		if (cantidad.stripTrailingZeros().scale() > 3 || cantidad.compareTo(MAXIMO_CANTIDAD) > 0) {
			throw new ErrorDeNegocio(CodigoError.VALIDACION_FALLIDA,
					"La cantidad admite hasta tres decimales y no puede exceder 99999999999.999.");
		}
		return cantidad;
	}

	private static String motivoValido(String motivo) {
		if (motivo == null || motivo.isBlank()) {
			throw new ErrorDeNegocio(CodigoError.MOTIVO_REQUERIDO, "El motivo es obligatorio.");
		}
		if (motivo.length() > MAXIMO_MOTIVO) {
			throw new ErrorDeNegocio(CodigoError.VALIDACION_FALLIDA, "El motivo admite hasta 500 caracteres.");
		}
		return motivo.trim();
	}

	private static ErrorDeNegocio stockInsuficiente() {
		return new ErrorDeNegocio(CodigoError.STOCK_INSUFICIENTE, "No hay existencias suficientes en la ubicación de origen.");
	}

	private static CambioExistencia cambio(Inventario inventario, BigDecimal antes) {
		Producto producto = inventario.getProducto();
		return new CambioExistencia(producto.getId(), producto.getSku(), inventario.getAlmacen().getId(),
				producto.getUnidadMedida(), antes, inventario.getCantidadFisica());
	}
}
