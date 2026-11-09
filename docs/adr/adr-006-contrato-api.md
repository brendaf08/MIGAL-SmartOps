# ADR-006: Contrato de la API REST

## Contexto

La [Arquitectura tecnológica](../arquitectura-tecnologica.md) define un backend Spring Boot que expone una API REST para el frontend React, y un microservicio Python para OCR y Fuzzy Matching que se comunica con el backend mediante interfaces definidas. El [Modelo y diccionario de clases](../modelo-y-diccionario-clases.md) identifica los controladores de la capa de presentación (`AuthController`, `UsuarioController`, `ProductoController`, `ClienteController`, `InventarioController`, `VentaController`, `CotizacionController`, `DespachoController`, `AbastecimientoController`, `OcrDigitalController` y `ChatIAController`) y sus métodos clave. Los [Casos de uso](../casos-de-uso.md) y sus especificaciones definen los actores CU-01 a CU-14.

Los requerimientos que condicionan el contrato son los siguientes:

| Requerimiento | Implicancia para el contrato |
| --- | --- |
| RF-USR-01, RF-USR-02, RF-USR-03, RNF-09 | Autenticación personal y autorización por rol en cada operación. |
| RF-USR-04, RNF-10 | Cada operación que modifique inventario identifica al usuario responsable y registra fecha, tipo y origen. |
| RNF-06 | Las consultas habituales responden en no más de 3 segundos; los listados grandes se paginan en el servidor. |
| RNF-14 | Los módulos se comunican mediante interfaces definidas, sin acoplamiento directo. |
| RNF-04 | Los servicios se despliegan en contenedores; el contrato no depende del entorno. |
| RF-COT-08 | Crear, emitir, rechazar o vencer una cotización no modifica ni reserva existencias. |
| RF-CLI-04 | El cliente es opcional en la cotización. |
| RF-FUZ-05 | Las coincidencias de fuzzy matching por debajo del umbral requieren validación manual. |
| RF-IA-08, RF-IA-09 | El agente indica falta de información sin inventar datos y respeta los permisos del usuario autenticado. |

### Contrato implícito del frontend

El frontend contiene una capa de acceso simulada en [`frontend/src/api/mock.ts`](../../frontend/src/api/mock.ts), con 19 archivos `api.ts` en `frontend/src/features/**`. Cada función de esos archivos se sustituirá por una llamada HTTP sin cambiar su firma. Del análisis de esos archivos se identifican los siguientes patrones.

**Forma de la capa de acceso.** Cada módulo exporta funciones con verbos en español (`listar`, `obtener`, `registrar`, `crear`, `actualizar`, `cambiarEstado`, `desactivar`, `confirmar`, `procesar`, `validar`, `descartar`). Su correspondencia con HTTP es directa, salvo en las operaciones de negocio que se describen más adelante.

**Formas de respuesta.** Los objetos usan camelCase en español (`razonSocial`, `estadoActivo` en el modelo, `estado` en el frontend). La mayoría de los listados devuelven arreglos completos sin paginación. Solo dos consultas son paginadas: `buscarCatalogo` (parámetros `page` y `pageSize`, respuesta `Page<T>` con `items` y `total`) y `consultarDisponibilidad` (además devuelve `enAlerta` y `totalGeneral`). El Kardex filtrado (`listarMovimientosFiltrados`) no está paginado. El orden se fija en el servidor: el Kardex va del más reciente al más antiguo, y las alertas van por nivel y por faltante.

**Filtros.** Se identifican los parámetros `q` (texto libre sobre SKU, nombre, razón social o RUC), `categoria`, `estado`, `soloAlerta`, `tipo`, `origen`, `ubicacion`, y el rango `desde` y `hasta` en formato `aaaa-mm-dd`, inclusivo.

**Errores.** Las funciones lanzan un `Error` cuyo mensaje es un código en mayúsculas (`ORDEN_NO_ENCONTRADA`, `PROVEEDOR_INACTIVO`, `RUC_DUPLICADO`, `USUARIO_DUPLICADO`, `NO_ENCONTRADO`) o un mensaje en español para reglas de negocio (`"Sin stock disponible para las unidades pendientes."`). Algunos errores llevan datos adicionales mediante clases propias: `RucDuplicadoError` con el identificador del proveedor existente, `DocumentoDuplicadoError` con el cliente existente y `SkuDuplicadoError`.

**Operaciones con efectos sobre inventario.** La confirmación de salida de despacho (`confirmarSalida`), la confirmación de recepción (`confirmarRecepcion`), los movimientos manuales (`registrarMovimiento`) y el registro de venta (`registrarVenta`) devuelven la existencia antes y después por ubicación (`cambios`). La recepción devuelve además `estadoOrden`, `porRecibir` y `lineasPendientes`.

**Estados.** Los valores de estado coinciden en gran medida con las enumeraciones del modelo (`EstadoVenta`, `EstadoGuia`, `EstadoCompra`, `EstadoDocumento`). Las enumeraciones de movimientos usan minúsculas en la entrada (`entrada`, `salida`, `traslado`) y capitalización en la salida (`Entrada`, `Salida`).

**Autenticación y roles.** `iniciarSesion(usuario, clave)` devuelve un `User` con `name` y `role`, sin token. Los roles se definen en la constante `ROLES` (`Administrador`, `Vendedor`, `Almacenero`, `Reponedor`).

