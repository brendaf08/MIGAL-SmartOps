"""Extracción de líneas con un modelo multimodal de Gemini (ADR-011, capa de extracción).

El modelo solo transcribe: devuelve el texto de cada línea y su cantidad. No recibe el catálogo ni asigna SKU. La salida
se fuerza con un esquema JSON, y un documento sin texto interpretable se informa como ilegible.
"""

import logging
from decimal import Decimal, InvalidOperation
from typing import Protocol

from pydantic import BaseModel, Field

from app.esquemas import CANTIDAD_MAXIMA

REGISTRO = logging.getLogger(__name__)

INSTRUCCIONES = """\
Eres un transcriptor de documentos de una ferretería peruana. Lee la imagen o el PDF y transcribe cada línea de \
producto: el texto tal como aparece (con sus abreviaturas y medidas) y la cantidad pedida o recibida. Las notas pueden \
ser manuscritas o capturas de WhatsApp; las guías y facturas tienen columnas de cantidad, unidad y descripción. No \
corrijas ni completes nombres de productos, no inventes líneas y no incluyas precios, totales, fechas ni datos de \
personas. Si una cantidad no se lee con claridad, déjala en null. Si el documento no tiene líneas de productos \
legibles, responde legible = false y una lista vacía."""


class LineaLeida(BaseModel):
    """Línea transcrita por el modelo."""

    textoCrudo: str = Field(description="Descripción del producto tal como aparece en el documento.")
    cantidadLeida: float | None = Field(description="Cantidad de la línea, o null si no se lee con claridad.")


class Extraccion(BaseModel):
    """Salida estructurada del modelo."""

    legible: bool = Field(description="false si el documento no tiene líneas de productos legibles.")
    lineas: list[LineaLeida]


class ModeloVision(Protocol):
    """Puerto hacia el modelo multimodal; las pruebas lo reemplazan por respuestas grabadas."""

    def extraer(self, contenido: bytes, tipo_mime: str) -> Extraccion: ...


class ModeloGemini:
    """Modelo multimodal sobre la API de Gemini, con salida estructurada y temperatura 0."""

    def __init__(self, api_key: str, modelo: str, tiempo_segundos: float) -> None:
        from google import genai
        from google.genai import types

        self._types = types
        self._modelo = modelo
        self._cliente = genai.Client(
            api_key=api_key, http_options=types.HttpOptions(timeout=int(tiempo_segundos * 1000))
        )

    def extraer(self, contenido: bytes, tipo_mime: str) -> Extraccion:
        types = self._types
        respuesta = self._cliente.models.generate_content(
            model=self._modelo,
            contents=[types.Part.from_bytes(data=contenido, mime_type=tipo_mime), INSTRUCCIONES],
            config=types.GenerateContentConfig(
                temperature=0, response_mime_type="application/json", response_schema=Extraccion
            ),
        )
        return Extraccion.model_validate_json(respuesta.text or "")


def cantidad_valida(valor: float | None) -> Decimal | None:
    """Cantidad no negativa, menor a 10^11 y con hasta tres decimales (CantidadNoNegativa y numeric(14,3)); otra
    lectura, como un RUC tomado por cantidad, se descarta."""
    if valor is None:
        return None
    try:
        cantidad = Decimal(str(valor)).quantize(Decimal("0.001"))
    except (InvalidOperation, ValueError):
        return None
    if cantidad < 0 or cantidad >= CANTIDAD_MAXIMA:
        return None
    return cantidad


def es_reintentable(error: Exception) -> bool:
    """Solo los fallos transitorios se reintentan: 5xx de la API, tiempos agotados y errores de red. Un 4xx (cuota
    agotada 429, clave inválida 403, solicitud rechazada 400) o una respuesta que no cumple el esquema
    (``pydantic.ValidationError``) se repetiría igual y solo gastaría otra solicitud de la cuota."""
    codigo = getattr(error, "code", None)
    if isinstance(codigo, int):
        return codigo >= 500
    if isinstance(error, TimeoutError | ConnectionError):
        return True
    nombre = type(error).__name__
    return "Timeout" in nombre or "Connect" in nombre or "Network" in nombre


def extraer_con_reintento(modelo: ModeloVision, contenido: bytes, tipo_mime: str) -> Extraccion:
    """Llama al modelo y reintenta una vez ante un fallo transitorio. Si vuelve a fallar, o el error no es
    reintentable, propaga el error (ADR-012, punto 4)."""
    try:
        return modelo.extraer(contenido, tipo_mime)
    except Exception as error:  # noqa: BLE001 - se decide por el tipo de fallo
        if not es_reintentable(error):
            raise
        REGISTRO.warning("Falló la extracción con el modelo; se reintenta una vez", exc_info=True)
        return modelo.extraer(contenido, tipo_mime)
