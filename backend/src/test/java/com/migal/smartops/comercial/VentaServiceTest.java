package com.migal.smartops.comercial;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;

import com.migal.smartops.TestcontainersConfiguration;
import com.migal.smartops.comercial.model.EstadoVenta;
import com.migal.smartops.comercial.service.VentaService;
import com.migal.smartops.comercial.service.VentaService.Cobertura;
import com.migal.smartops.comercial.service.VentaService.VentaDetalle;
import com.migal.smartops.comun.errores.CodigoError;
import com.migal.smartops.comun.errores.ErrorDeNegocio;
import com.migal.smartops.inventario.EscenarioInventario;
import com.migal.smartops.inventario.service.ReservaInventarioService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * Verifica la conversión, la cobertura, el reintento, la cancelación y la concurrencia de las ventas con PostgreSQL
 * real. Las operaciones se confirman, por lo que la clase no es transaccional y cada prueba elimina sus datos.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class VentaServiceTest {

	@Autowired
	private VentaService servicio;
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

	@BeforeEach
	void preparar() {
		escenario = new EscenarioInventario(jdbc);
		usuario = escenario.usuario();
		producto = escenario.producto("10");
		tienda = escenario.almacen("tienda");
		deposito = escenario.almacen("almacen");
		cliente = jdbc.queryForObject("INSERT INTO cliente (nombre_razon_social) VALUES ('Cliente venta') RETURNING id_cliente",
				Long.class);
	}

	@AfterEach
	void limpiar() {
		jdbc.update("DELETE FROM reserva_stock WHERE id_detalle_venta IN (SELECT d.id_detalle_venta FROM detalle_venta d "
				+ "JOIN venta v ON v.id_venta = d.id_venta WHERE v.id_cliente = ?)", cliente);
		jdbc.update("DELETE FROM detalle_venta WHERE id_venta IN (SELECT id_venta FROM venta WHERE id_cliente = ?)", cliente);
		jdbc.update("DELETE FROM venta WHERE id_cliente = ?", cliente);
		jdbc.update("DELETE FROM detalle_cotizacion WHERE id_cotizacion IN (SELECT id_cotizacion FROM cotizacion WHERE id_cliente = ?)",
				cliente);
		jdbc.update("DELETE FROM cotizacion WHERE id_cliente = ?", cliente);
		jdbc.update("DELETE FROM cliente WHERE id_cliente = ?", cliente);
		escenario.limpiar();
	}

	/** Cotización con una línea de {@code cantidad} unidades a 10.00 del producto de prueba, en el estado indicado. */
	private long cotizacion(String estado, String cantidad) {
		long id = jdbc.queryForObject("""
				INSERT INTO cotizacion (id_cliente, id_usuario_responsable, fecha_vencimiento, descuento, estado)
				VALUES (?, ?, CURRENT_DATE + 30, 0, ?) RETURNING id_cotizacion
				""", Long.class, cliente, usuario, estado);
		jdbc.update("INSERT INTO detalle_cotizacion (id_cotizacion, id_producto, cantidad_cotizada, precio_unitario) "
				+ "VALUES (?, ?, CAST(? AS numeric), 10.00)", id, producto, cantidad);
		return id;
	}

	private static Cobertura unica(VentaDetalle detalle) {
		return detalle.coberturaPorLinea().values().iterator().next();
	}

	private static void igual(String esperado, BigDecimal real) {
		assertEquals(0, new BigDecimal(esperado).compareTo(real), "esperado " + esperado + " y real " + real);
	}

	private static void assertCodigo(CodigoError codigo, org.junit.jupiter.api.function.Executable accion) {
		assertEquals(codigo, assertThrows(ErrorDeNegocio.class, accion).codigo());
	}

	@Test
	void convierteCotizacionValidadaYReservaLaLineaCompleta() {
		escenario.inventario(producto, tienda, "100");
		VentaDetalle detalle = servicio.registrarDesdeCotizacion(usuario, cotizacion("validada", "10"));

		assertEquals(EstadoVenta.PENDIENTE_DESPACHO, detalle.venta().getEstado());
		igual("10", unica(detalle).reservada());
		igual("0", unica(detalle).pendiente());
		assertEquals(1, detalle.reservas().size());
		assertEquals("activa", detalle.reservas().get(0).estado());
		assertEquals(tienda, detalle.reservas().get(0).idUbicacion());
		igual("100", escenario.fisica(producto, tienda));
		assertEquals(0, escenario.movimientos(producto));
	}

	@Test
	void reservaParcialDejaElRestoPendienteDeAbastecimiento() {
		escenario.inventario(producto, tienda, "30");
		escenario.inventario(producto, deposito, "0");
		VentaDetalle detalle = servicio.registrarDesdeCotizacion(usuario, cotizacion("validada", "40"));

		assertEquals(EstadoVenta.PENDIENTE_ABASTECIMIENTO, detalle.venta().getEstado());
		igual("30", unica(detalle).reservada());
		igual("10", unica(detalle).pendiente());
	}

	@Test
	void rechazaCotizacionesNoConvertibles() {
		escenario.inventario(producto, tienda, "100");
		assertCodigo(CodigoError.COTIZACION_NO_CONVERTIBLE,
				() -> servicio.registrarDesdeCotizacion(usuario, cotizacion("aceptada", "5")));
		assertCodigo(CodigoError.COTIZACION_VENCIDA,
				() -> servicio.registrarDesdeCotizacion(usuario, cotizacion("vencida", "5")));

		long validada = cotizacion("validada", "5");
		servicio.registrarDesdeCotizacion(usuario, validada);
		assertCodigo(CodigoError.COTIZACION_YA_CONVERTIDA, () -> servicio.registrarDesdeCotizacion(usuario, validada));

		jdbc.update("UPDATE cliente SET estado_activo = false WHERE id_cliente = ?", cliente);
		assertCodigo(CodigoError.CLIENTE_INACTIVO,
				() -> servicio.registrarDesdeCotizacion(usuario, cotizacion("validada", "5")));
	}

	@Test
	void unaConversionFallidaNoDejaReservasNiVenta() {
		escenario.inventario(producto, tienda, "100");
		long otroProducto = escenario.producto("10");
		escenario.inventario(otroProducto, tienda, "100");
		long cot = cotizacion("validada", "10");
		jdbc.update("INSERT INTO detalle_cotizacion (id_cotizacion, id_producto, cantidad_cotizada, precio_unitario) "
				+ "VALUES (?, ?, 5, 10.00)", cot, otroProducto);
		Mockito.doCallRealMethod().doThrow(new IllegalStateException("fallo simulado")).when(reservas)
				.reservar(anyLong(), anyLong(), anyLong(), any());

		assertThrows(IllegalStateException.class, () -> servicio.registrarDesdeCotizacion(usuario, cot));

		assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM venta WHERE id_cotizacion = ?", Integer.class, cot));
		assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM reserva_stock WHERE id_producto IN (?, ?)",
				Integer.class, producto, otroProducto));
	}

	@Test
	void dosConversionesConcurrentesNuncaReservanMasQueElStockLibre() throws Exception {
		escenario.inventario(producto, tienda, "40");
		List<Long> cotizaciones = List.of(cotizacion("validada", "30"), cotizacion("validada", "30"));
		ExecutorService hilos = Executors.newFixedThreadPool(2);
		CountDownLatch largada = new CountDownLatch(1);
		List<Future<VentaDetalle>> resultados = new ArrayList<>();
		for (long cot : cotizaciones) {
			resultados.add(hilos.submit(() -> {
				largada.await();
				return servicio.registrarDesdeCotizacion(usuario, cot);
			}));
		}
		largada.countDown();
		BigDecimal reservadoTotal = BigDecimal.ZERO;
		try {
			for (Future<VentaDetalle> resultado : resultados) {
				reservadoTotal = reservadoTotal.add(unica(resultado.get()).reservada());
			}
		} finally {
			hilos.shutdown();
		}
		igual("40", reservadoTotal);
		igual("40", jdbc.queryForObject("SELECT COALESCE(SUM(cantidad_reservada), 0) FROM reserva_stock "
				+ "WHERE id_producto = ? AND estado = 'activa'", BigDecimal.class, producto));
	}

	@Test
	void reintentarCoberturaReservaLoPendienteCuandoHayStock() {
		escenario.inventario(producto, tienda, "10");
		long venta = servicio.registrarDesdeCotizacion(usuario, cotizacion("validada", "25")).venta().getId();
		jdbc.update("UPDATE inventario SET cantidad_fisica = 30 WHERE id_producto = ? AND id_almacen = ?", producto, tienda);

		VentaDetalle detalle = servicio.reintentarCobertura(usuario, venta);

		assertEquals(EstadoVenta.PENDIENTE_DESPACHO, detalle.venta().getEstado());
		igual("25", unica(detalle).reservada());
		igual("0", unica(detalle).pendiente());
	}

	@Test
	void cancelarLiberaLasReservasYRegistraElMotivo() {
		escenario.inventario(producto, tienda, "100");
		long venta = servicio.registrarDesdeCotizacion(usuario, cotizacion("validada", "10")).venta().getId();

		VentaDetalle cancelada = servicio.cancelar(usuario, venta, "Cliente desistió de la obra");

		assertEquals(EstadoVenta.CANCELADA, cancelada.venta().getEstado());
		assertEquals("Cliente desistió de la obra", cancelada.venta().getMotivoCancelacion());
		assertNotNull(cancelada.venta().getFechaCancelacion());
		assertEquals("liberada", cancelada.reservas().get(0).estado());
		assertNotNull(cancelada.reservas().get(0).fechaCierre());
		igual("0", unica(cancelada).reservada());
		igual("100", escenario.fisica(producto, tienda));
		assertEquals(0, escenario.movimientos(producto));
	}

	@Test
	void cancelarExigeMotivoYRechazaVentasDespachadas() {
		escenario.inventario(producto, tienda, "100");
		long venta = servicio.registrarDesdeCotizacion(usuario, cotizacion("validada", "10")).venta().getId();
		assertCodigo(CodigoError.MOTIVO_REQUERIDO, () -> servicio.cancelar(usuario, venta, "   "));

		jdbc.update("UPDATE venta SET estado = 'despachada' WHERE id_venta = ?", venta);
		assertCodigo(CodigoError.VENTA_NO_CANCELABLE, () -> servicio.cancelar(usuario, venta, "Motivo"));
	}
}
