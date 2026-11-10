package com.migal.smartops.agenteia.model;

/**
 * Autor de un mensaje de una conversación con el agente (columna {@code rol} de la tabla {@code mensaje_agente}).
 * El valor persistido y expuesto en el contrato es el texto en minúsculas.
 */
public enum RolMensaje {

	USUARIO("usuario"),
	AGENTE("agente");

	private final String valor;

	RolMensaje(String valor) {
		this.valor = valor;
	}

	/** Valor persistido y expuesto en el contrato. */
	public String valor() {
		return valor;
	}

	/** Obtiene el autor a partir de su valor persistido. */
	public static RolMensaje de(String valor) {
		for (RolMensaje rol : values()) {
			if (rol.valor.equals(valor)) {
				return rol;
			}
		}
		throw new IllegalArgumentException("Rol de mensaje desconocido: " + valor);
	}
}
