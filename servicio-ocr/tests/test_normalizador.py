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

    assert normalizado.texto == "cemento king kong 2 tuberia galvanizado"


def test_f_grado_se_lee_como_fierro() -> None:
    assert normalizar('F° corrug 1/2"').texto == "fierro corrugado 1/2in"


def test_la_misma_medida_escrita_distinto_es_igual() -> None:
    assert normalizar('fierro 1/2"').medidas == normalizar("fierro media pulgada").medidas


def test_texto_sin_medidas() -> None:
    normalizado = normalizar("Pegamento para mayólica")

    assert normalizado.medidas == frozenset()
    assert normalizado.texto == "pegamento mayolica"


def test_tipo_de_medida() -> None:
    assert tipo_de_medida("1/2in") == "in"
    assert tipo_de_medida("42.5kg") == "kg"
    assert tipo_de_medida("18huecos") == "huecos"


@pytest.mark.parametrize(
    ("texto", "esperado"),
    [
        ("ϕ 5/8", "fierro 5/8in"),
        ("Ø 8MM", "fierro 8mm"),
        ("∅ 1/2", "fierro 1/2in"),
        ("⌀ 3/4", "fierro 3/4in"),
        ("ϕ cor 1/2", "fierro corrugado 1/2in"),
    ],
)
def test_el_diametro_de_las_notas_se_lee_como_fierro(texto: str, esperado: str) -> None:
    assert normalizar(texto).texto == esperado


@pytest.mark.parametrize(
    ("texto", "esperado"),
    [
        ('20 varillas F° 1/2"', "fierro 1/2in"),
        ("10 bls cem sol", "cemento sol"),
        ("mtrs arena gruesa", "arena gruesa"),
        ("Kg alambre 16", "alambre 16"),
    ],
)
def test_descarta_la_cantidad_y_la_unidad_de_venta(texto: str, esperado: str) -> None:
    assert normalizar(texto).texto == esperado


def test_una_medida_inicial_no_se_confunde_con_la_cantidad() -> None:
    assert "42.5kg" in normalizar("42.5 kg cemento").medidas
    assert "3in" in normalizar("3 pulgadas clavo").medidas


def test_compara_en_singular() -> None:
    assert normalizar("uniones CPVC").texto == "union cpvc"
    assert normalizar("codos").texto == normalizar("codo").texto
    assert normalizar("caja octagonales").texto == normalizar("Caja octogonal").texto


def test_el_largo_de_la_pieza_es_medida_pero_no_texto() -> None:
    normalizado = normalizar("Fierro corrugado 1/2 pulgada x 9 m")

    assert normalizado.texto == "fierro corrugado 1/2in"
    assert normalizado.medidas == frozenset({"1/2in", "9m"})


def test_el_formato_se_escribe_igual_con_o_sin_espacios() -> None:
    assert normalizar("Ladrillo de techo 15 x 30 cm").texto == normalizar("ladrillo techo 15x30").texto


def test_kk_seguido_del_numero_es_el_ladrillo_de_huecos() -> None:
    assert normalizar("LADRILLO KK18").texto == normalizar("lad kk 18 huecos").texto
    assert "18huecos" in normalizar("Ladrillo KK-18").medidas


@pytest.mark.parametrize(
    ("texto", "medida"),
    [
        ("Pegamento PVC 1/4 galón", "1/4gal"),
        ("galón pegamento 1/4", "1/4gal"),
        ("Pintura látex 4 L", "4l"),
        ("Thinner 500 ml", "500ml"),
        ("Martillo de uña 16 oz", "16oz"),
        ("Taladro percutor 650 W", "650w"),
    ],
)
def test_reconoce_volumen_peso_y_potencia(texto: str, medida: str) -> None:
    assert medida in normalizar(texto).medidas


def test_un_galon_no_se_confunde_con_pulgadas() -> None:
    assert "1/4in" not in normalizar("Pegamento PVC 1/4 galón").medidas


def test_el_envase_tras_la_cantidad_no_es_el_tipo_de_producto() -> None:
    assert normalizar('2 cajas clavo 3"').texto.startswith("clavo")
    assert normalizar("caja octagonales").texto == "caja octogonal"


def test_plurales_en_ces_y_palabras_que_no_son_plurales() -> None:
    assert normalizar("luces led").texto == "luz led"
    assert normalizar("arnés de seguridad").texto == "arnes seguridad"


def test_el_simbolo_de_diametro_leido_como_letras_designa_el_fierro():
    for texto in ("fi 1/4", "phi 1/4", "Ø 1/4"):
        assert normalizar(texto).texto == "fierro 1/4in"
