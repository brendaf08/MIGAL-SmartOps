"""Registro del resultado en el backend (ADR-012, punto 5).

Usa el token de resultados recibido en la solicitud, sin modificarlo. Reintenta ante fallos de red o respuestas 5xx.
Un 409 significa que el documento ya no está pendiente (otro resultado llegó antes): el resultado se descarta.
"""

import logging
import time

import httpx

from app.esquemas import ResultadoDocumento

REGISTRO = logging.getLogger(__name__)
REINTENTOS = 2


def registrar(
    backend_url: str,
    id_documento: str,
    token: str,
    resultado: ResultadoDocumento,
    tiempo_segundos: float,
    cliente: httpx.Client | None = None,
    espera_base: float = 1.0,
) -> bool:
    """Envía el resultado. Devuelve True si el backend lo registró."""
    url = f"{backend_url.rstrip('/')}/api/v1/documentos/{id_documento}/resultados"
    cuerpo = resultado.model_dump(mode="json")
    propio = cliente is None
    http = cliente or httpx.Client(timeout=tiempo_segundos)
    try:
        for intento in range(REINTENTOS + 1):
            try:
                respuesta = http.put(url, json=cuerpo, headers={"Authorization": f"Bearer {token}"})
            except httpx.TransportError as error:
                REGISTRO.warning("No se pudo contactar al backend para el documento %s: %s", id_documento, error)
            else:
                if respuesta.status_code < 300:
                    return True
                if respuesta.status_code == 409:
                    REGISTRO.info("El documento %s ya no está pendiente; se descarta el resultado", id_documento)
                    return False
                if respuesta.status_code < 500:
                    REGISTRO.error(
                        "El backend rechazó el resultado del documento %s con %s", id_documento, respuesta.status_code
                    )
                    return False
                REGISTRO.warning("El backend respondió %s para el documento %s", respuesta.status_code, id_documento)
            if intento < REINTENTOS:
                time.sleep(espera_base * (2**intento))
        return False
    finally:
        if propio:
            http.close()
