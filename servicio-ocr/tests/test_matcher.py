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


NOTAS = preparar_catalogo(
    [
        ProductoCatalogo(sku=sku, nombreOficial=nombre, marca=None, presentacion=None, unidadMedida="unidad")
        for sku, nombre in [
            ("FIE-002", "Fierro corrugado 1/2 pulgada x 9 m"),
            ("SAN-009", "Tubo PVC agua 1/2 pulgada x 5 m"),
            ("SAN-019", "Tubo CPVC agua caliente 1/2 pulgada x 5 m"),
            ("SAN-021", "Tee CPVC 1/2 pulgada"),
            ("SAN-023", "Unión CPVC 1/2 pulgada"),
            ("AGR-001", "Arena gruesa"),
        ]
    ]
)


def test_lineas_de_notas_de_venta_reales() -> None:
    for texto, sku in [
        ("ϕ 1/2", "FIE-002"),
        ('20 varillas F° 1/2"', "FIE-002"),
        ("tubo CPVC 1/2", "SAN-019"),
        ('tubo agua 1/2"', "SAN-009"),
        ("uniones CPVC", "SAN-023"),
        ("mtrs arena gruesa", "AGR-001"),
    ]:
        resultado = coincidencias(texto, NOTAS, 0.6)
        assert resultado, texto
        assert resultado[0].sku == sku, texto


def test_el_tipo_de_producto_pesa_mas_que_los_atributos() -> None:
    linea = normalizar("tubo cpvc 1/2")

    assert puntaje(linea, NOTAS[2].normalizado) > puntaje(linea, NOTAS[4].normalizado)


def test_un_material_distinto_penaliza_el_puntaje() -> None:
    linea = normalizar("tubo cpvc 1/2")

    assert puntaje(linea, NOTAS[1].normalizado) < 0.4


def test_una_medida_de_volumen_distinta_penaliza_el_puntaje() -> None:
    galon = preparar_catalogo(
        [
            ProductoCatalogo(
                sku="P-4", nombreOficial="Pintura látex 4 L", marca=None, presentacion=None, unidadMedida="u"
            ),
            ProductoCatalogo(
                sku="P-1", nombreOficial="Pintura látex 1 L", marca=None, presentacion=None, unidadMedida="u"
            ),
        ]
    )

    assert coincidencias("pintura latex 1 L", galon, 0.0)[0].sku == "P-1"


def test_el_prefiltro_conserva_el_mejor_candidato_en_catalogos_grandes() -> None:
    relleno = [
        ProductoCatalogo(
            sku=f"R-{i}", nombreOficial=f"Producto variado {i}", marca=None, presentacion=None, unidadMedida="u"
        )
        for i in range(200)
    ]
    grande = preparar_catalogo([*relleno, *CATALOGO])

    assert coincidencias("lad kk 18 huecos", grande, 0.6)[0].sku == "LAD-018"


def test_los_plurales_se_emparejan_con_el_producto_en_singular() -> None:
    assert coincidencias('3 fierros corrugados 1/2"', CANDIDATOS, 0.6)[0].sku == "FIE-012"
    assert coincidencias("5 ladrillos king kong 18 huecos", CANDIDATOS, 0.6)[0].sku == "LAD-018"


def test_una_letra_mal_escrita_se_corrige_con_el_catalogo() -> None:
    # Falta una letra, sobra una, letra cambiada e intercambio de dos letras.
    assert coincidencias('fierro corugado 1/2"', CANDIDATOS, 0.6)[0].sku == "FIE-012"
    assert coincidencias("cemmento sol", CANDIDATOS, 0.6)[0].sku == "CEM-001"
    assert coincidencias("cenento sol", CANDIDATOS, 0.6)[0].sku == "CEM-001"
    assert coincidencias("cemetno sol", CANDIDATOS, 0.6)[0].sku == "CEM-001"


def test_una_palabra_corregida_tiene_menos_confianza_que_la_escrita_bien() -> None:
    bien = coincidencias("cemento sol tipo I", CANDIDATOS, 0.6)[0].confianza
    mal = coincidencias("cemnto sol tipo I", CANDIDATOS, 0.6)[0].confianza
    assert 0.6 <= mal < bien


def test_no_corrige_palabras_cortas_ni_inventa_coincidencias() -> None:
    assert coincidencias("xyz", CANDIDATOS, 0.6) == []
    # Una palabra a más de una letra de distancia no se corrige.
    assert coincidencias("cmnto", CANDIDATOS, 0.6) == []