**OCR y agente.** `procesarDocumento(archivo, tipo)` recibe un archivo y devuelve el documento con sus líneas de forma síncrona. `consultarAgente(pregunta)` devuelve `consultas` (lista de consultas realizadas), `texto` e `incierta`.

### Discrepancias entre el contrato implícito y las decisiones previas

| Tema | Contrato implícito del frontend | Decisión previa (modelo, requerimientos o reglas del ADR-003) | Resolución en este ADR |
| --- | --- | --- | --- |
| Autenticación | El login devuelve `User` sin token. | `AuthController.login` devuelve `Token`; RNF-09 exige autenticación. | La sesión devuelve un JWT. |
| Paginación | Solo dos listados están paginados. | RNF-06 exige respuestas en tiempo acotado. | Todos los listados que pueden crecer se paginan. |
| Identificadores | Los códigos de negocio (`OC-0087`, `R-0012`, `CLI-001`) se usan como clave de ruta; los documentos OCR usan números. | El modelo define `idX` como identificadores técnicos. | `id` opaco como clave de ruta y `codigo` como campo de negocio; el SKU es la clave natural de productos. |
| Ubicaciones | Se usan textos (`"Almacén 1"`) y tres campos fijos (`tienda`, `almacen1`, `almacen2`). | El modelo define la entidad `Almacen` e `Inventario` por producto y ubicación. | Se referencia `ubicacionId` del recurso `almacenes`. |
| Enumeraciones | Movimientos con minúsculas en la entrada y capitalización en la salida; etiquetas de tipo de documento en español. | El modelo usa valores en PascalCase sin acentos. | Valores PascalCase sin acentos en el JSON; las etiquetas se traducen en la interfaz. |
| Recepción | `confirmarRecepcion` registra y valida en una sola llamada, sin estado `Pendiente`. | `postOrdenRecepcion` y `postConfirmarRecepcion` son operaciones separadas; `EstadoRecepcion` incluye `Pendiente`. | Registro y confirmación en dos operaciones. |
| Almacén de recepción | Fijo en `"Almacén 1"`. | `DetalleRecepcion` incluye `idAlmacenDestino`. | `ubicacionId` obligatorio por línea. |
| Origen de la venta | `registrarVenta` acepta venta sin cotización, cliente opcional y una `modalidad` de atención. | `VentaController` no contempla ventas directas; la venta proviene de una cotización validada. Regla del ADR-003 (texto): cliente obligatorio. | La venta solo se crea por conversión de cotización. El cliente es obligatorio en la conversión. La `modalidad` queda fuera de la versión 1 hasta definirla. |
| Entrega parcial | `Venta.entrega` y `OrdenDespacho.entrega` admiten `Parcial`. | Regla del ADR-003 (texto): despacho único y completo. El modelo no incluye `TipoAtencion` parcial. | La entrega es siempre completa. La atención parcial solo incrementa la reserva. Se elimina el campo `entrega` del contrato, sujeto a conciliación con PC2. |
| Cancelación | `cancelarVenta` no recibe motivo. | `cancelarVentaAntesDespacho(idVenta, motivo)`. | El motivo es obligatorio. |
| Terminología de despacho | Se usa "orden de despacho" (identificadores `D-…`). | CU-06 habla de "orden de despacho"; el modelo usa la clase `GuiaDespacho`. | Recurso `guias-despacho`. Se debe alinear la terminología de CU-06. |
| Movimientos manuales | "Salida por merma o ajuste" genera una salida de Kardex sin despacho. | Regla del ADR-003 (texto): la salida de Kardex solo se registra al confirmar el despacho. | Se modela con origen `Ajuste`. Queda pendiente confirmar la excepción con el ADR-003. |
| Validación de cotización | Aceptar y convertir se resuelven con `registrarVenta`; no hay validación de viabilidad. | `postValidarViabilidad` y `postConvertirAVenta`. | Operaciones separadas. |
| Proveedores | `skus: string[]`, sin precio ni plazo. | `CatalogoProveedor` incluye `precioCompra` y `plazoEntregaReferencialDias`. | Recurso `proveedores/{id}/catalogo`. |
| Kardex | `fecha` sin hora y `usuario` con el nombre visible. | RNF-10 exige fecha y hora, usuario responsable, tipo y origen. | `fechaHora` con hora y `usuarioId`. |
| Usuarios | `name`, y baja mediante `revocarAcceso`. | El modelo usa `nombres` y `apellidos`, y `putActualizarEstado`. | Campos `nombres` y `apellidos`; baja como cambio de `estado`. |
| OCR | Procesamiento síncrono de un segundo, con el resultado en la respuesta. | El servicio Python procesa el archivo de forma independiente y devuelve resultados al backend. | Respuesta `202 Accepted` y recepción de resultados por un endpoint de servicio. |
| Agente | Campo `pregunta`. | `postPreguntaAgente(mensaje)`. | Campo `mensaje`. |

## Decisión

Se adopta un contrato **contract-first** basado en **OpenAPI 3.1**. El contrato se escribe y se revisa antes de implementar el backend o el cliente del frontend, y es la fuente de verdad para ambos.

### Estructura del contrato

