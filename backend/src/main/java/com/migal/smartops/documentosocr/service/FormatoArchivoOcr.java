package com.migal.smartops.documentosocr.service;

import java.util.Optional;

/**
 * Detección del formato de un archivo por su firma inicial (magic bytes). Solo se admiten JPG, PNG y PDF (contrato,
 * POST /documentos). La extensión declarada por el cliente no se considera, porque no garantiza el contenido.
 */
public final class FormatoArchivoOcr {

	private static final byte[] JPG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
	private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
	private static final byte[] PDF = {'%', 'P', 'D', 'F', '-'};

	private FormatoArchivoOcr() {
	}

	/** Extensión del formato detectado, o vacío cuando el formato no es admitido. */
	public static Optional<String> extensionDe(byte[] contenido) {
		if (empiezaCon(contenido, JPG)) {
			return Optional.of("jpg");
		}
		if (empiezaCon(contenido, PNG)) {
			return Optional.of("png");
		}
		if (empiezaCon(contenido, PDF)) {
			return Optional.of("pdf");
		}
		return Optional.empty();
	}

	private static boolean empiezaCon(byte[] contenido, byte[] firma) {
		if (contenido == null || contenido.length < firma.length) {
			return false;
		}
		for (int i = 0; i < firma.length; i++) {
			if (contenido[i] != firma[i]) {
				return false;
			}
		}
		return true;
	}
}
