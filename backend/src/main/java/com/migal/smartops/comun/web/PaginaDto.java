package com.migal.smartops.comun.web;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/**
 * Sobre de paginación del contrato (esquema común {@code Pagina}): los elementos, el total y la página
 * (en base 1) con su tamaño.
 */
public record PaginaDto<T>(List<T> items, long total, int page, int pageSize) {

	public static <E, T> PaginaDto<T> de(Page<E> pagina, Function<E, T> conversion) {
		return new PaginaDto<>(pagina.getContent().stream().map(conversion).toList(), pagina.getTotalElements(),
				pagina.getNumber() + 1, pagina.getSize());
	}
}
