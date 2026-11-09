package com.migal.smartops;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Verifica la restricción diferible de traslados de la migración V7 con PostgreSQL real.
 * La restricción se evalúa al confirmar la transacción, por lo que cada caso se ejecuta en una transacción explícita.
 * La limpieza desactiva el disparador de solo anexión solo para eliminar las filas de prueba.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class MigracionKardexTrasladoTest {

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
	void aceptaTrasladoConSalidaYEntradaIguales() {
		Long idUsuario = crearUsuario();
		Long idOrigen = crearInventario();
		Long idDestino = crearInventarioDelMismoProducto(idOrigen);
		UUID referencia = UUID.randomUUID();

		transaccion(() -> {
			insertarMovimiento(idOrigen, idUsuario, "salida", "5.000", referencia);
			insertarMovimiento(idDestino, idUsuario, "entrada", "5.000", referencia);
		});

		assertEquals(1, contarMovimientos(idOrigen));
		assertEquals(1, contarMovimientos(idDestino));
	}

	@Test
	void rechazaTrasladoSinEntrada() {
		Long idUsuario = crearUsuario();
		Long idOrigen = crearInventario();

		assertThrows(TransactionSystemException.class, () -> transaccion(
				() -> insertarMovimiento(idOrigen, idUsuario, "salida", "5.000", UUID.randomUUID())));
	}

	@Test
	void rechazaTrasladoConCantidadesDistintas() {
		Long idUsuario = crearUsuario();
		Long idOrigen = crearInventario();
		Long idDestino = crearInventarioDelMismoProducto(idOrigen);
		UUID referencia = UUID.randomUUID();

		assertThrows(TransactionSystemException.class, () -> transaccion(() -> {
			insertarMovimiento(idOrigen, idUsuario, "salida", "5.000", referencia);
			insertarMovimiento(idDestino, idUsuario, "entrada", "4.000", referencia);
		}));
	}

	@Test
	void rechazaTrasladoEntreProductosDistintos() {
		Long idUsuario = crearUsuario();
		Long idOrigen = crearInventario();
		Long idDestino = crearInventario();
		UUID referencia = UUID.randomUUID();

		assertThrows(TransactionSystemException.class, () -> transaccion(() -> {
			insertarMovimiento(idOrigen, idUsuario, "salida", "5.000", referencia);
			insertarMovimiento(idDestino, idUsuario, "entrada", "5.000", referencia);
		}));
	}

	@Test
	void rechazaTrasladoEnElMismoInventario() {
		Long idUsuario = crearUsuario();
		Long idInventario = crearInventario();
		UUID referencia = UUID.randomUUID();

		assertThrows(TransactionSystemException.class, () -> transaccion(() -> {
			insertarMovimiento(idInventario, idUsuario, "salida", "5.000", referencia);
			insertarMovimiento(idInventario, idUsuario, "entrada", "5.000", referencia);
		}));
	}

	private void transaccion(Runnable operaciones) {
		new TransactionTemplate(new DataSourceTransactionManager(dataSource))
				.executeWithoutResult(estado -> operaciones.run());
	}

	private void insertarMovimiento(Long idInventario, Long idUsuario, String tipo, String cantidad, UUID referencia) {
		jdbc.update("""
				INSERT INTO movimiento_kardex (id_inventario, tipo_operacion, cantidad, motivo, id_usuario_responsable,
				                               origen_movimiento, referencia_traslado)
				VALUES (?, ?, CAST(? AS numeric), 'Reubicacion interna', ?, 'traslado', ?)
				""", idInventario, tipo, cantidad, idUsuario, referencia);
	}

	private int contarMovimientos(Long idInventario) {
		return jdbc.queryForObject("SELECT count(*) FROM movimiento_kardex WHERE id_inventario = ?",
				Integer.class, idInventario);
	}

	private String sufijo() {
		return UUID.randomUUID().toString().substring(0, 8);
	}

	private Long crearUsuario() {
		return jdbc.queryForObject("""
				INSERT INTO usuario (nombres, apellidos, username, password_hash, rol)
				VALUES ('Nombre', 'Apellido', ?, 'hash-ficticio', 'almacenero')
				RETURNING id_usuario
				""", Long.class, "traslado-" + sufijo());
	}

	private Long crearInventario() {
		Long idProducto = jdbc.queryForObject("""
				INSERT INTO producto (sku, nombre_oficial, unidad_medida, precio_referencial, stock_minimo, id_categoria)
				VALUES (?, 'Producto ficticio', 'bolsa', 10.00, 0, (SELECT id_categoria FROM categoria WHERE nombre = 'Cemento y aglomerantes'))
				RETURNING id_producto
				""", Long.class, "TRA-" + sufijo());
		Long idAlmacen = jdbc.queryForObject("""
				INSERT INTO almacen (nombre, tipo_ubicacion)
				VALUES (?, 'almacen')
				RETURNING id_almacen
				""", Long.class, "Almacen " + sufijo());
		return jdbc.queryForObject("""
				INSERT INTO inventario (id_producto, id_almacen, cantidad_fisica)
				VALUES (?, ?, 100.000)
				RETURNING id_inventario
				""", Long.class, idProducto, idAlmacen);
	}

	private Long crearInventarioDelMismoProducto(Long idInventario) {
		Long idAlmacen = jdbc.queryForObject("""
				INSERT INTO almacen (nombre, tipo_ubicacion)
				VALUES (?, 'almacen')
				RETURNING id_almacen
				""", Long.class, "Almacen " + sufijo());
		return jdbc.queryForObject("""
				INSERT INTO inventario (id_producto, id_almacen, cantidad_fisica)
				SELECT id_producto, ?, 100.000 FROM inventario WHERE id_inventario = ?
				RETURNING id_inventario
				""", Long.class, idAlmacen, idInventario);
	}

	private void limpiar() {
		jdbc.update("ALTER TABLE movimiento_kardex DISABLE TRIGGER tg_movimiento_kardex_solo_anexion");
		jdbc.update("DELETE FROM movimiento_kardex");
		jdbc.update("ALTER TABLE movimiento_kardex ENABLE TRIGGER tg_movimiento_kardex_solo_anexion");
		jdbc.update("DELETE FROM inventario");
		jdbc.update("DELETE FROM producto");
		jdbc.update("DELETE FROM almacen");
		jdbc.update("DELETE FROM usuario");
	}

}