El contrato vive en `backend/api/` con la siguiente organización:

| Archivo | Contenido |
| --- | --- |
| `backend/api/openapi.yaml` | Archivo raíz. Define la información general, los servidores, la seguridad global, los componentes compartidos (parámetros, respuestas, esquemas de error y paginación) y referencia los archivos de módulo. |
| `backend/api/autenticacion-usuarios.yaml` | Sesiones, usuarios y roles (CU-01, CU-02). |
| `backend/api/catalogo.yaml` | Productos, categorías y almacenes (CU-03). |
| `backend/api/inventario.yaml` | Disponibilidad, existencias, Kardex, movimientos manuales y alertas (CU-04, CU-05, CU-07). |
| `backend/api/comercial.yaml` | Clientes, cotizaciones, ventas y atención de ventas (CU-09, CU-10). |
| `backend/api/despacho.yaml` | Guías de despacho, salida física y reimpresión (CU-06). |
| `backend/api/abastecimiento.yaml` | Proveedores, catálogo de proveedor, órdenes de compra y recepciones (CU-11, CU-12, CU-13). |
| `backend/api/documentos-ocr.yaml` | Carga, consulta, validación y descarte de documentos, y recepción de resultados del OCR (CU-08). |
| `backend/api/agente-ia.yaml` | Consultas al agente (CU-14). |

Los esquemas compartidos se definen una sola vez en el archivo raíz. Cada archivo de módulo los referencia mediante `$ref`. Un cambio en un módulo no requiere modificar los demás.

### Tecnologías candidatas

Se consideran las siguientes herramientas. Las versiones se fijan al incorporarlas al repositorio.

- Validación y linting del contrato: Redocly CLI o Spectral.
- Detección de cambios incompatibles entre versiones del contrato: oasdiff.
- Generación de interfaces y controladores para Spring Boot a partir del contrato: openapi-generator, con el generador de Spring.
- Generación del cliente tipado del frontend: openapi-typescript, junto con un cliente HTTP ligero basado en `fetch`, integrado con la capa de acceso y TanStack Query definidas en [ADR-001](adr-001-stack-frontend.md).

### Versionado y rutas

- Todas las rutas comienzan con el prefijo `/api/v1`.
- Un cambio incompatible (eliminar un campo, cambiar un tipo, hacer obligatorio un parámetro, cambiar el significado de un estado) requiere una nueva versión `/api/v2`. Ambas versiones coexisten durante un período de transición que se registra en la documentación del módulo afectado.
- Las adiciones compatibles (nuevos campos opcionales, nuevos endpoints, nuevos valores de enumeración documentados como abiertos) se incorporan a `v1` sin cambiar la versión.

### Recursos y nombres

- Los recursos se nombran en plural y en español, con minúsculas y guiones: `productos`, `clientes`, `proveedores`, `ordenes-compra`, `recepciones`, `cotizaciones`, `ventas`, `guias-despacho`, `documentos`, `almacenes`, `usuarios`.
- Las acciones de estado se modelan como sub-recursos de acción con método `POST`, por ejemplo `POST /cotizaciones/{id}/aceptacion`. Los cambios de estado de maestros (activar o desactivar) usan `PUT /{recurso}/{id}/estado`, de acuerdo con `putActualizarEstado` del modelo.
- Los campos JSON usan camelCase. El esquema SQL utiliza snake_case, según lo indicado para la persistencia. La traducción ocurre en dos puntos: en la capa de persistencia, donde cada atributo de entidad JPA se asocia a su columna; y en la capa de presentación, donde los DTO generados a partir del contrato se mapean desde las entidades del dominio. Ningún nombre de columna aparece en el JSON. El esquema SQL todavía no existe en `database/migraciones/`, por lo que esta convención se verifica al definir las migraciones.
- Cada recurso expone `id` como identificador técnico opaco, tratado como cadena en el contrato. Cuando el recurso tiene un código de negocio (`OC-0087`, `R-0012`, `CLI-001`), se expone en el campo `codigo`, que se muestra al usuario y no se utiliza como clave de ruta. Los productos usan el SKU como clave natural.
- Las enumeraciones usan valores en PascalCase sin acentos (`PendienteAbastecimiento`, `SalidaConfirmada`, `Entrada`). Las etiquetas visibles se resuelven en el frontend.
- Las fechas de negocio usan `date` (`aaaa-mm-dd`). Las marcas de auditoría y de hora de movimiento usan `date-time` en RFC 3339 con desplazamiento explícito.
- Los importes se expresan como números con dos decimales y se redondean en el servidor a céntimos. Las cantidades son numéricas; las unidades que lo requieren (por ejemplo, agregados por metro cúbico) admiten hasta tres decimales.

### Mapa de endpoints principales

