"""Verifica los mensajes del servicio contra los contratos del backend y del propio servicio."""

from decimal import Decimal
from pathlib import Path
from typing import Any

import jsonschema
import yaml

from app.esquemas import Coincidencia, Linea, ProductoCatalogo, ResultadoDocumento, SolicitudProcesamiento

RAIZ = Path(__file__).resolve().parents[2]
CONTRATO_SERVICIO = RAIZ / "servicio-ocr" / "api" / "procesar.yaml"
CONTRATO_DOCUMENTOS = RAIZ / "backend" / "api" / "modulos" / "documentos-ocr.yaml"


def cargar(ruta: Path) -> dict[str, Any]:
    return yaml.safe_load(ruta.read_text(encoding="utf-8"))


def resolver(nodo: Any, base: Path) -> Any:
    """Reemplaza las referencias $ref, locales o a otros archivos, por su definición."""
    if isinstance(nodo, list):
        return [resolver(elemento, base) for elemento in nodo]
    if not isinstance(nodo, dict):
        return nodo
    if "$ref" in nodo:
        archivo, _, fragmento = nodo["$ref"].partition("#")
        destino = (base.parent / archivo).resolve() if archivo else base
        definicion: Any = cargar(destino)
        for clave in fragmento.strip("/").split("/"):
            definicion = definicion[clave]
        return resolver(definicion, destino)
    return {clave: resolver(valor, base) for clave, valor in nodo.items()}


def test_la_solicitud_tiene_los_campos_del_contrato_del_servicio() -> None:
    esquemas = cargar(CONTRATO_SERVICIO)["components"]["schemas"]

    for modelo, nombre in ((SolicitudProcesamiento, "SolicitudProcesamiento"), (ProductoCatalogo, "ProductoCatalogo")):
        assert set(modelo.model_fields) == set(esquemas[nombre]["properties"])
        assert set(esquemas[nombre]["required"]) == set(modelo.model_fields)


def test_el_resultado_cumple_documento_resultados_del_backend() -> None:
    esquema = resolver(cargar(CONTRATO_DOCUMENTOS)["components"]["schemas"]["DocumentoResultados"], CONTRATO_DOCUMENTOS)
    procesado = ResultadoDocumento(
        resultado="procesado",
        detalles=[
            Linea(
                textoCrudo="CEM SOL 42.5",
                cantidadLeida=Decimal("2.5"),
                coincidencias=[Coincidencia(sku="CEM-001", confianza=0.912)],
            ),
            Linea(textoCrudo="pintura", cantidadLeida=None, coincidencias=[]),
        ],
    )

    for resultado in (procesado, ResultadoDocumento.ilegible()):
        documento = resultado.model_dump(mode="json")
        for linea in documento["detalles"]:
            if linea["cantidadLeida"] is not None:
                linea["cantidadLeida"] = float(linea["cantidadLeida"])
        jsonschema.validate(documento, esquema, cls=jsonschema.Draft202012Validator)
