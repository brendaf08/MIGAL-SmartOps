package com.migal.smartops.agenteia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.migal.smartops.TestcontainersConfiguration;
import com.migal.smartops.agenteia.service.CatalogoHerramientas;
import com.migal.smartops.agenteia.service.DefinicionHerramienta;
import com.migal.smartops.agenteia.service.InvocacionHerramienta;
import com.migal.smartops.comun.errores.CodigoError;
import com.migal.smartops.seguridad.model.RolUsuario;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verifica las herramientas del agente sobre ventas, despachos y recepciones contra PostgreSQL real (RF-IA-09): visibilidad
 * por rol, ejecución con los servicios de negocio y códigos de error. Cada prueba se revierte.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class HerramientasOperacionesTest {

	@Autowired
	private CatalogoHerramientas catalogo;

	@Test
	void vendedorConsultaVentasYNoGuiasDeDespacho() {
		InvocacionHerramienta ventas = catalogo.ejecutar(RolUsuario.VENDEDOR, "listarVentas", Map.of());
		InvocacionHerramienta guias = catalogo.ejecutar(RolUsuario.VENDEDOR, "listarGuiasDespacho", Map.of());

		assertNull(ventas.codigo());
		assertEquals(CodigoError.PERMISO_INSUFICIENTE, guias.codigo());
	}

	@Test
	void reponedorNoPuedeListarRecepciones() {
		InvocacionHerramienta invocacion = catalogo.ejecutar(RolUsuario.REPONEDOR, "listarRecepciones", Map.of());

		assertEquals(CodigoError.PERMISO_INSUFICIENTE, invocacion.codigo());
	}

	@Test
	void almaceneroListaRecepcionesSinReferenciasDeTipoNoDefinido() {
		InvocacionHerramienta invocacion = catalogo.ejecutar(RolUsuario.ALMACENERO, "listarRecepciones", Map.of());

		assertNull(invocacion.codigo());
		assertTrue(invocacion.referencias().isEmpty());
	}

	@Test
	void ventaInexistenteRespondeRecursoNoEncontrado() {
		InvocacionHerramienta invocacion = catalogo.ejecutar(RolUsuario.VENDEDOR, "consultarVenta",
				Map.of("id", "999999"));

		assertEquals(CodigoError.RECURSO_NO_ENCONTRADO, invocacion.codigo());
	}

	@Test
	void documentoDeGuiaInexistenteRespondeRecursoNoEncontrado() {
		InvocacionHerramienta invocacion = catalogo.ejecutar(RolUsuario.ALMACENERO, "obtenerDocumentoGuia",
				Map.of("id", "999999"));

		assertEquals(CodigoError.RECURSO_NO_ENCONTRADO, invocacion.codigo());
	}

	@Test
	void estadoFueraDelCatalogoRespondeValidacionFallida() {
		InvocacionHerramienta invocacion = catalogo.ejecutar(RolUsuario.ADMINISTRADOR, "listarVentas",
				Map.of("estado", "inexistente"));

		assertEquals(CodigoError.VALIDACION_FALLIDA, invocacion.codigo());
	}

	@Test
	void catalogoDelVendedorIncluyeSoloLasVentas() {
		List<String> nombres = catalogo.disponiblesPara(RolUsuario.VENDEDOR).stream()
				.map(DefinicionHerramienta::nombre).toList();

		assertTrue(nombres.contains("listarVentas"));
		assertTrue(nombres.contains("consultarVenta"));
		assertFalse(nombres.contains("obtenerDocumentoGuia"));
		assertFalse(nombres.contains("listarRecepciones"));
	}
}
