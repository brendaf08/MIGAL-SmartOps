# ADR-011: Estrategia de OCR y Fuzzy Matching para el catálogo ferretero

## Contexto

La [Arquitectura tecnológica](../arquitectura-tecnologica.md) y los requerimientos del sistema establecen que el procesamiento de documentos operativos se ejecutará mediante un microservicio en Python desacoplado del backend principal (`RNF-03`, `ADR-004`). El procesamiento abarca dos tipos de documentos con características operativas contrastantes (`RF-OCR-01`, `RF-OCR-02`):

1. **`solicitud_cliente` (CU-08, CU-09):** Fotografías de notas manuscritas tomadas en mostrador por clientes o maestros de obra, y capturas de pantalla de solicitudes por WhatsApp, con caligrafía informal, abreviaturas ferreteras y formatos libres.
2. **`documento_recepcion` (CU-08, CU-13):** Guías de remisión de proveedores y facturas comerciales impresas en láser o matriz de puntos, con tablas semiestructuradas de varias columnas (cantidad, unidad, descripción, precio).

Los requerimientos no funcionales condicionan esta solución:
- `RNF-03`: Microservicio desarrollado en Python.
- `RNF-07`: Tiempo objetivo de procesamiento no mayor a 10 segundos.
- `RF-FUZ-01` a `RF-FUZ-07`: Normalización de los términos leídos contra el catálogo maestro de SKU, cálculo de un nivel de coincidencia de 0 a 1 y asignación de umbrales de revisión manual (`alta: 0.85`, `baja: 0.60`).
- Principio de diseño: El procesamiento OCR y Fuzzy Matching **no genera por sí mismo ventas, reservas ni movimientos de Kardex**; su función es únicamente precargar información preliminar sujeta a revisión humana obligatoria.

El backend Spring Boot ya tiene implementado el contrato OpenAPI de `documentos-ocr.yaml`, las entidades JPA (migraciones V9 y V13) y la autenticación por token de servicio (`serviceAuth` con alcance `ocr:resultados:escribir`), pero actualmente opera con un proveedor simulado determinista (`ProveedorOcrSimulado`).

Se requiere definir qué herramientas y modelos componen el microservicio Python para satisfacer la precisión requerida en ferretería y cumplir con los tiempos de respuesta.

## Decisión

Se adopta una **estrategia complementaria de dos capas** para el microservicio Python:

1. **Capa de Extracción de Texto y Cantidades (VLM - Gemini 1.5 Flash):**
   - Utilizar el modelo multimodal de visión **Gemini 1.5 Flash** mediante su API oficial (`google-genai` / SDK REST) como motor de extracción visual y estructural.
   - El modelo recibe la imagen (PNG, JPG o PDF renderizado) junto con un prompt de extracción con formato de salida JSON estricto:
     `[ { "textoCrudo": string, "cantidadLeida": number | null } ]`.
   - El modelo descifra tanto escritura manuscrita compleja como tablas impresas sin requerir pipelines pesados de visión artificial local.
   - Si el archivo no contiene información textual interpretable, se emite el estado `resultado: "ilegible"`.

2. **Capa de Normalización y Similitud Determinista (Preprocesador Ferretero + RapidFuzz):**
   - El modelo de lenguaje **no asigna directamente los SKU del catálogo** para evitar alucinaciones en códigos de producto o inventario.
   - La asignación de SKU y el cálculo de confianza se delegan en un pipeline determinista en Python:
     * **Normalizador de jergas ferreteras:** Mapeo de abreviaturas locales (`kk` -> `king kong`, `f°` -> `fierro`, `bls` -> `bolsa`, `pulg` o `"` -> `pulgada`, `cem` -> `cemento`).
     * **Extractor y validador de medidas críticas:** Detección de dimensiones y calibres (`1/2"`, `3/8"`, `42.5 kg`, `18 huecos`, `14 AWG`).
     * **Similitud léxica con RapidFuzz:** Aplicación de `rapidfuzz.fuzz.token_set_ratio` entre el término normalizado y el catálogo maestro de productos activos.
     * **Regla de consistencia técnica:** Penalización drástica (reducción del 70% del puntaje) si una medida extraída no coincide con la medida del producto candidato (ejemplo: fierro 1/2 vs fierro 3/8).
     * **Filtrado y ordenamiento:** Se descartan coincidencias inferiores al umbral bajo (`baja = 0.60`), se ordenan descendentemente por confianza y se seleccionan las mejores coincidencias.

3. **Orquestación y Comunicación Asíncrona:**
   - El microservicio se implementa con **FastAPI**.
   - Expone un endpoint interno al que el backend Spring Boot envía la solicitud de procesamiento encolada (`SolicitudOcr`).
   - Al terminar el pipeline, el microservicio firma un JWT HS256 con el secreto compartido (`MIGAL_JWT_SECRETO`), identificándose como `tipo: servicio` y `alcances: ["ocr:resultados:escribir"]`.
   - Realiza la llamada de retorno: `PUT /api/v1/documentos/{id}/resultados` entregando el resultado formateado según el contrato `DocumentoResultados`.

## Justificación

