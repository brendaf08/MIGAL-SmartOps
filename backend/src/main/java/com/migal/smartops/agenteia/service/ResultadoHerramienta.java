package com.migal.smartops.agenteia.service;

import java.util.List;

/**
 * Resultado de la ejecución correcta de una herramienta: un resumen en español para redactar la respuesta y los registros
 * de negocio consultados.
 *
 * @param resumen      texto breve con los datos obtenidos
 * @param referencias  registros consultados, en el orden de la respuesta
 */
public record ResultadoHerramienta(String resumen, List<ReferenciaAgente> referencias) {

	public ResultadoHerramienta {
		referencias = List.copyOf(referencias);
	}
}
