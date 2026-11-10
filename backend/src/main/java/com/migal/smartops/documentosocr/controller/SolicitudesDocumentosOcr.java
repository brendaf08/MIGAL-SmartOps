package com.migal.smartops.documentosocr.controller;

import com.migal.smartops.comun.errores.CodigoError;
import com.migal.smartops.comun.errores.ErrorDeNegocio;
import com.migal.smartops.documentosocr.service.DocumentoOcrService.CoincidenciaOcr;
import com.migal.smartops.documentosocr.service.DocumentoOcrService.CorreccionDetalle;
import com.migal.smartops.documentosocr.service.DocumentoOcrService.LineaOcr;
import com.migal.smartops.documentosocr.service.DocumentoOcrService.ResultadoOcr;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Cuerpos de solicitud del módulo de documentos OCR, con las restricciones de los esquemas del contrato. La corrección de
 * una línea se interpreta a partir del mapa del cuerpo, porque el contrato distingue un campo omitido de un campo nulo.
 */
public final class SolicitudesDocumentosOcr {

	private static final String IDENTIFICADOR = "^[0-9]{1,18}$";
	private static final Set<String> CAMPOS_CORRECCION = Set.of("textoCorregido", "idProducto", "cantidad");

	private SolicitudesDocumentosOcr() {
	}

	/** {@code DocumentoResultadoCoincidencia}: SKU sugerido por el OCR y su confianza. */
	public record CoincidenciaSolicitud(@NotBlank @Size(max = 50) String sku,
			@NotNull @DecimalMin("0") @DecimalMax("1") BigDecimal confianza) {
	}

	/** {@code DocumentoResultadoLinea}: línea leída por el OCR. */
	public record LineaSolicitud(@NotBlank @Size(max = 500) String textoCrudo, @DecimalMin("0") BigDecimal cantidadLeida,
			@NotNull @Valid List<CoincidenciaSolicitud> coincidencias) {
	}

	/** {@code DocumentoResultados}: resultado del OCR para un documento pendiente. */
	public record ResultadosSolicitud(@NotNull @Pattern(regexp = "^(procesado|ilegible)$") String resultado,
			@NotNull @Valid List<LineaSolicitud> detalles) {

		/** Convierte la solicitud al resultado del servicio, sin validar las reglas de negocio. */
		public ResultadoOcr aResultado() {
			List<LineaOcr> lineas = new ArrayList<>();
			for (LineaSolicitud linea : detalles) {
				List<CoincidenciaOcr> coincidencias = linea.coincidencias().stream()
						.map(c -> new CoincidenciaOcr(c.sku(), c.confianza())).toList();
				lineas.add(new LineaOcr(linea.textoCrudo(), linea.cantidadLeida(), coincidencias));
			}
			return new ResultadoOcr("ilegible".equals(resultado), lineas);
		}
	}

	/** {@code DocumentoAsociacion}: exactamente una de las dos operaciones, validada por el servicio. */
	public record AsociacionSolicitud(@Pattern(regexp = IDENTIFICADOR) String idCotizacion,
			@Pattern(regexp = IDENTIFICADOR) String idRecepcion) {
	}

	/**
	 * Interpreta el cuerpo de la corrección de una línea. Los campos omitidos conservan su valor; los nulos informados
	 * eliminan el dato. El cuerpo debe tener al menos un campo y solo campos del contrato.
	 */
	public static CorreccionDetalle aCorreccion(Map<String, Object> cuerpo) {
		if (cuerpo == null || cuerpo.isEmpty()) {
			throw invalido("El cuerpo debe informar al menos un campo de la corrección.");
		}
		for (String campo : cuerpo.keySet()) {
			if (!CAMPOS_CORRECCION.contains(campo)) {
				throw invalido("El campo " + campo + " no pertenece a la corrección de una línea.");
			}
		}
		boolean conTexto = cuerpo.containsKey("textoCorregido");
		String texto = null;
		if (conTexto && cuerpo.get("textoCorregido") != null) {
			if (!(cuerpo.get("textoCorregido") instanceof String valor) || valor.length() > 500) {
				throw invalido("El texto corregido debe ser una cadena de hasta 500 caracteres.");
			}
			texto = valor;
		}
		boolean conProducto = cuerpo.containsKey("idProducto");
		Long idProducto = null;
		if (conProducto && cuerpo.get("idProducto") != null) {
			if (!(cuerpo.get("idProducto") instanceof String valor) || !valor.matches(IDENTIFICADOR)) {
				throw invalido("El identificador del producto no tiene el formato esperado.");
			}
			idProducto = Long.parseLong(valor);
		}
		boolean conCantidad = cuerpo.containsKey("cantidad");
		BigDecimal cantidad = null;
		if (conCantidad && cuerpo.get("cantidad") != null) {
			if (!(cuerpo.get("cantidad") instanceof Number valor)) {
				throw invalido("La cantidad debe ser un número.");
			}
			cantidad = new BigDecimal(valor.toString());
		}
		return new CorreccionDetalle(conTexto, texto, conProducto, idProducto, conCantidad, cantidad);
	}

	private static ErrorDeNegocio invalido(String detalle) {
		return new ErrorDeNegocio(CodigoError.VALIDACION_FALLIDA, detalle);
	}
}
