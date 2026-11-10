package com.migal.smartops.comercial;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.migal.smartops.TestcontainersConfiguration;
import com.migal.smartops.catalogo.model.Producto;
import com.migal.smartops.catalogo.repository.CategoriaRepository;
import com.migal.smartops.catalogo.repository.ProductoRepository;
import com.migal.smartops.comercial.model.Cliente;
import com.migal.smartops.comercial.model.Cotizacion;
import com.migal.smartops.comercial.model.EstadoVenta;
import com.migal.smartops.comercial.model.TipoDocumentoCliente;
import com.migal.smartops.comercial.model.Venta;
import com.migal.smartops.comercial.repository.ClienteRepository;
import com.migal.smartops.comercial.repository.CotizacionRepository;
import com.migal.smartops.comercial.repository.VentaRepository;
import com.migal.smartops.seguridad.model.RolUsuario;
import com.migal.smartops.seguridad.model.Usuario;
import com.migal.smartops.seguridad.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verifica el mapeo de las entidades de venta y las restricciones de las migraciones V4, V8 y V12 con PostgreSQL
 * real. Cada prueba se revierte al terminar.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class VentaRepositoryTest {

	private static final Instant MOMENTO = Instant.parse("2026-11-02T20:00:00Z");
	private static final LocalDate VENCIMIENTO = LocalDate.of(2026, 11, 9);

	@Autowired
	private ClienteRepository clientes;
	@Autowired
	private CotizacionRepository cotizaciones;
	@Autowired
	private VentaRepository ventas;
	@Autowired
	private ProductoRepository productos;
	@Autowired
	private CategoriaRepository categorias;
	@Autowired
	private UsuarioRepository usuarios;
	@Autowired
	private EntityManager entityManager;
	@Autowired
	private JdbcClient jdbc;

	private Usuario vendedor;
	private Producto producto;

	@BeforeEach
	void preparar() {
		String sufijo = UUID.randomUUID().toString().substring(0, 8);
		producto = productos.saveAndFlush(new Producto("VEN-" + sufijo, "Cemento de prueba", null, "bolsa",
				new BigDecimal("28.50"), new BigDecimal("10"), categorias.findAll().get(0), null, null, true));
		vendedor = usuarios.saveAndFlush(new Usuario("Prueba", "Ventas", "ven-" + sufijo, "{hash}", RolUsuario.VENDEDOR));
	}

	private Cliente cliente() {
		String numero = String.valueOf(10_000_000 + ThreadLocalRandom.current().nextInt(89_999_999));
		return clientes.saveAndFlush(new Cliente(TipoDocumentoCliente.DNI, numero, "Cliente de prueba", null, null));
	}

	/** Cotización validada, lista para originar una venta. */
	private Cotizacion cotizacionValidada(Cliente cliente) {
		Cotizacion cotizacion = new Cotizacion(cliente, vendedor, MOMENTO, VENCIMIENTO, null, new BigDecimal("25.00"));
		cotizacion.agregarLinea(producto, new BigDecimal("40"), new BigDecimal("28.50"));
		cotizacion.registrarAceptacion(MOMENTO);
		cotizacion.registrarValidacion(MOMENTO);
		return cotizaciones.saveAndFlush(cotizacion);
	}

	@Test
	void guardaVentaConLineasYRecuperaLosTotalesDerivados() {
		Cotizacion cotizacion = cotizacionValidada(cliente());
		Venta nueva = new Venta(cotizacion, vendedor, MOMENTO, new BigDecimal("25.00"));
		nueva.agregarLinea(producto, new BigDecimal("40"), new BigDecimal("28.50"));
		Long id = ventas.saveAndFlush(nueva).getId();
		entityManager.clear();

		Venta recuperada = ventas.findById(id).orElseThrow();
		assertNotNull(recuperada.getCliente().getFechaRegistro());
		assertEquals(EstadoVenta.PENDIENTE_ABASTECIMIENTO, recuperada.getEstado());
		assertEquals(cotizacion.getId(), recuperada.getCotizacion().getId());
		assertEquals(1, recuperada.getLineas().size());
		assertEquals(new BigDecimal("1140.00"), recuperada.subtotal());
		assertEquals(new BigDecimal("1115.00"), recuperada.total());
		assertTrue(ventas.existsByCotizacionId(cotizacion.getId()));
		assertNotNull(ventas.bloquearPorId(id).orElseThrow());
	}

	@Test
	void registraLaCancelacionConMotivoUsuarioYFecha() {
		Venta nueva = new Venta(cotizacionValidada(cliente()), vendedor, MOMENTO, BigDecimal.ZERO);
		nueva.agregarLinea(producto, new BigDecimal("5"), new BigDecimal("28.50"));
		nueva.cancelar(vendedor, MOMENTO, "Cliente desistió de la obra");
		Long id = ventas.saveAndFlush(nueva).getId();
		entityManager.clear();

		Venta recuperada = ventas.findById(id).orElseThrow();
		assertEquals(EstadoVenta.CANCELADA, recuperada.getEstado());
		assertEquals("Cliente desistió de la obra", recuperada.getMotivoCancelacion());
		assertEquals(vendedor.getId(), recuperada.getUsuarioAutorizante().getId());
		assertNotNull(recuperada.getFechaCancelacion());
	}

	@Test
	void rechazaUnaCancelacionSinMotivo() {
		Venta nueva = new Venta(cotizacionValidada(cliente()), vendedor, MOMENTO, BigDecimal.ZERO);
		nueva.cancelar(vendedor, MOMENTO, " ");
		assertThrows(DataIntegrityViolationException.class, () -> ventas.saveAndFlush(nueva));
	}

	@Test
	void rechazaDosVentasParaLaMismaCotizacion() {
		Cotizacion cotizacion = cotizacionValidada(cliente());
		ventas.saveAndFlush(new Venta(cotizacion, vendedor, MOMENTO, BigDecimal.ZERO));
		assertThrows(DataIntegrityViolationException.class,
				() -> ventas.saveAndFlush(new Venta(cotizacion, vendedor, MOMENTO, BigDecimal.ZERO)));
	}

	@Test
	void rechazaUnClienteDistintoDelDeLaCotizacion() {
		Cotizacion cotizacion = cotizacionValidada(cliente());
		Cliente otro = cliente();
		assertThrows(DataIntegrityViolationException.class, () -> jdbc.sql("""
				INSERT INTO venta (id_cotizacion, id_cliente, id_usuario_responsable, descuento, estado)
				VALUES (:cotizacion, :cliente, :usuario, 0, 'pendiente_abastecimiento')
				""").param("cotizacion", cotizacion.getId()).param("cliente", otro.getId())
				.param("usuario", vendedor.getId()).update());
	}
}
