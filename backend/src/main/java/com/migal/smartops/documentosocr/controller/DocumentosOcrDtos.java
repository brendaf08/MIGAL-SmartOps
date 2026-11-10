package com.migal.smartops.documentosocr.controller;

import com.migal.smartops.catalogo.model.Producto;
import com.migal.smartops.documentosocr.config.PropiedadesOcr;
import com.migal.smartops.documentosocr.model.CoincidenciaDetalle;
import com.migal.smartops.documentosocr.model.DetalleExtraido;
import com.migal.smartops.documentosocr.model.DocumentoDigital;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Respuestas del módulo de documentos OCR, según los esquemas del contrato. Los identificadores se expresan como texto.
 * Las asociaciones opcionales se informan siempre, aunque sean nulas, porque el contrato las declara obligatorias.
 */
public final class DocumentosOcrDtos {

	private DocumentosOcrDtos() {
	}

	/** {@code DocumentoResumen}: documento sin sus líneas extraídas. */
	public record ResumenDto(String id, String nombreArchivo, String tipoDocumento, String estadoProcesamiento,
			String idUsuario, String idCotizacion, String idRecepcion, String codigoError, Instant fechaSubida) {

		public static ResumenDto de(DocumentoDigital documento) {
			return new ResumenDto(String.valueOf(documento.getId()), documento.getNombreArchivo(),
					documento.getTipo().valor(), documento.getEstado().valor(), String.valueOf(documento.getUsuario().getId()),
					documento.getIdCotizacion() == null ? null : String.valueOf(documento.getIdCotizacion()),
					documento.getIdRecepcion() == null ? null : String.valueOf(documento.getIdRecepcion()),
					documento.getCodigoError(), documento.getFechaSubida());
		}
	}

	/** {@code DocumentoCoincidencia}: producto propuesto con su confianza. */
	public record CoincidenciaDto(String idProducto, String sku, String nombreOficial, BigDecimal confianza) {

		static CoincidenciaDto de(CoincidenciaDetalle coincidencia) {
			Producto producto = coincidencia.getProducto();
			return new CoincidenciaDto(String.valueOf(producto.getId()), producto.getSku(), producto.getNombreOficial(),
					coincidencia.getSimilitud());
		}
	}

	/** {@code DocumentoDetalle}: línea extraída. {@code cantidadCorregida} amplía el esquema con la corrección vigente. */
	public record DetalleDto(String id, String textoCrudo, String textoCorregido, BigDecimal cantidadLeida,
			BigDecimal cantidadCorregida, String idProducto, String sku, List<CoincidenciaDto> coincidencias,
			boolean requiereRevision, boolean descartado) {

		public static DetalleDto de(DetalleExtraido linea) {
			Producto producto = linea.getProducto();
			return new DetalleDto(String.valueOf(linea.getId()), linea.getTextoCrudo(), linea.getTextoCorregido(),
					linea.getCantidadLeida(), linea.getCantidadCorregida(),
					producto == null ? null : String.valueOf(producto.getId()),
					producto == null ? null : producto.getSku(),
					linea.getCoincidencias().stream().map(CoincidenciaDto::de).toList(),
					linea.isRequiereRevision(), linea.isDescartado());
		}
	}

	/** {@code DocumentoUmbrales}: umbrales vigentes del Fuzzy Matching. */
	public record UmbralesDto(BigDecimal alta, BigDecimal baja) {

		static UmbralesDto de(PropiedadesOcr.Umbrales umbrales) {
			return new UmbralesDto(umbrales.alta(), umbrales.baja());
		}
	}

	/** {@code Documento}: documento con sus líneas extraídas y los umbrales vigentes. */
	public record DocumentoDto(String id, String nombreArchivo, String tipoDocumento, String estadoProcesamiento,
			String idUsuario, String idCotizacion, String idRecepcion, String codigoError, Instant fechaSubida,
			List<DetalleDto> detalles, UmbralesDto umbrales) {

		public static DocumentoDto de(DocumentoDigital documento, PropiedadesOcr.Umbrales umbrales) {
			ResumenDto resumen = ResumenDto.de(documento);
			return new DocumentoDto(resumen.id(), resumen.nombreArchivo(), resumen.tipoDocumento(),
					resumen.estadoProcesamiento(), resumen.idUsuario(), resumen.idCotizacion(), resumen.idRecepcion(),
					resumen.codigoError(), resumen.fechaSubida(),
					documento.getDetalles().stream().map(DetalleDto::de).toList(), UmbralesDto.de(umbrales));
		}
	}
}
