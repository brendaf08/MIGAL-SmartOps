import json

import httpx
import pytest
import respx
from fastapi.testclient import TestClient
from pydantic import ValidationError

from app import main
from app.extractor import Extraccion, LineaLeida

PNG = b"\x89PNG\r\n\x1a\n" + b"\x00" * 8
URL_RESULTADOS = "http://backend.local/api/v1/documentos/812/resultados"


class ErrorApi(Exception):
    """Error de la API con su código HTTP, como ``google.genai.errors.APIError``."""

    def __init__(self, code: int) -> None:
        super().__init__(f"Error {code}")
        self.code = code


class ModeloGrabado:
    """Respuesta grabada del modelo multimodal; las pruebas no llaman a la API real."""

    def __init__(
        self, extraccion: Extraccion | None = None, falla: bool = False, error: Exception | None = None
    ) -> None:
        self.extraccion = extraccion
        self.error = error or (TimeoutError("API no disponible") if falla else None)
        self.llamadas = 0

    def extraer(self, contenido: bytes, tipo_mime: str) -> Extraccion:
        self.llamadas += 1
        if self.error:
            raise self.error
        return self.extraccion


def solicitud(**cambios: object) -> str:
    datos = {
        "idDocumento": "812",
        "tipo": "solicitud_cliente",
        "nombreArchivo": "nota.png",
        "umbralBaja": 0.6,
        "tokenResultados": "token-812",
        "catalogo": [
            {
                "sku": "CEM-001",
                "nombreOficial": "Cemento Sol tipo I",
                "marca": "Sol",
                "presentacion": "Bolsa 42.5 kg",
                "unidadMedida": "bolsa",
            }
        ],
    }
    datos.update(cambios)
    return json.dumps(datos)


@pytest.fixture
def cliente(monkeypatch: pytest.MonkeyPatch) -> TestClient:
    monkeypatch.setenv("BACKEND_URL", "http://backend.local")
    main.configuracion.cache_clear()
    yield TestClient(main.app)
    main.configuracion.cache_clear()


def enviar(cliente: TestClient, datos: str, contenido: bytes = PNG, tipo: str = "image/png") -> httpx.Response:
    return cliente.post("/procesar", files={"archivo": ("3f2a.png", contenido, tipo)}, data={"solicitud": datos})


@respx.mock
def test_procesa_y_registra_el_resultado_con_el_token_recibido(
    cliente: TestClient, monkeypatch: pytest.MonkeyPatch
) -> None:
    modelo = ModeloGrabado(
        Extraccion(
            legible=True,
            lineas=[
                LineaLeida(textoCrudo="cem sol 42.5 kg", cantidadLeida=2),
                LineaLeida(textoCrudo="pintura azul", cantidadLeida=None),
            ],
        )
    )
    monkeypatch.setattr(main, "_modelo", modelo)
    ruta = respx.put(URL_RESULTADOS).mock(return_value=httpx.Response(200))

    respuesta = enviar(cliente, solicitud())

    assert respuesta.status_code == 202
    assert ruta.called
    peticion = ruta.calls.last.request
    assert peticion.headers["Authorization"] == "Bearer token-812"
    cuerpo = json.loads(peticion.content)
    assert cuerpo["resultado"] == "procesado"
    assert cuerpo["detalles"][0]["cantidadLeida"] == 2
    assert cuerpo["detalles"][0]["coincidencias"][0]["sku"] == "CEM-001"
    assert cuerpo["detalles"][1] == {"textoCrudo": "pintura azul", "cantidadLeida": None, "coincidencias": []}


@respx.mock
def test_un_documento_sin_lineas_se_registra_como_ilegible(
    cliente: TestClient, monkeypatch: pytest.MonkeyPatch
) -> None:
    monkeypatch.setattr(main, "_modelo", ModeloGrabado(Extraccion(legible=False, lineas=[])))
    ruta = respx.put(URL_RESULTADOS).mock(return_value=httpx.Response(200))

    enviar(cliente, solicitud())

    assert json.loads(ruta.calls.last.request.content) == {"resultado": "ilegible", "detalles": []}


@respx.mock
def test_un_fallo_del_modelo_se_reintenta_y_no_envia_resultado(
    cliente: TestClient, monkeypatch: pytest.MonkeyPatch
) -> None:
    modelo = ModeloGrabado(falla=True)
    monkeypatch.setattr(main, "_modelo", modelo)
    ruta = respx.put(URL_RESULTADOS).mock(return_value=httpx.Response(200))

    assert enviar(cliente, solicitud()).status_code == 202
    assert modelo.llamadas == 2
    assert not ruta.called


def test_rechaza_una_solicitud_fuera_del_contrato(cliente: TestClient) -> None:
    assert enviar(cliente, solicitud(idDocumento="0")).status_code == 422
    assert enviar(cliente, solicitud(extra="x")).status_code == 422
    assert enviar(cliente, "no es json").status_code == 422


def test_rechaza_formatos_no_admitidos_y_archivos_vacios(cliente: TestClient) -> None:
    assert enviar(cliente, solicitud(), b"GIF89a", "image/gif").status_code == 422
    assert enviar(cliente, solicitud(), b"").status_code == 422


def test_salud(cliente: TestClient) -> None:
    assert cliente.get("/salud").json() == {"estado": "disponible"}


@respx.mock
@pytest.mark.parametrize("codigo", [429, 403, 400])
def test_un_error_4xx_de_la_api_no_se_reintenta(
    cliente: TestClient, monkeypatch: pytest.MonkeyPatch, codigo: int
) -> None:
    modelo = ModeloGrabado(error=ErrorApi(codigo))
    monkeypatch.setattr(main, "_modelo", modelo)
    ruta = respx.put(URL_RESULTADOS).mock(return_value=httpx.Response(200))

    assert enviar(cliente, solicitud()).status_code == 202
    assert modelo.llamadas == 1
    assert not ruta.called


@respx.mock
def test_un_error_5xx_de_la_api_se_reintenta(cliente: TestClient, monkeypatch: pytest.MonkeyPatch) -> None:
    modelo = ModeloGrabado(error=ErrorApi(504))
    monkeypatch.setattr(main, "_modelo", modelo)
    respx.put(URL_RESULTADOS).mock(return_value=httpx.Response(200))

    assert enviar(cliente, solicitud()).status_code == 202
    assert modelo.llamadas == 2


@respx.mock
def test_una_respuesta_fuera_del_esquema_no_se_reintenta(cliente: TestClient, monkeypatch: pytest.MonkeyPatch) -> None:
    try:
        Extraccion.model_validate_json("{}")
    except ValidationError as error:
        invalida = error
    modelo = ModeloGrabado(error=invalida)
    monkeypatch.setattr(main, "_modelo", modelo)

    assert enviar(cliente, solicitud()).status_code == 202
    assert modelo.llamadas == 1


def test_exige_el_token_interno_cuando_esta_configurado(cliente: TestClient, monkeypatch: pytest.MonkeyPatch) -> None:
    monkeypatch.setenv("MIGAL_OCR_TOKEN_INTERNO", "secreto-compartido")
    main.configuracion.cache_clear()
    monkeypatch.setattr(main, "_modelo", ModeloGrabado(extraccion=Extraccion(legible=False, lineas=[])))

    assert enviar(cliente, solicitud()).status_code == 401
    respuesta = cliente.post(
        "/procesar",
        files={"archivo": ("3f2a.png", PNG, "image/png")},
        data={"solicitud": solicitud()},
        headers={"X-Token-Interno": "secreto-compartido"},
    )
    assert respuesta.status_code == 202
