package com.migal.smartops.documentosocr;

import com.migal.smartops.seguridad.config.UsuarioAutenticado;
import com.migal.smartops.seguridad.model.RolUsuario;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Datos de prueba del módulo de documentos OCR: usuarios con cada rol y contenidos de archivo con firma válida. Los
 * usuarios se identifican con el prefijo {@code ocr-} y se eliminan con {@link #limpiar()}, junto con sus documentos,
 * líneas y coincidencias, en el orden que exigen las claves foráneas.
 */
public final class DatosOcr {

	/** Firma mínima de un PNG válido. */
	public static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
	/** Firma mínima de un JPG válido. */
	public static final byte[] JPG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0};
	/** Firma mínima de un PDF válido. */
	public static final byte[] PDF = "%PDF-1.7\n".getBytes(StandardCharsets.US_ASCII);

	private final JdbcTemplate jdbc;

	public DatosOcr(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	/** Crea un usuario activo con el rol indicado y devuelve su identidad autenticada. */
	public UsuarioAutenticado usuario(RolUsuario rol) {
		String username = "ocr-" + rol.valor() + "-" + UUID.randomUUID().toString().substring(0, 8);
		long id = jdbc.queryForObject("""
				INSERT INTO usuario (nombres, apellidos, username, password_hash, rol)
				VALUES ('Prueba', 'OCR', ?, '{hash}', ?) RETURNING id_usuario
				""", Long.class, username, rol.valor());
		return new UsuarioAutenticado(id, username, rol);
	}

	/** Contenido de tamaño indicado que comienza con la firma de un PDF. */
	public static byte[] pdfDeTamano(int tamano) {
		byte[] contenido = new byte[tamano];
		System.arraycopy(PDF, 0, contenido, 0, Math.min(PDF.length, tamano));
		return contenido;
	}

	/** Elimina los datos de los usuarios de prueba y todo lo que depende de ellos. */
	public void limpiar() {
		String documentosDePrueba = "SELECT d.id_documento FROM documento_digital d JOIN usuario u "
				+ "ON u.id_usuario = d.id_usuario WHERE u.username LIKE 'ocr-%'";
		String lineasDePrueba = "SELECT l.id_detalle_extraido FROM detalle_extraido l WHERE l.id_documento IN ("
				+ documentosDePrueba + ")";
		jdbc.update("DELETE FROM coincidencia_detalle WHERE id_detalle_extraido IN (" + lineasDePrueba + ")");
		jdbc.update("DELETE FROM detalle_extraido WHERE id_documento IN (" + documentosDePrueba + ")");
		jdbc.update("DELETE FROM documento_digital WHERE id_usuario IN (SELECT id_usuario FROM usuario "
				+ "WHERE username LIKE 'ocr-%')");
		jdbc.update("DELETE FROM usuario WHERE username LIKE 'ocr-%'");
	}
}
