# ADR-012: Integración del servicio de OCR y del Agente de IA con Gemini

## Contexto

El [ADR-011](adr-011-estrategia-ocr-fuzzy-matching.md) definió la estrategia del microservicio Python de OCR: extracción visual con un modelo multimodal de Gemini y asignación determinista de SKU con un preprocesador ferretero y RapidFuzz. Su contraste con el código del backend y con el contrato `documentos-ocr.yaml` reveló vacíos que deben resolverse antes de implementar:

1. **Modelo retirado.** El ADR-011 fija Gemini 1.5 Flash, que Google retiró de su API. Un identificador de modelo fijo en el código o en la documentación queda obsoleto con cada retiro.
2. **Catálogo inaccesible.** El Fuzzy Matching compara contra el catálogo maestro (`RF-FUZ-02`), pero el token de servicio solo tiene el alcance `ocr:resultados:escribir`. Las operaciones de catálogo exigen un usuario con rol, por lo que el microservicio no tiene cómo leer los productos activos.
3. **Archivo en disco compartido.** `SolicitudOcr.ruta` es una ruta relativa a la carpeta de ejecución del backend. En contenedores distintos, o con el backend y el microservicio en máquinas distintas, esa ruta no existe para el microservicio.
4. **Fallos sin salida.** El contrato `DocumentoResultados` solo admite `procesado` e `ilegible`. Si el microservicio o la API de Gemini fallan, no hay forma de informarlo, y `reprocesarDocumento` exige un `codigoError` registrado. El documento queda en `pendiente` de forma indefinida. La mitigación descrita en el ADR-011 no es ejecutable con el contrato vigente.
5. **Condición de carrera.** `DocumentoOcrService` invoca `ProveedorOcr.encolar` dentro de la transacción de carga. Si el envío al microservicio ocurre en ese momento, el resultado puede llegar antes de la confirmación de la transacción y no encontrar el documento.
6. **Secreto compartido.** El ADR-011 indica que el microservicio firma el token de servicio con `MIGAL_JWT_SECRETO`. Esa clave también firma los tokens de los usuarios: quien la obtenga desde el microservicio puede emitir tokens de Administrador.
7. **Agente sin proveedor definido.** El módulo `agenteia` opera con `ProveedorLlmSimulado`. `RF-IA-03` exige tool calling real, y las definiciones de herramientas (`DefinicionHerramienta`) no describen sus parámetros, que un modelo necesita para invocarlas.

`RNF-03` asigna los componentes especializados de IA al microservicio Python. El [ADR-004](adr-004-arquitectura-backend.md) ubica el Agente de IA en el módulo `agenteia` del backend, porque sus herramientas son funciones de consulta del backend que validan los permisos del usuario (`RF-IA-04`, `RF-IA-09`).

## Decisión

