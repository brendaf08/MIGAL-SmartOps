package com.migal.smartops.agenteia.service;

import com.migal.smartops.comun.errores.CodigoError;
import java.util.List;
import java.util.Map;

/**
 * Llamada a una herramienta del agente y su resultado, según el esquema {@code AgenteHerramientaInvocada} del contrato.
 * Si la herramienta no estaba permitida al rol o no pudo ejecutarse, el código indica el motivo y el resumen lo declara.
 *
 * @param nombre      nombre de la herramienta, igual al operationId de la operación del contrato
 * @param parametros  parámetros con los que se invocó la operación
 * @param resumen     texto breve del resultado o del motivo del fallo
 * @param codigo      código de error, o nulo cuando la herramienta se ejecutó correctamente
 * @param referencias registros consultados; vacío cuando no hubo ejecución
 */
public record InvocacionHerramienta(String nombre, Map<String, String> parametros, String resumen, CodigoError codigo,
		List<ReferenciaAgente> referencias) {

	public InvocacionHerramienta {
		parametros = parametros == null ? Map.of() : Map.copyOf(parametros);
		referencias = List.copyOf(referencias);
	}

	/** Invocación ejecutada con éxito. */
	public static InvocacionHerramienta exitosa(String nombre, Map<String, String> parametros, ResultadoHerramienta resultado) {
		return new InvocacionHerramienta(nombre, parametros, resultado.resumen(), null, resultado.referencias());
	}

	/** Invocación no ejecutada o fallida, con el código que lo explica. */
	public static InvocacionHerramienta fallida(String nombre, Map<String, String> parametros, String resumen, CodigoError codigo) {
		return new InvocacionHerramienta(nombre, parametros, resumen, codigo, List.of());
	}
}
