# Servicio de OCR y Fuzzy Matching

Microservicio Python que procesa los documentos operativos de MIGAL SmartOps (CU-08, CU-09 y CU-13), según el
[ADR-011](../docs/adr/adr-011-estrategia-ocr-fuzzy-matching.md) y el
[ADR-012](../docs/adr/adr-012-integracion-ocr-y-agente-con-gemini.md).

1. El backend envía el archivo y el catálogo activo a `POST /procesar` ([contrato](api/procesar.yaml)) y el servicio
   responde 202.
2. En segundo plano, un modelo multimodal de Gemini transcribe las líneas y sus cantidades. No recibe el catálogo ni
   asigna SKU.
3. El normalizador ferretero y RapidFuzz asignan hasta tres SKU por línea, con la regla de consistencia técnica de
   medidas.
4. El servicio registra el resultado en `PUT /api/v1/documentos/{id}/resultados` con el token que recibió del backend.
   Si el modelo falla dos veces, no envía resultado y el backend marca el documento como `SERVICIO_NO_DISPONIBLE`.

El servicio no accede a la base de datos ni al almacenamiento del backend, y no conoce la clave de firma de los JWT.

## Estructura

| Archivo | Responsabilidad |
| --- | --- |
| `app/main.py` | API FastAPI: `POST /procesar` y `GET /salud`. |
| `app/esquemas.py` | Modelos Pydantic de la solicitud y del resultado. |
| `app/extractor.py` | Llamada a Gemini con salida estructurada. |
| `app/normalizador.py` | Jerga ferretera y medidas en forma canónica. |
| `app/matcher.py` | Puntaje RapidFuzz y regla de consistencia técnica. |
| `app/resultados.py` | Registro del resultado en el backend, con reintentos. |
| `evaluar.py` | Métricas sobre el [conjunto de evaluación](tests/datos/evaluacion/README.md). |

## Ejecutar en local

Requiere Python 3.12. Desde `servicio-ocr/`:

```powershell
python -m venv .venv
.venv\Scripts\activate
pip install -e ".[desarrollo]"
$env:GEMINI_API_KEY = "<clave-de-la-api>"
$env:BACKEND_URL = "http://localhost:8080"
uvicorn app.main:app --port 8000
```

El backend usa el servicio con `MIGAL_OCR_PROVEEDOR=http` y `MIGAL_OCR_URL=http://localhost:8000`.

| Variable | Uso | Valor por defecto |
| --- | --- | --- |
| `GEMINI_API_KEY` | Clave de la API de Gemini, de un proyecto con facturación habilitada. | Sin valor |
| `MIGAL_OCR_GEMINI_MODELO` | Modelos Gemini estables separados por coma, por orden de preferencia; pasa al siguiente si uno agota su cuota. | `gemini-3.5-flash-lite,gemini-3.1-flash-lite,gemini-3.6-flash` |
| `BACKEND_URL` | Dirección del backend. | `http://localhost:8080` |

## Verificar

```powershell
ruff check .
ruff format --check .
python -m pytest -q
```

Las pruebas no llaman a la API de Gemini: usan respuestas grabadas del modelo y un backend simulado. Las pruebas de
contrato comparan los mensajes con `api/procesar.yaml` y con `DocumentoResultados` del backend.

## Calibrar

Con documentos en `tests/datos/evaluacion/` y la clave definida, `python evaluar.py` informa la tasa de SKU correcto en la
primera posición y entre las tres primeras, la exactitud de cantidades y el tiempo por documento. Los pesos del puntaje,
el factor de penalización y el diccionario de jerga se ajustan hasta alcanzar las metas del plan de implementación.
