"""Modelos de los mensajes del servicio.

``SolicitudProcesamiento`` y ``ProductoCatalogo`` siguen ``api/procesar.yaml``. ``ResultadoDocumento`` sigue el esquema
``DocumentoResultados`` de ``backend/api/modulos/documentos-ocr.yaml``. Las pruebas de contrato verifican ambos.
"""

from decimal import Decimal
from typing import Literal

from pydantic import BaseModel, ConfigDict, Field, field_serializer

TEXTO_MAXIMO = 500
# numeric(14,3) admite hasta 11 dígitos enteros; una lectura mayor (un RUC o un teléfono) se descarta.
CANTIDAD_MAXIMA = Decimal("100000000000")


class ProductoCatalogo(BaseModel):
    """Producto activo del catálogo maestro enviado por el backend."""

    model_config = ConfigDict(extra="forbid", frozen=True)

    sku: str = Field(min_length=1, max_length=50)
    nombreOficial: str = Field(min_length=1)
    marca: str | None
    presentacion: str | None
    unidadMedida: str = Field(min_length=1)


class SolicitudProcesamiento(BaseModel):
    """Solicitud de procesamiento de un documento (procesarDocumento)."""

    model_config = ConfigDict(extra="forbid")

    idDocumento: str = Field(pattern=r"^[1-9][0-9]*$")
    tipo: Literal["solicitud_cliente", "documento_recepcion"]
    nombreArchivo: str = Field(min_length=1, max_length=255)
    umbralBaja: float = Field(ge=0, le=1)
    tokenResultados: str = Field(min_length=1)
    catalogo: list[ProductoCatalogo]


class Coincidencia(BaseModel):
    """Coincidencia sugerida para una línea: SKU del catálogo y confianza de 0 a 1."""

    sku: str
    confianza: float = Field(ge=0, le=1)


class Linea(BaseModel):
    """Línea extraída con sus coincidencias, como DocumentoResultadoLinea."""

    textoCrudo: str = Field(min_length=1, max_length=TEXTO_MAXIMO)
    cantidadLeida: Decimal | None = Field(default=None, ge=0, lt=CANTIDAD_MAXIMA, decimal_places=3)
    coincidencias: list[Coincidencia]

    @field_serializer("cantidadLeida")
    def _cantidad_como_numero(self, valor: Decimal | None) -> int | float | None:
        """El contrato (CantidadNoNegativa) define un número JSON: pydantic serializa Decimal como texto, incluso en
        notación científica (``"1E+2"``)."""
        if valor is None:
            return None
        return int(valor) if valor == valor.to_integral_value() else float(valor)


class ResultadoDocumento(BaseModel):
    """Resultado que se registra en el backend (DocumentoResultados)."""

    resultado: Literal["procesado", "ilegible"]
    detalles: list[Linea]

    @classmethod
    def ilegible(cls) -> "ResultadoDocumento":
        return cls(resultado="ilegible", detalles=[])