| Módulo | Método y ruta | Operación | Roles |
| --- | --- | --- | --- |
| Autenticación | `POST /api/v1/sesiones` | Iniciar sesión y obtener el JWT. | Público |
| Autenticación | `GET /api/v1/sesiones/actual` | Obtener el usuario y el rol de la sesión. | Todos los autenticados |
| Usuarios | `GET`, `POST /api/v1/usuarios` | Listar y registrar usuarios (CU-02). | Administrador |
| Usuarios | `PUT /api/v1/usuarios/{id}`, `PUT /api/v1/usuarios/{id}/estado` | Actualizar nombre y rol; activar o desactivar acceso. | Administrador |
| Catálogo | `GET`, `POST /api/v1/productos` | Buscar y registrar productos (CU-03). | Almacenero, Administrador |
| Catálogo | `GET`, `PUT /api/v1/productos/{sku}` | Consultar y modificar un producto. | Almacenero, Administrador |
| Catálogo | `GET /api/v1/almacenes` | Listar tienda principal y almacenes. | Todos los autenticados |
| Inventario | `GET /api/v1/inventario/disponibilidad` | Disponibilidad paginada (CU-04). | Almacenero, Vendedor |
| Inventario | `GET /api/v1/productos/{sku}/existencias` | Existencias físicas, reservadas y libres por ubicación. | Almacenero, Vendedor |
| Inventario | `GET /api/v1/inventario/alertas` | Productos en nivel crítico o en el mínimo (P12). | Almacenero, Reponedor |
| Inventario | `GET`, `POST /api/v1/inventario/movimientos` | Consultar el Kardex (CU-07) y registrar movimientos manuales (CU-05). | Almacenero (Kardex y movimientos); Administrador (Kardex) |
| Comercial | `GET`, `POST /api/v1/clientes`; `PUT /api/v1/clientes/{id}`; `PUT /api/v1/clientes/{id}/estado` | Gestionar clientes. | Vendedor, Administrador |
| Comercial | `GET`, `POST /api/v1/cotizaciones`; `PUT /api/v1/cotizaciones/{id}` | Listar, emitir y modificar cotizaciones emitidas (CU-09). | Vendedor |
| Comercial | `POST /api/v1/cotizaciones/{id}/aceptacion` | Registrar la aceptación del cliente. | Vendedor |
| Comercial | `POST /api/v1/cotizaciones/{id}/rechazo` | Registrar el rechazo del cliente. | Vendedor |
| Comercial | `POST /api/v1/cotizaciones/{id}/validacion` | Validar la viabilidad comercial por MIGAL. | Vendedor, Administrador |
| Comercial | `POST /api/v1/cotizaciones/{id}/conversion` | Convertir la cotización en venta (`201 Created`). | Vendedor |
| Comercial | `GET /api/v1/ventas` | Listar ventas con filtro por estado (CU-10). | Vendedor, Almacenero |
| Comercial | `GET /api/v1/ventas/{id}`, `GET /api/v1/ventas/{id}/reservas` | Consultar la venta y sus reservas. | Vendedor, Almacenero |
| Comercial | `POST /api/v1/ventas/{id}/atencion` | Reservar las cantidades pendientes con el stock libre actual. | Vendedor, Almacenero |
| Comercial | `POST /api/v1/ventas/{id}/cancelacion` | Cancelar la venta antes del despacho, con motivo. | Vendedor |
| Despacho | `POST /api/v1/ventas/{id}/guias-despacho` | Generar la guía de despacho de una venta lista (CU-06). | Almacenero |
| Despacho | `GET /api/v1/guias-despacho`, `GET /api/v1/guias-despacho/{id}` | Listar y consultar guías. | Almacenero |
| Despacho | `GET /api/v1/guias-despacho/{id}/documento` | Obtener los datos imprimibles de la guía. | Almacenero |
| Despacho | `POST /api/v1/guias-despacho/{id}/salida` | Confirmar la salida física completa y registrar el Kardex. | Almacenero |
| Despacho | `POST /api/v1/guias-despacho/{id}/reimpresiones` | Registrar una reimpresión con motivo. | Almacenero |
| Abastecimiento | `GET`, `POST /api/v1/proveedores`; `PUT /api/v1/proveedores/{id}`; `PUT /api/v1/proveedores/{id}/estado` | Gestionar proveedores (CU-11). | Reponedor, Administrador |
| Abastecimiento | `GET`, `PUT /api/v1/proveedores/{id}/catalogo` | Consultar y mantener el catálogo del proveedor. | Reponedor, Administrador |
| Abastecimiento | `GET`, `POST /api/v1/ordenes-compra` | Listar y crear órdenes de compra en estado `Pendiente` (CU-12). | Reponedor |
| Abastecimiento | `POST /api/v1/ordenes-compra/{id}/recepciones` | Registrar una recepción en estado `Pendiente` (CU-13). | Almacenero |
| Abastecimiento | `POST /api/v1/recepciones/{id}/confirmacion` | Confirmar las cantidades recibidas y registrar el Kardex de entrada. | Almacenero |
| OCR | `POST /api/v1/documentos` | Cargar un documento (`multipart/form-data`) y devolver `202 Accepted` (CU-08). | Vendedor, Almacenero |
| OCR | `GET /api/v1/documentos`, `GET /api/v1/documentos/{id}` | Listar y consultar documentos y sus líneas extraídas. | Vendedor, Almacenero |
| OCR | `POST /api/v1/documentos/{id}/validacion` | Validar las líneas con las correcciones del usuario. | Vendedor, Almacenero |
| OCR | `DELETE /api/v1/documentos/{id}` | Descartar un documento (eliminación lógica). | Vendedor, Almacenero |
| OCR | `PUT /api/v1/documentos/{id}/resultados` | Recibir los resultados del servicio Python. Ver la sección de OCR. | Servicio OCR |
| Agente IA | `POST /api/v1/agente/consultas` | Enviar una consulta en lenguaje natural (CU-14). | Todos los autenticados, según permisos |

