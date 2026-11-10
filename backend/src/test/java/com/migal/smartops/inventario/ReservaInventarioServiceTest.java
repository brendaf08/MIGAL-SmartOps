package com.migal.smartops.inventario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.migal.smartops.TestcontainersConfiguration;
import com.migal.smartops.comun.errores.CodigoError;
import com.migal.smartops.comun.errores.ErrorDeNegocio;
import com.migal.smartops.inventario.model.OrigenMovimiento;
import com.migal.smartops.inventario.model.TipoOperacion;
import com.migal.smartops.inventario.service.MovimientoDetalle;
import com.migal.smartops.inventario.service.MovimientoInventarioService;
import com.migal.smartops.inventario.service.MovimientoInventarioService.SolicitudAjuste;
import com.migal.smartops.inventario.service.ReservaInventarioService;
import com.migal.smartops.inventario.service.ReservaInventarioService.Asignacion;
import com.migal.smartops.inventario.service.ReservaInventarioService.EntradaRecepcion;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/** Verifica reservas, liberación, consumo por despacho y entradas por recepción con PostgreSQL real. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ReservaInventarioServiceTest {

	@Autowired
	private ReservaInventarioService servicio;
	@Autowired
	private MovimientoInventarioService movimientos;
	@Autowired
	private JdbcTemplate jdbc;

	private EscenarioInventario escenario;
	private long usuario;
	private long producto;
	private long tienda;
	private long deposito;
	private long cliente;
	private long cotizacion;
	private long venta;
	private long detalle;
	private Long guia;
	private Long proveedor;
	private Long orden;
	private Long recepcion;

	@BeforeEach
	void preparar() {
		escenario = new EscenarioInventario(jdbc);
		usuario = escenario.usuario();
		producto = escenario.producto("10");
		tienda = escenario.almacen("tienda");
		deposito = escenario.almacen("almacen");
		escenario.inventario(producto, tienda, "30");
		escenario.inventario(producto, deposito, "50");
		cliente = jdbc.queryForObject("INSERT INTO cliente (nombre_razon_social) VALUES ('Cliente prueba') RETURNING id_cliente", Long.class);
		cotizacion = jdbc.queryForObject("""
				INSERT INTO cotizacion (id_cliente, id_usuario_responsable, fecha_vencimiento, descuento, estado)
				VALUES (?, ?, CURRENT_DATE + 30, 0, 'validada') RETURNING id_cotizacion
				""", Long.class, cliente, usuario);
		venta = jdbc.queryForObject("""
				INSERT INTO venta (id_cotizacion, id_cliente, id_usuario_responsable, descuento, estado)
				VALUES (?, ?, ?, 0, 'pendiente_abastecimiento') RETURNING id_venta
				""", Long.class, cotizacion, cliente, usuario);
		detalle = detalle("100");
	}

	/** El estado de la venta es indiferente para estas pruebas; la línea admite la cantidad indicada. */
	private long detalle(String cantidad) {
		return jdbc.queryForObject("""
				INSERT INTO detalle_venta (id_venta, id_producto, cantidad_vendida, precio_unitario)
				VALUES (?, ?, CAST(? AS numeric), 10.00) RETURNING id_detalle_venta
				""", Long.class, venta, producto, cantidad);
	}

	@AfterEach
	void limpiar() {
		jdbc.execute("ALTER TABLE movimiento_kardex DISABLE TRIGGER tg_movimiento_kardex_solo_anexion");
		try {
			jdbc.update("DELETE FROM movimiento_kardex WHERE id_inventario IN (SELECT id_inventario FROM inventario WHERE id_producto = ?)", producto);
		} finally {
			jdbc.execute("ALTER TABLE movimiento_kardex ENABLE TRIGGER tg_movimiento_kardex_solo_anexion");
		}
		jdbc.update("DELETE FROM reserva_stock WHERE id_producto = ?", producto);
		if (recepcion != null) {
			jdbc.update("DELETE FROM orden_recepcion WHERE id_recepcion = ?", recepcion);
		}
		if (orden != null) {
			jdbc.update("DELETE FROM orden_compra WHERE id_orden = ?", orden);
		}
		if (proveedor != null) {
			jdbc.update("DELETE FROM proveedor WHERE id_proveedor = ?", proveedor);
		}
		if (guia != null) {
			jdbc.update("DELETE FROM guia_despacho WHERE id_guia = ?", guia);
		}
		jdbc.update("DELETE FROM detalle_venta WHERE id_venta = ?", venta);
		jdbc.update("DELETE FROM venta WHERE id_venta = ?", venta);
		jdbc.update("DELETE FROM cotizacion WHERE id_cotizacion = ?", cotizacion);
		jdbc.update("DELETE FROM cliente WHERE id_cliente = ?", cliente);
		escenario.limpiar();
	}

	private BigDecimal reservadoActivo() {
		return jdbc.queryForObject("SELECT coalesce(sum(cantidad_reservada), 0) FROM reserva_stock WHERE id_producto = ? AND estado = 'activa'",
				BigDecimal.class, producto);
	}

	@Test
	void reservaPrimeroEnLaTiendaYCompletaConElAlmacen() {
		List<Asignacion> asignaciones = servicio.reservar(usuario, detalle, producto, new BigDecimal("45"));

		assertEquals(2, asignaciones.size());
		assertEquals(tienda, asignaciones.get(0).idUbicacion());
		assertEquals(0, new BigDecimal("30").compareTo(asignaciones.get(0).cantidad()));
		assertEquals(deposito, asignaciones.get(1).idUbicacion());
		assertEquals(0, new BigDecimal("15").compareTo(asignaciones.get(1).cantidad()));
		assertEquals(0, escenario.movimientos(producto));
		assertEquals(0, new BigDecimal("80").compareTo(escenario.fisica(producto, tienda).add(escenario.fisica(producto, deposito))));
	}

	@Test
	void reservaSoloLoDisponibleCuandoElStockNoAlcanza() {
		List<Asignacion> asignaciones = servicio.reservar(usuario, detalle, producto, new BigDecimal("100"));

		BigDecimal total = asignaciones.stream().map(Asignacion::cantidad).reduce(BigDecimal.ZERO, BigDecimal::add);
		assertEquals(0, new BigDecimal("80").compareTo(total));
		assertEquals(0, servicio.reservar(usuario, detalle("5") , producto, new BigDecimal("5")).size());
	}

	@Test
	void unaSalidaManualNoInvadeElStockReservado() {
		servicio.reservar(usuario, detalle, producto, new BigDecimal("25"));

		ErrorDeNegocio error = assertThrows(ErrorDeNegocio.class, () -> movimientos.registrarAjuste(usuario,
				new SolicitudAjuste(TipoOperacion.SALIDA, producto, tienda, new BigDecimal("6"), "Merma", null)));
		assertEquals(CodigoError.STOCK_INSUFICIENTE, error.codigo());
		movimientos.registrarAjuste(usuario,
				new SolicitudAjuste(TipoOperacion.SALIDA, producto, tienda, new BigDecimal("5"), "Merma", null));
		assertEquals(0, new BigDecimal("25").compareTo(escenario.fisica(producto, tienda)));
	}

	@Test
	void liberarCierraLasReservasSinTocarElSaldo() {
		servicio.reservar(usuario, detalle, producto, new BigDecimal("40"));

		int liberadas = servicio.liberar(List.of(detalle));

		assertEquals(2, liberadas);
		assertEquals(0, BigDecimal.ZERO.compareTo(reservadoActivo()));
		assertEquals(0, new BigDecimal("30").compareTo(escenario.fisica(producto, tienda)));
		assertEquals(0, servicio.liberar(List.of(detalle)));
	}

	@Test
	void despacharConsumeLasReservasYRegistraSalidasDeKardex() {
		servicio.reservar(usuario, detalle, producto, new BigDecimal("40"));
		guia = jdbc.queryForObject("""
				INSERT INTO guia_despacho (id_venta, direccion_entrega, estado) VALUES (?, 'Jr. de prueba 1', 'emitida')
				RETURNING id_guia
				""", Long.class, venta);

		List<MovimientoDetalle> salidas = servicio.consumirDespacho(usuario, guia, List.of(detalle));

		assertEquals(2, salidas.size());
		assertEquals(OrigenMovimiento.DESPACHO, salidas.get(0).origenMovimiento());
		assertEquals(TipoOperacion.SALIDA, salidas.get(0).tipoOperacion());
		assertEquals(guia, salidas.get(0).idGuia());
		assertEquals(0, BigDecimal.ZERO.compareTo(escenario.fisica(producto, tienda)));
		assertEquals(0, new BigDecimal("40").compareTo(escenario.fisica(producto, deposito)));
		assertEquals(0, BigDecimal.ZERO.compareTo(reservadoActivo()));
		assertEquals(2, escenario.movimientos(producto));
	}

	@Test
	void unaRecepcionAumentaElSaldoYCreaLaExistenciaFaltante() {
		long nuevo = escenario.almacen("almacen");
		String ruc = String.valueOf(20000000000L + (Math.abs(UUID.randomUUID().getLeastSignificantBits()) % 99999999L));
		proveedor = jdbc.queryForObject("INSERT INTO proveedor (ruc, razon_social, contacto) VALUES (?, 'Proveedor prueba', 'Contacto') RETURNING id_proveedor",
				Long.class, ruc);
		orden = jdbc.queryForObject("INSERT INTO orden_compra (id_proveedor, id_usuario_responsable, estado) VALUES (?, ?, 'pendiente') RETURNING id_orden",
				Long.class, proveedor, usuario);
		recepcion = jdbc.queryForObject("""
				INSERT INTO orden_recepcion (id_orden, id_usuario_responsable, fecha_recepcion, estado)
				VALUES (?, ?, CURRENT_DATE, 'pendiente') RETURNING id_recepcion
				""", Long.class, orden, usuario);

		List<MovimientoDetalle> entradas = servicio.registrarEntradas(usuario, recepcion, List.of(
				new EntradaRecepcion(producto, deposito, new BigDecimal("12")),
				new EntradaRecepcion(producto, nuevo, new BigDecimal("7.5"))));

		assertEquals(2, entradas.size());
		assertEquals(OrigenMovimiento.RECEPCION, entradas.get(0).origenMovimiento());
		assertEquals(recepcion, entradas.get(0).idRecepcion());
		assertEquals(0, new BigDecimal("62").compareTo(escenario.fisica(producto, deposito)));
		assertEquals(0, new BigDecimal("7.5").compareTo(escenario.fisica(producto, nuevo)));
	}
}
