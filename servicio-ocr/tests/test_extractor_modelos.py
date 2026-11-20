"""Cambio automático de modelo de Gemini cuando uno agota su cuota."""

from types import SimpleNamespace

import pytest

from app.extractor import ModeloGemini

JSON_OK = '{"legible": true, "lineas": [{"textoCrudo": "CEMENTO", "cantidadLeida": 2}]}'


class ErrorApi(Exception):
    def __init__(self, code: int) -> None:
        super().__init__(f"error {code}")
        self.code = code


class ClienteFalso:
    """Registra los modelos consultados y falla con el código indicado para los agotados."""

    def __init__(self, fallos: dict[str, int]) -> None:
        self.llamados: list[str] = []
        self._fallos = fallos
        self.models = SimpleNamespace(generate_content=self._generar)

    def _generar(self, model, contents, config):
        self.llamados.append(model)
        if model in self._fallos:
            raise ErrorApi(self._fallos[model])
        return SimpleNamespace(text=JSON_OK)


def crear(modelos: str, fallos: dict[str, int], reloj) -> tuple[ModeloGemini, ClienteFalso]:
    modelo = ModeloGemini("clave-ficticia", modelos, 5.0, espera_agotado=300.0, reloj=reloj)
    cliente = ClienteFalso(fallos)
    modelo._cliente = cliente
    return modelo, cliente


def test_con_la_cuota_agotada_usa_el_siguiente_modelo_y_omite_el_agotado_despues():
    ahora = [0.0]
    modelo, cliente = crear("a-lite, b-lite", {"a-lite": 429}, lambda: ahora[0])

    assert modelo.extraer(b"x", "image/png").lineas[0].cantidadLeida == 2
    modelo.extraer(b"x", "image/png")

    assert cliente.llamados == ["a-lite", "b-lite", "b-lite"]


def test_el_modelo_agotado_se_vuelve_a_probar_tras_la_espera():
    ahora = [0.0]
    modelo, cliente = crear("a-lite,b-lite", {"a-lite": 429}, lambda: ahora[0])
    modelo.extraer(b"x", "image/png")

    ahora[0] = 301.0
    modelo.extraer(b"x", "image/png")

    assert cliente.llamados == ["a-lite", "b-lite", "a-lite", "b-lite"]


def test_si_todos_los_modelos_fallan_propaga_el_ultimo_error():
    modelo, _ = crear("a-lite,b-lite", {"a-lite": 429, "b-lite": 429}, lambda: 0.0)

    with pytest.raises(ErrorApi):
        modelo.extraer(b"x", "image/png")


def test_un_error_de_solicitud_no_cambia_de_modelo():
    modelo, cliente = crear("a-lite,b-lite", {"a-lite": 400}, lambda: 0.0)

    with pytest.raises(ErrorApi):
        modelo.extraer(b"x", "image/png")
    assert cliente.llamados == ["a-lite"]