Los roles de la tabla se derivan de los actores de las especificaciones de casos de uso. Las filas marcadas con permisos ampliados por la presencia de Administrador (por ejemplo, la lectura de movimientos) se confirman al cerrar la matriz de permisos de RF-USR-02.

### Autenticación y autorización

- La autenticación usa un JWT en la cabecera `Authorization: Bearer <token>`. Todas las operaciones requieren esta cabecera, salvo `POST /api/v1/sesiones`.
- Cada operación declara sus roles en la extensión `x-roles` del contrato (por ejemplo, `x-roles: [Almacenero, Administrador]`). El backend aplica esa lista mediante un mecanismo de autorización centralizado, de modo que la lista del contrato y la validación del backend provengan de una única fuente.
- El token contiene el identificador del usuario y su rol. El rol se verifica en cada operación; no se confía únicamente en la interfaz para ocultar acciones.
- La duración del token y el mecanismo de renovación quedan pendientes de definir.
- El acceso del agente de IA se controla con los mismos roles de cada operación (RF-IA-09).

### Formato de errores y catálogo de códigos

Los errores usan `application/problem+json` según RFC 9457. Cada respuesta incluye los campos estándar (`type`, `title`, `status`, `detail`, `instance`) y el campo de extensión `code`, que contiene el código de negocio estable. El campo `detail` se redacta en español y es el texto que se muestra al usuario. Los errores de validación de campos agregan la lista `errores` con `campo`, `codigo` y `mensaje`.

```json
{
  "type": "https://migal.example/problemas/stock-insuficiente",
  "title": "Stock insuficiente",
  "status": 409,
  "detail": "La existencia libre de Cemento Sol 42.5 kg no alcanza para la salida solicitada.",
  "code": "STOCK_INSUFICIENTE",
  "instance": "/api/v1/inventario/movimientos"
}
```

El dominio definitivo de los identificadores `type` queda pendiente. Se usa un dominio de ejemplo en el contrato hasta su definición.

Catálogo inicial de códigos:

| Código | Estado HTTP | Situación |
| --- | --- | --- |
| `NO_AUTENTICADO` | 401 | Falta el token o no es válido. |
| `PERMISO_INSUFICIENTE` | 403 | El rol del usuario no permite la operación. |
| `RECURSO_NO_ENCONTRADO` | 404 | El identificador no existe. |
| `SOLICITUD_MAL_FORMADA` | 400 | El cuerpo no es un JSON válido. |
| `VALIDACION_FALLIDA` | 422 | Uno o más campos no cumplen las reglas. Incluye `errores`. |
| `SKU_DUPLICADO` | 409 | El SKU ya existe en el catálogo. |
| `RUC_DUPLICADO` | 409 | El RUC ya pertenece a un proveedor. Incluye `proveedorId`. |
| `DOCUMENTO_CLIENTE_DUPLICADO` | 409 | El tipo y número de documento ya pertenecen a un cliente. Incluye `clienteId`. |
| `USUARIO_DUPLICADO` | 409 | El nombre de usuario ya existe. |
| `CLIENTE_INACTIVO` | 422 | El cliente está inactivo y no puede asociarse a una cotización o venta. |
| `CLIENTE_REQUERIDO` | 422 | La conversión de una cotización no tiene cliente asociado. |
| `PROVEEDOR_INACTIVO` | 422 | La orden de compra se dirige a un proveedor inactivo. |
| `STOCK_INSUFICIENTE` | 409 | Una salida o un traslado manual supera la existencia física del origen. |
| `VIABILIDAD_NO_CONFIRMADA` | 409 | La conversión requiere viabilidad confirmada por la propietaria de abastecimiento y no la tiene. |
| `COTIZACION_NO_EDITABLE` | 409 | La cotización no está en estado `Emitida`. |
| `COTIZACION_VENCIDA` | 409 | La cotización superó su fecha de vencimiento. |
| `COTIZACION_NO_CONVERTIBLE` | 409 | La cotización no está aceptada y validada. |
| `COTIZACION_YA_CONVERTIDA` | 409 | La cotización ya dio origen a una venta. |
| `VENTA_NO_CANCELABLE` | 409 | La venta está despachada o cancelada. |
| `VENTA_NO_DESPACHABLE` | 409 | La venta no está en estado `ListaDespacho`. |
| `GUIA_EXISTENTE` | 409 | La venta ya tiene una guía de despacho. |
| `RESERVA_INCOMPLETA` | 409 | Alguna línea no tiene reservada la cantidad completa. |
| `SALIDA_NO_CONFIRMABLE` | 409 | La guía no está en estado `Emitida`. |
| `ORDEN_COMPRA_CERRADA` | 409 | La orden de compra está completada. |
| `RECEPCION_NO_CONFIRMABLE` | 409 | La recepción no está en estado `Pendiente`. |
| `SOBRANTE_SIN_NOTA` | 422 | Se recibe más de lo pedido sin una nota explicativa. |
| `MOTIVO_REQUERIDO` | 422 | La operación exige un motivo no vacío (cancelación, reimpresión, ajuste). |
| `IDEMPOTENCIA_CONFLICTO` | 409 | La `Idempotency-Key` ya se usó con un cuerpo distinto. |
| `PETICION_CONCURRENTE` | 409 | Otra operación modifica el mismo inventario; se debe reintentar. |
| `DOCUMENTO_ILEGIBLE` | 422 | El OCR no pudo leer el documento. |
| `DOCUMENTO_NO_VALIDABLE` | 409 | El documento no está en estado `Procesado_OCR`. |
| `SERVICIO_NO_DISPONIBLE` | 503 | El servicio OCR o el agente no responden. |

