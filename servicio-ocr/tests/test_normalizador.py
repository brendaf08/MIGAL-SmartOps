import pytest

from app.normalizador import normalizar, tipo_de_medida


@pytest.mark.parametrize(
    ("texto", "medida"),
    [
        ('fierro 1/2"', "1/2in"),
        ("F° 3/8 pulg", "3/8in"),
        ("fierro de media pulgada", "1/2in"),
        ("fierro 1/2", "1/2in"),
        ("cemento 42,5 kgs", "42.5kg"),
        ("cemento 42.50 kilos", "42.5kg"),
        ("ladrillo 18 huecos", "18huecos"),
        ("cable 14 AWG", "14awg"),
        ("cable n° 12 awg", "12awg"),
        ("clavo 3 pulgadas", "3in"),
        ("disco 115 mm", "115mm"),
    ],
)
def test_detecta_medidas_en_forma_canonica(texto: str, medida: str) -> None:
    assert medida in normalizar(texto).medidas


def test_expande_la_jerga_y_quita_tildes() -> None:
    normalizado = normalizar("Cem. KK 2 bls Tubería galv")

    assert normalizado.texto == "cemento king kong 2 bolsa tuberia galvanizado"


def test_f_grado_se_lee_como_fierro() -> None:
    assert normalizar('F° corrug 1/2"').texto == "fierro corrugado 1/2in"


def test_la_misma_medida_escrita_distinto_es_igual() -> None:
    assert normalizar('fierro 1/2"').medidas == normalizar("fierro media pulgada").medidas


def test_texto_sin_medidas() -> None:
    normalizado = normalizar("Pegamento para mayólica")

    assert normalizado.medidas == frozenset()
    assert normalizado.texto == "pegamento para mayolica"


def test_tipo_de_medida() -> None:
    assert tipo_de_medida("1/2in") == "in"
    assert tipo_de_medida("42.5kg") == "kg"
    assert tipo_de_medida("18huecos") == "huecos"
