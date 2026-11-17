"""API del servicio de OCR (contrato ``api/procesar.yaml``)."""

import logging

from fastapi import BackgroundTasks, FastAPI, File, Form, HTTPException, Response, UploadFile
from pydantic import ValidationError

from app import resultados
from app.config import configuracion
from app.esquemas import TEXTO_MAXIMO, Linea, ResultadoDocumento, SolicitudProcesamiento
from app.extractor import ModeloGemini, ModeloVision, cantidad_valida, extraer_con_reintento
from app.matcher import coincidencias, preparar_catalogo

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s: %(message)s")
REGISTRO = logging.getLogger("servicio-ocr")

TAMANO_MAXIMO = 10 * 1024 * 1024
TIPOS_ADMITIDOS = {"image/jpeg", "image/png", "application/pdf"}

app = FastAPI(title="MIGAL SmartOps - Servicio de OCR", version="1.0.0")
_modelo: ModeloVision | None = None


def modelo() -> ModeloVision:
    """Modelo multimodal del proceso; se crea al primer uso para no exigir la clave en las pruebas."""
    global _modelo
    if _modelo is None:
        ajustes = configuracion()
        if not ajustes.gemini_api_key:
            raise RuntimeError("Falta la variable de entorno GEMINI_API_KEY")
        _modelo = ModeloGemini(ajustes.gemini_api_key, ajustes.migal_ocr_gemini_modelo, ajustes.tiempo_modelo_segundos)
    return _modelo


def procesar_documento(solicitud: SolicitudProcesamiento, contenido: bytes, tipo_mime: str) -> None:
    """Extrae, asigna SKU y registra el resultado. Un fallo del modelo no envía resultado (ADR-012, punto 4)."""
    try:
        extraccion = extraer_con_reintento(modelo(), contenido, tipo_mime)
    except Exception as error:  # noqa: BLE001 - el backend registrará SERVICIO_NO_DISPONIBLE al vencer el plazo
        if getattr(error, "code", None) == 429:
            # Cuota de la API agotada: un mensaje breve basta, la traza no aporta.
            REGISTRO.error("Cuota de Gemini agotada; no se procesó el documento %s", solicitud.idDocumento)
        else:
            REGISTRO.exception("No se pudo extraer el documento %s", solicitud.idDocumento)
        return
    lineas_leidas = [linea for linea in extraccion.lineas if linea.textoCrudo.strip()]
    if not extraccion.legible or not lineas_leidas:
        resultado = ResultadoDocumento.ilegible()
    else:
        candidatos = preparar_catalogo(solicitud.catalogo)
        resultado = ResultadoDocumento(
            resultado="procesado",
            detalles=[
                Linea(
                    textoCrudo=linea.textoCrudo.strip()[:TEXTO_MAXIMO],
                    cantidadLeida=cantidad_valida(linea.cantidadLeida),
                    coincidencias=coincidencias(linea.textoCrudo, candidatos, solicitud.umbralBaja),
                )
                for linea in lineas_leidas
            ],
        )
    ajustes = configuracion()
    resultados.registrar(
        ajustes.backend_url,
        solicitud.idDocumento,
        solicitud.tokenResultados,
        resultado,
        ajustes.tiempo_backend_segundos,
    )


@app.post("/procesar", status_code=202)
async def procesar(tareas: BackgroundTasks, archivo: UploadFile = File(...), solicitud: str = Form(...)) -> Response:
    """Valida la solicitud, responde 202 y procesa en segundo plano."""
    try:
        datos = SolicitudProcesamiento.model_validate_json(solicitud)
    except ValidationError as error:
        raise HTTPException(status_code=422, detail="La solicitud no cumple el contrato.") from error
    tipo_mime = (archivo.content_type or "").split(";")[0].strip().lower()
    if tipo_mime not in TIPOS_ADMITIDOS:
        raise HTTPException(status_code=422, detail="Formato de archivo no admitido.")
    contenido = await archivo.read(TAMANO_MAXIMO + 1)
    if not contenido or len(contenido) > TAMANO_MAXIMO:
        raise HTTPException(status_code=422, detail="El archivo está vacío o supera 10 MB.")
    tareas.add_task(procesar_documento, datos, contenido, tipo_mime)
    return Response(status_code=202)


@app.get("/salud")
def salud() -> dict[str, str]:
    """Disponibilidad del servicio."""
    return {"estado": "disponible"}
