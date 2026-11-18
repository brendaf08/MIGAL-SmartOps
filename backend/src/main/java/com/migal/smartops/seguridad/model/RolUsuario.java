package com.migal.smartops.seguridad.model;

import java.util.Arrays;
import java.util.List;

/**
 * Nivel de acceso de un usuario. El valor almacenado en la base de datos y expuesto en el contrato
 * es el texto en minúsculas ({@code administrador}, {@code vendedor}, {@code almacenero} y
 * {@code reponedor}), según el ADR-009.
 *
 * <p>{@link #DEMOSTRACION} no es un rol del negocio (ADR-013): reúne los permisos de los cuatro roles para presentar el
 * sistema con una sola cuenta. Solo funciona si {@code MIGAL_ROL_DEMOSTRACION} está activo y no se asigna por la API.
 */
public enum RolUsuario {

	ADMINISTRADOR("administrador"),
	VENDEDOR("vendedor"),
	ALMACENERO("almacenero"),
	REPONEDOR("reponedor"),
	DEMOSTRACION("demostracion");

	private final String valor;

	RolUsuario(String valor) {
		this.valor = valor;
	}

	/** Valor persistido y expuesto en el contrato. */
	public String valor() {
		return valor;
	}

	/** Roles del negocio cuyos permisos reúne el rol de demostración. */
	public static final List<RolUsuario> ROLES_DEL_NEGOCIO = List.of(ADMINISTRADOR, VENDEDOR,
			ALMACENERO, REPONEDOR);

	/** Indica si este rol tiene los permisos de {@code otro}: el mismo rol, o el de demostración, que los reúne todos. */
	public boolean incluye(RolUsuario otro) {
		return this == otro || this == DEMOSTRACION;
	}

	/** Obtiene el rol a partir de su valor persistido. */
	public static RolUsuario desdeValor(String valor) {
		return Arrays.stream(values())
				.filter(rol -> rol.valor.equals(valor))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Rol desconocido: " + valor));
	}
}
