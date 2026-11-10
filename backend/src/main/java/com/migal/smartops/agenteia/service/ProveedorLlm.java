package com.migal.smartops.agenteia.service;

import java.util.List;

/**
 * Puerto hacia el modelo de lenguaje del agente (ADR-004, RF-IA-03). El proveedor decide qué herramientas invocar y redacta
 * la respuesta a partir de sus resultados. Solo puede elegir entre las herramientas que recibe, que ya están filtradas por
 * el rol del usuario. Las implementaciones son intercambiables mediante la propiedad {@code migal.agente.proveedor}.
 */
public interface ProveedorLlm {

	/**
	 * Elige las herramientas a invocar para una consulta. Puede devolver una lista vacía si ninguna herramienta responde a
	 * la consulta.
	 *
	 * @param consulta    texto de la consulta del usuario
	 * @param disponibles herramientas que el rol del usuario puede usar
	 * @throws com.migal.smartops.comun.errores.ErrorDeNegocio con SERVICIO_NO_DISPONIBLE si el proveedor no responde
	 */
	List<LlamadaHerramienta> planificar(String consulta, List<DefinicionHerramienta> disponibles);

	/**
	 * Redacta la respuesta a partir de las invocaciones realizadas.
	 *
	 * @throws com.migal.smartops.comun.errores.ErrorDeNegocio con SERVICIO_NO_DISPONIBLE si el proveedor no responde
	 */
	RespuestaRedactada redactar(String consulta, List<InvocacionHerramienta> invocaciones);
}
