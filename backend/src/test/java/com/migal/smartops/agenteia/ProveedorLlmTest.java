package com.migal.smartops.agenteia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.migal.smartops.agenteia.service.DefinicionHerramienta;
import com.migal.smartops.agenteia.service.InvocacionHerramienta;
import com.migal.smartops.agenteia.service.LlamadaHerramienta;
import com.migal.smartops.agenteia.service.ProveedorLlmNoConfigurado;
import com.migal.smartops.agenteia.service.ProveedorLlmSimulado;
import com.migal.smartops.comun.errores.CodigoError;
import com.migal.smartops.comun.errores.ErrorDeNegocio;
import com.migal.smartops.seguridad.model.RolUsuario;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Verifica el proveedor simulado (selección determinista de herramientas) y el proveedor no configurado. */
class ProveedorLlmTest {

	private static final List<DefinicionHerramienta> DISPONIBLES = List.of(
			definicion("consultarDisponibilidad"), definicion("listarAlertasStock"));

	@Test
	void simuladoElijeDisponibilidadYConservaLosTerminosDeLaConsulta() {
		List<LlamadaHerramienta> llamadas = new ProveedorLlmSimulado()
				.planificar("Disponibilidad de cemento Sol 42.5 kg", DISPONIBLES);

		assertEquals(1, llamadas.size());
		assertEquals("consultarDisponibilidad", llamadas.get(0).nombre());
		assertEquals(Map.of("q", "cemento sol 42.5 kg"), llamadas.get(0).parametros());
	}

	@Test
	void simuladoNoElijeHerramientasFueraDeLasDisponibles() {
		List<LlamadaHerramienta> llamadas = new ProveedorLlmSimulado()
				.planificar("Kardex de cemento", DISPONIBLES);

		assertTrue(llamadas.isEmpty());
	}

	@Test
	void simuladoSinPalabraClaveNoElijeHerramientas() {
		assertTrue(new ProveedorLlmSimulado().planificar("hola", DISPONIBLES).isEmpty());
	}

	@Test
	void redaccionSinDatosExitososEsIncierta() {
		var respuesta = new ProveedorLlmSimulado().redactar("hola", List.of());

		assertTrue(respuesta.incierta());
		assertFalse(respuesta.texto().isEmpty());
	}

	@Test
	void redaccionConDatosExitososNoEsIncierta() {
		var resultado = new InvocacionHerramienta("consultarDisponibilidad", Map.of(), "Hay 80 bolsas libres.", null,
				List.of());

		var respuesta = new ProveedorLlmSimulado().redactar("disponibilidad", List.of(resultado));

		assertFalse(respuesta.incierta());
		assertEquals("Hay 80 bolsas libres.", respuesta.texto());
	}

	@Test
	void noConfiguradoResponderServicioNoDisponible() {
		var proveedor = new ProveedorLlmNoConfigurado();

		assertEquals(CodigoError.SERVICIO_NO_DISPONIBLE, assertThrows(ErrorDeNegocio.class,
				() -> proveedor.planificar("hola", DISPONIBLES)).codigo());
		assertEquals(CodigoError.SERVICIO_NO_DISPONIBLE, assertThrows(ErrorDeNegocio.class,
				() -> proveedor.redactar("hola", List.of())).codigo());
	}

	private static DefinicionHerramienta definicion(String nombre) {
		return new DefinicionHerramienta(nombre, "Prueba", "GET /prueba", Set.of(RolUsuario.ALMACENERO),
				parametros -> null);
	}
}
