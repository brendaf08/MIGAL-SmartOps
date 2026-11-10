package com.migal.smartops.agenteia.service;

import com.migal.smartops.comun.errores.CodigoError;
import com.migal.smartops.comun.errores.ErrorDeNegocio;
import java.util.List;

/**
 * Proveedor usado cuando no hay un modelo de lenguaje configurado. Toda consulta responde SERVICIO_NO_DISPONIBLE, de modo
 * que el agente no devuelve respuestas inventadas.
 */
public class ProveedorLlmNoConfigurado implements ProveedorLlm {

	private static final String DETALLE = "El agente de IA no está configurado. Intente más tarde.";

	@Override
	public List<LlamadaHerramienta> planificar(String consulta, List<DefinicionHerramienta> disponibles) {
		throw new ErrorDeNegocio(CodigoError.SERVICIO_NO_DISPONIBLE, DETALLE);
	}

	@Override
	public RespuestaRedactada redactar(String consulta, List<InvocacionHerramienta> invocaciones) {
		throw new ErrorDeNegocio(CodigoError.SERVICIO_NO_DISPONIBLE, DETALLE);
	}
}
