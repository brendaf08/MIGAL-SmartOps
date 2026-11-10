package com.migal.smartops.agenteia.service;

import java.util.Map;

/**
 * Herramienta que el proveedor del modelo decide invocar para una consulta, con sus parámetros.
 *
 * @param nombre     nombre de la herramienta, igual al operationId de la operación del contrato
 * @param parametros parámetros de la invocación
 */
public record LlamadaHerramienta(String nombre, Map<String, String> parametros) {

	public LlamadaHerramienta {
		parametros = parametros == null ? Map.of() : Map.copyOf(parametros);
	}
}
