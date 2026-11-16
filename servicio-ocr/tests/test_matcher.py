from app.esquemas import ProductoCatalogo
from app.matcher import coincidencias, medidas_incompatibles, preparar_catalogo, puntaje
from app.normalizador import normalizar

CATALOGO = [
    ProductoCatalogo(
        sku="CEM-001",
        nombreOficial="Cemento Sol tipo I",
        marca="Sol",
        presentacion="Bolsa 42.5 kg",
        unidadMedida="bolsa",
    ),
    ProductoCatalogo(
        sku="FIE-012",
        nombreOficial='Fierro corrugado 1/2"',
        marca=None,
        presentacion="Varilla 9 m",
        unidadMedida="varilla",
    ),
    ProductoCatalogo(
        sku="FIE-038",
        nombreOficial='Fierro corrugado 3/8"',
        marca=None,
        presentacion="Varilla 9 m",
        unidadMedida="varilla",
    ),
    ProductoCatalogo(
        sku="LAD-018",
        nombreOficial="Ladrillo King Kong 18 huecos",
        marca="Lark",
        presentacion=None,
        unidadMedida="millar",
    ),
    ProductoCatalogo(
        sku="TUB-PVC", nombreOficial='Tubo PVC desagüe 2"', marca="Pavco", presentacion="3 m", unidadMedida="unidad"
    ),
]
CANDIDATOS = preparar_catalogo(CATALOGO)


def test_la_medida_decide_entre_productos_casi_iguales() -> None:
    resultado = coincidencias('F° corrug 1/2"', CANDIDATOS, 0.6)

    assert resultado[0].sku == "FIE-012"
    assert all(c.sku != "FIE-038" for c in resultado)


def test_una_medida_incompatible_penaliza_el_puntaje() -> None:
    linea = normalizar('fierro corrugado 3/8"')

    assert puntaje(linea, CANDIDATOS[2].normalizado) > 0.85
    assert puntaje(linea, CANDIDATOS[1].normalizado) < 0.4


def test_jerga_de_mostrador() -> None:
    assert coincidencias("lad kk 18 huecos", CANDIDATOS, 0.6)[0].sku == "LAD-018"
    assert coincidencias("cem sol 42.5kg", CANDIDATOS, 0.6)[0].sku == "CEM-001"


def test_un_texto_generico_no_alcanza_la_confianza_maxima() -> None:
    resultado = coincidencias("cemento", CANDIDATOS, 0.0)

    assert resultado[0].sku == "CEM-001"
    assert resultado[0].confianza < 0.85


def test_filtra_por_umbral_ordena_y_limita_a_tres() -> None:
    resultado = coincidencias("fierro corrugado", CANDIDATOS, 0.0)

    assert len(resultado) == 3
    assert [c.confianza for c in resultado] == sorted((c.confianza for c in resultado), reverse=True)
    assert coincidencias("pintura látex blanca", CANDIDATOS, 0.6) == []


def test_las_confianzas_estan_entre_cero_y_uno_con_tres_decimales() -> None:
    for coincidencia in coincidencias('tubo pvc 2"', CANDIDATOS, 0.0):
        assert 0 <= coincidencia.confianza <= 1
        assert round(coincidencia.confianza, 3) == coincidencia.confianza


def test_medidas_incompatibles_solo_compara_el_mismo_tipo() -> None:
    assert medidas_incompatibles(frozenset({"1/2in"}), frozenset({"3/8in"}))
    assert not medidas_incompatibles(frozenset({"1/2in"}), frozenset({"1/2in", "9m"}))
    assert not medidas_incompatibles(frozenset({"42.5kg"}), frozenset({"1/2in"}))
    assert not medidas_incompatibles(frozenset({"1/2in"}), frozenset())