1. **Modelo configurable.** El servicio de OCR y el Agente de IA utilizan un modelo de la familia Gemini Flash en versión estable. El identificador se define por variable de entorno (`MIGAL_OCR_GEMINI_MODELO` en el microservicio y `MIGAL_AGENTE_GEMINI_MODELO` en el backend). No se admiten versiones preliminares (`preview`, `exp`) ni alias móviles (`latest`). El valor por defecto se fija al implementar, a partir de la lista oficial de modelos vigentes, y se revisa cuando Google anuncie su retiro. Esto reemplaza la versión 1.5 indicada en el ADR-011.
2. **Envío del archivo y del catálogo.** El backend envía al microservicio una solicitud `multipart/form-data` con el archivo (`archivo`) y un documento JSON (`solicitud`) que contiene `idDocumento`, `tipo`, `nombreArchivo`, `umbralBaja`, `tokenResultados` y `catalogo`. El catálogo es la lista de productos activos con `sku`, `nombreOficial`, `marca`, `presentacion` y `unidadMedida`. El microservicio no lee archivos del almacenamiento del backend ni consulta la base de datos. El catálogo no se envía a Gemini: solo se usa en el Fuzzy Matching local.
3. **Encolado después de la confirmación.** `ProveedorOcrHttp` publica la solicitud como evento y un componente la envía al microservicio en la fase `AFTER_COMMIT`, igual que `ProcesadorOcrSimulado`. El microservicio responde `202 Accepted` y procesa en segundo plano. Si el envío falla, el backend registra `SERVICIO_NO_DISPONIBLE` en `codigoError` en una transacción nueva, y el documento queda disponible para `reprocesarDocumento`.
4. **Vigilancia de pendientes.** Una tarea programada del backend registra `SERVICIO_NO_DISPONIBLE` en los documentos `pendiente` sin `codigoError` cuyo último encolado supere un plazo (`MIGAL_OCR_PLAZO_RESULTADO`, 60 segundos por defecto, seis veces el objetivo de `RNF-07`). La fecha del último encolado se registra en la columna nueva `fecha_encolado` de `documento_digital`, porque `fecha_subida` no cambia al reprocesar. Cubre la caída del microservicio, el fallo de la API de Gemini y la pérdida del resultado sin modificar el esquema `DocumentoResultados`. Un resultado que llega después del plazo se registra si el documento sigue pendiente; si ya fue procesado, el backend responde `TRANSICION_NO_PERMITIDA` y el microservicio lo descarta.
5. **Token de resultados emitido por el backend.** El backend emite, para cada encolado, un JWT de servicio con `tipo: servicio`, `alcances: ["ocr:resultados:escribir"]`, la reclamación `idDocumento` y una vigencia igual al plazo de vigilancia más un margen de cinco minutos. El microservicio lo usa sin modificarlo en `PUT /api/v1/documentos/{id}/resultados`. El backend rechaza un token cuyo `idDocumento` no coincida con el de la ruta. El microservicio no conoce `MIGAL_JWT_SECRETO`. Esto reemplaza la firma del token por el microservicio indicada en el ADR-011.
6. **Puntaje del Fuzzy Matching.** El puntaje base combina `0.6 × token_sort_ratio + 0.4 × token_set_ratio` de RapidFuzz, normalizado de 0 a 1, sobre el texto normalizado de la línea y el texto normalizado de `nombreOficial`, `marca` y `presentacion`. `token_set_ratio` aislado otorga el puntaje máximo cuando un texto es subconjunto del otro, lo que sobrevalora candidatos genéricos. Sobre el puntaje base se aplica la regla de consistencia técnica del ADR-011 (factor 0.3 si una medida leída no coincide con la del candidato). El microservicio envía como máximo tres coincidencias por línea con confianza igual o mayor a `umbralBaja`. El backend conserva la autoridad sobre los umbrales: descarta coincidencias bajo `baja` o con SKU inexistente y clasifica según `alta`. Los pesos y factores se calibran con el conjunto de evaluación del punto 8 y se documentan en el microservicio.
7. **Agente con tool calling nativo.** Se incorpora `ProveedorLlmGemini`, seleccionado con `migal.agente.proveedor=gemini`. `DefinicionHerramienta` incorpora la lista de sus parámetros (nombre, tipo, descripción, obligatoriedad y valores admitidos), coherente con los parámetros de la operación del contrato, y una prueba verifica esa coherencia. El proveedor declara al modelo solo las herramientas del rol del usuario, ejecuta una ronda de planificación (como máximo tres invocaciones) y una de redacción, y trata los resultados de las herramientas como datos y no como instrucciones. El agente permanece en el backend conforme al ADR-004; el modelo es un servicio externo y no un componente ejecutado localmente, por lo que no requiere el microservicio Python para cumplir `RNF-03`.
8. **Conjunto de evaluación.** El microservicio mantiene un conjunto de evaluación con documentos reales anonimizados de ambos tipos y sus líneas y SKU esperados. Las métricas son la exactitud de cantidades y la tasa de SKU correcto en la primera posición y entre las tres primeras. Las pruebas automatizadas no invocan la API de Gemini: utilizan respuestas grabadas del extractor.
9. **Credencial y uso de datos.** `GEMINI_API_KEY` se define por variable de entorno en el microservicio y en el backend. Se utiliza un proyecto con facturación habilitada, cuyos términos excluyen el uso de los datos enviados para mejorar los productos de Google; la condición se verifica al contratar el servicio.

## Justificación

1. **Vigencia del modelo.** Un identificador configurable permite sustituir un modelo retirado sin cambiar el código ni registrar un ADR nuevo.
2. **Catálogo sin alcances adicionales.** Enviar el catálogo con la solicitud evita crear un alcance de lectura para el servicio y mantiene un único punto de acceso a la base de datos (`ADR-004`). El backend ya consulta el catálogo y los umbrales, y la solicitud solo incluye productos activos, lo que garantiza `RF-FUZ-02` y `RF-FUZ-07`.
3. **Despliegue independiente.** Transferir el archivo en la solicitud permite ejecutar el backend y el microservicio en contenedores o máquinas distintas sin volúmenes compartidos (`RNF-04`).
4. **Ningún documento bloqueado.** El registro del fallo de envío y la vigilancia de pendientes garantizan que todo documento termina procesado, ilegible o con `SERVICIO_NO_DISPONIBLE`, y que el usuario puede reintentar (CU-08, flujo alternativo 2) sin cambiar el esquema del contrato.
5. **Consistencia transaccional.** El envío en `AFTER_COMMIT` replica el mecanismo ya probado del proveedor simulado y elimina la condición de carrera.
6. **Menor superficie de ataque.** El token por documento limita el daño de un microservicio comprometido a escribir resultados de documentos pendientes durante su vigencia, sin acceso a la clave de firma (`RNF-09`).
7. **Confianza calibrada.** La combinación de puntajes y la evaluación con documentos reales convierten los umbrales `alta` y `baja` en valores medidos, no supuestos (`RF-FUZ-03` a `RF-FUZ-05`).
8. **Tool calling verificable.** Declarar los parámetros de cada herramienta es condición del tool calling (`RF-IA-03`), y filtrarlas por rol antes de enviarlas al modelo cumple `RF-IA-09`.

