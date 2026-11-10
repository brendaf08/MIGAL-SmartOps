package com.migal.smartops.comun.web;

import com.migal.smartops.comun.errores.CodigoError;
import com.migal.smartops.comun.errores.ErrorDeNegocio;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Interpreta los parámetros de paginación y ordenamiento del contrato: {@code page} en base 1,
 * {@code pageSize} y {@code orden=campo:direccion,...} con una lista blanca de campos por recurso.
 */
public final class Ordenamiento {

	private Ordenamiento() {
	}

	/**
	 * Construye la paginación.
	 *
	 * @param permitidos campos ordenables del contrato asociados al atributo de la entidad
	 * @param porDefecto ordenamiento cuando no se indica {@code orden}
	 */
	public static Pageable paginacion(int page, int pageSize, String orden, Map<String, String> permitidos, Sort porDefecto) {
		Sort sort = orden == null || orden.isBlank() ? porDefecto : interpretar(orden, permitidos);
		return PageRequest.of(page - 1, pageSize, sort);
	}

	private static Sort interpretar(String orden, Map<String, String> permitidos) {
		List<Sort.Order> ordenes = new ArrayList<>();
		for (String parte : orden.split(",", -1)) {
			String[] campoYDireccion = parte.split(":");
			String atributo = campoYDireccion.length == 2 ? permitidos.get(campoYDireccion[0]) : null;
			boolean direccionValida = campoYDireccion.length == 2
					&& (campoYDireccion[1].equals("asc") || campoYDireccion[1].equals("desc"));
			if (atributo == null || !direccionValida) {
				throw new ErrorDeNegocio(CodigoError.VALIDACION_FALLIDA,
						"El ordenamiento '" + parte + "' no es válido. Campos permitidos: " + String.join(", ", permitidos.keySet()) + ".");
			}
			ordenes.add(campoYDireccion[1].equals("asc") ? Sort.Order.asc(atributo) : Sort.Order.desc(atributo));
		}
		return Sort.by(ordenes);
	}
}
