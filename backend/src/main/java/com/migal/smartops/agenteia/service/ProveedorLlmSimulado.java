package com.migal.smartops.agenteia.service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Proveedor simulado para desarrollo y pruebas. No realiza llamadas de red: interpreta la consulta por palabras clave y
 * elige como máximo una herramienta, siempre entre las disponibles para el rol. La redacción concatena los resúmenes de las
 * invocaciones exitosas; si no hay datos, la respuesta es incierta y no incluye cifras (RF-IA-08).
 */
public class ProveedorLlmSimulado implements ProveedorLlm {

	private static final String NO_HAY_DATOS = "No dispongo de información suficiente para responder esta consulta. "
			+ "Puede precisar el producto, el almacén o el periodo.";
	private static final Set<String> PALABRAS_DESCARTADAS = Set.of("de", "del", "la", "el", "los", "las", "en", "hay",
			"cuanto", "cuánto", "por", "con", "y", "a", "al", "un", "una", "stock", "disponibilidad", "disponible",
			"producto", "productos", "movimiento", "movimientos", "kardex", "alerta", "alertas", "venta", "ventas",
			"guia", "guía", "guias", "guías", "recepcion", "recepción", "recepciones", "pendientes", "pendiente", "despacho");

	@Override
	public List<LlamadaHerramienta> planificar(String consulta, List<DefinicionHerramienta> disponibles) {
		String minusculas = consulta.toLowerCase(Locale.ROOT);
		String herramienta = herramientaPara(minusculas);
		if (herramienta == null || disponibles.stream().noneMatch(definicion -> definicion.nombre().equals(herramienta))) {
			return List.of();
		}
		String terminos = terminos(minusculas);
		return List.of(new LlamadaHerramienta(herramienta, terminos.isEmpty() ? Map.of() : Map.of("q", terminos)));
	}

	@Override
	public RespuestaRedactada redactar(String consulta, List<InvocacionHerramienta> invocaciones) {
		String datos = invocaciones.stream().filter(invocacion -> invocacion.codigo() == null)
				.map(InvocacionHerramienta::resumen).collect(Collectors.joining(" "));
		if (datos.isEmpty()) {
			return new RespuestaRedactada(NO_HAY_DATOS, true);
		}
		return new RespuestaRedactada(datos, false);
	}

	/** Herramienta asociada a la primera palabra clave encontrada, en orden de prioridad. */
	private static String herramientaPara(String consulta) {
		if (contieneAlguna(consulta, "alerta", "reponer", "bajo stock")) {
			return "listarAlertasStock";
		}
		if (contieneAlguna(consulta, "kardex", "movimiento")) {
			return "consultarKardex";
		}
		if (contieneAlguna(consulta, "recepci")) {
			return "listarRecepciones";
		}
		if (contieneAlguna(consulta, "pendiente")) {
			return "listarVentasPendientesDespacho";
		}
		if (contieneAlguna(consulta, "guía", "guia", "despacho")) {
			return "listarGuiasDespacho";
		}
		if (contieneAlguna(consulta, "venta")) {
			return "listarVentas";
		}
		if (contieneAlguna(consulta, "disponib", "stock", "cuánto hay", "cuanto hay", "hay ")) {
			return "consultarDisponibilidad";
		}
		if (contieneAlguna(consulta, "producto", "catálogo", "catalogo")) {
			return "listarProductos";
		}
		return null;
	}

	/** Términos de la consulta sin las palabras de control, separados por espacios. */
	private static String terminos(String consulta) {
		return java.util.Arrays.stream(consulta.split("[\\s,;:?¿!¡]+"))
				.filter(palabra -> !palabra.isBlank() && !PALABRAS_DESCARTADAS.contains(palabra))
				.collect(Collectors.joining(" "));
	}

	private static boolean contieneAlguna(String texto, String... claves) {
		for (String clave : claves) {
			if (texto.contains(clave)) {
				return true;
			}
		}
		return false;
	}
}
