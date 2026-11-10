package com.migal.smartops.abastecimiento;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.migal.smartops.TestcontainersConfiguration;
import com.migal.smartops.abastecimiento.model.CatalogoProveedor;
import com.migal.smartops.abastecimiento.model.EstadoOrdenCompra;
import com.migal.smartops.abastecimiento.model.OrdenCompra;
import com.migal.smartops.abastecimiento.model.Proveedor;
import com.migal.smartops.abastecimiento.service.OrdenCompraService;
import com.migal.smartops.abastecimiento.service.OrdenCompraService.DatosActualizacionOrden;
import com.migal.smartops.abastecimiento.service.OrdenCompraService.DatosOrden;
import com.migal.smartops.abastecimiento.service.OrdenCompraService.LineaOrden;
import com.migal.smartops.abastecimiento.service.OrdenCompraService.OrdenDetalle;
import com.migal.smartops.abastecimiento.service.ProveedorService;
import com.migal.smartops.abastecimiento.service.ProveedorService.DatosProveedor;
import com.migal.smartops.abastecimiento.service.ProveedorService.EntradaCatalogo;
import com.migal.smartops.catalogo.model.Almacen;
import com.migal.smartops.catalogo.model.Producto;
import com.migal.smartops.catalogo.repository.AlmacenRepository;
import com.migal.smartops.catalogo.repository.CategoriaRepository;
import com.migal.smartops.catalogo.repository.ProductoRepository;
import com.migal.smartops.comun.errores.CodigoError;
import com.migal.smartops.comun.errores.ErrorDeNegocio;
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
 * Verifica las reglas de los servicios de proveedores y de órdenes de compra con PostgreSQL real: unicidad del RUC,
 * vigencia, catálogo, registro y modificación de órdenes, y cierre. Cada prueba se revierte al terminar.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class AbastecimientoServiceTest {

	private static final String CEMENTO = "Cemento y aglomerantes";

	@Autowired
	private ProveedorService proveedorService;
	@Autowired
	private OrdenCompraService ordenes;
	@Autowired
	private ProductoRepository productos;
	@Autowired
	private CategoriaRepository categorias;
	@Autowired
	private AlmacenRepository almacenes;
	@Autowired
	private RecepcionRepository recepciones;
	@Autowired
	private UsuarioRepository usuarios;

	private Usuario reponedor;
	private Proveedor activo;
	private Producto cemento;
	private Producto fierro;

	@BeforeEach
	void preparar() {
		reponedor = usuarios.saveAndFlush(new Usuario("Ana", "Ruiz", "abs-" + sufijo(), "{hash}", RolUsuario.REPONEDOR));
		activo = proveedorService.registrar(new DatosProveedor(ruc(), "Proveedor de servicio " + sufijo(),
				"ventas@prueba.example", "Pago al contado."), true);
		cemento = producto();
		fierro = producto();
	}

	@Test
	void rechazaUnRucQueYaPerteneceAOtroProveedor() {
		ErrorDeNegocio error = assertThrows(ErrorDeNegocio.class, () -> proveedorService.registrar(
				new DatosProveedor(activo.getRuc(), "Otro", "otro@prueba.example", null), true));

		assertEquals(CodigoError.RUC_DUPLICADO, error.codigo());
		assertEquals(activo.getId().toString(), error.propiedades().get("proveedorId"));
	}

	@Test
	void rechazaUnCatalogoConSkuRepetidoYNoModificaLasAsociaciones() {
		List<EntradaCatalogo> entradas = List.of(new EntradaCatalogo(cemento.getSku(), new BigDecimal("26.50"), 3),
				new EntradaCatalogo(cemento.getSku(), new BigDecimal("27.00"), 3));

		ErrorDeNegocio error = assertThrows(ErrorDeNegocio.class,
				() -> proveedorService.actualizarCatalogo(activo.getId(), entradas));

		assertEquals(CodigoError.VALIDACION_FALLIDA, error.codigo());
		assertTrue(proveedorService.catalogo(activo.getId()).isEmpty());
	}

	@Test
	void reemplazaElCatalogoYEliminaLosProductosOmitidos() {
		proveedorService.actualizarCatalogo(activo.getId(), List.of(
				new EntradaCatalogo(cemento.getSku(), new BigDecimal("26.50"), 3),
				new EntradaCatalogo(fierro.getSku(), new BigDecimal("3.20"), null)));

		List<CatalogoProveedor> resultado = proveedorService.actualizarCatalogo(activo.getId(),
				List.of(new EntradaCatalogo(fierro.getSku(), new BigDecimal("3.50"), null)));

		assertEquals(1, resultado.size());
		assertEquals(fierro.getId(), resultado.get(0).getProducto().getId());
	}

	@Test
	void rechazaLaOrdenDeUnProveedorInactivo() {
		proveedorService.cambiarEstado(activo.getId(), false);

		ErrorDeNegocio error = assertThrows(ErrorDeNegocio.class, () -> registrarOrden(100));

		assertEquals(CodigoError.PROVEEDOR_INACTIVO, error.codigo());
	}

	@Test
	void rechazaLineasConSkuRepetidoOInexistente() {
		DatosOrden datos = new DatosOrden(activo.getId(), null, null, null, List.of(
				new LineaOrden(cemento.getSku(), new BigDecimal("10"), new BigDecimal("26.50")),
				new LineaOrden(cemento.getSku(), new BigDecimal("5"), new BigDecimal("26.50")),
				new LineaOrden("NO-EXISTE-" + sufijo(), new BigDecimal("5"), new BigDecimal("1"))));

		ErrorDeNegocio error = assertThrows(ErrorDeNegocio.class, () -> ordenes.registrar(reponedor.getId(), datos));

		assertEquals(CodigoError.VALIDACION_FALLIDA, error.codigo());
		assertEquals(2, ((List<?>) error.propiedades().get("errores")).size());
	}

	@Test
	void registraLaOrdenPendienteConElTotalDerivado() {
		OrdenDetalle registrada = registrarOrden(100);

		assertEquals(EstadoOrdenCompra.PENDIENTE, registrada.orden().getEstado());
		assertEquals(0, new BigDecimal("2650.00").compareTo(registrada.orden().total()));
		assertTrue(registrada.orden().getCodigo().startsWith("OC-"));
	}

	@Test
	void noPermiteModificarUnaOrdenConRecepcionesRegistradas() {
		OrdenDetalle registrada = registrarOrden(100);
		Almacen tienda = almacenes.saveAndFlush(new Almacen("Tienda orden " + sufijo(), "tienda", null));
		OrdenCompra orden = ordenes.bloquear(registrada.orden().getId());
		Recepcion pendiente = new Recepcion(orden, reponedor, LocalDate.of(2026, 11, 7), null);
		pendiente.reemplazarLineas(List.of(new Recepcion.NuevaLinea(cemento, tienda, new BigDecimal("10"))));
		recepciones.saveAndFlush(pendiente);

		ErrorDeNegocio error = assertThrows(ErrorDeNegocio.class, () -> ordenes.actualizar(orden.getId(),
				new DatosActualizacionOrden(null, null, null, List.of(new LineaOrden(cemento.getSku(),
						new BigDecimal("50"), new BigDecimal("26.50"))))));

		assertEquals(CodigoError.TRANSICION_NO_PERMITIDA, error.codigo());
	}

	@Test
	void noCierraUnaOrdenPendienteSinRecepciones() {
		OrdenDetalle registrada = registrarOrden(100);

		ErrorDeNegocio error = assertThrows(ErrorDeNegocio.class,
				() -> ordenes.cerrar(registrada.orden().getId(), "Motivo"));

		assertEquals(CodigoError.TRANSICION_NO_PERMITIDA, error.codigo());
	}

	@Test
	void exigeMotivoAlCerrarConCantidadesPendientesYLoAnexaALasObservaciones() {
		OrdenCompra orden = ordenParcial(100, 40);

		ErrorDeNegocio sinMotivo = assertThrows(ErrorDeNegocio.class, () -> ordenes.cerrar(orden.getId(), "  "));
		assertEquals(CodigoError.MOTIVO_REQUERIDO, sinMotivo.codigo());

		OrdenDetalle cerrada = ordenes.cerrar(orden.getId(), "Proveedor sin stock del resto");

		assertEquals(EstadoOrdenCompra.COMPLETADA, cerrada.orden().getEstado());
		assertTrue(cerrada.orden().getObservaciones().contains("Proveedor sin stock del resto"));
		assertEquals(0, new BigDecimal("40.000").compareTo(cerrada.recibidoPorProducto().get(cemento.getId())));
	}

	@Test
	void respondeOrdenCerradaAlIntentarCerrarOModificarUnaOrdenCompletada() {
		OrdenCompra orden = ordenParcial(100, 40);
		ordenes.cerrar(orden.getId(), "Cierre");

		ErrorDeNegocio cerrar = assertThrows(ErrorDeNegocio.class, () -> ordenes.cerrar(orden.getId(), "Otra vez"));
		ErrorDeNegocio modificar = assertThrows(ErrorDeNegocio.class, () -> ordenes.actualizar(orden.getId(),
				new DatosActualizacionOrden(null, null, null, List.of(new LineaOrden(cemento.getSku(),
						new BigDecimal("50"), new BigDecimal("26.50"))))));

		assertEquals(CodigoError.ORDEN_COMPRA_CERRADA, cerrar.codigo());
		assertEquals(CodigoError.ORDEN_COMPRA_CERRADA, modificar.codigo());
	}

	private OrdenDetalle registrarOrden(int cantidad) {
		return ordenes.registrar(reponedor.getId(), new DatosOrden(activo.getId(), LocalDate.of(2026, 11, 6), null,
				"Crédito de 30 días", List.of(new LineaOrden(cemento.getSku(), new BigDecimal(cantidad),
						new BigDecimal("26.50")))));
	}

	/** Orden con una recepción validada de {@code recibida} unidades, en estado parcial. */
	private OrdenCompra ordenParcial(int comprada, int recibida) {
		OrdenDetalle registrada = registrarOrden(comprada);
		Almacen tienda = almacenes.saveAndFlush(new Almacen("Tienda parcial " + sufijo(), "tienda", null));
		OrdenCompra orden = ordenes.bloquear(registrada.orden().getId());
		Recepcion recepcion = new Recepcion(orden, reponedor, LocalDate.of(2026, 11, 7), null);
		recepcion.reemplazarLineas(List.of(new Recepcion.NuevaLinea(cemento, tienda, new BigDecimal(recibida))));
		recepcion.validar(Instant.parse("2026-11-07T20:40:12Z"));
		recepciones.saveAndFlush(recepcion);
		assertEquals(EstadoOrdenCompra.PARCIAL, ordenes.recalcularCumplimiento(orden));
		return orden;
	}

	private Producto producto() {
		return productos.saveAndFlush(new Producto("ABS-" + sufijo(), "Producto de servicio", null, "bolsa",
				new BigDecimal("28.50"), new BigDecimal("20"), categorias.findByNombre(CEMENTO).orElseThrow(), "Sol", null,
				true));
	}

	private static String ruc() {
		return "20" + String.format("%09d", ThreadLocalRandom.current().nextInt(1_000_000_000));
	}

	private static String sufijo() {
		return UUID.randomUUID().toString().substring(0, 6);
	}
}
