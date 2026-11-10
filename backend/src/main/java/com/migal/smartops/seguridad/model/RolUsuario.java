package com.migal.smartops.seguridad.model;

import java.util.Arrays;

/**
 * Nivel de acceso de un usuario. El valor almacenado en la base de datos y expuesto en el contrato
 * es el texto en minúsculas ({@code administrador}, {@code vendedor}, {@code almacenero} y
 * {@code reponedor}), según el ADR-009.
 */
public enum RolUsuario {

	ADMINISTRADOR("administrador"),
	VENDEDOR("vendedor"),
	ALMACENERO("almacenero"),
	REPONEDOR("reponedor");

	private final String valor;

	RolUsuario(String valor) {
		this.valor = valor;
	}

	/** Valor persistido y expuesto en el contrato. */
	public String valor() {
		return valor;
	}

	/** Obtiene el rol a partir de su valor persistido. */
	public static RolUsuario desdeValor(String valor) {
		return Arrays.stream(values())
				.filter(rol -> rol.valor.equals(valor))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Rol desconocido: " + valor));
	}
}
