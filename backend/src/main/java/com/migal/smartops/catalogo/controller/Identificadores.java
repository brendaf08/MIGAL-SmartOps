package com.migal.smartops.catalogo.controller;

import com.migal.smartops.comun.errores.CodigoError;
import com.migal.smartops.comun.errores.ErrorDeNegocio;

/**
 * Convierte el identificador técnico de una ruta a número. Un valor que no es numérico no puede corresponder a
 * ningún registro, por lo que responde {@code RECURSO_NO_ENCONTRADO}, como declara el contrato para las rutas
 * con identificador.
 */
final class Identificadores {

	private Identificadores() {
	}

	static long numero(String id) {
		try {
			return Long.parseLong(id);
		} catch (NumberFormatException e) {
			throw new ErrorDeNegocio(CodigoError.RECURSO_NO_ENCONTRADO, "El identificador no existe.");
		}
	}
}
