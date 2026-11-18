package com.migal.smartops.comun.web;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Verifica que el patrón de búsqueda escape los comodines de LIKE del texto del usuario. */
class BusquedaTest {

	@Test
	void escapaLosComodinesYPasaAMinusculas() {
		assertEquals("%cemento%", Busqueda.patron("  Cemento "));
		assertEquals("%50\\%%", Busqueda.patron("50%"));
		assertEquals("%a\\_b%", Busqueda.patron("A_b"));
		assertEquals("%c:\\\\x%", Busqueda.patron("C:\\x"));
	}
}
