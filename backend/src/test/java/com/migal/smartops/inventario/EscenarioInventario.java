package com.migal.smartops.inventario;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Datos de prueba de inventario confirmados en la base de datos, con su limpieza. Como el Kardex es de solo
 * anexión, la limpieza desactiva momentáneamente su disparador de protección; solo se usa en pruebas con una base
 * de datos desechable.
 */
final class EscenarioInventario {

	private final JdbcTemplate jdbc;
	private final List<Long> productos = new ArrayList<>();
	private final List<Long> almacenes = new ArrayList<>();
	private final List<Long> usuarios = new ArrayList<>();

	EscenarioInventario(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	long usuario() {
		long id = jdbc.queryForObject("""
				INSERT INTO usuario (nombres, apellidos, username, password_hash, rol)
				VALUES ('Prueba', 'Inventario', ?, '{hash}', 'almacenero') RETURNING id_usuario
				""", Long.class, "inv-" + UUID.randomUUID().toString().substring(0, 12));
		usuarios.add(id);
		return id;
	}

	long producto(String stockMinimo) {
		long categoria = jdbc.queryForObject("SELECT min(id_categoria) FROM categoria", Long.class);
		long id = jdbc.queryForObject("""
				INSERT INTO producto (sku, nombre_oficial, unidad_medida, precio_referencial, stock_minimo, id_categoria)
				VALUES (?, 'Producto de prueba', 'unidad', 10.00, ?, ?) RETURNING id_producto
				""", Long.class, "T-" + UUID.randomUUID().toString().substring(0, 12), new BigDecimal(stockMinimo), categoria);
		productos.add(id);
		return id;
	}

	long almacen(String tipo) {
		long id = jdbc.queryForObject("""
				INSERT INTO almacen (nombre, tipo_ubicacion) VALUES (?, ?) RETURNING id_almacen
				""", Long.class, "Ubicacion " + UUID.randomUUID(), tipo);
		almacenes.add(id);
		return id;
	}

	long inventario(long producto, long almacen, String cantidad) {
		return jdbc.queryForObject("""
				INSERT INTO inventario (id_producto, id_almacen, cantidad_fisica) VALUES (?, ?, ?) RETURNING id_inventario
				""", Long.class, producto, almacen, new BigDecimal(cantidad));
	}

	BigDecimal fisica(long producto, long almacen) {
		return jdbc.queryForObject("SELECT cantidad_fisica FROM inventario WHERE id_producto = ? AND id_almacen = ?",
				BigDecimal.class, producto, almacen);
	}

	int movimientos(long producto) {
		return jdbc.queryForObject("""
				SELECT count(*) FROM movimiento_kardex m JOIN inventario i ON i.id_inventario = m.id_inventario
				 WHERE i.id_producto = ?
				""", Integer.class, producto);
	}

	void limpiar() {
		if (productos.isEmpty() && almacenes.isEmpty() && usuarios.isEmpty()) {
			return;
		}
		jdbc.execute("ALTER TABLE movimiento_kardex DISABLE TRIGGER tg_movimiento_kardex_solo_anexion");
		try {
			for (long producto : productos) {
				jdbc.update("""
						UPDATE movimiento_kardex SET id_movimiento_corregido = NULL
						 WHERE id_inventario IN (SELECT id_inventario FROM inventario WHERE id_producto = ?)
						""", producto);
				jdbc.update("""
						DELETE FROM movimiento_kardex
						 WHERE id_inventario IN (SELECT id_inventario FROM inventario WHERE id_producto = ?)
						""", producto);
				jdbc.update("DELETE FROM inventario WHERE id_producto = ?", producto);
				jdbc.update("DELETE FROM producto WHERE id_producto = ?", producto);
			}
		} finally {
			jdbc.execute("ALTER TABLE movimiento_kardex ENABLE TRIGGER tg_movimiento_kardex_solo_anexion");
		}
		almacenes.forEach(id -> jdbc.update("DELETE FROM almacen WHERE id_almacen = ?", id));
		usuarios.forEach(id -> jdbc.update("DELETE FROM usuario WHERE id_usuario = ?", id));
		productos.clear();
		almacenes.clear();
		usuarios.clear();
	}
}