## Alternativas consideradas

| Alternativa | Motivo de descarte |
| --- | --- |
| Alcance `catalogo:leer` para el token de servicio | Exige cambios en la seguridad del backend y en el contrato, y añade una llamada por documento. El envío del catálogo con la solicitud resuelve lo mismo sin alcances nuevos. |
| Fuzzy Matching en el backend Java | Contradice `RNF-03` y el ADR-011, que asignan la normalización de información no estructurada al microservicio Python. |
| Volumen compartido entre contenedores | Acopla el despliegue a una ruta común y no funciona con el backend y el microservicio en máquinas distintas. |
| Resultado `fallido` en `DocumentoResultados` | Informa el fallo más rápido, pero modifica el esquema del contrato y no cubre la caída del microservicio. La vigilancia de pendientes cubre ambos casos; el valor puede incorporarse después sin contradecir esta decisión. |
| Envío asíncrono con `@Async` | Añade un ejecutor y su configuración. El microservicio responde `202` de inmediato, por lo que el envío en `AFTER_COMMIT` con tiempos de espera acotados no demora la respuesta de forma apreciable. |
| Firma del token con `MIGAL_JWT_SECRETO` en el microservicio | Expone la clave que firma los tokens de los usuarios fuera del backend. |
| Clave de firma separada para tokens de servicio | Protege la clave de usuarios, pero obliga a un segundo decodificador en la seguridad del backend. El token por documento es más simple y además limita el token a un documento. |
| Agente de IA en el microservicio Python | Las herramientas son funciones del backend que validan permisos; ubicar el agente fuera obligaría a exponerlas como API para el servicio y a propagar la identidad del usuario (`ADR-004`). |

## Consecuencias

**Favorables:**

- Ningún documento queda pendiente sin posibilidad de reintento.
- El microservicio no conoce la clave de firma ni accede a la base de datos o al almacenamiento del backend.
- Sustituir el modelo de Gemini solo requiere cambiar una variable de entorno.
- Los umbrales de confianza se sustentan en métricas medidas sobre documentos reales.
- El catálogo y los datos de clientes no se envían a la API externa.

**Desfavorables:**

- `cargarDocumento` y `reprocesarDocumento` responden `202` aunque el envío al microservicio falle después; el fallo se observa en `codigoError`. *Mitigación:* el frontend consulta el estado del documento, como ya lo hace mientras está pendiente.
- La solicitud crece con el tamaño del catálogo (del orden de 1 MB para 10 000 productos). *Mitigación:* si el catálogo crece, el microservicio puede conservar una copia identificada por una versión que el backend envía en lugar de la lista completa.
- Un fallo del microservicio se informa al usuario tras el plazo de vigilancia y no de inmediato.
- Se requiere una migración para `fecha_encolado`.
- Los cambios en el contrato se limitan a descripciones: `codigoError` del documento y `reprocesarDocumento` admiten `SERVICIO_NO_DISPONIBLE` además de `DOCUMENTO_ILEGIBLE`.
- El conjunto de evaluación requiere recopilar y anonimizar documentos reales antes de fijar los umbrales.

## Referencias

- [ADR-004: Arquitectura y stack del backend](adr-004-arquitectura-backend.md)
- [ADR-008: Ajustes al contrato de la API REST](adr-008-ajustes-contrato-api.md)
- [ADR-009: Ajustes de seguridad, sesión y componentes del contrato](adr-009-ajustes-seguridad-y-componentes-contrato.md)
- [ADR-011: Estrategia de OCR y Fuzzy Matching para el catálogo ferretero](adr-011-estrategia-ocr-fuzzy-matching.md)
- [Requerimientos del sistema (RF-OCR, RF-FUZ, RF-IA, RNF-03, RNF-04, RNF-07, RNF-09)](../requerimientos.md)
- [Contrato OpenAPI de Documentos OCR](../../backend/api/modulos/documentos-ocr.yaml)
- [Contrato OpenAPI del Agente de IA](../../backend/api/modulos/agente-ia.yaml)
- [Plan de implementación del OCR y del Agente de IA](../plan-implementacion-ia-ocr.md)