### Paginación, filtrado y ordenamiento

- Los listados usan paginación por desplazamiento con los parámetros `page` (base 1) y `pageSize`. El valor predeterminado de `pageSize` es 20 y el máximo es 100.
- La respuesta paginada usa un sobre común: `items`, `total`, `page` y `pageSize`. El tipo `Page<T>` del frontend se amplía con los dos últimos campos, lo cual es una adición compatible.
- Los filtros usan nombres de parámetro en camelCase, según el campo que filtran. `q` es la búsqueda libre. Los campos de estado admiten valores múltiples separados por coma (`estado=PendienteDespacho,ListaDespacho`). Los rangos de fecha usan `desde` y `hasta` en `aaaa-mm-dd`, inclusivos.
- El ordenamiento usa el parámetro `orden` con la forma `campo:direccion` (`orden=fechaHora:desc,codigo:asc`). Cada recurso declara en el contrato la lista de campos ordenables y su orden predeterminado. Un campo fuera de la lista genera `VALIDACION_FALLIDA`.
- Los listados que no necesitan paginación en la versión 1 (estados de catálogos pequeños, como almacenes o categorías) devuelven un arreglo directamente. Esta excepción se documenta en cada operación.
- Los listados de Kardex, cotizaciones, ventas, documentos y disponibilidad se paginan. El filtrado se resuelve en el servidor, de acuerdo con RNF-06.

### Operaciones de estado e idempotencia

- Las transiciones de estado se modelan como sub-recursos de acción con método `POST`. Cada operación declara su estado de origen y su estado resultante en la descripción del contrato.
- Las operaciones que afectan inventario o reservas exigen la cabecera `Idempotency-Key` (UUID). Son las siguientes: registro de la conversión de cotización a venta, atención de venta, cancelación de venta, confirmación de salida de guía, confirmación de recepción y movimientos manuales.
- El backend guarda la respuesta asociada a cada clave durante un período que se fija al implementar. Un reintento con la misma clave y el mismo cuerpo devuelve la respuesta original con la cabecera `Idempotent-Replayed: true`. Una clave reutilizada con un cuerpo distinto devuelve `IDEMPOTENCIA_CONFLICTO`.
- Las respuestas de confirmación devuelven los efectos sobre inventario, como `cambios` con existencia antes y después por ubicación, para que el frontend pueda presentarlos sin consultar de nuevo.

### Reglas de negocio reflejadas en el contrato

Las reglas siguientes provienen del ADR-003, que aún no se encuentra integrado en el repositorio. Se citan como texto y el contrato las aplica de la siguiente manera:

- **Salida de Kardex solo al confirmar el despacho.** La salida por despacho se registra únicamente en `POST /guias-despacho/{id}/salida`. La generación de la guía y su reimpresión no producen movimientos. Los movimientos manuales de salida se registran con origen `Ajuste`, lo cual requiere confirmar la excepción con el ADR-003.
- **La venta solo reserva.** La atención de la venta (`POST /ventas/{id}/atencion`) reserva existencias libres y no genera movimientos de Kardex. Las cantidades no atendidas permanecen pendientes de abastecimiento.
- **Cliente obligatorio.** La conversión de cotización exige un cliente activo. La cotización puede emitirse sin cliente, de acuerdo con RF-CLI-04, pero no se convierte sin él.
- **Despacho único y completo.** Una venta tiene como máximo una guía de despacho (`GUIA_EXISTENTE`). La salida solo se confirma si todas las líneas tienen reservada la cantidad completa (`RESERVA_INCOMPLETA`).

Además, el contrato aplica las siguientes reglas del modelo:

- La cotización no reserva existencias en ninguna operación (RF-COT-08).
- La cancelación antes del despacho exige motivo, libera las reservas activas y no genera movimientos.
- Una recepción registra su almacén de destino por línea, actualiza la orden a `Parcial` o `Completada`, y solo genera movimientos de entrada al confirmarse.
- La validación de viabilidad de abastecimiento se acepta automáticamente en las categorías cemento, ladrillos, fierros y agregados. En las demás categorías requiere la confirmación explícita de la propietaria (`VIABILIDAD_NO_CONFIRMADA`).

### Endpoints para el agente de IA

- El agente de IA se expone mediante `POST /api/v1/agente/consultas`, con el cuerpo `{ "mensaje": "..." }` y una respuesta con `consultas`, `texto` e `incierta`.
- El contrato marca las operaciones que el agente puede invocar con la extensión `x-herramienta-agente: true`. Solo se marcan operaciones de lectura (`GET`). Una validación de CI verifica que ninguna operación marcada use un método distinto de `GET`.
- El agente no accede a la base de datos ni invoca operaciones de escritura. Las herramientas ejecutan las mismas operaciones del contrato con los permisos del usuario autenticado, de modo que una consulta nunca devuelve datos que el rol no puede ver (RF-IA-09).
- Cuando la información es insuficiente, la respuesta marca `incierta: true` y no incluye cantidades ni ubicaciones inventadas (RF-IA-08).

