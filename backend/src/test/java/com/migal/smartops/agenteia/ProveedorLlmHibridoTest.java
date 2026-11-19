package com.migal.smartops.agenteia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.migal.smartops.agenteia.service.DefinicionHerramienta;
import com.migal.smartops.agenteia.service.InvocacionHerramienta;
import com.migal.smartops.agenteia.service.LlamadaHerramienta;
import com.migal.smartops.agenteia.service.ProveedorLlm;
import com.migal.smartops.agenteia.service.ProveedorLlmHibrido;
import com.migal.smartops.agenteia.service.ProveedorLlmSimulado;
import com.migal.smartops.agenteia.service.RespuestaRedactada;
import com.migal.smartops.agenteia.service.ResultadoHerramienta;
import com.migal.smartops.seguridad.model.RolUsuario;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** Las consultas frecuentes se resuelven por reglas sin llamar al modelo; el resto se delega en él. */
class ProveedorLlmHibridoTest {

	private static final List<DefinicionHerramienta> DISPONIBLES = List.of(definicion("consultarDisponibilidad"),
			definicion("listarAlertasStock"), definicion("consultarKardex"));

	private final AtomicInteger llamadasAlModelo = new AtomicInteger();
	private final ProveedorLlm modelo = new ProveedorLlm() {
		@Override
		public List<LlamadaHerramienta> planificar(String consulta, List<DefinicionHerramienta> disponibles) {
			llamadasAlModelo.incrementAndGet();
			return List.of();
		}

		@Override
		public RespuestaRedactada redactar(String consulta, List<InvocacionHerramienta> invocaciones) {
			llamadasAlModelo.incrementAndGet();
			return new RespuestaRedactada("del modelo", false);
		}
	};
	private final ProveedorLlm hibrido = new ProveedorLlmHibrido(new ProveedorLlmSimulado(), modelo);

	@Test
	void laDisponibilidadDeUnProductoNoLlamaAlModelo() {
		String consulta = "Cuanto de cemento hay en stock";
		List<LlamadaHerramienta> llamadas = hibrido.planificar(consulta, DISPONIBLES);

		assertEquals("consultarDisponibilidad", llamadas.get(0).nombre());
		assertEquals(Map.of("q", "cemento"), llamadas.get(0).parametros());
		RespuestaRedactada respuesta = hibrido.redactar(consulta, List.of(exitosa("consultarDisponibilidad")));
		assertEquals("Hay 80 bolsas.", respuesta.texto());
		assertFalse(respuesta.incierta());
		assertEquals(0, llamadasAlModelo.get());
	}

	@Test
	void lasAlertasDeStockNoLlamanAlModelo() {
		List<LlamadaHerramienta> llamadas = hibrido.planificar("¿Productos en stock crítico?", DISPONIBLES);

		assertEquals("listarAlertasStock", llamadas.get(0).nombre());
		assertEquals(0, llamadasAlModelo.get());
	}

	@Test
	void unaConsultaCompleja_seDelegaEnElModelo() {
		hibrido.planificar("cuánto cemento hay y cuánto se vendió ayer", DISPONIBLES);
		hibrido.planificar("compara el stock de cemento y fierro", DISPONIBLES);
		hibrido.planificar("movimientos del kardex de cemento", DISPONIBLES);
		hibrido.planificar("¿y en el almacén 2?", DISPONIBLES);

		assertEquals(4, llamadasAlModelo.get());
	}

	@Test
	void unaDisponibilidadSinProductoOConDemasiadosTerminosSeDelega() {
		hibrido.planificar("stock", DISPONIBLES);
		hibrido.planificar("hay stock de cemento sol tipo uno bolsa grande", DISPONIBLES);

		assertEquals(2, llamadasAlModelo.get());
	}

	@Test
	void sinLaHerramientaDisponibleParaElRolSeDelega() {
		hibrido.planificar("Cuanto de cemento hay en stock", List.of(definicion("consultarKardex")));

		assertEquals(1, llamadasAlModelo.get());
	}

	@Test
	void laRedaccionDeUnaConsultaNoRapidaSeDelegaEnElModelo() {
		RespuestaRedactada respuesta = hibrido.redactar("movimientos del kardex de cemento",
				List.of(exitosa("consultarKardex")));

		assertEquals("del modelo", respuesta.texto());
		assertTrue(llamadasAlModelo.get() == 1);
	}

	private static InvocacionHerramienta exitosa(String nombre) {
		return InvocacionHerramienta.exitosa(nombre, Map.of(), new ResultadoHerramienta("Hay 80 bolsas.", List.of()));
	}

	private static DefinicionHerramienta definicion(String nombre) {
		return new DefinicionHerramienta(nombre, "Prueba", "GET /prueba", Set.of(RolUsuario.ALMACENERO), List.of(),
				parametros -> new ResultadoHerramienta("ok", List.of()));
	}
}
