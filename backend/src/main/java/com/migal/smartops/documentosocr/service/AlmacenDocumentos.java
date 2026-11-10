package com.migal.smartops.documentosocr.service;

import com.migal.smartops.comun.errores.CodigoError;
import com.migal.smartops.comun.errores.ErrorDeNegocio;
import com.migal.smartops.documentosocr.config.PropiedadesDocumentos;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Almacenamiento de los archivos cargados en el sistema de archivos local. Cada archivo recibe un nombre aleatorio, de
 * modo que el nombre original del cliente nunca forma parte de la ruta. La base de datos conserva solo la ruta relativa.
 */
@Component
public class AlmacenDocumentos {

	private static final Logger REGISTRO = LoggerFactory.getLogger(AlmacenDocumentos.class);

	private final Path raiz;

	public AlmacenDocumentos(PropiedadesDocumentos propiedades) {
		this.raiz = Path.of(propiedades.directorio()).toAbsolutePath().normalize();
	}

	/** Guarda el contenido y devuelve la ruta relativa al directorio raíz. Un fallo de escritura responde SERVICIO_NO_DISPONIBLE. */
	public String guardar(byte[] contenido, String extension) {
		String relativa = UUID.randomUUID() + "." + extension;
		try {
			Files.createDirectories(raiz);
			Files.write(raiz.resolve(relativa), contenido);
			return relativa;
		} catch (IOException e) {
			throw new ErrorDeNegocio(CodigoError.SERVICIO_NO_DISPONIBLE,
					"No fue posible almacenar el archivo. Intente nuevamente.");
		}
	}

	/** Ruta absoluta de un archivo almacenado. */
	public Path resolver(String relativa) {
		return raiz.resolve(relativa);
	}

	/** Elimina un archivo almacenado. Un fallo se registra sin interrumpir la operación que lo solicitó. */
	public void eliminar(String relativa) {
		try {
			Files.deleteIfExists(raiz.resolve(relativa));
		} catch (IOException e) {
			REGISTRO.warn("No fue posible eliminar el archivo {} del almacenamiento", relativa, e);
		}
	}
}
