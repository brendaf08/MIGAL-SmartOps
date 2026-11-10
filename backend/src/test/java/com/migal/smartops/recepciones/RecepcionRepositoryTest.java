package com.migal.smartops.recepciones;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.migal.smartops.TestcontainersConfiguration;
import com.migal.smartops.abastecimiento.model.OrdenCompra;
import com.migal.smartops.abastecimiento.model.Proveedor;
import com.migal.smartops.abastecimiento.repository.OrdenCompraRepository;
import com.migal.smartops.abastecimiento.repository.ProveedorRepository;
import com.migal.smartops.catalogo.model.Almacen;
import com.migal.smartops.catalogo.model.Producto;
import com.migal.smartops.catalogo.repository.AlmacenRepository;
import com.migal.smartops.catalogo.repository.CategoriaRepository;
import com.migal.smartops.catalogo.repository.ProductoRepository;
import com.migal.smartops.recepciones.model.EstadoRecepcion;
import com.migal.smartops.recepciones.model.Recepcion;
import com.migal.smartops.recepciones.repository.RecepcionRepository;
import com.migal.smartops.seguridad.model.RolUsuario;
import com.migal.smartops.seguridad.model.Usuario;
import com.migal.smartops.seguridad.repository.UsuarioRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verifica el mapeo de las recepciones y de sus líneas con PostgreSQL real, incluidos el código generado y el reemplazo
 * de líneas sin conflictos de clave. Cada prueba se revierte al terminar.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class RecepcionRepositoryTest {

	private static final String CEMENTO = "Cemento y aglomerantes";

	@Autowired
	private RecepcionRepository recepciones;
	@Autowired
	private OrdenCompraRepository ordenes;
	@Autowired
	private ProveedorRepository proveedores;
	@Autowired
	private ProductoRepository productos;
	@Autowired
	private AlmacenRepository almacenes;
	@Autowired
	private CategoriaRepository categorias;
	@Autowired
	private UsuarioRepository usuarios;

	private Usuario almacenero;
	private OrdenCompra orden;

	@BeforeEach
	void preparar() {
		String sufijo = sufijo();
		almacenero = usuarios.saveAndFlush(new Usuario("Luis", "Mamani", "rec-" + sufijo, "{hash}", RolUsuario.ALMACENERO));
		Proveedor proveedor = proveedores.saveAndFlush(new Proveedor(
				"20" + String.format("%09d", ThreadLocalRandom.current().nextInt(1_000_000_000)),
				"Proveedor de recepcion " + sufijo, "ventas@prueba.example", null, true));
		orden = new OrdenCompra(proveedor, almacenero, null, null, null);
		orden.reemplazarLineas(List.of());
		ordenes.saveAndFlush(orden);
	}

	@Test
	void generaCodigoYEstadoPendienteAlRegistrar() {
		Recepcion recepcion = recepciones.saveAndFlush(new Recepcion(orden, almacenero, LocalDate.of(2026, 11, 7), null));

		assertTrue(recepcion.getCodigo().matches("R-[0-9]{4,}"));
		assertEquals(EstadoRecepcion.PENDIENTE, recepcion.getEstado());
		assertNull(recepcion.getFechaValidacion());
	}

	@Test
	void registraLineasPorProductoYUbicacion() {
		Producto producto = producto();
		Almacen tienda = almacen("tienda");
		Recepcion recepcion = new Recepcion(orden, almacenero, LocalDate.of(2026, 11, 7), null);
		recepcion.reemplazarLineas(List.of(new Recepcion.NuevaLinea(producto, tienda, new BigDecimal("150"))));

		recepciones.saveAndFlush(recepcion);

		assertEquals(1, recepciones.findById(recepcion.getId()).orElseThrow().getLineas().size());
		assertEquals(0, new BigDecimal("150.000").compareTo(recepcion.getLineas().get(0).getCantidadRecibida()));
	}

	@Test
	void reemplazaLasLineasSinReinsertarLasClavesQuePermanecen() {
		Producto producto = producto();
		Almacen tienda = almacen("tienda");
		Almacen deposito = almacen("almacen");
		Recepcion recepcion = new Recepcion(orden, almacenero, LocalDate.of(2026, 11, 7), null);
		recepcion.reemplazarLineas(List.of(new Recepcion.NuevaLinea(producto, tienda, new BigDecimal("150"))));
		recepciones.saveAndFlush(recepcion);
		var linea = recepcion.getLineas().get(0);

		recepcion.reemplazarLineas(List.of(
				new Recepcion.NuevaLinea(producto, tienda, new BigDecimal("0")),
				new Recepcion.NuevaLinea(producto, deposito, new BigDecimal("20"))));
		recepciones.saveAndFlush(recepcion);

		assertEquals(2, recepcion.getLineas().size());
		assertSame(linea, recepcion.getLineas().stream()
				.filter(l -> l.getAlmacen().getId().equals(tienda.getId())).findFirst().orElseThrow());
	}

	@Test
	void validarRegistraLaFechaDeValidacion() {
		Recepcion recepcion = recepciones.saveAndFlush(new Recepcion(orden, almacenero, LocalDate.of(2026, 11, 7), null));

		recepcion.validar(Instant.parse("2026-11-07T20:40:12Z"));
		recepciones.saveAndFlush(recepcion);

		assertEquals(EstadoRecepcion.VALIDADA, recepciones.findById(recepcion.getId()).orElseThrow().getEstado());
		assertNotNull(recepcion.getFechaValidacion());
	}

	@Test
	void bloqueaLaRecepcionPorIdentificador() {
		Recepcion recepcion = recepciones.saveAndFlush(new Recepcion(orden, almacenero, LocalDate.of(2026, 11, 7), null));

		assertEquals(recepcion.getId(), recepciones.bloquearPorId(recepcion.getId()).orElseThrow().getId());
	}

	private Producto producto() {
		return productos.saveAndFlush(new Producto("REC-" + sufijo(), "Producto de recepcion", null, "bolsa",
				new BigDecimal("28.50"), new BigDecimal("20"), categorias.findByNombre(CEMENTO).orElseThrow(), "Sol", null,
				true));
	}

	private Almacen almacen(String tipo) {
		return almacenes.saveAndFlush(new Almacen("Recepcion " + tipo + " " + sufijo(), tipo, null));
	}

	private static String sufijo() {
		return UUID.randomUUID().toString().substring(0, 6);
	}
}
