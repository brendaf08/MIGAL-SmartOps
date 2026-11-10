package com.migal.smartops.catalogo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.migal.smartops.catalogo.model.Almacen;
import com.migal.smartops.catalogo.repository.AlmacenRepository;
import com.migal.smartops.catalogo.service.AlmacenService;
import com.migal.smartops.comun.errores.CodigoError;
import com.migal.smartops.comun.errores.ErrorDeNegocio;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

/** Verifica la consulta y la activación de las ubicaciones sin acceso a la base de datos. */
class AlmacenServiceTest {

	private final AlmacenRepository almacenes = mock(AlmacenRepository.class);
	private final AlmacenService servicio = new AlmacenService(almacenes);

	private Almacen almacen(long id) {
		Almacen almacen = new Almacen("Tienda principal", "tienda", null);
		ReflectionTestUtils.setField(almacen, "id", id);
		when(almacenes.findById(id)).thenReturn(Optional.of(almacen));
		when(almacenes.save(any(Almacen.class))).thenAnswer(i -> i.getArgument(0));
		return almacen;
	}

	@Test
	void listaTodasLasUbicacionesEnElOrdenDeSuIdentificador() {
		when(almacenes.findAll(Sort.by("id")))
				.thenReturn(List.of(new Almacen("Tienda principal", "tienda", null), new Almacen("Almacen 1", "almacen", null)));

		assertEquals(2, servicio.listar().size());
	}

	@Test
	void desactivaUnaUbicacionSinEliminarla() {
		Almacen almacen = almacen(1L);

		Almacen resultado = servicio.cambiarEstado(1L, false);

		assertFalse(resultado.isEstadoActivo());
		assertFalse(almacen.isEstadoActivo());
	}

	@Test
	void activaUnaUbicacionDesactivada() {
		Almacen almacen = almacen(2L);
		almacen.cambiarEstado(false);

		assertTrue(servicio.cambiarEstado(2L, true).isEstadoActivo());
	}

	@Test
	void cambiarEstadoDeUnAlmacenInexistenteResponde404() {
		when(almacenes.findById(99L)).thenReturn(Optional.empty());

		assertEquals(CodigoError.RECURSO_NO_ENCONTRADO,
				assertThrows(ErrorDeNegocio.class, () -> servicio.cambiarEstado(99L, false)).codigo());
	}
}