### Endpoints del OCR

- La carga del documento (`POST /api/v1/documentos`) recibe el archivo y el tipo de documento en `multipart/form-data`. Responde `202 Accepted` con el documento en estado `Pendiente` y su identificador, sin esperar el resultado del OCR.
- El servicio Python procesa el archivo y entrega los resultados mediante `PUT /api/v1/documentos/{id}/resultados`. Esta operación está marcada con la extensión `x-audiencia: servicio`, se autentica con una identidad de servicio con el alcance `ocr:resultados:escribir` y no se expone al frontend.
- El backend registra las líneas extraídas, con el texto original, el SKU sugerido, la similitud y la cantidad, y cambia el estado del documento a `Procesado_OCR`. Esta operación no crea ventas, reservas ni movimientos de Kardex.
- El usuario revisa las líneas y las confirma mediante `POST /api/v1/documentos/{id}/validacion`. Las correcciones se aplican antes de cambiar el estado a `Validado`. Las coincidencias por debajo del umbral se marcan para validación manual (RF-FUZ-05).
- El frontend consulta el estado del documento mediante `GET /api/v1/documentos/{id}` hasta que cambie a `Procesado_OCR`.

### Validación del contrato y generación del cliente

- El linter de OpenAPI se ejecuta en CI sobre `backend/api/openapi.yaml` y sus archivos referenciados. Un error de validación bloquea la integración.
- La comparación con la versión anterior se ejecuta en CI para detectar cambios incompatibles en `v1`. Un cambio incompatible sin nueva versión de ruta bloquea la integración.
- El cliente tipado del frontend se genera a partir del contrato y se versiona en el repositorio. Las funciones de `frontend/src/features/**/api.ts` conservan su firma y reemplazan su implementación simulada por llamadas al cliente generado.
- La generación del cliente se ejecuta en CI. Si el contrato cambia y el cliente generado no se actualiza, la verificación falla.
- Los errores de negocio del frontend se mapean por `code`, no por el texto del mensaje.

## Justificación

**Contract-first.** El frontend y el backend se desarrollan en paralelo sin esperar la implementación. El frontend ya simula la API en `mock.ts`, de modo que el contrato formaliza una interfaz que el equipo ya utiliza y permite reemplazar la simulación sin cambiar las vistas. Esto cumple RNF-14, porque los módulos se acoplan a una interfaz definida y no a la implementación de otro módulo.

**OpenAPI 3.1.** Es el estándar con el que las herramientas candidatas trabajan, y su alineación con JSON Schema permite describir los campos de negocio con restricciones verificables, como longitudes, rangos y enumeraciones.

**Un archivo por módulo.** La estructura refleja los paquetes del sistema y facilita que cada integrante revise y modifique su módulo sin conflictos en un archivo común. El archivo raíz centraliza los componentes compartidos, de modo que los errores y la paginación se describen una sola vez (RNF-14).

**Versionado por ruta.** Es visible en cada solicitud, fácil de auditar y compatible con los proxies y las herramientas habituales. Facilita coexistir con una versión anterior durante una transición.

**Recursos en español y en plural.** Mantiene la misma terminología del dominio que el modelo de clases, los casos de uso y el frontend. Los sub-recursos de acción expresan las transiciones de estado del modelo (`EstadoVenta`, `EstadoCotizacion`, `EstadoGuia`) sin introducir verbos genéricos en las rutas.

**camelCase en JSON y snake_case en SQL.** Es la convención del frontend y de los tipos de Java del modelo. Separar la persistencia del contrato permite cambiar el esquema de base de datos sin alterar las respuestas.

**JWT con roles por operación.** Cumple RNF-09 y RF-USR-03. Declarar los roles en el contrato permite revisarlos en la misma revisión que el resto de la interfaz y verificarlos automáticamente.

**RFC 9457.** Es un formato estándar, reconocido por las bibliotecas de Spring Boot, y separa un código estable para el software de un mensaje para el usuario. El frontend puede decidir su presentación por `code` sin depender del texto.

**Paginación por desplazamiento.** Es suficiente para los volúmenes de una ferretería con dos almacenes y se implementa de forma directa con las consultas paginadas de Spring Data. Resuelve los listados de RNF-06 sin introducir cursores opacos que el frontend tendría que gestionar.

**Acciones como sub-recursos `POST` con `Idempotency-Key`.** Las transiciones de estado son operaciones de negocio con efectos, no actualizaciones de un campo. La clave de idempotencia protege los movimientos de Kardex y las reservas ante reintentos de red, lo que evita movimientos repetidos mencionados en la [Arquitectura tecnológica](../arquitectura-tecnologica.md).

**Reglas del ADR-003 en el contrato.** La salida de Kardex solo al confirmar el despacho, la venta que solo reserva, el cliente obligatorio y el despacho único y completo se expresan como restricciones de las operaciones correspondientes. De este modo, el contrato no depende de que el frontend las aplique.

