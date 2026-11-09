package com.migal.smartops;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Verifica la migración V8 (tabla reserva_stock y vistas de disponibilidad) con PostgreSQL real.
 * Cada prueba elimina las filas que inserta, en orden inverso a las claves foráneas.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class MigracionReservasTest {

	@Autowired
	private JdbcTemplate jdbc;

	@BeforeEach
	void limpiarAntesDeCada() {
		limpiar();
	}

	@AfterEach
	void limpiarDespuesDeCada() {
		limpiar();
	}

	@Test
	void existenLaTablaYLasVistasDeReserva() {
		Integer cantidad = jdbc.queryForObject("""
				SELECT count(*) FROM information_schema.tables
				WHERE table_schema = 'public'
				  AND table_name IN ('reserva_stock', 'v_disponibilidad_inventario', 'v_cobertura_detalle_venta')
				""", Integer.class);
		assertEquals(3, cantidad);
	}

	@Test
	void aceptaReservaActivaSinFechaDeCierre() {
		Long idUsuario = crearUsuario();
		Long idProducto = crearProducto();
		Long idInventario = crearInventario(idProducto, "10.000");
		Long idDetalle = crearDetalleVenta(idProducto, idUsuario, "4.000", "pendiente_abastecimiento");

		insertarReserva(idDetalle, idInventario, idProducto, idUsuario, "4.000", "activa", false);

		assertEquals(1, contarReservas(idDetalle));
	}

	@Test
	void aceptaReservaConsumidaConFechaDeCierre() {
		Long idUsuario = crearUsuario();
		Long idProducto = crearProducto();
		Long idInventario = crearInventario(idProducto, "10.000");
		Long idDetalle = crearDetalleVenta(idProducto, idUsuario, "4.000", "despachada");

		insertarReserva(idDetalle, idInventario, idProducto, idUsuario, "4.000", "consumida", true);

		assertEquals(1, contarReservas(idDetalle));
	}

	@Test
	void rechazaEstadoDeReservaFueraDelDominio() {
		Long idUsuario = crearUsuario();
		Long idProducto = crearProducto();
		Long idInventario = crearInventario(idProducto, "10.000");
		Long idDetalle = crearDetalleVenta(idProducto, idUsuario, "4.000", "pendiente_abastecimiento");

		assertThrows(DataIntegrityViolationException.class,
				() -> insertarReserva(idDetalle, idInventario, idProducto, idUsuario, "4.000", "pendiente", false));
	}

	@Test
	void rechazaCantidadReservadaNoPositiva() {
		Long idUsuario = crearUsuario();
		Long idProducto = crearProducto();
		Long idInventario = crearInventario(idProducto, "10.000");
		Long idDetalle = crearDetalleVenta(idProducto, idUsuario, "4.000", "pendiente_abastecimiento");

		assertThrows(DataIntegrityViolationException.class,
				() -> insertarReserva(idDetalle, idInventario, idProducto, idUsuario, "0.000", "activa", false));
	}

	@Test
	void rechazaReservaActivaConFechaDeCierre() {
		Long idUsuario = crearUsuario();
		Long idProducto = crearProducto();
		Long idInventario = crearInventario(idProducto, "10.000");
		Long idDetalle = crearDetalleVenta(idProducto, idUsuario, "4.000", "pendiente_abastecimiento");

		assertThrows(DataIntegrityViolationException.class,
				() -> insertarReserva(idDetalle, idInventario, idProducto, idUsuario, "4.000", "activa", true));
	}

	@Test
	void rechazaReservaLiberadaSinFechaDeCierre() {
		Long idUsuario = crearUsuario();
		Long idProducto = crearProducto();
		Long idInventario = crearInventario(idProducto, "10.000");
		Long idDetalle = crearDetalleVenta(idProducto, idUsuario, "4.000", "cancelada");

		assertThrows(DataIntegrityViolationException.class,
				() -> insertarReserva(idDetalle, idInventario, idProducto, idUsuario, "4.000", "liberada", false));
	}

	@Test
	void rechazaReservaDeProductoDistintoDeLaLinea() {
		Long idUsuario = crearUsuario();
		Long idProducto = crearProducto();
		Long idOtroProducto = crearProducto();
		Long idInventario = crearInventario(idOtroProducto, "10.000");
		Long idDetalle = crearDetalleVenta(idProducto, idUsuario, "4.000", "pendiente_abastecimiento");

		assertThrows(DataIntegrityViolationException.class,
				() -> insertarReserva(idDetalle, idInventario, idOtroProducto, idUsuario, "4.000", "activa", false));
	}

	@Test
	void rechazaReservaDeInventarioDeOtroProducto() {
		Long idUsuario = crearUsuario();
		Long idProducto = crearProducto();
		Long idOtroProducto = crearProducto();
		Long idInventario = crearInventario(idOtroProducto, "10.000");
		Long idDetalle = crearDetalleVenta(idProducto, idUsuario, "4.000", "pendiente_abastecimiento");

		assertThrows(DataIntegrityViolationException.class,
				() -> insertarReserva(idDetalle, idInventario, idProducto, idUsuario, "4.000", "activa", false));
	}

	private void insertarReserva(Long idDetalle, Long idInventario, Long idProducto, Long idUsuario,
			String cantidad, String estado, boolean conFechaCierre) {
		jdbc.update("""
				INSERT INTO reserva_stock (id_detalle_venta, id_inventario, id_producto, cantidad_reservada, estado,
				                           fecha_cierre, id_usuario_responsable)
				VALUES (?, ?, ?, CAST(? AS numeric), ?, CASE WHEN ? THEN now() ELSE NULL END, ?)
				""", idDetalle, idInventario, idProducto, cantidad, estado, conFechaCierre, idUsuario);
	}

	private int contarReservas(Long idDetalle) {
		return jdbc.queryForObject("SELECT count(*) FROM reserva_stock WHERE id_detalle_venta = ?",
				Integer.class, idDetalle);
	}

	private String sufijo() {
		return UUID.randomUUID().toString().substring(0, 8);
	}

	private Long crearUsuario() {
		return jdbc.queryForObject("""
				INSERT INTO usuario (nombres, apellidos, username, password_hash, rol)
				VALUES ('Nombre', 'Apellido', ?, 'hash-ficticio', 'vendedor')
				RETURNING id_usuario
				""", Long.class, "reserva-" + sufijo());
	}

	private Long crearProducto() {
		return jdbc.queryForObject("""
				INSERT INTO producto (sku, nombre_oficial, unidad_medida, precio_referencial, stock_minimo, id_categoria)
				VALUES (?, 'Producto ficticio', 'bolsa', 10.00, 0, (SELECT id_categoria FROM categoria WHERE nombre = 'Cemento y aglomerantes'))
				RETURNING id_producto
				""", Long.class, "RES-" + sufijo());
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
				INSERT INTO venta (id_cotizacion, id_cliente, id_usuario_responsable, descuento, estado,
				                   motivo_cancelacion, fecha_cancelacion, id_usuario_autorizante)
				VALUES (?, ?, ?, 0, ?, CASE WHEN ? = 'cancelada' THEN 'Cliente desistio' END,
				        CASE WHEN ? = 'cancelada' THEN now() END, CASE WHEN ? = 'cancelada' THEN ? END)
				RETURNING id_venta
				""", Long.class, idCotizacion, idCliente, idUsuario, estadoVenta,
				estadoVenta, estadoVenta, estadoVenta, idUsuario);
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
