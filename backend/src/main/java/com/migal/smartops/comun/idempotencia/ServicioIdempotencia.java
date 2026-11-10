package com.migal.smartops.comun.idempotencia;

import com.migal.smartops.comun.errores.CodigoError;
import com.migal.smartops.comun.errores.ErrorDeNegocio;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

/**
 * Ejecuta una operación una sola vez por {@code Idempotency-Key} (ADR-009 y ADR-010). La clave se reserva en la
 * tabla {@code idempotencia} en una transacción propia, de modo que una solicitud concurrente o repetida la vea de
 * inmediato:
 *
 * <ul>
 *   <li>Clave nueva: se ejecuta la acción y se guarda su resultado.
 *   <li>Clave repetida con la misma huella de solicitud: se devuelve la respuesta guardada sin ejecutar la acción.
 *   <li>Clave repetida con otra huella: {@code IDEMPOTENCIA_CONFLICTO}.
 *   <li>Clave reservada cuya operación aún no termina: {@code PETICION_CONCURRENTE}.
 *   <li>Acción que falla: se libera la clave para que la solicitud pueda reintentarse.
 * </ul>
 *
 * <p>La acción se ejecuta con la transacción del llamante: debe ser un método transaccional de un servicio de
 * negocio, de modo que sus cambios se confirmen antes de guardar el resultado.
 */
@Service
public class ServicioIdempotencia {

	/** Vigencia de un registro de idempotencia. */
	public static final Duration VIGENCIA = Duration.ofHours(24);

	private static final int LONGITUD_MAXIMA_CLAVE = 100;

	/**
	 * Resultado de una operación idempotente.
	 *
	 * @param estado   estado HTTP de la respuesta
	 * @param cuerpo   cuerpo de la respuesta
	 * @param repetida {@code true} cuando la respuesta se tomó de una ejecución anterior
	 */
	public record Resultado<T>(int estado, T cuerpo, boolean repetida) {
	}

	private final JdbcClient jdbc;
	private final TransactionTemplate transaccionNueva;
	private final ObjectMapper mapeador;
	private final Clock reloj;

	public ServicioIdempotencia(JdbcClient jdbc, PlatformTransactionManager transacciones, ObjectMapper mapeador,
			Clock reloj) {
		this.jdbc = jdbc;
		this.transaccionNueva = new TransactionTemplate(transacciones);
		this.transaccionNueva.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		this.mapeador = mapeador;
		this.reloj = reloj;
	}

	/**
	 * Ejecuta la acción de forma idempotente.
	 *
	 * @param idUsuario   usuario autenticado
	 * @param operacion   operación invocada, por ejemplo {@code POST /inventario/traslados}
	 * @param clave       valor de la cabecera {@code Idempotency-Key}
	 * @param solicitud   cuerpo de la solicitud, para calcular su huella
	 * @param estadoExito estado HTTP de una ejecución correcta
	 * @param tipo        tipo del cuerpo de la respuesta
	 * @param accion      operación de negocio
	 */
	public <T> Resultado<T> ejecutar(long idUsuario, String operacion, String clave, Object solicitud, int estadoExito,
			Class<T> tipo, Supplier<T> accion) {
		validarClave(clave);
		String huella = huella(operacion, solicitud);

		Long reserva = reservar(idUsuario, operacion, clave, huella);
		if (reserva == null) {
			return repetir(idUsuario, operacion, clave, huella, tipo);
		}
		T cuerpo;
		try {
			cuerpo = accion.get();
		} catch (RuntimeException e) {
			liberar(reserva);
			throw e;
		}
		guardar(reserva, estadoExito, cuerpo);
		return new Resultado<>(estadoExito, cuerpo, false);
	}

	private static void validarClave(String clave) {
		if (clave == null || clave.isBlank() || clave.length() > LONGITUD_MAXIMA_CLAVE) {
			throw new ErrorDeNegocio(CodigoError.SOLICITUD_MAL_FORMADA,
					"La cabecera Idempotency-Key es obligatoria y admite hasta 100 caracteres.");
		}
	}

