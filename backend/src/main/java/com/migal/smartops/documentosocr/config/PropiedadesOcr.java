package com.migal.smartops.documentosocr.config;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Parámetros del procesamiento OCR. El proveedor selecciona la implementación de {@code ProveedorOcr}: {@code simulado}
 * (por defecto, para desarrollo y pruebas) o {@code no-configurado}, que responde SERVICIO_NO_DISPONIBLE. Los umbrales
 * del Fuzzy Matching son de ejemplo y quedan pendientes de definir en el servicio de OCR (contrato, DocumentoUmbrales).
 *
 * @param proveedor implementación activa del proveedor OCR
 * @param umbrales  umbrales de confianza de las coincidencias
 */
@ConfigurationProperties(prefix = "migal.ocr")
public record PropiedadesOcr(String proveedor, Umbrales umbrales) {

	/** Valor del proveedor por defecto. */
	public static final String PROVEEDOR_SIMULADO = "simulado";
	/** Valor del proveedor cuando no hay un servicio de OCR configurado. */
	public static final String PROVEEDOR_NO_CONFIGURADO = "no-configurado";

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
	}
}
