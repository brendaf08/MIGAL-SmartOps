package com.migal.smartops.recepciones;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;

import com.migal.smartops.TestcontainersConfiguration;
import com.migal.smartops.abastecimiento.model.EstadoOrdenCompra;
import com.migal.smartops.abastecimiento.service.OrdenCompraService;
import com.migal.smartops.abastecimiento.service.OrdenCompraService.DatosOrden;
import com.migal.smartops.abastecimiento.service.OrdenCompraService.LineaOrden;
import com.migal.smartops.comun.errores.CodigoError;
import com.migal.smartops.comun.errores.ErrorDeNegocio;
import com.migal.smartops.inventario.EscenarioInventario;
import com.migal.smartops.inventario.service.ReservaInventarioService;
import com.migal.smartops.recepciones.model.EstadoRecepcion;
import com.migal.smartops.recepciones.service.RecepcionService;
import com.migal.smartops.recepciones.service.RecepcionService.DatosRecepcion;
import com.migal.smartops.recepciones.service.RecepcionService.LineaRecepcion;
import com.migal.smartops.recepciones.service.RecepcionService.ResultadoValidacion;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * Verifica la validación de recepciones con PostgreSQL real: entradas de Kardex, estado de la orden, sobrantes, atomicidad
 * ante un fallo del registro de entradas y concurrencia entre dos validaciones de la misma recepción. La clase no es
 * transaccional porque las pruebas de concurrencia requieren confirmaciones independientes; cada prueba elimina sus datos.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class RecepcionServiceTest {

	@Autowired
	private RecepcionService recepciones;
	@Autowired
	private OrdenCompraService ordenes;
	@Autowired
	private JdbcTemplate jdbc;
	@MockitoSpyBean
	private ReservaInventarioService reservas;

	private EscenarioInventario escenario;
	private long usuario;
	private long producto;
	private long tienda;
	private long deposito;
	private long proveedor;
	private String sku;

	@BeforeEach
	void preparar() {
		escenario = new EscenarioInventario(jdbc);
		usuario = escenario.usuario();
		producto = escenario.producto("10");
		tienda = escenario.almacen("tienda");
		deposito = escenario.almacen("almacen");
		escenario.inventario(producto, tienda, "30");
		sku = jdbc.queryForObject("SELECT sku FROM producto WHERE id_producto = ?", String.class, producto);
		proveedor = jdbc.queryForObject("INSERT INTO proveedor (ruc, razon_social, contacto) VALUES (?, 'Proveedor recepciones', "
				+ "'ventas@prueba.example') RETURNING id_proveedor", Long.class, ruc());
	}

	@AfterEach
	void limpiar() {
		jdbc.execute("ALTER TABLE movimiento_kardex DISABLE TRIGGER tg_movimiento_kardex_solo_anexion");
		try {
			jdbc.update("DELETE FROM movimiento_kardex WHERE id_recepcion IN (SELECT r.id_recepcion FROM orden_recepcion r "
					+ "JOIN orden_compra o ON o.id_orden = r.id_orden WHERE o.id_proveedor = ?)", proveedor);
		} finally {
			jdbc.execute("ALTER TABLE movimiento_kardex ENABLE TRIGGER tg_movimiento_kardex_solo_anexion");
		}
		jdbc.update("DELETE FROM detalle_recepcion WHERE id_recepcion IN (SELECT r.id_recepcion FROM orden_recepcion r "
				+ "JOIN orden_compra o ON o.id_orden = r.id_orden WHERE o.id_proveedor = ?)", proveedor);
		jdbc.update("DELETE FROM orden_recepcion WHERE id_orden IN (SELECT id_orden FROM orden_compra WHERE id_proveedor = ?)",
				proveedor);
		jdbc.update("DELETE FROM detalle_compra WHERE id_orden IN (SELECT id_orden FROM orden_compra WHERE id_proveedor = ?)",
				proveedor);
		jdbc.update("DELETE FROM orden_compra WHERE id_proveedor = ?", proveedor);
		jdbc.update("DELETE FROM proveedor WHERE id_proveedor = ?", proveedor);
		escenario.limpiar();
	}

	@Test
	void registrarNoModificaElInventarioNiElKardex() {
		long orden = ordenPendiente(100);

		recepciones.registrar(usuario, datos(orden, List.of(new LineaRecepcion(producto, tienda, new BigDecimal("40")))));

		assertEquals(0, new BigDecimal("30.000").compareTo(escenario.fisica(producto, tienda)));
		assertEquals(0, escenario.movimientos(producto));
	}

	@Test
	void validarRegistraLaEntradaYDejaLaOrdenParcial() {
		long orden = ordenPendiente(100);
		long recepcion = recepcion(orden, 40, null);

		ResultadoValidacion resultado = recepciones.validar(usuario, recepcion, null, false);

		assertEquals(EstadoRecepcion.VALIDADA, resultado.recepcion().recepcion().getEstado());
		assertEquals(1, resultado.movimientos().size());
		assertEquals(0, new BigDecimal("70.000").compareTo(escenario.fisica(producto, tienda)));
		assertEquals(0, new BigDecimal("30.000").compareTo(resultado.cambios().get(0).antes()));
		assertEquals(EstadoOrdenCompra.PARCIAL, resultado.ordenCompra().orden().getEstado());
	}

	@Test
	void validarCompletaLaOrdenCuandoSeRecibeLaCantidadTotal() {
		long orden = ordenPendiente(100);
		long recepcion = recepcion(orden, 100, null);

		ResultadoValidacion resultado = recepciones.validar(usuario, recepcion, null, false);

		assertEquals(EstadoOrdenCompra.COMPLETADA, resultado.ordenCompra().orden().getEstado());
	}

	@Test
	void rechazaValidarDosVecesLaMismaRecepcion() {
		long orden = ordenPendiente(100);
		long recepcion = recepcion(orden, 40, null);
		recepciones.validar(usuario, recepcion, null, false);

		ErrorDeNegocio error = assertThrows(ErrorDeNegocio.class, () -> recepciones.validar(usuario, recepcion, null, false));

		assertEquals(CodigoError.RECEPCION_NO_CONFIRMABLE, error.codigo());
		assertEquals(1, escenario.movimientos(producto));
	}

	@Test
	void sobranteSinNotaRechazaLaValidacionYConNotaLaPermite() {
		long orden = ordenPendiente(100);
		long recepcion = recepcion(orden, 120, null);

		ErrorDeNegocio error = assertThrows(ErrorDeNegocio.class, () -> recepciones.validar(usuario, recepcion, null, false));
		assertEquals(CodigoError.SOBRANTE_SIN_NOTA, error.codigo());
		assertEquals(0, escenario.movimientos(producto));

		ResultadoValidacion resultado = recepciones.validar(usuario, recepcion, "Llegaron 20 bolsas adicionales", true);
		assertEquals(EstadoRecepcion.VALIDADA, resultado.recepcion().recepcion().getEstado());
	}

	@Test
	void ordenCompletadaRechazaNuevasRecepciones() {
		long orden = ordenPendiente(100);
		recepciones.validar(usuario, recepcion(orden, 100, null), null, false);

		ErrorDeNegocio error = assertThrows(ErrorDeNegocio.class,
				() -> recepciones.registrar(usuario, datos(orden, List.of(new LineaRecepcion(producto, tienda, BigDecimal.ONE)))));

		assertEquals(CodigoError.ORDEN_COMPRA_CERRADA, error.codigo());
	}

	@Test
	void rechazaUnProductoOUnaUbicacionInactivos() {
		long orden = ordenPendiente(100);
		jdbc.update("UPDATE almacen SET estado_activo = false WHERE id_almacen = ?", deposito);
		ErrorDeNegocio almacen = assertThrows(ErrorDeNegocio.class, () -> recepciones.registrar(usuario,
				datos(orden, List.of(new LineaRecepcion(producto, deposito, BigDecimal.ONE)))));
		assertEquals(CodigoError.ALMACEN_INACTIVO, almacen.codigo());

		jdbc.update("UPDATE producto SET estado_activo = false WHERE id_producto = ?", producto);
		ErrorDeNegocio productoInactivo = assertThrows(ErrorDeNegocio.class, () -> recepciones.registrar(usuario,
				datos(orden, List.of(new LineaRecepcion(producto, tienda, BigDecimal.ONE)))));
		assertEquals(CodigoError.PRODUCTO_INACTIVO, productoInactivo.codigo());
	}

	@Test
	void unFalloAlRegistrarLasEntradasDejaLaRecepcionYLaOrdenSinCambios() {
		long orden = ordenPendiente(100);
		long recepcion = recepcion(orden, 40, null);
		doThrow(new IllegalStateException("Fallo simulado")).when(reservas).registrarEntradas(anyLong(), anyLong(), anyList());

		assertThrows(IllegalStateException.class, () -> recepciones.validar(usuario, recepcion, null, false));

		assertEquals(EstadoRecepcion.PENDIENTE, recepciones.consultar(recepcion).recepcion().getEstado());
		assertEquals(EstadoOrdenCompra.PENDIENTE, ordenes.consultar(orden).orden().getEstado());
		assertEquals(0, new BigDecimal("30.000").compareTo(escenario.fisica(producto, tienda)));
		assertEquals(0, escenario.movimientos(producto));
	}

	@Test
	void dosValidacionesSimultaneasRegistranLasEntradasUnaSolaVez() throws Exception {
		long orden = ordenPendiente(100);
		long recepcion = recepcion(orden, 40, null);
		ExecutorService pool = Executors.newFixedThreadPool(2);
		CountDownLatch salida = new CountDownLatch(1);
		Callable<ResultadoValidacion> validacion = () -> {
			salida.await();
			return recepciones.validar(usuario, recepcion, null, false);
		};
		try {
			Future<ResultadoValidacion> primera = pool.submit(validacion);
			Future<ResultadoValidacion> segunda = pool.submit(validacion);
			salida.countDown();
			int exitos = 0;
			for (Future<ResultadoValidacion> futuro : List.of(primera, segunda)) {
				try {
					futuro.get();
					exitos++;
				} catch (java.util.concurrent.ExecutionException e) {
					ErrorDeNegocio error = (ErrorDeNegocio) e.getCause();
					assertTrue(error.codigo() == CodigoError.RECEPCION_NO_CONFIRMABLE
							|| error.codigo() == CodigoError.PETICION_CONCURRENTE, "Código inesperado: " + error.codigo());
				}
			}
			assertEquals(1, exitos);
		} finally {
			pool.shutdownNow();
		}
		assertEquals(1, escenario.movimientos(producto));
		assertEquals(0, new BigDecimal("70.000").compareTo(escenario.fisica(producto, tienda)));
	}

	/** Orden pendiente de un producto con la cantidad indicada. */
	private long ordenPendiente(int cantidad) {
		return ordenes.registrar(usuario, new DatosOrden(proveedor, null, null, null,
				List.of(new LineaOrden(sku, new BigDecimal(cantidad), new BigDecimal("26.50"))))).orden().getId();
	}

	/** Recepción pendiente con una sola línea en la tienda. */
	private long recepcion(long orden, int cantidad, String observaciones) {
		return recepciones.registrar(usuario, new DatosRecepcion(orden, LocalDate.of(2026, 11, 7), observaciones, null, null,
				List.of(new LineaRecepcion(producto, tienda, new BigDecimal(cantidad))))).recepcion().getId();
	}

	private DatosRecepcion datos(long orden, List<LineaRecepcion> lineas) {
		return new DatosRecepcion(orden, LocalDate.of(2026, 11, 7), null, null, null, lineas);
	}

	private static String ruc() {
		return "20" + String.format("%09d", ThreadLocalRandom.current().nextInt(1_000_000_000));
	}
}
