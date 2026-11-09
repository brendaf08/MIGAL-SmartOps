package com.migal.smartops;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Verifica los disparadores diferibles de reserva_stock y las vistas de disponibilidad de la migración V8.
 * Las restricciones diferibles se evalúan al confirmar la transacción, por lo que sus casos usan una transacción explícita.
 * Cada prueba elimina las filas que inserta, en orden inverso a las claves foráneas.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class MigracionReservasDisparadoresTest {

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private DataSource dataSource;

	@BeforeEach
	void limpiarAntesDeCada() {
		limpiar();
	}

	@AfterEach
	void limpiarDespuesDeCada() {
		limpiar();
	}

	@Test
	void aceptaReservasQueNoSuperanLaCantidadFisica() {
		Long idUsuario = crearUsuario();
		Long idProducto = crearProducto();
		Long idInventario = crearInventario(idProducto, "10.000");
		Long idDetalle = crearDetalleVenta(idProducto, idUsuario, "10.000", "pendiente_abastecimiento");

		transaccion(() -> {
			insertarReserva(idDetalle, idInventario, idProducto, idUsuario, "6.000", "activa", false);
			insertarReserva(idDetalle, idInventario, idProducto, idUsuario, "4.000", "activa", false);
		});
	}

	@Test
	void rechazaReservasQueSuperanLaCantidadFisica() {
		Long idUsuario = crearUsuario();
		Long idProducto = crearProducto();
		Long idInventario = crearInventario(idProducto, "10.000");
		Long idDetalle = crearDetalleVenta(idProducto, idUsuario, "12.000", "pendiente_abastecimiento");

		assertThrows(TransactionSystemException.class, () -> transaccion(() -> {
			insertarReserva(idDetalle, idInventario, idProducto, idUsuario, "6.000", "activa", false);
			insertarReserva(idDetalle, idInventario, idProducto, idUsuario, "6.000", "activa", false);
		}));
	}

	@Test
	void rechazaReduccionDeCantidadFisicaBajoReservasActivas() {
		Long idUsuario = crearUsuario();
		Long idProducto = crearProducto();
		Long idInventario = crearInventario(idProducto, "10.000");
		Long idDetalle = crearDetalleVenta(idProducto, idUsuario, "6.000", "pendiente_abastecimiento");
		insertarReserva(idDetalle, idInventario, idProducto, idUsuario, "6.000", "activa", false);

		assertThrows(DataIntegrityViolationException.class,
				() -> jdbc.update("UPDATE inventario SET cantidad_fisica = 5.000 WHERE id_inventario = ?", idInventario));
	}

	@Test
	void aceptaReservasQueNoSuperanLaCantidadVendida() {
		Long idUsuario = crearUsuario();
		Long idProducto = crearProducto();
		Long idInventario = crearInventario(idProducto, "100.000");
		Long idDetalle = crearDetalleVenta(idProducto, idUsuario, "10.000", "pendiente_abastecimiento");

		transaccion(() -> {
			insertarReserva(idDetalle, idInventario, idProducto, idUsuario, "4.000", "activa", false);
			insertarReserva(idDetalle, idInventario, idProducto, idUsuario, "6.000", "activa", false);
		});
	}

	@Test
	void rechazaReservasQueSuperanLaCantidadVendidaDeLaLinea() {
		Long idUsuario = crearUsuario();
		Long idProducto = crearProducto();
		Long idInventario = crearInventario(idProducto, "100.000");
		Long idDetalle = crearDetalleVenta(idProducto, idUsuario, "10.000", "pendiente_abastecimiento");

		assertThrows(TransactionSystemException.class, () -> transaccion(() -> {
			insertarReserva(idDetalle, idInventario, idProducto, idUsuario, "6.000", "activa", false);
			insertarReserva(idDetalle, idInventario, idProducto, idUsuario, "6.000", "activa", false);
		}));
	}

	@Test
	void rechazaReduccionDeCantidadVendidaBajoReservasActivas() {
		Long idUsuario = crearUsuario();
		Long idProducto = crearProducto();
		Long idInventario = crearInventario(idProducto, "100.000");
		Long idDetalle = crearDetalleVenta(idProducto, idUsuario, "10.000", "pendiente_abastecimiento");
		insertarReserva(idDetalle, idInventario, idProducto, idUsuario, "6.000", "activa", false);

		assertThrows(DataIntegrityViolationException.class,
				() -> jdbc.update("UPDATE detalle_venta SET cantidad_vendida = 5.000 WHERE id_detalle_venta = ?", idDetalle));
	}

	@Test
	void calculaDisponibilidadDelInventarioRestandoReservasActivas() {
		Long idUsuario = crearUsuario();
		Long idProducto = crearProducto();
		Long idInventario = crearInventario(idProducto, "10.000");
		Long idDetalle = crearDetalleVenta(idProducto, idUsuario, "4.000", "pendiente_abastecimiento");
		insertarReserva(idDetalle, idInventario, idProducto, idUsuario, "4.000", "activa", false);

		Boolean libre = jdbc.queryForObject(
				"SELECT cantidad_libre = 6.000 AND cantidad_reservada = 4.000 FROM v_disponibilidad_inventario WHERE id_inventario = ?",
				Boolean.class, idInventario);
		assertTrue(libre);
	}

	@Test
	void calculaPendienteDeAbastecimientoPorLineaSegunEstadoDeLaVenta() {
		Long idUsuario = crearUsuario();
		Long idProducto = crearProducto();
		Long idInventario = crearInventario(idProducto, "20.000");
		Long idActiva = crearDetalleVenta(idProducto, idUsuario, "10.000", "pendiente_abastecimiento");
		Long idDespachada = crearDetalleVenta(idProducto, idUsuario, "5.000", "despachada");
		insertarReserva(idActiva, idInventario, idProducto, idUsuario, "4.000", "activa", false);
		insertarReserva(idDespachada, idInventario, idProducto, idUsuario, "5.000", "consumida", true);

		Boolean pendienteActiva = jdbc.queryForObject(
				"SELECT cantidad_pendiente_abastecimiento = 6.000 FROM v_cobertura_detalle_venta WHERE id_detalle_venta = ?",
				Boolean.class, idActiva);
		Boolean pendienteDespachada = jdbc.queryForObject(
				"SELECT cantidad_pendiente_abastecimiento = 0 FROM v_cobertura_detalle_venta WHERE id_detalle_venta = ?",
				Boolean.class, idDespachada);
		assertTrue(pendienteActiva);
		assertTrue(pendienteDespachada);
	}

	@Test
	void serializaLasVerificacionesConcurrentesSobreUnMismoInventario() throws Exception {
		Long idUsuario = crearUsuario();
		Long idProducto = crearProducto();
		Long idInventario = crearInventario(idProducto, "10.000");
		Long idDetalleA = crearDetalleVenta(idProducto, idUsuario, "6.000", "pendiente_abastecimiento");
		Long idDetalleB = crearDetalleVenta(idProducto, idUsuario, "6.000", "pendiente_abastecimiento");
		ExecutorService ejecutor = Executors.newSingleThreadExecutor();
		try (Connection a = dataSource.getConnection(); Connection b = dataSource.getConnection()) {
			a.setAutoCommit(false);
			b.setAutoCommit(false);
			insertarReservaActiva(a, idDetalleA, idInventario, idProducto, idUsuario);
			insertarReservaActiva(b, idDetalleB, idInventario, idProducto, idUsuario);
			// A verifica de inmediato y conserva el bloqueo del inventario hasta confirmar.
			a.createStatement().execute("SET CONSTRAINTS ALL IMMEDIATE");
			// B debe esperar a A: sin el bloqueo, no vería la reserva aún no confirmada de A.
			Future<Boolean> verificacionB = ejecutor.submit(() -> {
				b.createStatement().execute("SET CONSTRAINTS ALL IMMEDIATE");
				return true;
			});
			Thread.sleep(500);
			assertFalse(verificacionB.isDone());
			a.commit();
			assertThrows(ExecutionException.class, () -> verificacionB.get(10, TimeUnit.SECONDS));
			b.rollback();
		} finally {
			ejecutor.shutdownNow();
		}
	}

	private void insertarReservaActiva(Connection conexion, Long idDetalle, Long idInventario, Long idProducto,
			Long idUsuario) throws SQLException {
		try (PreparedStatement sentencia = conexion.prepareStatement("""
				INSERT INTO reserva_stock (id_detalle_venta, id_inventario, id_producto, cantidad_reservada, estado,
				                           id_usuario_responsable)
				VALUES (?, ?, ?, 6, 'activa', ?)
				""")) {
			sentencia.setLong(1, idDetalle);
			sentencia.setLong(2, idInventario);
			sentencia.setLong(3, idProducto);
			sentencia.setLong(4, idUsuario);
			sentencia.executeUpdate();
		}
	}

	private void transaccion(Runnable operaciones) {
		new TransactionTemplate(new DataSourceTransactionManager(dataSource))
				.executeWithoutResult(estado -> operaciones.run());
	}

	private void insertarReserva(Long idDetalle, Long idInventario, Long idProducto, Long idUsuario,
			String cantidad, String estado, boolean conFechaCierre) {
		jdbc.update("""
				INSERT INTO reserva_stock (id_detalle_venta, id_inventario, id_producto, cantidad_reservada, estado,
				                           fecha_cierre, id_usuario_responsable)
				VALUES (?, ?, ?, CAST(? AS numeric), ?, CASE WHEN ? THEN now() ELSE NULL END, ?)
				""", idDetalle, idInventario, idProducto, cantidad, estado, conFechaCierre, idUsuario);
	}

	private String sufijo() {
		return UUID.randomUUID().toString().substring(0, 8);
	}

	private Long crearUsuario() {
		return jdbc.queryForObject("""
				INSERT INTO usuario (nombres, apellidos, username, password_hash, rol)
				VALUES ('Nombre', 'Apellido', ?, 'hash-ficticio', 'vendedor')
				RETURNING id_usuario
				""", Long.class, "disparador-" + sufijo());
	}

	private Long crearProducto() {
		return jdbc.queryForObject("""
				INSERT INTO producto (sku, nombre_oficial, unidad_medida, precio_referencial, stock_minimo, categoria_producto)
				VALUES (?, 'Producto ficticio', 'bolsa', 10.00, 0, 'cementos')
				RETURNING id_producto
				""", Long.class, "DIS-" + sufijo());
	}

	private Long crearInventario(Long idProducto, String cantidadFisica) {
		Long idAlmacen = jdbc.queryForObject("""
				INSERT INTO almacen (nombre, tipo_ubicacion)
				VALUES (?, 'almacen')
				RETURNING id_almacen
				""", Long.class, "Almacen " + sufijo());
		return jdbc.queryForObject("""
				INSERT INTO inventario (id_producto, id_almacen, cantidad_fisica)
				VALUES (?, ?, CAST(? AS numeric))
				RETURNING id_inventario
				""", Long.class, idProducto, idAlmacen, cantidadFisica);
	}

	private Long crearDetalleVenta(Long idProducto, Long idUsuario, String cantidadVendida, String estadoVenta) {
		Long idCliente = jdbc.queryForObject("""
				INSERT INTO cliente (nombre_razon_social)
				VALUES ('Cliente ficticio')
				RETURNING id_cliente
				""", Long.class);
		Long idCotizacion = jdbc.queryForObject("""
				INSERT INTO cotizacion (id_cliente, id_usuario_responsable, fecha_vencimiento, descuento, estado)
				VALUES (?, ?, CURRENT_DATE + 30, 0, 'validada')
				RETURNING id_cotizacion
				""", Long.class, idCliente, idUsuario);
		Long idVenta = jdbc.queryForObject("""
				INSERT INTO venta (id_cotizacion, id_cliente, id_usuario_responsable, descuento, estado)
				VALUES (?, ?, ?, 0, ?)
				RETURNING id_venta
				""", Long.class, idCotizacion, idCliente, idUsuario, estadoVenta);
		return jdbc.queryForObject("""
				INSERT INTO detalle_venta (id_venta, id_producto, cantidad_vendida, precio_unitario)
				VALUES (?, ?, CAST(? AS numeric), 10.00)
				RETURNING id_detalle_venta
				""", Long.class, idVenta, idProducto, cantidadVendida);
	}

	private void limpiar() {
		jdbc.update("DELETE FROM reserva_stock");
		jdbc.update("DELETE FROM detalle_venta");
		jdbc.update("DELETE FROM venta");
		jdbc.update("DELETE FROM cotizacion");
		jdbc.update("DELETE FROM cliente");
		jdbc.update("DELETE FROM inventario");
		jdbc.update("DELETE FROM producto");
		jdbc.update("DELETE FROM almacen");
		jdbc.update("DELETE FROM usuario");
	}

}
