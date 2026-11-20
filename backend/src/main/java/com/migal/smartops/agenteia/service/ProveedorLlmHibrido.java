package com.migal.smartops.agenteia.service;

import java.util.List;
import java.util.Optional;

/**
 * Resuelve al instante, sin llamar al modelo, las consultas frecuentes y sin ambigüedad (alertas de stock y
 * disponibilidad de un producto); el resto lo atiende el proveedor del modelo. Cada llamada a Gemini tarda segundos y una
 * consulta hace dos (planificar y redactar), mientras que las herramientas devuelven un resumen listo para mostrar.
 */
public class ProveedorLlmHibrido implements ProveedorLlm {

	private final ProveedorLlmSimulado reglas;
	private final ProveedorLlm modelo;

	public ProveedorLlmHibrido(ProveedorLlmSimulado reglas, ProveedorLlm modelo) {
		this.reglas = reglas;
		this.modelo = modelo;
	}

	@Override
	public List<LlamadaHerramienta> planificar(String consulta, List<DefinicionHerramienta> disponibles) {
		return planificar(consulta, disponibles, List.of());
	}

	@Override
	public List<LlamadaHerramienta> planificar(String consulta, List<DefinicionHerramienta> disponibles,
			List<TurnoAgente> historial) {
		Optional<LlamadaHerramienta> rapida = reglas.planificarSeguro(consulta, disponibles);
		return rapida.isPresent() ? List.of(rapida.get()) : modelo.planificar(consulta, disponibles, historial);
	}

	@Override
	public RespuestaRedactada redactar(String consulta, List<InvocacionHerramienta> invocaciones) {
		return redactar(consulta, invocaciones, List.of());
	}

	@Override
	public RespuestaRedactada redactar(String consulta, List<InvocacionHerramienta> invocaciones,
			List<TurnoAgente> historial) {
		if (esRapida(consulta, invocaciones)) {
			return reglas.redactar(consulta, invocaciones);
		}
		return modelo.redactar(consulta, invocaciones, historial);
	}

	/** La consulta la planificaron las reglas si coincide con una de ellas y se invocó esa misma herramienta. */
	private boolean esRapida(String consulta, List<InvocacionHerramienta> invocaciones) {
		return invocaciones.size() == 1
				&& reglas.herramientaSegura(consulta).filter(invocaciones.get(0).nombre()::equals).isPresent();
	}

	@Override
	public String toString() {
		return "ProveedorLlmHibrido[" + modelo + "]";
	}
}
