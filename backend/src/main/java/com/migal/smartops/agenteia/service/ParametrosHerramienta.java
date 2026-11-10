package com.migal.smartops.agenteia.service;

import com.migal.smartops.comun.errores.CodigoError;
import com.migal.smartops.comun.errores.ErrorDeNegocio;
import java.time.LocalDate;
import java.util.Map;

/**
 * Lectura de los parámetros de una invocación de herramienta. Los valores llegan como texto; los vacíos se tratan como
 * ausentes. Un valor mal formado responde VALIDACION_FALLIDA con el nombre del parámetro.
 */
final class ParametrosHerramienta {

	/** Número máximo de registros que una herramienta devuelve al agente en una sola consulta. */
	static final int LIMITE = 10;

	private ParametrosHerramienta() {
	}

	/** Texto sin espacios laterales, o nulo si el parámetro falta o está vacío. */
	static String texto(Map<String, String> parametros, String nombre) {
		String valor = parametros.get(nombre);
		return valor == null || valor.isBlank() ? null : valor.trim();
	}

	/** Número entero, o nulo si el parámetro falta. */
	static Long entero(Map<String, String> parametros, String nombre) {
		String valor = texto(parametros, nombre);
		if (valor == null) {
			return null;
		}
		try {
			return Long.valueOf(valor);
		} catch (NumberFormatException error) {
			throw invalido(nombre);
		}
	}

	/** Número entero obligatorio. */
	static long requerido(Map<String, String> parametros, String nombre) {
		Long valor = entero(parametros, nombre);
		if (valor == null) {
			throw new ErrorDeNegocio(CodigoError.VALIDACION_FALLIDA,
					"Falta el parámetro " + nombre + " de la consulta.");
		}
		return valor;
	}

	/** Valor lógico, falso si el parámetro falta. */
	static boolean booleano(Map<String, String> parametros, String nombre) {
		String valor = texto(parametros, nombre);
		return valor != null && Boolean.parseBoolean(valor);
	}

	/** Fecha en formato ISO, o nula si el parámetro falta. */
	static LocalDate fecha(Map<String, String> parametros, String nombre) {
		String valor = texto(parametros, nombre);
		if (valor == null) {
			return null;
		}
		try {
			return LocalDate.parse(valor);
		} catch (RuntimeException error) {
			throw invalido(nombre);
		}
	}

	private static ErrorDeNegocio invalido(String nombre) {
		return new ErrorDeNegocio(CodigoError.VALIDACION_FALLIDA, "El parámetro " + nombre + " no es válido.");
	}
}
