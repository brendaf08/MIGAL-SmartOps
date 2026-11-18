package com.migal.smartops.comun.web;

/**
 * Patrón de búsqueda por contenido para {@code LIKE}. Escapa los comodines del texto del usuario: sin escapar, buscar
 * "50%" o "a_b" haría coincidir cualquier carácter en esas posiciones. Se usa junto con {@link #ESCAPE} en
 * {@code CriteriaBuilder.like}.
 */
public final class Busqueda {

	/** Carácter de escape de los patrones generados. */
	public static final char ESCAPE = '\\';

	private Busqueda() {
	}

	/** Patrón en minúsculas que contiene el texto en cualquier posición, con sus comodines escapados. */
	public static String patron(String texto) {
		String escapado = texto.trim().toLowerCase().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
		return "%" + escapado + "%";
	}
}
