package com.migal.smartops.documentosocr.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Parámetros del almacenamiento de documentos. El directorio se resuelve respecto de la carpeta de ejecución y no se
 * versiona. El tamaño máximo corresponde al límite de 10 MB del contrato (DocumentoCargaSolicitud).
 *
 * @param directorio        carpeta donde se guardan los archivos cargados
 * @param tamanoMaximoBytes tamaño máximo admitido de un archivo
 */
@ConfigurationProperties(prefix = "migal.documentos")
public record PropiedadesDocumentos(String directorio, Long tamanoMaximoBytes) {

	/** Límite de 10 MB del contrato. */
	public static final long TAMANO_MAXIMO_POR_DEFECTO = 10L * 1024 * 1024;

	public PropiedadesDocumentos {
		directorio = directorio == null || directorio.isBlank() ? "almacen-documentos" : directorio.trim();
		tamanoMaximoBytes = tamanoMaximoBytes == null ? TAMANO_MAXIMO_POR_DEFECTO : tamanoMaximoBytes;
		if (tamanoMaximoBytes <= 0) {
			throw new IllegalStateException("migal.documentos.tamano-maximo-bytes debe ser mayor que cero");
		}
	}
}
