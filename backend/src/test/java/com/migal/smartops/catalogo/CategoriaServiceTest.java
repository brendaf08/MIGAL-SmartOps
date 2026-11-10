package com.migal.smartops.catalogo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.migal.smartops.catalogo.model.Categoria;
import com.migal.smartops.catalogo.repository.CategoriaRepository;
import com.migal.smartops.catalogo.service.CategoriaService;
import com.migal.smartops.comun.errores.CodigoError;
import com.migal.smartops.comun.errores.ErrorDeNegocio;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

/** Verifica la consulta de categorías y la validación de su existencia sin acceso a la base de datos. */
class CategoriaServiceTest {

	private final CategoriaRepository categorias = mock(CategoriaRepository.class);
	private final CategoriaService servicio = new CategoriaService(categorias);

	@Test
	void listaTodasLasCategoriasEnElOrdenDeSuIdentificador() {
		when(categorias.findAll(Sort.by("id"))).thenReturn(List.of(new Categoria("Herramientas", null, false)));

		assertEquals(1, servicio.listar().size());
	}

	@Test
	void devuelveLaCategoriaCuandoExiste() {
		Categoria cemento = new Categoria("Cemento y aglomerantes", null, true);
		ReflectionTestUtils.setField(cemento, "id", 3L);
		when(categorias.findById(3L)).thenReturn(Optional.of(cemento));

		assertEquals("Cemento y aglomerantes", servicio.validarExistencia(3L).getNombre());
	}

	@Test
	void rechazaUnaCategoriaInexistente() {
		when(categorias.findById(99L)).thenReturn(Optional.empty());

		assertEquals(CodigoError.CATEGORIA_NO_ENCONTRADA,
				assertThrows(ErrorDeNegocio.class, () -> servicio.validarExistencia(99L)).codigo());
	}
}
