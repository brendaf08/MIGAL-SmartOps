package com.migal.smartops.inventario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.migal.smartops.TestcontainersConfiguration;
import com.migal.smartops.catalogo.model.Almacen;
import com.migal.smartops.catalogo.model.Producto;
import com.migal.smartops.catalogo.repository.AlmacenRepository;
import com.migal.smartops.catalogo.repository.CategoriaRepository;
import com.migal.smartops.catalogo.repository.ProductoRepository;
import com.migal.smartops.comun.errores.CodigoError;
import com.migal.smartops.comun.errores.ErrorDeNegocio;
import com.migal.smartops.inventario.model.EstadoReserva;
import com.migal.smartops.inventario.model.Inventario;
import com.migal.smartops.inventario.model.MovimientoKardex;
import com.migal.smartops.inventario.model.OrigenMovimiento;
import com.migal.smartops.inventario.model.TipoOperacion;
import com.migal.smartops.inventario.repository.InventarioRepository;
import com.migal.smartops.inventario.repository.MovimientoKardexRepository;
import com.migal.smartops.seguridad.model.RolUsuario;
import com.migal.smartops.seguridad.model.Usuario;
import com.migal.smartops.seguridad.repository.UsuarioRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verifica el mapeo de las entidades de inventario con PostgreSQL real. Cada prueba se revierte al terminar,
 * porque el Kardex es de solo anexión y sus filas no pueden eliminarse.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class InventarioRepositoryTest {

	@Autowired
	private ProductoRepository productos;
	@Autowired
	private CategoriaRepository categorias;
	@Autowired
	private AlmacenRepository almacenes;
	@Autowired
	private UsuarioRepository usuarios;
	@Autowired
	private InventarioRepository inventarios;
	@Autowired
	private MovimientoKardexRepository kardex;

	private Producto producto;
	private Almacen tienda;
	private Almacen deposito;
	private Usuario responsable;

	@BeforeEach
	void preparar() {
		var categoria = categorias.findAll().get(0);
		producto = productos.saveAndFlush(new Producto("INV-" + UUID.randomUUID().toString().substring(0, 8), "Producto de prueba",
				null, "unidad", new BigDecimal("10.00"), new BigDecimal("5"), categoria, null, null, true));
		tienda = almacenes.saveAndFlush(new Almacen("Tienda prueba " + UUID.randomUUID(), "tienda", null));
		deposito = almacenes.saveAndFlush(new Almacen("Deposito prueba " + UUID.randomUUID(), "almacen", null));
		responsable = usuarios.saveAndFlush(new Usuario("Prueba", "Inventario", "inv-" + UUID.randomUUID().toString().substring(0, 8),
				"{hash}", RolUsuario.ALMACENERO));
	}

	private Inventario existencia(Almacen almacen, String cantidad) {
		return inventarios.saveAndFlush(new Inventario(producto, almacen, new BigDecimal(cantidad)));
	}

	@Test
	void guardaYRecuperaLaExistenciaPorProductoYUbicacion() {
		Inventario guardado = existencia(tienda, "40");

		Inventario leido = inventarios.findByProductoIdAndAlmacenId(producto.getId(), tienda.getId()).orElseThrow();

		assertEquals(guardado.getId(), leido.getId());
		assertEquals(0, new BigDecimal("40").compareTo(leido.getCantidadFisica()));
	}

	@Test
	void sumarYRestarActualizanLaCantidadFisica() {
		Inventario inventario = existencia(tienda, "40");

		inventario.sumar(new BigDecimal("10"));
		inventario.restar(new BigDecimal("25.500"));

		assertEquals(0, new BigDecimal("24.5").compareTo(inventario.getCantidadFisica()));
	}

	@Test
	void restarMasDeLoExistenteResponde409() {
		Inventario inventario = existencia(tienda, "5");

		ErrorDeNegocio error = assertThrows(ErrorDeNegocio.class, () -> inventario.restar(new BigDecimal("5.001")));

		assertEquals(CodigoError.STOCK_INSUFICIENTE, error.codigo());
	}

	@Test
	void bloqueaLasFilasEnOrdenAscendenteDeIdentificador() {
		Inventario primero = existencia(tienda, "10");
		Inventario segundo = existencia(deposito, "20");

		List<Inventario> bloqueados = inventarios.bloquearPorIds(List.of(segundo.getId(), primero.getId()));

		assertEquals(List.of(primero.getId(), segundo.getId()), bloqueados.stream().map(Inventario::getId).toList());
		List<Inventario> porUbicacion = inventarios.bloquearPorProductoYAlmacenes(producto.getId(),
				List.of(deposito.getId(), tienda.getId()));
		assertEquals(List.of(primero.getId(), segundo.getId()), porUbicacion.stream().map(Inventario::getId).toList());
	}

	@Test
	void registraUnAjusteConSuMotivoYObtieneLaFecha() {
		Inventario inventario = existencia(tienda, "40");

		MovimientoKardex guardado = kardex.saveAndFlush(MovimientoKardex.ajuste(inventario, TipoOperacion.SALIDA,
				new BigDecimal("5"), "Bolsas dañadas", responsable, null));

		assertNotNull(guardado.getId());
		assertNotNull(guardado.getFechaHora());
		assertTrue(guardado.getFechaHora().isBefore(Instant.now().plusSeconds(60)));
		assertEquals(OrigenMovimiento.AJUSTE, guardado.getOrigenMovimiento());
		assertEquals(TipoOperacion.SALIDA, guardado.getTipoOperacion());
	}

	@Test
	void unAjusteSinMotivoLoRechazaLaBaseDeDatos() {
		Inventario inventario = existencia(tienda, "40");

		assertThrows(Exception.class, () -> kardex.saveAndFlush(MovimientoKardex.ajuste(inventario,
				TipoOperacion.SALIDA, new BigDecimal("5"), "  ", responsable, null)));
	}

	@Test
	void agrupaLasDosPatasDeUnTrasladoPorSuReferencia() {
		Inventario origen = existencia(tienda, "40");
		Inventario destino = existencia(deposito, "0");
		UUID referencia = UUID.randomUUID();

		kardex.save(MovimientoKardex.traslado(origen, TipoOperacion.SALIDA, new BigDecimal("20"), "Reposición", responsable, referencia));
		kardex.saveAndFlush(MovimientoKardex.traslado(destino, TipoOperacion.ENTRADA, new BigDecimal("20"), "Reposición", responsable, referencia));

		List<MovimientoKardex> patas = kardex.findByReferenciaTrasladoOrderByIdAsc(referencia);
		assertEquals(2, patas.size());
		assertEquals(TipoOperacion.SALIDA, patas.get(0).getTipoOperacion());
		assertEquals(TipoOperacion.ENTRADA, patas.get(1).getTipoOperacion());
	}

	@Test
	void unaReservaActivaSoloPuedeCerrarseUnaVez() {
		Inventario inventario = existencia(tienda, "40");
		var reserva = new com.migal.smartops.inventario.model.ReservaStock(1L, inventario, producto.getId(),
				new BigDecimal("5"), responsable);

		reserva.liberar(Instant.now());

		assertEquals(EstadoReserva.LIBERADA, reserva.getEstado());
		assertThrows(IllegalStateException.class, () -> reserva.consumir(Instant.now()));
	}
}
