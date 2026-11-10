package com.migal.smartops.despacho;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.migal.smartops.TestcontainersConfiguration;
import com.migal.smartops.comercial.model.Venta;
import com.migal.smartops.comercial.repository.VentaRepository;
import com.migal.smartops.despacho.model.EstadoGuia;
import com.migal.smartops.despacho.model.GuiaDespacho;
import com.migal.smartops.despacho.model.ReimpresionGuia;
import com.migal.smartops.despacho.repository.GuiaDespachoRepository;
import com.migal.smartops.despacho.repository.ReimpresionGuiaRepository;
import com.migal.smartops.seguridad.model.RolUsuario;
import com.migal.smartops.seguridad.model.Usuario;
import com.migal.smartops.seguridad.repository.UsuarioRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verifica el mapeo de la guía de despacho y de sus reimpresiones con PostgreSQL real, incluidas las restricciones
 * de las migraciones V6 y V12. Cada prueba se revierte al terminar.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class GuiaDespachoRepositoryTest {

	private static final Instant MOMENTO = Instant.parse("2026-11-07T14:32:10Z");

	@Autowired
	private GuiaDespachoRepository guias;
	@Autowired
	private ReimpresionGuiaRepository reimpresiones;
	@Autowired
	private VentaRepository ventas;
	@Autowired
	private UsuarioRepository usuarios;
	@Autowired
	private JdbcClient jdbc;

	private Usuario almacenero;
	private Venta venta;

	@BeforeEach
	void preparar() {
		String sufijo = UUID.randomUUID().toString().substring(0, 8);
		almacenero = usuarios.saveAndFlush(new Usuario("Prueba", "Despacho", "dsp-" + sufijo, "{hash}", RolUsuario.ALMACENERO));
		Long cliente = jdbc.sql("INSERT INTO cliente (nombre_razon_social) VALUES ('Cliente de despacho') RETURNING id_cliente")
				.query(Long.class).single();
		Long cotizacion = jdbc.sql("""
						INSERT INTO cotizacion (id_cliente, id_usuario_responsable, fecha_vencimiento, descuento, estado)
						VALUES (:cliente, :usuario, CURRENT_DATE + 30, 0, 'validada') RETURNING id_cotizacion
						""")
				.param("cliente", cliente).param("usuario", almacenero.getId()).query(Long.class).single();
		Long idVenta = jdbc.sql("""
						INSERT INTO venta (id_cotizacion, id_cliente, id_usuario_responsable, descuento, estado)
						VALUES (:cotizacion, :cliente, :usuario, 0, 'pendiente_despacho') RETURNING id_venta
						""")
				.param("cotizacion", cotizacion).param("cliente", cliente).param("usuario", almacenero.getId())
				.query(Long.class).single();
		venta = ventas.findById(idVenta).orElseThrow();
	}

	@Test
	void guardaLaGuiaEmitidaConCodigoGeneradoYFechaDeEmision() {
		GuiaDespacho guardada = guias.saveAndFlush(new GuiaDespacho(venta, "Av. de prueba 100"));

		assertEquals(EstadoGuia.EMITIDA, guardada.getEstado());
		assertNotNull(guardada.getFechaEmision());
		assertEquals("D-" + String.format("%04d", guardada.getId()), guardada.getCodigo());
		assertTrue(guias.existsByVentaId(venta.getId()));
		assertEquals(venta.getId(), guias.idVentaDeGuia(guardada.getId()).orElseThrow());
		assertEquals(guardada.getId(), guias.bloquearPorVenta(venta.getId()).orElseThrow().getId());
	}

	@Test
	void rechazaUnaSegundaGuiaParaLaMismaVenta() {
		guias.saveAndFlush(new GuiaDespacho(venta, "Av. de prueba 100"));

		assertThrows(DataIntegrityViolationException.class,
				() -> guias.saveAndFlush(new GuiaDespacho(venta, "Av. de prueba 200")));
	}

	@Test
	void registraLaConfirmacionDeSalidaYLaEntrega() {
		GuiaDespacho guia = guias.saveAndFlush(new GuiaDespacho(venta, "Av. de prueba 100"));

		guia.confirmarSalida(almacenero, MOMENTO);
		guias.flush();
		assertEquals(EstadoGuia.SALIDA_CONFIRMADA, guia.getEstado());
		assertEquals(MOMENTO, guia.getFechaSalidaReal());
		assertEquals(almacenero.getId(), guia.getUsuarioResponsable().getId());

		guia.registrarEntrega(MOMENTO.plusSeconds(3600));
		guias.flush();
		assertEquals(EstadoGuia.ENTREGADA, guia.getEstado());
		assertEquals(MOMENTO.plusSeconds(3600), guia.getFechaEntrega());
	}

	@Test
	void unaGuiaConSalidaConfirmadaNoSeAnula() {
		GuiaDespacho guia = guias.saveAndFlush(new GuiaDespacho(venta, "Av. de prueba 100"));
		guia.confirmarSalida(almacenero, MOMENTO);

		assertThrows(IllegalStateException.class, guia::anular);
	}

	@Test
	void anulaUnaGuiaEmitidaSinDatosDeSalida() {
		GuiaDespacho guia = guias.saveAndFlush(new GuiaDespacho(venta, "Av. de prueba 100"));

		guia.anular();
		guias.flush();

		assertEquals(EstadoGuia.ANULADA, guias.findById(guia.getId()).orElseThrow().getEstado());
		assertNull(guia.getFechaSalidaReal());
		assertNull(guia.getUsuarioResponsable());
	}

	@Test
	void registraReimpresionesDeSoloAnexion() {
		GuiaDespacho guia = guias.saveAndFlush(new GuiaDespacho(venta, "Av. de prueba 100"));
		ReimpresionGuia reimpresion = reimpresiones.saveAndFlush(
				new ReimpresionGuia(guia, almacenero, "Documento dañado durante el traslado"));

		assertNotNull(reimpresion.getFechaReimpresion());
		assertEquals(1, reimpresiones.findByGuiaIdOrderByIdAsc(guia.getId()).size());
		assertEquals(EstadoGuia.EMITIDA, guia.getEstado());
		assertThrows(DataIntegrityViolationException.class, () -> jdbc.sql(
						"UPDATE reimpresion_guia SET motivo = 'otro' WHERE id_reimpresion = :id")
				.param("id", reimpresion.getId()).update());
	}

	@Test
	void rechazaUnaReimpresionSinMotivo() {
		GuiaDespacho guia = guias.saveAndFlush(new GuiaDespacho(venta, "Av. de prueba 100"));

		assertThrows(DataIntegrityViolationException.class,
				() -> reimpresiones.saveAndFlush(new ReimpresionGuia(guia, almacenero, "   ")));
	}
}
