package com.migal.smartops.documentosocr.config;

import java.math.BigDecimal;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

/**
 * Parámetros del procesamiento OCR. El proveedor selecciona la implementación de {@code ProveedorOcr}: {@code simulado}
 * (por defecto, para desarrollo y pruebas), {@code no-configurado}, que responde SERVICIO_NO_DISPONIBLE, o {@code http},
 * que envía los documentos al servicio de OCR (ADR-012). Los umbrales del Fuzzy Matching son de ejemplo y se calibran con
 * el conjunto de evaluación del servicio de OCR.
 *
 * @param proveedor implementación activa del proveedor OCR
 * @param umbrales  umbrales de confianza de las coincidencias
 * @param servicio  conexión con el servicio de OCR; solo se usa con el proveedor {@code http}
 */
@ConfigurationProperties(prefix = "migal.ocr")
public record PropiedadesOcr(String proveedor, Umbrales umbrales, Servicio servicio) {

	/** Valor del proveedor por defecto. */
	public static final String PROVEEDOR_SIMULADO = "simulado";
	/** Valor del proveedor cuando no hay un servicio de OCR configurado. */
	public static final String PROVEEDOR_NO_CONFIGURADO = "no-configurado";
	/** Valor del proveedor que envía los documentos al servicio de OCR por HTTP. */
	public static final String PROVEEDOR_HTTP = "http";

	/**
	 * Conexión con el servicio de OCR (ADR-012, puntos 3 y 4).
	 *
	 * @param url               dirección base del servicio, por ejemplo {@code http://localhost:8000}
	 * @param plazoResultado    tiempo máximo de espera del resultado antes de registrar SERVICIO_NO_DISPONIBLE
	 * @param intervaloVigilancia frecuencia con que se revisan los documentos pendientes
	 * @param tiempoConexion    tiempo máximo para conectar con el servicio
	 * @param tiempoLectura     tiempo máximo para recibir la respuesta del encolado
	 */
	public record Servicio(String url, Duration plazoResultado, Duration intervaloVigilancia, Duration tiempoConexion,
			Duration tiempoLectura, String tokenInterno) {

		/** Servicio sin token interno. */
		public Servicio(String url, Duration plazoResultado, Duration intervaloVigilancia, Duration tiempoConexion,
				Duration tiempoLectura) {
			this(url, plazoResultado, intervaloVigilancia, tiempoConexion, tiempoLectura, null);
		}

		@ConstructorBinding
		public Servicio {
			url = url == null ? "" : url.trim();
			tokenInterno = tokenInterno == null ? "" : tokenInterno.trim();
			plazoResultado = plazoResultado == null ? Duration.ofSeconds(60) : plazoResultado;
			intervaloVigilancia = intervaloVigilancia == null ? Duration.ofSeconds(30) : intervaloVigilancia;
			tiempoConexion = tiempoConexion == null ? Duration.ofSeconds(2) : tiempoConexion;
			tiempoLectura = tiempoLectura == null ? Duration.ofSeconds(5) : tiempoLectura;
			for (Duration duracion : new Duration[] {plazoResultado, intervaloVigilancia, tiempoConexion, tiempoLectura}) {
				if (duracion.isNegative() || duracion.isZero()) {
					throw new IllegalStateException("Las duraciones de migal.ocr.servicio deben ser positivas");
				}
			}
		}
	}

	/**
	 * Umbrales de confianza, de 0 a 1. Una coincidencia con confianza igual o superior a {@code alta} se propone sin
	 * advertencia; entre {@code baja} y {@code alta}, con revisión; por debajo de {@code baja}, no se propone.
	 */
	public record Umbrales(BigDecimal alta, BigDecimal baja) {

		public Umbrales {
			alta = alta == null ? new BigDecimal("0.85") : alta;
			baja = baja == null ? new BigDecimal("0.60") : baja;
			if (baja.signum() < 0 || alta.compareTo(BigDecimal.ONE) > 0 || baja.compareTo(alta) >= 0) {
				throw new IllegalStateException("Los umbrales OCR deben cumplir 0 <= baja < alta <= 1");
			}
		}
	}

	public PropiedadesOcr {
		proveedor = proveedor == null || proveedor.isBlank() ? PROVEEDOR_SIMULADO : proveedor.trim();
		umbrales = umbrales == null ? new Umbrales(null, null) : umbrales;
		servicio = servicio == null ? new Servicio(null, null, null, null, null, null) : servicio;
		if (PROVEEDOR_HTTP.equals(proveedor) && servicio.url().isEmpty()) {
			throw new IllegalStateException("El proveedor http exige migal.ocr.servicio.url");
		}
	}
}
