package com.migal.smartops.comercial;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.migal.smartops.TestcontainersConfiguration;
import com.migal.smartops.catalogo.service.CategoriaService;
import com.migal.smartops.catalogo.service.ProductoService;
import com.migal.smartops.comercial.model.Cotizacion;
import com.migal.smartops.comercial.model.EstadoCotizacion;
import com.migal.smartops.comercial.model.OrigenRechazoCotizacion;
import com.migal.smartops.comercial.model.TipoDocumentoCliente;
import com.migal.smartops.comercial.repository.CotizacionRepository;
import com.migal.smartops.comercial.service.ClienteService;
import com.migal.smartops.comercial.service.CotizacionService;
import com.migal.smartops.comercial.service.CotizacionService.DatosCotizacion;
import com.migal.smartops.comercial.service.CotizacionService.LineaDatos;
import com.migal.smartops.comercial.service.CotizacionService.LineaDivision;
import com.migal.smartops.comun.errores.CodigoError;
import com.migal.smartops.comun.errores.ErrorDeNegocio;
import com.migal.smartops.inventario.service.ConsultaInventarioService;
import com.migal.smartops.seguridad.model.RolUsuario;
import com.migal.smartops.seguridad.model.Usuario;
import com.migal.smartops.seguridad.service.UsuarioService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verifica las transiciones, las reglas de datos y el vencimiento automático de las cotizaciones con PostgreSQL real.
 * Las operaciones usan un reloj fijo (ADR-010): 2 de noviembre de 2026 a las 10:00 en Lima, con validez de 7 días.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class CotizacionServiceTest {

	private static final Instant EMISION = Instant.parse("2026-11-02T15:00:00Z");

	@Autowired
	private CotizacionRepository cotizaciones;
	@Autowired
	private ClienteService clientes;
	@Autowired
	private ProductoService productos;
	@Autowired
	private CategoriaService categorias;
	@Autowired
	private UsuarioService usuarios;
	@Autowired
	private ConsultaInventarioService inventario;

	private CotizacionService servicio;
	private Usuario vendedor;
	private long idProducto;
	private long idCliente;

	private CotizacionService servicioEn(Instant instante) {
		return new CotizacionService(cotizaciones, clientes, productos, usuarios, inventario,
				Clock.fixed(instante, ZoneOffset.UTC));
	}

	@BeforeEach
	void preparar() {
		servicio = servicioEn(EMISION);
		String sufijo = UUID.randomUUID().toString().substring(0, 8);
		vendedor = usuarios.registrar("Prueba", "Comercial", "vend" + sufijo, "ClaveTemporal1", RolUsuario.VENDEDOR);
		idProducto = productos.registrar("COT-" + sufijo, new ProductoService.DatosProducto("Cemento de prueba", null,
				"bolsa", new BigDecimal("28.50"), new BigDecimal("10"), categorias.listar().get(0).getId(), null, null),
				true).getId();
		idCliente = clientes.registrar(new ClienteService.DatosCliente(TipoDocumentoCliente.RUC,
				"20" + ThreadLocalRandom.current().nextLong(100_000_000L, 999_999_999L), "Cliente de prueba", null,
				null)).getId();
	}

	private static LineaDatos linea(long producto, String cantidad, String precio) {
		return new LineaDatos(producto, new BigDecimal(cantidad), new BigDecimal(precio));
	}

	private static DatosCotizacion datos(Long cliente, String descuento, LineaDatos... lineas) {
		return new DatosCotizacion(cliente, List.of(lineas), descuento == null ? null : new BigDecimal(descuento), 7,
				null);
	}

	private Cotizacion emitida() {
		return servicio.crear(datos(idCliente, "25", linea(idProducto, "40", "28.50")), vendedor.getId());
	}

	private static void assertCodigo(CodigoError codigo, Executable accion) {
		assertEquals(codigo, assertThrows(ErrorDeNegocio.class, accion).codigo());
	}

	@Test
	void creaUnaCotizacionEmitidaConVencimientoYTotalesDerivados() {
		Cotizacion nueva = emitida();
		assertEquals(EstadoCotizacion.EMITIDA, nueva.getEstado());
		assertEquals(LocalDate.of(2026, 11, 9), nueva.getFechaVencimiento());
		assertEquals(0, new BigDecimal("1115.00").compareTo(nueva.total()));
	}

	@Test
	void rechazaLasReglasDeClienteLineasYDescuento() {
		assertCodigo(CodigoError.CLIENTE_REQUERIDO,
				() -> servicio.crear(datos(null, null, linea(idProducto, "1", "1")), vendedor.getId()));
		clientes.cambiarEstado(idCliente, false);
		assertCodigo(CodigoError.CLIENTE_INACTIVO,
				() -> servicio.crear(datos(idCliente, null, linea(idProducto, "1", "1")), vendedor.getId()));
		clientes.cambiarEstado(idCliente, true);
		assertCodigo(CodigoError.VALIDACION_FALLIDA,
				() -> servicio.crear(datos(idCliente, null, linea(idProducto, "1", "1"), linea(idProducto, "2", "1")),
						vendedor.getId()));
		assertCodigo(CodigoError.VALIDACION_FALLIDA,
				() -> servicio.crear(datos(idCliente, "500", linea(idProducto, "1", "1")), vendedor.getId()));
	}

	@Test
	void aceptaUnaVezYValidaSoloUnaCotizacionAceptada() {
		Cotizacion nueva = emitida();
		Cotizacion aceptada = servicio.registrarAceptacion(nueva.getId());
		assertEquals(EstadoCotizacion.ACEPTADA, aceptada.getEstado());
		assertNotNull(aceptada.getFechaAceptacionCliente());
		assertCodigo(CodigoError.COTIZACION_NO_EDITABLE, () -> servicio.registrarAceptacion(nueva.getId()));

		Cotizacion validada = servicio.registrarValidacion(nueva.getId());
		assertEquals(EstadoCotizacion.VALIDADA, validada.getEstado());
		assertNotNull(validada.getFechaValidacionMigal());
		assertCodigo(CodigoError.COTIZACION_NO_CONVERTIBLE, () -> servicio.registrarValidacion(nueva.getId()));
	}

	@Test
	void rechazoDeMigalSoloLoRegistraElAdministrador() {
		Cotizacion nueva = emitida();
		servicio.registrarAceptacion(nueva.getId());
		assertCodigo(CodigoError.PERMISO_INSUFICIENTE, () -> servicio.registrarRechazo(nueva.getId(),
				OrigenRechazoCotizacion.MIGAL, "Sin abastecimiento", vendedor.getId(), RolUsuario.VENDEDOR));
		Cotizacion rechazada = servicio.registrarRechazo(nueva.getId(), OrigenRechazoCotizacion.MIGAL,
				"Sin abastecimiento", vendedor.getId(), RolUsuario.ADMINISTRADOR);
		assertEquals(EstadoCotizacion.RECHAZADA, rechazada.getEstado());
		assertEquals(OrigenRechazoCotizacion.MIGAL, rechazada.getOrigenRechazo());
	}

	@Test
	void rechazoDelClienteRequiereEstadoEmitidaYMotivo() {
		Cotizacion nueva = emitida();
		assertCodigo(CodigoError.VALIDACION_FALLIDA, () -> servicio.registrarRechazo(nueva.getId(),
				OrigenRechazoCotizacion.CLIENTE, " ", vendedor.getId(), RolUsuario.VENDEDOR));
		servicio.registrarAceptacion(nueva.getId());
		assertCodigo(CodigoError.COTIZACION_NO_EDITABLE, () -> servicio.registrarRechazo(nueva.getId(),
				OrigenRechazoCotizacion.CLIENTE, "Cambio de planes", vendedor.getId(), RolUsuario.VENDEDOR));
	}

	@Test
	void venceAutomaticamenteYRechazaLaAceptacionPosterior() {
		Cotizacion nueva = emitida();
		CotizacionService posterior = servicioEn(EMISION.plusSeconds(10L * 86_400));
		assertEquals(EstadoCotizacion.VENCIDA, posterior.consultar(nueva.getId()).getEstado());
		assertCodigo(CodigoError.COTIZACION_VENCIDA, () -> posterior.registrarAceptacion(nueva.getId()));
	}

	@Test
	void dividirTrasladaLaCantidadYConservaLaOriginal() {
		Cotizacion nueva = emitida();
		var resultado = servicio.dividir(nueva.getId(), List.of(new LineaDivision(idProducto, new BigDecimal("20"))));
		assertEquals(0, new BigDecimal("20.000").compareTo(resultado.original().getLineas().get(0).getCantidadCotizada()));
		assertEquals(0, new BigDecimal("20.000").compareTo(resultado.nueva().getLineas().get(0).getCantidadCotizada()));
		assertEquals(nueva.getFechaVencimiento(), resultado.nueva().getFechaVencimiento());
		assertEquals(0, BigDecimal.ZERO.compareTo(resultado.nueva().getDescuento()));
		assertCodigo(CodigoError.VALIDACION_FALLIDA,
				() -> servicio.dividir(resultado.original().getId(), List.of(new LineaDivision(idProducto, new BigDecimal("20")))));
	}

	@Test
	void editarRecalculaElVencimientoSoloSiEstaEmitida() {
		Cotizacion nueva = emitida();
		Cotizacion editada = servicio.actualizar(nueva.getId(),
				new DatosCotizacion(idCliente, List.of(linea(idProducto, "10", "28.50")), null, 14, null));
		assertEquals(LocalDate.of(2026, 11, 16), editada.getFechaVencimiento());
		servicio.registrarAceptacion(nueva.getId());
		assertCodigo(CodigoError.COTIZACION_NO_EDITABLE, () -> servicio.actualizar(nueva.getId(),
				new DatosCotizacion(idCliente, List.of(linea(idProducto, "10", "28.50")), null, 14, null)));
	}

	@Test
	void registrarVencimientoRequiereQueLaFechaSeHayaSuperado() {
		Cotizacion nueva = emitida();
		assertCodigo(CodigoError.VALIDACION_FALLIDA, () -> servicio.registrarVencimiento(nueva.getId()));
		CotizacionService posterior = servicioEn(EMISION.plusSeconds(10L * 86_400));
		assertEquals(EstadoCotizacion.VENCIDA, posterior.registrarVencimiento(nueva.getId()).getEstado());
	}

	@Test
	void listaLasCotizacionesPorEstadoConPaginacion() {
		emitida();
		var pagina = servicio.listar(null, List.of(EstadoCotizacion.EMITIDA), idCliente, null, null,
				PageRequest.of(0, 10));
		assertEquals(1, pagina.getTotalElements());
	}
}