1. **Adaptabilidad a la realidad ferretera:** Los OCRs tradicionales (como Tesseract) presentan tasas de error superiores al 60% en notas manuscritas con letra cursiva o fotos con mala iluminación tomadas con celular en almacén. Gemini 1.5 Flash posee una comprensión multimodal contextual que descifra terminología y caligrafía de mostrador de forma fiable (`RF-OCR-04`).
2. **Rendimiento y cumplimiento de RNF-07:** La inferencia multimodal de Gemini Flash toma habitualmente entre 1.5 y 2.5 segundos, y el matching con RapidFuzz toma menos de 10 milisegundos para catálogos de hasta 10,000 ítems. El tiempo total de procesamiento se sitúa en ~2 a 4 segundos, muy por debajo del objetivo máximo de 10 segundos (`RNF-07`).
3. **Cero alucinaciones transaccionales:** Al separar la transcripción visual (hecha por el VLM) de la resolución del SKU (hecha por RapidFuzz contra la base de datos real), se garantiza que solo se sugieran productos formalmente existentes y activos en PostgreSQL (`RF-FUZ-02`, `RF-FUZ-07`).
4. **Bajo costo operativo y simplicidad de despliegue:** No requiere aprovisionar ni costear servidores con tarjetas gráficas dedicadas (GPUs) para inferencia local de modelos de visión pesados. Puede desplegarse en un contenedor Docker liviano en cualquier VPS estándar o máquina local junto al backend (`RNF-04`).
5. **Alineación con el contrato existente:** Respeta al 100% la estructura de datos definida en `documentos-ocr.yaml`, la máquina de estados de `DocumentoOcrService.java` y la pantalla de validación humana en `RevisionOcrPage.tsx`.

## Alternativas consideradas

| Alternativa | Motivo de descarte |
| --- | --- |
| **Desarrollar OCR y modelos desde cero** | Inviable en tiempo y costo. Entrenar redes neuronales CTC o transformadores de visión propios requeriría miles de imágenes de ferretería etiquetadas y semanas de computación en GPU, con resultados inferiores a los modelos fundacionales actuales. |
| **Tesseract OCR (pytesseract)** | Incapaz de procesar caligrafía manuscrita de albañiles y fotos inclinadas tomadas con celular. Rompe la asociación de celdas en tablas y confunde símbolos esenciales de ferretería (como pulgadas `"` y fracciones `1/2`). |
| **PaddleOCR 100% On-Premise** | Excelente para guías y tablas impresas, pero su módulo de reconocimiento manuscrito en español tiene dificultades con letra desordenada y notas informales de mostrador. |
| **VLM End-to-End asignando directamente el SKU** | Descartado porque los modelos de lenguaje tienden a inventar o aproximar códigos SKU que no coinciden con las claves exactas de la base de datos, violando la integridad referencial exigida por el catálogo maestro. |
| **Levenshtein básico sin preprocesador ferretero** | Da puntajes altos a productos incompatibles que difieren solo en dimensiones críticas (como clavos de 2" vs 3", o fierros de 1/2" vs 3/8"), generando errores graves de despacho en obra. |

## Consecuencias

**Favorables:**
- Máxima tasa de reconocimiento en el caso de uso más difícil de la ferretería: pedidos informales manuscritos y capturas de WhatsApp (`solicitud_cliente`).
- Tiempos de respuesta reducidos (2 a 4 segundos por documento).
- Contenedor del microservicio sumamente liviano (< 200 MB de RAM), ya que la carga pesada de visión se delega en la API externa.
- Cumplimiento estricto del principio de validación humana: el sistema precarga sugerencias en el frontend pero exige confirmación explícita antes de asociar a cotizaciones o recepciones.

**Desfavorables:**
- Dependencia de conectividad a internet y de la disponibilidad de la API externa (Google AI Studio / Gemini API).
  * *Mitigación:* Si la API externa no responde o falla la red, el microservicio reporta error y el backend marca el documento con `codigoError: SERVICIO_NO_DISPONIBLE`, permitiendo al usuario reprocesar el documento con el botón de reintento (`POST /api/v1/documentos/{id}/procesamiento`) o ingresar los ítems manualmente.
- Requiere custodia de la credencial externa `GEMINI_API_KEY`.
  * *Mitigación:* Se gestiona mediante variable de entorno en el archivo `.env` excluido de Git, siguiendo las pautas de seguridad establecidas en `ADR-004` y `guia-despliegue-y-pendientes.pdf`.

## Referencias

- [Requerimientos del Sistema (RF-OCR-01..08, RF-FUZ-01..07, RNF-03, RNF-07)](../requerimientos.md)
- [Especificaciones de Casos de Uso (CU-08, CU-09, CU-13)](../especificaciones-casos-de-uso.md)
- [ADR-004: Arquitectura y stack del backend](adr-004-arquitectura-backend.md)
- [ADR-006: Contrato de la API REST](adr-006-contrato-api.md)
- [ADR-008: Ajustes al contrato de la API REST](adr-008-ajustes-contrato-api.md)
- [Contrato OpenAPI de Documentos OCR](../../backend/api/modulos/documentos-ocr.yaml)
- [Guía de despliegue y pendientes](../guia-despliegue-y-pendientes.pdf)
