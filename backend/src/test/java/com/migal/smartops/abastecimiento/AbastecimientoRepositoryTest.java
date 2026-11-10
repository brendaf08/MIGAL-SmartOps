package com.migal.smartops.abastecimiento;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.migal.smartops.TestcontainersConfiguration;
import com.migal.smartops.abastecimiento.model.CatalogoProveedor;
import com.migal.smartops.abastecimiento.model.DetalleCompra;
import com.migal.smartops.abastecimiento.model.EstadoOrdenCompra;
import com.migal.smartops.abastecimiento.model.OrdenCompra;
import com.migal.smartops.abastecimiento.model.Proveedor;
import com.migal.smartops.abastecimiento.repository.CatalogoProveedorRepository;
import com.migal.smartops.abastecimiento.repository.OrdenCompraRepository;
import com.migal.smartops.abastecimiento.repository.ProveedorRepository;
import com.migal.smartops.catalogo.model.Producto;
import com.migal.smartops.catalogo.repository.CategoriaRepository;
import com.migal.smartops.catalogo.repository.ProductoRepository;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verifica el mapeo de proveedores, catálogos y órdenes de compra con PostgreSQL real, incluidas las restricciones de
 * las migraciones V2 y V5 y los códigos generados. Cada prueba se revierte al terminar.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class AbastecimientoRepositoryTest {

	private static final String CEMENTO = "Cemento y aglomerantes";

	@Autowired
	private ProveedorRepository proveedores;
	@Autowired
	private CatalogoProveedorRepository catalogos;
	@Autowired
	private OrdenCompraRepository ordenes;
	@Autowired
	private ProductoRepository productos;
	@Autowired
	private CategoriaRepository categorias;
	@Autowired
	private UsuarioRepository usuarios;

	private Usuario reponedor;

	@BeforeEach
	void preparar() {
		reponedor = usuarios.saveAndFlush(new Usuario("Rosa", "Quispe", "abs-" + sufijo(), "{hash}", RolUsuario.REPONEDOR));
	}

	@Test
	void asignaLaFechaDeRegistroDelProveedor() {
		Proveedor proveedor = proveedores.saveAndFlush(proveedor());

		assertNotNull(proveedor.getFechaRegistro());
		assertTrue(proveedor.isEstadoActivo());
	}

	@Test
	void rechazaUnRucDuplicado() {
		Proveedor original = proveedores.saveAndFlush(proveedor());
		Proveedor duplicado = new Proveedor(original.getRuc(), "Otro proveedor", "otro@prueba.example", null, true);

		assertThrows(DataIntegrityViolationException.class, () -> proveedores.saveAndFlush(duplicado));
	}

	@Test
	void registraElCatalogoConSuPrecioYLaMarcaDeActualizacion() {
		Proveedor proveedor = proveedores.saveAndFlush(proveedor());
		Producto producto = producto();
		Instant inicio = Instant.parse("2026-11-03T10:00:00Z");
		CatalogoProveedor linea = catalogos.saveAndFlush(
				new CatalogoProveedor(proveedor, producto, new BigDecimal("26.50"), 3, inicio));

		assertEquals(inicio, linea.getFechaActualizacionPrecio());
		assertEquals(1, catalogos.findByIdIdProveedorOrderByIdIdProductoAsc(proveedor.getId()).size());
	}

	@Test
	void laMarcaDeActualizacionCambiaSoloCuandoCambiaElPrecio() {
		Proveedor proveedor = proveedores.saveAndFlush(proveedor());
		Producto producto = producto();
		Instant inicio = Instant.parse("2026-11-03T10:00:00Z");
		Instant despues = inicio.plusSeconds(3600);
		CatalogoProveedor linea = catalogos.saveAndFlush(
				new CatalogoProveedor(proveedor, producto, new BigDecimal("26.50"), 3, inicio));

		linea.actualizar(new BigDecimal("26.50"), 5, despues);
		assertEquals(inicio, linea.getFechaActualizacionPrecio());

		linea.actualizar(new BigDecimal("27.00"), 5, despues);
		assertEquals(despues, linea.getFechaActualizacionPrecio());
	}

	@Test
	void generaCodigoFechaDeEmisionYTotalDeLaOrden() {
		Proveedor proveedor = proveedores.saveAndFlush(proveedor());
		Producto producto = producto();
		OrdenCompra orden = new OrdenCompra(proveedor, reponedor, LocalDate.of(2026, 11, 6), null, "Credito de 30 dias");
		orden.reemplazarLineas(List.of(orden.nuevaLinea(producto, new BigDecimal("100"), new BigDecimal("26.50"))));

		ordenes.saveAndFlush(orden);

		assertTrue(orden.getCodigo().matches("OC-[0-9]{4,}"));
		assertNotNull(orden.getFechaEmision());
		assertEquals(EstadoOrdenCompra.PENDIENTE, orden.getEstado());
		assertEquals(0, new BigDecimal("2650.00").compareTo(orden.total()));
	}

	@Test
	void reemplazaLasLineasSinReinsertarLasClavesQuePermanecen() {
		Proveedor proveedor = proveedores.saveAndFlush(proveedor());
		Producto cemento = producto();
		Producto fierro = producto();
		OrdenCompra orden = new OrdenCompra(proveedor, reponedor, null, null, null);
		orden.reemplazarLineas(List.of(
				orden.nuevaLinea(cemento, new BigDecimal("10"), new BigDecimal("20")),
				orden.nuevaLinea(fierro, new BigDecimal("5"), new BigDecimal("30"))));
		ordenes.saveAndFlush(orden);
		DetalleCompra lineaCemento = orden.getLineas().stream()
				.filter(l -> l.getProducto().getId().equals(cemento.getId())).findFirst().orElseThrow();

		Producto nuevo = producto();
		orden.reemplazarLineas(List.of(
				orden.nuevaLinea(cemento, new BigDecimal("12"), new BigDecimal("21")),
				orden.nuevaLinea(nuevo, new BigDecimal("4"), new BigDecimal("8"))));
		ordenes.saveAndFlush(orden);

		assertEquals(2, orden.getLineas().size());
		assertSame(lineaCemento, orden.getLineas().stream()
				.filter(l -> l.getProducto().getId().equals(cemento.getId())).findFirst().orElseThrow());
		assertFalse(orden.getLineas().stream().anyMatch(l -> l.getProducto().getId().equals(fierro.getId())));
	}

	private Proveedor proveedor() {
		String ruc = "20" + String.format("%09d", ThreadLocalRandom.current().nextInt(1_000_000_000));
		return new Proveedor(ruc, "Proveedor de prueba " + sufijo(), "ventas@prueba.example", "Pago al contado.", true);
	}

	private Producto producto() {
		return productos.saveAndFlush(new Producto("ABS-" + sufijo(), "Producto de abastecimiento", null, "bolsa",
				new BigDecimal("28.50"), new BigDecimal("20"), categorias.findByNombre(CEMENTO).orElseThrow(), "Sol", null,
				true));
	}

	private static String sufijo() {
		return UUID.randomUUID().toString().substring(0, 6);
	}
}
