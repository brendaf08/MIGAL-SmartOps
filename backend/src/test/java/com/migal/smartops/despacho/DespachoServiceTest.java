package com.migal.smartops.despacho;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;

import com.migal.smartops.TestcontainersConfiguration;
import com.migal.smartops.comercial.service.VentaService;
import com.migal.smartops.comun.errores.CodigoError;
import com.migal.smartops.comun.errores.ErrorDeNegocio;
import com.migal.smartops.despacho.model.EstadoGuia;
import com.migal.smartops.despacho.service.DespachoService;
import com.migal.smartops.despacho.service.DespachoService.Cambio;
import com.migal.smartops.despacho.service.DespachoService.GuiaDetalle;
import com.migal.smartops.despacho.service.DespachoService.ResultadoSalida;
import com.migal.smartops.inventario.EscenarioInventario;
import com.migal.smartops.inventario.model.OrigenMovimiento;
import com.migal.smartops.inventario.model.TipoOperacion;
import com.migal.smartops.inventario.service.ReservaInventarioService;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * Verifica la emisión, la confirmación de la salida, la entrega, la reimpresión y la anulación de guías con PostgreSQL
 * real. Las operaciones se confirman, por lo que la clase no es transaccional y cada prueba elimina sus datos.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class DespachoServiceTest {

	private static final String DIRECCION = "Av. de prueba 100";

	@Autowired
	private DespachoService servicio;
	@Autowired
	private VentaService ventas;
	@Autowired
	private JdbcTemplate jdbc;
	@MockitoSpyBean
	private ReservaInventarioService reservas;

	private EscenarioInventario escenario;
	private long usuario;
	private long producto;
	private long tienda;
	private long deposito;
	private long cliente;
	private long venta;
	private long detalle;

	@BeforeEach
	void preparar() {
		escenario = new EscenarioInventario(jdbc);
		usuario = escenario.usuario();
		producto = escenario.producto("10");
		tienda = escenario.almacen("tienda");
		deposito = escenario.almacen("almacen");
		escenario.inventario(producto, tienda, "30");
		escenario.inventario(producto, deposito, "50");
		cliente = jdbc.queryForObject("INSERT INTO cliente (nombre_razon_social, direccion_referencia) "
				+ "VALUES ('Cliente despacho', ?) RETURNING id_cliente", Long.class, DIRECCION);
	}

	@AfterEach
	void limpiar() {
		jdbc.execute("ALTER TABLE movimiento_kardex DISABLE TRIGGER tg_movimiento_kardex_solo_anexion");
		jdbc.execute("ALTER TABLE reimpresion_guia DISABLE TRIGGER tg_reimpresion_guia_solo_anexion");
		try {
			jdbc.update("DELETE FROM movimiento_kardex WHERE id_guia IN (SELECT g.id_guia FROM guia_despacho g "
					+ "JOIN venta v ON v.id_venta = g.id_venta WHERE v.id_cliente = ?)", cliente);
			jdbc.update("DELETE FROM reimpresion_guia WHERE id_guia IN (SELECT g.id_guia FROM guia_despacho g "
					+ "JOIN venta v ON v.id_venta = g.id_venta WHERE v.id_cliente = ?)", cliente);
		} finally {
			jdbc.execute("ALTER TABLE reimpresion_guia ENABLE TRIGGER tg_reimpresion_guia_solo_anexion");
			jdbc.execute("ALTER TABLE movimiento_kardex ENABLE TRIGGER tg_movimiento_kardex_solo_anexion");
		}
		jdbc.update("DELETE FROM guia_despacho WHERE id_venta IN (SELECT id_venta FROM venta WHERE id_cliente = ?)", cliente);
		jdbc.update("DELETE FROM reserva_stock WHERE id_detalle_venta IN (SELECT d.id_detalle_venta FROM detalle_venta d "
				+ "JOIN venta v ON v.id_venta = d.id_venta WHERE v.id_cliente = ?)", cliente);
		jdbc.update("DELETE FROM detalle_venta WHERE id_venta IN (SELECT id_venta FROM venta WHERE id_cliente = ?)", cliente);
		jdbc.update("DELETE FROM venta WHERE id_cliente = ?", cliente);
		jdbc.update("DELETE FROM cotizacion WHERE id_cliente = ?", cliente);
		jdbc.update("DELETE FROM cliente WHERE id_cliente = ?", cliente);
		escenario.limpiar();
	}

	/** Crea una venta de una línea con la cantidad vendida y el estado indicados. No reserva stock. */
	private void crearVenta(String vendida, String estado) {
		long cotizacion = jdbc.queryForObject("""
				INSERT INTO cotizacion (id_cliente, id_usuario_responsable, fecha_vencimiento, descuento, estado)
				VALUES (?, ?, CURRENT_DATE + 30, 0, 'validada') RETURNING id_cotizacion
				""", Long.class, cliente, usuario);
		venta = jdbc.queryForObject("""
				INSERT INTO venta (id_cotizacion, id_cliente, id_usuario_responsable, descuento, estado)
				VALUES (?, ?, ?, 0, ?) RETURNING id_venta
				""", Long.class, cotizacion, cliente, usuario, estado);
		detalle = jdbc.queryForObject("""
				INSERT INTO detalle_venta (id_venta, id_producto, cantidad_vendida, precio_unitario)
				VALUES (?, ?, CAST(? AS numeric), 10.00) RETURNING id_detalle_venta
				""", Long.class, venta, producto, vendida);
	}

	/** Venta de 40 unidades con todo el stock reservado (30 en la tienda y 10 en el almacén). */
	private void ventaReservada() {
		crearVenta("40", "pendiente_despacho");
		reservas.reservar(usuario, detalle, producto, new BigDecimal("40"));
	}

	private String estadoVenta() {
		return jdbc.queryForObject("SELECT estado FROM venta WHERE id_venta = ?", String.class, venta);
	}

	private String estadoGuia(long guia) {
		return jdbc.queryForObject("SELECT estado FROM guia_despacho WHERE id_guia = ?", String.class, guia);
	}

	private BigDecimal reservadoActivo() {
		return jdbc.queryForObject("SELECT coalesce(sum(cantidad_reservada), 0) FROM reserva_stock "
				+ "WHERE id_detalle_venta = ? AND estado = 'activa'", BigDecimal.class, detalle);
	}

	private static void assertCodigo(CodigoError codigo, Executable accion) {
		ErrorDeNegocio error = assertThrows(ErrorDeNegocio.class, accion);
		assertEquals(codigo, error.codigo());
	}

	/** Cambio de existencia de la ubicación indicada. */
	private static Cambio cambioEn(List<Cambio> cambios, long ubicacion) {
		return cambios.stream().filter(c -> c.ubicacionId() == ubicacion).findFirst().orElseThrow();
	}

	@Test
	void emitirCreaLaGuiaConLasUbicacionesDeLasReservasSinMoverElKardex() {
		ventaReservada();

		GuiaDetalle guia = servicio.emitir(venta);

		assertEquals(EstadoGuia.EMITIDA, guia.guia().getEstado());
		assertNotNull(guia.guia().getCodigo());
		assertEquals(DIRECCION, guia.guia().getDireccionEntrega());
		List<DespachoService.UbicacionSalida> ubicaciones = guia.lineas().get(0).ubicaciones();
		assertEquals(2, ubicaciones.size());
		assertEquals(0, new BigDecimal("30").compareTo(ubicaciones.get(0).cantidad()));
		assertEquals(0, new BigDecimal("10").compareTo(ubicaciones.get(1).cantidad()));
		assertEquals("lista_para_despacho", estadoVenta());
		assertEquals(0, escenario.movimientos(producto));
	}

	@Test
	void emitirRechazaUnaSegundaGuiaParaLaMismaVenta() {
		ventaReservada();
		servicio.emitir(venta);

		assertCodigo(CodigoError.GUIA_EXISTENTE, () -> servicio.emitir(venta));
	}

	@Test
	void emitirRechazaUnaVentaQueNoEstaPendienteDeDespacho() {
		crearVenta("40", "pendiente_abastecimiento");

		assertCodigo(CodigoError.VENTA_NO_DESPACHABLE, () -> servicio.emitir(venta));
	}

	@Test
	void emitirRechazaUnaVentaConReservaIncompleta() {
		crearVenta("100", "pendiente_despacho");
		reservas.reservar(usuario, detalle, producto, new BigDecimal("80"));

		assertCodigo(CodigoError.RESERVA_INCOMPLETA, () -> servicio.emitir(venta));
	}

	@Test
	void confirmarLaSalidaConsumeLasReservasYRegistraElKardex() {
		ventaReservada();
		long guia = servicio.emitir(venta).guia().getId();

		ResultadoSalida salida = servicio.confirmarSalida(usuario, guia);

		assertEquals(EstadoGuia.SALIDA_CONFIRMADA, salida.guia().guia().getEstado());
		assertNotNull(salida.guia().guia().getFechaSalidaReal());
		assertEquals(2, salida.movimientos().size());
		assertEquals(OrigenMovimiento.DESPACHO, salida.movimientos().get(0).origenMovimiento());
		assertEquals(TipoOperacion.SALIDA, salida.movimientos().get(0).tipoOperacion());
		assertEquals(guia, salida.movimientos().get(0).idGuia());
		assertEquals("despachada", estadoVenta());
		assertEquals(0, BigDecimal.ZERO.compareTo(escenario.fisica(producto, tienda)));
		assertEquals(0, new BigDecimal("40").compareTo(escenario.fisica(producto, deposito)));
		assertEquals(0, BigDecimal.ZERO.compareTo(reservadoActivo()));
		assertEquals(0, new BigDecimal("30").compareTo(cambioEn(salida.cambios(), tienda).antes()));
		assertEquals(0, BigDecimal.ZERO.compareTo(cambioEn(salida.cambios(), tienda).despues()));
		assertEquals(0, new BigDecimal("50").compareTo(cambioEn(salida.cambios(), deposito).antes()));
		assertEquals(0, new BigDecimal("40").compareTo(cambioEn(salida.cambios(), deposito).despues()));
	}

	@Test
	void unaSalidaYaConfirmadaNoSeRepite() {
		ventaReservada();
		long guia = servicio.emitir(venta).guia().getId();
		servicio.confirmarSalida(usuario, guia);

		assertCodigo(CodigoError.SALIDA_NO_CONFIRMABLE, () -> servicio.confirmarSalida(usuario, guia));
		assertEquals(2, escenario.movimientos(producto));
	}

	@Test
	void laConfirmacionEsAtomicaSiFallaElConsumoDeReservas() {
		ventaReservada();
		long guia = servicio.emitir(venta).guia().getId();
		doThrow(new IllegalStateException("Fallo simulado")).when(reservas)
				.consumirDespacho(anyLong(), anyLong(), anyCollection());

		assertThrows(IllegalStateException.class, () -> servicio.confirmarSalida(usuario, guia));

		assertEquals("emitida", estadoGuia(guia));
		assertEquals("lista_para_despacho", estadoVenta());
		assertEquals(0, new BigDecimal("30").compareTo(escenario.fisica(producto, tienda)));
		assertEquals(0, new BigDecimal("50").compareTo(escenario.fisica(producto, deposito)));
		assertEquals(0, escenario.movimientos(producto));
		assertEquals(0, new BigDecimal("40").compareTo(reservadoActivo()));
	}

	@Test
	void dosConfirmacionesSimultaneasSoloConsumenUnaVez() throws Exception {
		ventaReservada();
		long guia = servicio.emitir(venta).guia().getId();
		ExecutorService pool = Executors.newFixedThreadPool(2);
		CountDownLatch inicio = new CountDownLatch(1);
		Callable<String> intento = () -> {
			inicio.await();
			try {
				servicio.confirmarSalida(usuario, guia);
				return "OK";
			} catch (ErrorDeNegocio e) {
				return e.codigo().name();
			}
		};
		try {
			Future<String> primero = pool.submit(intento);
			Future<String> segundo = pool.submit(intento);
			inicio.countDown();
			List<String> resultados = List.of(primero.get(60, TimeUnit.SECONDS), segundo.get(60, TimeUnit.SECONDS));

			assertEquals(1, resultados.stream().filter("OK"::equals).count());
			assertEquals(1, resultados.stream().filter("SALIDA_NO_CONFIRMABLE"::equals).count());
		} finally {
			pool.shutdownNow();
		}
		assertEquals(2, escenario.movimientos(producto));
		assertEquals(0, BigDecimal.ZERO.compareTo(escenario.fisica(producto, tienda)));
		assertEquals(0, new BigDecimal("40").compareTo(escenario.fisica(producto, deposito)));
	}

	@Test
	void registraLaEntregaUnaSolaVez() {
		ventaReservada();
		long guia = servicio.emitir(venta).guia().getId();
		servicio.confirmarSalida(usuario, guia);

		GuiaDetalle entregada = servicio.registrarEntrega(guia);

		assertEquals(EstadoGuia.ENTREGADA, entregada.guia().getEstado());
		assertNotNull(entregada.guia().getFechaEntrega());
		assertCodigo(CodigoError.TRANSICION_NO_PERMITIDA, () -> servicio.registrarEntrega(guia));
	}

	@Test
	void laReimpresionExigeMotivoYNoCambiaElEstado() {
		ventaReservada();
		long guia = servicio.emitir(venta).guia().getId();

		assertCodigo(CodigoError.MOTIVO_REQUERIDO, () -> servicio.registrarReimpresion(usuario, guia, "   "));
		var reimpresion = servicio.registrarReimpresion(usuario, guia, "Documento dañado durante el traslado");

		assertEquals("Documento dañado durante el traslado", reimpresion.getMotivo());
		assertEquals("emitida", estadoGuia(guia));
		assertEquals(0, escenario.movimientos(producto));
	}

	@Test
	void cancelarLaVentaAnulaSuGuiaYLiberaLasReservas() {
		ventaReservada();
		long guia = servicio.emitir(venta).guia().getId();

		ventas.cancelar(usuario, venta, "Cliente desistió de la obra");

		assertEquals("anulada", estadoGuia(guia));
		assertEquals("cancelada", estadoVenta());
		assertEquals(0, BigDecimal.ZERO.compareTo(reservadoActivo()));
		assertEquals(0, escenario.movimientos(producto));
	}
}
