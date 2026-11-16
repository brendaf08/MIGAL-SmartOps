package com.migal.smartops.agenteia.service;

import java.util.ArrayList;
import java.util.List;

/**
 * Parámetro de una herramienta del agente, igual a un parámetro de la operación del contrato (ADR-012, punto 7). El
 * proveedor del modelo lo declara para que el modelo sepa qué valores puede enviar. Los valores siempre llegan a la
 * herramienta como texto y los valida {@link ParametrosHerramienta}.
 *
 * @param nombre      nombre del parámetro en el contrato
 * @param tipo        tipo del valor
 * @param descripcion descripción breve para el modelo
 * @param obligatorio verdadero si la operación lo exige
 * @param valores     valores admitidos; vacío si el parámetro no es un catálogo cerrado
 */
public record ParametroHerramienta(String nombre, Tipo tipo, String descripcion, boolean obligatorio,
		List<String> valores) {

	/** Tipo del valor de un parámetro. */
	public enum Tipo {
		TEXTO, ENTERO, BOOLEANO, FECHA
	}

	public ParametroHerramienta {
		valores = valores == null ? List.of() : List.copyOf(valores);
	}

	/** Texto opcional. */
	public static ParametroHerramienta texto(String nombre, String descripcion) {
		return new ParametroHerramienta(nombre, Tipo.TEXTO, descripcion, false, List.of());
	}

	/** Identificador técnico opcional, que el modelo envía como número entero. */
	public static ParametroHerramienta entero(String nombre, String descripcion) {
		return new ParametroHerramienta(nombre, Tipo.ENTERO, descripcion, false, List.of());
	}

	/** Identificador técnico obligatorio, por ejemplo el {id} de la ruta. */
	public static ParametroHerramienta identificador(String descripcion) {
		return new ParametroHerramienta("id", Tipo.ENTERO, descripcion, true, List.of());
	}

	/** Valor lógico opcional. */
	public static ParametroHerramienta booleano(String nombre, String descripcion) {
		return new ParametroHerramienta(nombre, Tipo.BOOLEANO, descripcion, false, List.of());
	}

	/** Fecha opcional en formato ISO (AAAA-MM-DD). */
	public static ParametroHerramienta fecha(String nombre, String descripcion) {
		return new ParametroHerramienta(nombre, Tipo.FECHA, descripcion, false, List.of());
	}

	/** Valor opcional de un catálogo cerrado. */
	public static ParametroHerramienta enumerado(String nombre, String descripcion, String... valores) {
		return new ParametroHerramienta(nombre, Tipo.TEXTO, descripcion, false, List.of(valores));
	}

	/** Parámetros de un listado seguidos del rango de fechas inclusivo {@code desde} y {@code hasta}. */
	public static List<ParametroHerramienta> conRangoDeFechas(ParametroHerramienta... parametros) {
		List<ParametroHerramienta> todos = new ArrayList<>(List.of(parametros));
		todos.add(fecha("desde", "Inicio del rango de fechas, inclusivo (AAAA-MM-DD)."));
		todos.add(fecha("hasta", "Fin del rango de fechas, inclusivo (AAAA-MM-DD)."));
		return List.copyOf(todos);
	}
}
