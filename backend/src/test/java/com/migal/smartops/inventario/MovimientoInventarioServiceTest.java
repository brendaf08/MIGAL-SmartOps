package com.migal.smartops.inventario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.migal.smartops.TestcontainersConfiguration;
import com.migal.smartops.comun.errores.CodigoError;
import com.migal.smartops.comun.errores.ErrorDeNegocio;
import com.migal.smartops.inventario.model.TipoOperacion;
import com.migal.smartops.inventario.service.MovimientoInventarioService;
import com.migal.smartops.inventario.service.MovimientoInventarioService.ResultadoMovimiento;
import com.migal.smartops.inventario.service.MovimientoInventarioService.ResultadoTraslado;
import com.migal.smartops.inventario.service.MovimientoInventarioService.SolicitudAjuste;
import com.migal.smartops.inventario.service.MovimientoInventarioService.SolicitudTraslado;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/** Verifica los movimientos manuales y traslados con PostgreSQL real, incluida la concurrencia. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class MovimientoInventarioServiceTest {

	@Autowired
	private MovimientoInventarioService servicio;
	@Autowired
	private JdbcTemplate jdbc;

	private EscenarioInventario escenario;
	private long usuario;
	private long producto;
	private long tienda;
	private long deposito;

	@BeforeEach
	void preparar() {
		escenario = new EscenarioInventario(jdbc);
		usuario = escenario.usuario();
		producto = escenario.producto("10");
		tienda = escenario.almacen("tienda");
		deposito = escenario.almacen("almacen");
		escenario.inventario(producto, tienda, "40");
	}

	@AfterEach
	void limpiar() {
		escenario.limpiar();
	}

	private SolicitudAjuste ajuste(TipoOperacion tipo, long ubicacion, String cantidad, String motivo, Long corregido) {
		return new SolicitudAjuste(tipo, producto, ubicacion, new BigDecimal(cantidad), motivo, corregido);
	}

	private ErrorDeNegocio rechazo(Runnable accion) {
		return assertThrows(ErrorDeNegocio.class, accion::run);
	}

	@Test
	void unaSalidaReduceElSaldoYRegistraElMovimiento() {
		ResultadoMovimiento r = servicio.registrarAjuste(usuario, ajuste(TipoOperacion.SALIDA, tienda, "5", "Bolsas dañadas", null));

		assertEquals(0, new BigDecimal("40").compareTo(r.cambio().antes()));
		assertEquals(0, new BigDecimal("35").compareTo(r.cambio().despues()));
		assertEquals(0, new BigDecimal("35").compareTo(escenario.fisica(producto, tienda)));
		assertEquals("ajuste", r.movimiento().origenMovimiento().valor());
		assertEquals("Bolsas dañadas", r.movimiento().motivo());
		assertNotNull(r.movimiento().fechaHora());
	}

	@Test
	void unaEntradaEnUnaUbicacionNuevaCreaLaExistencia() {
		ResultadoMovimiento r = servicio.registrarAjuste(usuario, ajuste(TipoOperacion.ENTRADA, deposito, "7.5", "Sobrante detectado", null));

		assertEquals(0, new BigDecimal("7.5").compareTo(r.cambio().despues()));
		assertEquals(0, new BigDecimal("7.5").compareTo(escenario.fisica(producto, deposito)));
	}

	@Test
	void unaSalidaMayorAlStockLibreSeRechazaSinMovimientos() {
		ErrorDeNegocio error = rechazo(() -> servicio.registrarAjuste(usuario,
				ajuste(TipoOperacion.SALIDA, tienda, "40.001", "Merma", null)));

		assertEquals(CodigoError.STOCK_INSUFICIENTE, error.codigo());
		assertEquals(0, escenario.movimientos(producto));
		assertEquals(0, new BigDecimal("40").compareTo(escenario.fisica(producto, tienda)));
	}

	@Test
	void unaSalidaSinExistenciaEnLaUbicacionEsStockInsuficiente() {
		assertEquals(CodigoError.STOCK_INSUFICIENTE, rechazo(() -> servicio.registrarAjuste(usuario,
				ajuste(TipoOperacion.SALIDA, deposito, "1", "Merma", null))).codigo());
	}

	@Test
	void validaMotivoCantidadYVigencia() {
		assertEquals(CodigoError.MOTIVO_REQUERIDO, rechazo(() -> servicio.registrarAjuste(usuario,
				ajuste(TipoOperacion.SALIDA, tienda, "1", "   ", null))).codigo());
		assertEquals(CodigoError.VALIDACION_FALLIDA, rechazo(() -> servicio.registrarAjuste(usuario,
				ajuste(TipoOperacion.SALIDA, tienda, "0", "Merma", null))).codigo());
		assertEquals(CodigoError.VALIDACION_FALLIDA, rechazo(() -> servicio.registrarAjuste(usuario,
				ajuste(TipoOperacion.SALIDA, tienda, "1.0001", "Merma", null))).codigo());
		assertEquals(CodigoError.RECURSO_NO_ENCONTRADO, rechazo(() -> servicio.registrarAjuste(usuario,
				new SolicitudAjuste(TipoOperacion.SALIDA, -1, tienda, BigDecimal.ONE, "Merma", null))).codigo());
		jdbc.update("UPDATE almacen SET estado_activo = false WHERE id_almacen = ?", tienda);
		assertEquals(CodigoError.ALMACEN_INACTIVO, rechazo(() -> servicio.registrarAjuste(usuario,
				ajuste(TipoOperacion.SALIDA, tienda, "1", "Merma", null))).codigo());
		jdbc.update("UPDATE producto SET estado_activo = false WHERE id_producto = ?", producto);
		jdbc.update("UPDATE almacen SET estado_activo = true WHERE id_almacen = ?", tienda);
		assertEquals(CodigoError.PRODUCTO_INACTIVO, rechazo(() -> servicio.registrarAjuste(usuario,
				ajuste(TipoOperacion.SALIDA, tienda, "1", "Merma", null))).codigo());
	}

	@Test
	void unaCorreccionCompensaConTipoOpuestoUnaSolaVez() {
		long original = servicio.registrarAjuste(usuario, ajuste(TipoOperacion.SALIDA, tienda, "5", "Error de conteo", null))
				.movimiento().id();

		assertEquals(CodigoError.MOVIMIENTO_NO_CORREGIBLE, rechazo(() -> servicio.registrarAjuste(usuario,
				ajuste(TipoOperacion.SALIDA, tienda, "5", "Mismo tipo", original))).codigo());
		ResultadoMovimiento compensacion = servicio.registrarAjuste(usuario,
				ajuste(TipoOperacion.ENTRADA, tienda, "5", "Corrección del conteo", original));

		assertEquals(original, compensacion.movimiento().idMovimientoCorregido());
		assertEquals(0, new BigDecimal("40").compareTo(escenario.fisica(producto, tienda)));
		assertEquals(CodigoError.MOVIMIENTO_NO_CORREGIBLE, rechazo(() -> servicio.registrarAjuste(usuario,
				ajuste(TipoOperacion.ENTRADA, tienda, "5", "Segunda corrección", original))).codigo());
		assertEquals(CodigoError.RECURSO_NO_ENCONTRADO, rechazo(() -> servicio.registrarAjuste(usuario,
				ajuste(TipoOperacion.ENTRADA, tienda, "5", "Inexistente", 999999999L))).codigo());
	}

	@Test
	void unTrasladoMueveElStockYGeneraDosMovimientosConLaMismaReferencia() {
		ResultadoTraslado r = servicio.registrarTraslado(usuario,
				new SolicitudTraslado(producto, tienda, deposito, new BigDecimal("15"), "Reposición"));

		assertEquals(0, new BigDecimal("25").compareTo(escenario.fisica(producto, tienda)));
		assertEquals(0, new BigDecimal("15").compareTo(escenario.fisica(producto, deposito)));
		assertEquals(2, r.movimientos().size());
		assertEquals(TipoOperacion.SALIDA, r.movimientos().get(0).tipoOperacion());
		assertEquals(TipoOperacion.ENTRADA, r.movimientos().get(1).tipoOperacion());
		assertEquals(r.referenciaTraslado(), r.movimientos().get(0).referenciaTraslado());
		assertEquals(r.referenciaTraslado(), r.movimientos().get(1).referenciaTraslado());
		assertEquals(2, r.cambios().size());
	}

	@Test
	void unTrasladoRechazadoNoDejaRastro() {
		assertEquals(CodigoError.STOCK_INSUFICIENTE, rechazo(() -> servicio.registrarTraslado(usuario,
				new SolicitudTraslado(producto, tienda, deposito, new BigDecimal("41"), "Reposición"))).codigo());
		assertEquals(CodigoError.VALIDACION_FALLIDA, rechazo(() -> servicio.registrarTraslado(usuario,
				new SolicitudTraslado(producto, tienda, tienda, BigDecimal.ONE, "Mismo lugar"))).codigo());
		assertEquals(CodigoError.STOCK_INSUFICIENTE, rechazo(() -> servicio.registrarTraslado(usuario,
				new SolicitudTraslado(producto, deposito, tienda, BigDecimal.ONE, "Origen vacío"))).codigo());

		assertEquals(0, escenario.movimientos(producto));
		assertEquals(0, new BigDecimal("40").compareTo(escenario.fisica(producto, tienda)));
		assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM inventario WHERE id_producto = ? AND id_almacen = ?",
				Integer.class, producto, deposito));
	}

	@Test
	void salidasSimultaneasNuncaSuperanElStockLibre() throws Exception {
		int solicitudes = 8;
		ExecutorService hilos = Executors.newFixedThreadPool(solicitudes);
		CountDownLatch largada = new CountDownLatch(1);
		List<Future<Boolean>> resultados = new ArrayList<>();
		for (int i = 0; i < solicitudes; i++) {
			Callable<Boolean> tarea = () -> {
				largada.await();
				try {
					servicio.registrarAjuste(usuario, ajuste(TipoOperacion.SALIDA, tienda, "10", "Venta mostrador", null));
					return true;
				} catch (ErrorDeNegocio e) {
					assertEquals(CodigoError.STOCK_INSUFICIENTE, e.codigo());
					return false;
				}
			};
			resultados.add(hilos.submit(tarea));
		}
		largada.countDown();
		int exitosas = 0;
		for (Future<Boolean> f : resultados) {
			exitosas += f.get() ? 1 : 0;
		}
		hilos.shutdown();

		assertEquals(4, exitosas);
		assertEquals(0, BigDecimal.ZERO.compareTo(escenario.fisica(producto, tienda)));
		assertEquals(4, escenario.movimientos(producto));
	}

	@Test
	void trasladosCruzadosSimultaneosNoGeneranInterbloqueo() throws Exception {
		escenario.inventario(producto, deposito, "40");
		int pares = 6;
		ExecutorService hilos = Executors.newFixedThreadPool(pares * 2);
		CountDownLatch largada = new CountDownLatch(1);
		List<Future<?>> tareas = new ArrayList<>();
		for (int i = 0; i < pares; i++) {
			tareas.add(hilos.submit(() -> {
				largada.await();
				servicio.registrarTraslado(usuario, new SolicitudTraslado(producto, tienda, deposito, BigDecimal.ONE, "Ida"));
				return null;
			}));
			tareas.add(hilos.submit(() -> {
				largada.await();
				servicio.registrarTraslado(usuario, new SolicitudTraslado(producto, deposito, tienda, BigDecimal.ONE, "Vuelta"));
				return null;
			}));
		}
		largada.countDown();
		for (Future<?> t : tareas) {
			t.get();
		}
		hilos.shutdown();

		BigDecimal total = escenario.fisica(producto, tienda).add(escenario.fisica(producto, deposito));
		assertEquals(0, new BigDecimal("80").compareTo(total));
		assertTrue(escenario.movimientos(producto) == pares * 4);
	}
}
