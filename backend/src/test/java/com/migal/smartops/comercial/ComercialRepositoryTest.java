package com.migal.smartops.comercial;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.migal.smartops.TestcontainersConfiguration;
import com.migal.smartops.catalogo.model.Producto;
import com.migal.smartops.catalogo.repository.CategoriaRepository;
import com.migal.smartops.catalogo.repository.ProductoRepository;
import com.migal.smartops.comercial.model.Cliente;
import com.migal.smartops.comercial.model.Cotizacion;
import com.migal.smartops.comercial.model.EstadoCotizacion;
import com.migal.smartops.comercial.model.OrigenRechazoCotizacion;
import com.migal.smartops.comercial.model.TipoDocumentoCliente;
import com.migal.smartops.comercial.repository.ClienteRepository;
import com.migal.smartops.comercial.repository.CotizacionRepository;
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
import org.springframework.transaction.annotation.Transactional;

/**
 * Verifica el mapeo de las entidades comerciales y las restricciones de las migraciones V11 y V12 con PostgreSQL
 * real. Cada prueba se revierte al terminar.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class ComercialRepositoryTest {

	private static final Instant EMISION = Instant.parse("2026-11-02T20:00:00Z");
	private static final LocalDate VENCIMIENTO = LocalDate.of(2026, 11, 9);

	@Autowired
	private ClienteRepository clientes;
	@Autowired
	private CotizacionRepository cotizaciones;
	@Autowired
	private ProductoRepository productos;
	@Autowired
	private CategoriaRepository categorias;
	@Autowired
	private UsuarioRepository usuarios;
	@Autowired
	private EntityManager entityManager;

	private Usuario vendedor;
	private Producto producto;

	@BeforeEach
	void preparar() {
		String sufijo = UUID.randomUUID().toString().substring(0, 8);
		producto = productos.saveAndFlush(new Producto("COM-" + sufijo, "Cemento de prueba", null, "bolsa",
				new BigDecimal("28.50"), new BigDecimal("10"), categorias.findAll().get(0), null, null, true));
		vendedor = usuarios.saveAndFlush(new Usuario("Prueba", "Comercial", "com-" + sufijo, "{hash}", RolUsuario.VENDEDOR));
	}

	private Cliente cliente(TipoDocumentoCliente tipo, String numero) {
		return clientes.saveAndFlush(new Cliente(tipo, numero, "Cliente de prueba", null, null));
	}

	private Cotizacion cotizacion(Cliente cliente) {
		return new Cotizacion(cliente, vendedor, EMISION, VENCIMIENTO, null, new BigDecimal("25.00"));
	}

	private String dniAleatorio() {
		return String.valueOf(10_000_000 + ThreadLocalRandom.current().nextInt(89_999_999));
	}

	@Test
	void guardaCotizacionConLineasYRecuperaLosTotalesDerivados() {
		Cotizacion nueva = cotizacion(cliente(TipoDocumentoCliente.RUC, "20601234567"));
		nueva.agregarLinea(producto, new BigDecimal("40"), new BigDecimal("28.50"));
		Long id = cotizaciones.saveAndFlush(nueva).getId();
		entityManager.clear();

		Cotizacion recuperada = cotizaciones.findById(id).orElseThrow();
		assertNotNull(recuperada.getCliente().getFechaRegistro());
		assertEquals(EstadoCotizacion.EMITIDA, recuperada.getEstado());
		assertEquals(1, recuperada.getLineas().size());
		assertEquals(new BigDecimal("1140.00"), recuperada.subtotal());
		assertEquals(new BigDecimal("1115.00"), recuperada.total());
		assertEquals(0, new BigDecimal("40.000").compareTo(recuperada.getLineas().get(0).getCantidadCotizada()));
		assertNotNull(cotizaciones.bloquearPorId(id).orElseThrow());
	}

	@Test
	void registraRechazoConOrigenUsuarioFechaYMotivo() {
		Cotizacion nueva = cotizacion(cliente(TipoDocumentoCliente.DNI, dniAleatorio()));
		nueva.registrarRechazo(OrigenRechazoCotizacion.CLIENTE, vendedor, EMISION, "Plazo distinto");
		Long id = cotizaciones.saveAndFlush(nueva).getId();
		entityManager.clear();

		Cotizacion recuperada = cotizaciones.findById(id).orElseThrow();
		assertEquals(EstadoCotizacion.RECHAZADA, recuperada.getEstado());
		assertEquals(OrigenRechazoCotizacion.CLIENTE, recuperada.getOrigenRechazo());
		assertEquals(vendedor.getId(), recuperada.getUsuarioRechazo().getId());
		assertEquals("Plazo distinto", recuperada.getMotivoRechazo());
	}

	@Test
	void rechazaUnRechazoSinMotivo() {
		Cotizacion nueva = cotizacion(cliente(TipoDocumentoCliente.DNI, dniAleatorio()));
		nueva.registrarRechazo(OrigenRechazoCotizacion.CLIENTE, vendedor, EMISION, " ");
		assertThrows(DataIntegrityViolationException.class, () -> cotizaciones.saveAndFlush(nueva));
	}

	@Test
	void rechazaUnDocumentoDuplicadoDelMismoTipo() {
		String numero = dniAleatorio();
		cliente(TipoDocumentoCliente.DNI, numero);
		assertThrows(DataIntegrityViolationException.class, () -> cliente(TipoDocumentoCliente.DNI, numero));
	}

	@Test
	void aceptaClientesDeMostradorSinDocumento() {
		Cliente mostrador = clientes.saveAndFlush(new Cliente(null, null, "Cliente de mostrador", null, null));
		assertNotNull(mostrador.getId());
		assertTrue(mostrador.isEstadoActivo());
		assertNull(mostrador.getTipoDocumento());
	}

	@Test
	void rechazaUnTipoDeDocumentoSinNumero() {
		assertThrows(DataIntegrityViolationException.class,
				() -> clientes.saveAndFlush(new Cliente(TipoDocumentoCliente.RUC, null, "Sin numero", null, null)));
	}
}