	/** Reserva la clave. Devuelve el identificador del registro, o {@code null} si la clave ya existe. */
	private Long reservar(long idUsuario, String operacion, String clave, String huella) {
		Instant ahora = reloj.instant();
		return transaccionNueva.execute(estado -> {
			// Un registro vencido no cuenta: se elimina para permitir reutilizar la clave.
			jdbc.sql("""
					DELETE FROM idempotencia
					 WHERE id_usuario = :usuario AND operacion = :operacion AND clave = :clave
					   AND fecha_expiracion < :ahora
					""")
					.param("usuario", idUsuario).param("operacion", operacion).param("clave", clave)
					.param("ahora", Timestamp.from(ahora)).update();
			return jdbc.sql("""
					INSERT INTO idempotencia (clave, id_usuario, operacion, huella, fecha_creacion, fecha_expiracion)
					VALUES (:clave, :usuario, :operacion, :huella, :ahora, :expira)
					ON CONFLICT (id_usuario, operacion, clave) DO NOTHING
					RETURNING id_idempotencia
					""")
					.param("clave", clave).param("usuario", idUsuario).param("operacion", operacion)
					.param("huella", huella).param("ahora", Timestamp.from(ahora))
					.param("expira", Timestamp.from(ahora.plus(VIGENCIA)))
					.query(Long.class).optional().orElse(null);
		});
	}

	private <T> Resultado<T> repetir(long idUsuario, String operacion, String clave, String huella, Class<T> tipo) {
		List<Map<String, Object>> filas = jdbc.sql("""
				SELECT huella, estado_http, respuesta::text AS respuesta
				  FROM idempotencia
				 WHERE id_usuario = :usuario AND operacion = :operacion AND clave = :clave
				""")
				.param("usuario", idUsuario).param("operacion", operacion).param("clave", clave)
				.query().listOfRows();
		if (filas.isEmpty()) {
			// La operación original falló y liberó la clave entre ambas consultas.
			throw new ErrorDeNegocio(CodigoError.PETICION_CONCURRENTE,
					"La operación se está procesando. Intente nuevamente.");
		}
		Map<String, Object> registro = filas.get(0);
		if (!huella.equals(String.valueOf(registro.get("huella")).trim())) {
			throw new ErrorDeNegocio(CodigoError.IDEMPOTENCIA_CONFLICTO,
					"La Idempotency-Key ya se utilizó con una solicitud distinta.");
		}
		if (registro.get("estado_http") == null) {
			throw new ErrorDeNegocio(CodigoError.PETICION_CONCURRENTE,
					"La operación se está procesando. Intente nuevamente.");
		}
		int estado = ((Number) registro.get("estado_http")).intValue();
		T cuerpo = mapeador.readValue(String.valueOf(registro.get("respuesta")), tipo);
		return new Resultado<>(estado, cuerpo, true);
	}

	private void guardar(long reserva, int estado, Object cuerpo) {
		String json = mapeador.writeValueAsString(cuerpo);
		transaccionNueva.executeWithoutResult(t -> jdbc.sql("""
				UPDATE idempotencia SET estado_http = :estado, respuesta = CAST(:respuesta AS jsonb)
				 WHERE id_idempotencia = :id
				""").param("estado", estado).param("respuesta", json).param("id", reserva).update());
	}

	private void liberar(long reserva) {
		transaccionNueva.executeWithoutResult(t ->
				jdbc.sql("DELETE FROM idempotencia WHERE id_idempotencia = :id").param("id", reserva).update());
	}

	/** Elimina los registros vencidos. Se invoca de forma periódica. */
	public int eliminarVencidos() {
		return jdbc.sql("DELETE FROM idempotencia WHERE fecha_expiracion < :ahora")
				.param("ahora", Timestamp.from(reloj.instant())).update();
	}

	private String huella(String operacion, Object solicitud) {
		String contenido = operacion + "\n" + (solicitud == null ? "" : mapeador.writeValueAsString(solicitud));
		try {
			byte[] resumen = MessageDigest.getInstance("SHA-256").digest(contenido.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(resumen);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 no está disponible", e);
		}
	}
}
