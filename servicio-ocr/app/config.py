"""Configuración del servicio, leída solo de variables de entorno (ADR-012, punto 9)."""

from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Configuracion(BaseSettings):
    """Parámetros del servicio. La clave de Gemini nunca se escribe en el repositorio."""

    model_config = SettingsConfigDict(env_prefix="", extra="ignore")

    backend_url: str = "http://localhost:8080"
    """Dirección base del backend, donde se registran los resultados."""

    gemini_api_key: str = ""
    """Clave de la API de Gemini, de un proyecto con facturación habilitada."""

    migal_ocr_gemini_modelo: str = "gemini-3.8-flash"
    """Modelo Gemini Flash estable; se revisa cuando Google anuncie su retiro."""

    tiempo_modelo_segundos: float = 20.0
    """Tiempo máximo de una llamada a la API del modelo."""

    tiempo_backend_segundos: float = 5.0
    """Tiempo máximo de cada intento de registrar el resultado en el backend."""

    migal_ocr_token_interno: str = ""
    """Secreto compartido con el backend (MIGAL_OCR_TOKEN_INTERNO). Si está definido, POST /procesar exige la cabecera
    X-Token-Interno con este valor, para que otro proceso de la red no consuma la cuota de Gemini."""


@lru_cache
def configuracion() -> Configuracion:
    """Configuración única del proceso."""
    return Configuracion()