**Agente de solo lectura.** Reutilizar las mismas operaciones con los permisos del usuario garantiza que el agente no tenga un canal de escritura ni un acceso a datos que la interfaz no ofrezca, en línea con [Arquitectura tecnológica](../arquitectura-tecnologica.md).

**OCR con respuesta asíncrona y callback.** El microservicio Python procesa archivos de duración variable y no debe bloquear al usuario ni crear operaciones comerciales. Recibir sus resultados en un endpoint de servicio separa la extracción de la confirmación, que permanece bajo responsabilidad del usuario y del backend.

## Alternativas consideradas

| Alternativa | Motivo de descarte |
| --- | --- |
| Code-first: generar la especificación a partir de anotaciones en Spring | La documentación depende de la implementación y no permite revisar el contrato antes de programar. El frontend quedaría bloqueado hasta que el backend esté listo. |
| GraphQL | Añade un modelo de consultas y de caché que el equipo no usa, y no aporta ventajas frente a los listados paginados y las operaciones de negocio, que son mayoritariamente de escritura. |
| OpenAPI 3.0 | Su esquema no alinea con JSON Schema 2020-12, lo que limita la descripción de restricciones que el contrato necesita. |
| Versionado por cabecera o por tipo de contenido | Oculta la versión en la solicitud y dificulta su auditoría, las pruebas manuales y la coexistencia de dos versiones. |
| Versionado por cambios sin número de versión | Impide comunicar a los clientes los cambios incompatibles y complica la reversión de un módulo. |
| Nombres de recursos en inglés | Diverge de la terminología del modelo, de los casos de uso y del frontend, lo que aumenta el costo de mantener la correspondencia. |
| Verbos en las rutas (`/crearVenta`, `/confirmarSalida`) | Mezcla la semántica HTTP con las operaciones y no expresa el recurso afectado. Las acciones se modelan como sub-recursos `POST`. |
| Problem Details con formato propio (sin RFC 9457) | No es interoperable con las bibliotecas estándar y obliga al frontend a conocer un formato específico. |
| Paginación por cursor | Requiere que el frontend conserve cursores opacos. Los volúmenes actuales no lo justifican. |
| Importes como cadenas decimales | Obliga a convertir en cada operación de presentación y de cálculo en el frontend. Se mitiga redondeando en el servidor. |
| Un solo endpoint para toda la gestión de estados | Oculta las precondiciones de cada transición y dificulta asociar a cada una su rol y sus códigos de error. |
| Agente con acceso directo a la base de datos o con un token de sistema | Viola la restricción de la arquitectura de que el agente no acceda directamente a la base de datos y elimina la verificación de permisos por usuario (RF-IA-09). |
| OCR síncrono que devuelve las líneas en la misma respuesta | El procesamiento puede tardar más de lo aceptable para una respuesta HTTP y acopla al backend con la duración del servicio Python. |

## Consecuencias

**Favorables:**

- El frontend y el backend trabajan sobre una interfaz común desde el inicio, y el cliente tipado detecta en compilación las diferencias de contrato.
- Las reglas del ADR-003 y los roles de cada operación se revisan en el contrato, en el mismo proceso de revisión que el resto de la interfaz.
- Los cambios incompatibles se detectan en CI antes de integrarse.
- El agente de IA y el OCR no introducen rutas de escritura ni de creación de operaciones comerciales fuera del backend.
- Los errores de negocio tienen un código estable, lo que permite pruebas automatizadas y mensajes consistentes.

**Desfavorables:**

- El contrato requiere mantenimiento continuo. Cada cambio en un módulo exige actualizar el archivo correspondiente y regenerar el cliente. Para mitigarlo, se valida en CI.
- La discrepancia entre el frontend actual y el contrato implica cambios en la capa de acceso y en varias pantallas: recepción en dos pasos, venta solo por conversión, ubicaciones por identificador, autenticación con token y paginación en todos los listados. Se registra la lista de cambios en la tarea correspondiente.
- La herramienta de generación de Spring y el cliente tipado introducen dependencias y un proceso de build adicional, que se documenta en el README de cada componente.
- Las decisiones abiertas (duración del token, renovación, modalidad de atención, dominio de los identificadores `type`, período de retención de `Idempotency-Key`, excepción de los movimientos de ajuste y conciliación de la entrega parcial con PC2) quedan pendientes y afectan a la implementación. Se resuelven antes de la primera versión publicada del contrato.
- El uso de `POST` para las acciones de estado requiere que los clientes externos, si los hubiera, conozcan la convención. Se documenta en la guía de uso del contrato.

## Referencias

- [Arquitectura tecnológica](../arquitectura-tecnologica.md)
- [Casos de uso](../casos-de-uso.md)
- [Especificaciones de casos de uso](../especificaciones-casos-de-uso.md)
- [Modelo y diccionario de clases](../modelo-y-diccionario-clases.md)
- [Modelo y diccionario de datos](../modelo-y-diccionario-datos.md)
- [Requerimientos](../requerimientos.md)
- [ADR-001: Stack tecnológico del frontend](adr-001-stack-frontend.md)
- ADR-003: reglas de Kardex, reservas, cliente obligatorio y despacho único (documento pendiente de integración, citado como texto)
- RFC 9457: Problem Details for HTTP APIs
- OpenAPI Specification 3.1
