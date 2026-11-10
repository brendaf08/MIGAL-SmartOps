package com.migal.smartops.documentosocr.service;

import com.migal.smartops.documentosocr.model.TipoDocumento;
import java.nio.file.Path;

/**
 * Documento encolado para el procesamiento OCR.
 *
 * @param idDocumento   identificador del documento en el módulo
 * @param nombreArchivo nombre original del archivo
 * @param ruta          ubicación física del archivo en el almacenamiento
 * @param tipo          clasificación del documento
 */
public record SolicitudOcr(long idDocumento, String nombreArchivo, Path ruta, TipoDocumento tipo) {
}
