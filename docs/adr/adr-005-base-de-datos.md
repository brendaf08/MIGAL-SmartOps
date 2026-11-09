# ADR-005: Base de datos: PostgreSQL, migraciones y concurrencia

## Contexto

La [Arquitectura tecnológica](../arquitectura-tecnologica.md) establece PostgreSQL como sistema gestor de la capa de persistencia y asigna al backend Spring Boot el control transaccional del inventario. El [Modelo y diccionario de datos](../modelo-y-diccionario-datos.md) define entidades, atributos, relaciones y restricciones, y el [Diagrama entidad-relación](../imagenes/MIGAL-DER-01-modelo-entidad-relacion.png) representa esa estructura. La carpeta [database](../../database/README.md) no contiene aún un esquema, y la [carpeta de migraciones](../../database/migraciones/README.md) espera la definición de la herramienta y de la convención.

La decisión debe satisfacer los siguientes requerimientos:

| Requerimiento | Implicancia para la base de datos |
| --- | --- |
| RNF-01 | Uso de PostgreSQL como gestor relacional. |
| RNF-06 | Consultas de inventario, compras, cotizaciones y ventas con respuesta no mayor a 3 segundos. Exige índices sobre las claves foráneas y consultas de disponibilidad acotadas. |
| RNF-08, RNF-15 | Operaciones de inventario y atención de ventas atómicas, sin actualizaciones parciales. |
| RNF-10, RF-KAR-05, RF-KAR-11 | Cada movimiento de Kardex registra fecha, usuario, tipo y operación de origen. |
| RNF-12 | Mecanismos periódicos de respaldo. |
| RNF-16, RF-KAR-06, RF-VEN-16 | Impedir existencias negativas y compromisos superiores al stock libre. |
| RF-PRO-04, RF-PROV-06 | Unicidad del SKU y del RUC, y uso exclusivo de proveedores activos en nuevas órdenes. |
| RF-CLI-08, RF-PROV-10, RF-USR-04 | Desactivación de registros sin pérdida del historial operativo. |
| RNF-09 | Acceso a la base de datos restringido a los componentes autorizados. |
| RNF-14 | Cambios de esquema versionados y revisables, que mantengan desacopladas las capas. |

Las reglas de negocio sobre ventas, reservas, Kardex y despacho se establecen en el ADR-003 (Reglas de ventas, reservas, Kardex y despacho). Ese registro aún no se encuentra integrado en la rama principal, por lo que se cita como texto y no se enlaza. Este ADR adopta sus reglas y las traduce a restricciones del esquema y a la estrategia de concurrencia.

### Divergencias entre requerimientos y modelo

Los requerimientos funcionales contienen reglas que el modelo no respalda. Las principales son:

- RF-KAR-04, RF-VEN-04 y RF-VEN-14 generan la salida de Kardex al confirmar la venta, mientras que la arquitectura y el modelo la generan al confirmar el despacho.
- RF-VEN-12 y RF-VEN-14 admiten atención y entrega parcial, mientras que la arquitectura exige un despacho completo.
- RF-CLI-03, RF-CLI-04, RF-CLI-06, RF-VEN-01 y RF-VEN-02 definen el cliente como opcional, mientras que el modelo lo exige en cotizaciones y ventas.
- RF-COT-05 define los estados Emitida, Aceptada, Rechazada y Vencida, mientras que el modelo incorpora la validación de viabilidad por MIGAL.

### Divergencias entre el DER y el diccionario

La imagen del DER es más reciente que el diccionario. La comparación de ambos muestra coincidencia en entidades, atributos, tipos derivados y cardinalidades principales. Se identifican las siguientes diferencias, que se corrigen en un pull request posterior que actualice ambos documentos:

1. Notación de claves: el DER subraya `id_cotizacion` en Venta, `id_venta` en Guia_Despacho, `sku`, `ruc` y `username` con el mismo trazo que las claves primarias. El diccionario declara `id_cotizacion` e `id_venta` como claves foráneas únicas. La notación no distingue clave primaria, clave alterna y clave foránea.
2. Responsable de la cancelación: ni el DER ni el diccionario registran el usuario que autoriza la cancelación de una venta. El diccionario exige conservar la evidencia del usuario autorizante, la fecha, la operación y el motivo.
3. Responsable de la confirmación del despacho: `id_usuario_responsable` de Guia_Despacho figura como opcional en ambos documentos. La confirmación de la salida física debe identificar al responsable (RNF-10, RF-USR-04).
4. Relación Reserva_Registrada: el DER contiene una relación con ese nombre que el diccionario no lista entre las relaciones sin atributos. Su extremo no se pudo verificar con certeza en la imagen.
5. Nombres de relaciones: se utilizan nombres distintos para relaciones equivalentes, como Genera (diccionario) y Genera_Guia (DER), Genera_Salida y Salida_Por_Guia, Genera_Ingreso e Ingreso_Por_Recepcion, Afecta y Afecta_Inventario, Tiene_Ordenes y Vende_Proveedor, Realiza_Venta y Realiza, o Solicita_Cotizacion y Solicita.
6. Estados de la reserva: el diccionario enumera Activa, Liberada y Consumida sin definir sus transiciones.
7. Claves de Detalle_Recepcion y Detalle_Cotizacion: la notación de la clave primaria compuesta no pudo verificarse en la imagen con la resolución disponible.

## Decisión

### Motor de base de datos

Se adopta PostgreSQL como único sistema gestor de la base de datos del proyecto (RNF-01). La imagen Docker se fija con versión mayor y menor explícitas, correspondientes a la versión mayor estable más reciente vigente al integrar el pull request de infraestructura. No se utilizan etiquetas flotantes como `latest`. Las actualizaciones de versión mayor requieren un ADR propio.

### Herramienta de migraciones

Se utiliza Flyway Community Edition con scripts SQL planos. Los scripts se ubican exclusivamente en `database/migraciones/` y cumplen las siguientes reglas:

- Nombre: `V<número>__<descripción>.sql`, con número entero correlativo y descripción en minúsculas, sin tildes y con palabras separadas por guion bajo. Por ejemplo, `V1__crear_catalogo_producto.sql`.
- Alcance: una migración por dominio y por pull request. Los dominios son usuarios y roles, catálogo, clientes, proveedores, inventario, comercial, abastecimiento, despacho y documentos OCR.
- Inmutabilidad: una migración integrada no se modifica. Flyway valida la suma de comprobación y falla ante cualquier cambio. Las correcciones se realizan con una migración nueva.
- Numeración: antes de integrar, un pull request se renumera sobre `main` si su número ya fue ocupado. Un número integrado no se cambia.
- Sin reversión automática: no se utilizan migraciones de deshacer, que Flyway Community no ofrece. Una reversión se resuelve con una migración compensatoria o con una restauración controlada desde respaldo.
- Privilegios: la ejecución de migraciones utiliza una cuenta distinta de la cuenta de la aplicación, con privilegios de definición de datos. La aplicación opera solo con privilegios de consulta y manipulación de datos. La integración con el backend se define en el ADR de arquitectura del backend.

### Convenciones de nombres y tipos

- Identificadores en `snake_case`, en minúsculas y sin tildes. Las tablas se nombran en singular (`usuario`, `guia_despacho`, `movimiento_kardex`) y los atributos conservan los nombres del diccionario.
- Claves primarias de tipo `bigint` con identidad generada por el motor (`GENERATED ALWAYS AS IDENTITY`), nombradas `id_<entidad>`. Las claves foráneas conservan el nombre de la clave referenciada.
- Instantes de registro y operación: `timestamptz`. Fechas sin hora, como `fecha_vencimiento`, `fecha_entrega_estimada`, `fecha_despacho` y `fecha_recepcion`: `date`.
- Cantidades: `numeric(14,3)`, conforme al diccionario, para admitir unidades fraccionarias como kilogramos y metros.
- Importes y precios unitarios (`precio_referencial`, `precio_unitario`, `costo_unitario`, `precio_compra`, `descuento`, totales): `numeric(14,2)`. Se asume una moneda única, por confirmar.
- Estados: `varchar` con longitud explícita y restricción CHECK que enumera los valores válidos, escritos en minúsculas y sin tildes (por ejemplo, `pendiente_abastecimiento`).
- Indicadores de vigencia: `boolean NOT NULL`, como `estado_activo`.
- Obligatoriedad: las columnas obligatorias del diccionario son `NOT NULL`; las opcionales admiten nulos.
- Restricciones con nombre explícito (prefijos `pk_`, `fk_`, `uq_`, `ck_` e `ix_`), para que los mensajes de error identifiquen la regla vulnerada.
- Índices: toda columna de clave foránea tiene índice. Se exceptúa la columna líder de una clave primaria compuesta que ya la cubre. Se agregan índices compuestos para las consultas críticas de inventario y Kardex.

### Integridad de datos

- Restricciones CHECK de no negatividad y positividad: `cantidad_fisica >= 0`; `cantidad_vendida`, `cantidad_reservada`, `cantidad_comprada` y la cantidad de movimiento de Kardex mayores que cero; `cantidad_recibida >= 0`; precios, costos y descuentos mayores o iguales a cero.
- Coherencia por estado y origen: `motivo_cancelacion` es obligatorio cuando la venta se encuentra cancelada; el motivo es obligatorio en ajustes, mermas y traslados; `id_guia` es obligatorio cuando el origen del movimiento es despacho, y `id_recepcion` cuando es recepción.
- Unicidad: `sku` en producto; `ruc` en proveedor; `username` en usuario; la combinación de `id_producto` e `id_almacen` en inventario; `id_cotizacion` en venta; `id_venta` en guía de despacho.
- Claves foráneas con acción `RESTRICT` en actualización y eliminación. No se utiliza eliminación en cascada.
- Consistencia de producto: las reservas referencian el detalle de venta y el inventario mediante claves foráneas compuestas que incluyen `id_producto`. Así, una reserva solo puede corresponder al producto de su línea de venta.
- Desactivación lógica: usuario, cliente, producto, almacén y proveedor utilizan `estado_activo`. La aplicación no elimina filas de entidades de negocio. La eliminación física se reserva a los datos de desarrollo.
- Restricciones entre filas: una restricción CHECK no puede comparar sumas de otras filas. Por ello, las siguientes invariantes se implementan mediante disparadores de restricción diferibles, que se verifican al confirmar la transacción:
  - La suma de reservas activas de un inventario no supera su `cantidad_fisica`.
  - La suma de reservas activas de una línea de venta no supera su `cantidad_vendida`.

### Valores derivados

- No se almacenan como columnas: `cantidad_reservada` y `cantidad_libre` de inventario; `cantidad_reservada_actual` y `cantidad_pendiente_abastecimiento` de detalle de venta; `subtotal_detalle`; `total_venta`; `total_cotizacion`; y `cantidad_pendiente_recepcion`.
- Se obtienen mediante vistas, como la disponibilidad por inventario y los pendientes por línea de venta, y mediante consultas dentro de las transacciones que las requieren.
- Excepción: `cantidad_fisica` se almacena. Es el saldo que se actualiza en la misma transacción del movimiento de Kardex, bajo bloqueo de la fila, y es la columna que protege la restricción de no negatividad. Una consulta de conciliación compara ese saldo con la suma de los movimientos.
- `precio_unitario` y `costo_unitario` se conservan como valores históricos de cada operación.
- Si una vista no cumple RNF-06, se evalúa en un ADR posterior una vista materializada o una columna controlada.

### Kardex de movimientos

- `movimiento_kardex` es de solo anexión. El rol de aplicación tiene únicamente privilegios de inserción y consulta sobre la tabla. Un disparador rechaza cualquier actualización o eliminación como defensa adicional.
- Las correcciones se registran con un movimiento compensatorio de tipo opuesto, origen `Ajuste` y motivo obligatorio. Se propone agregar el atributo `id_movimiento_corregido`, que referencia al movimiento corregido. El cambio se incorpora al DER y al diccionario en el mismo pull request.
- Solo generan movimientos la confirmación de una recepción (entrada), la confirmación de un despacho (salida), los ajustes, las mermas y los traslados. Un traslado genera una salida y una entrada vinculadas por `referencia_traslado`.

### Reglas de negocio adoptadas del ADR-003

- La salida de Kardex se registra únicamente al confirmar la salida física del despacho.
- La aprobación de una venta solo genera reservas de stock.
- Toda cotización y toda venta se asocian a un cliente registrado. Las operaciones de mostrador utilizan un registro de cliente cuyo documento es opcional.
- El despacho es único y completo por venta, y solo se autoriza cuando todas las líneas están completamente reservadas. No existe despacho parcial.
- La cancelación antes del despacho exige motivo, libera las reservas activas y no genera movimientos de Kardex.

Los estados de la reserva son los siguientes: `activa` al crearse; `consumida` al confirmar el despacho, en la misma transacción que la salida de Kardex; y `liberada` por cancelación. Solo una reserva activa puede cambiar de estado.

### Concurrencia e idempotencia

- Mecanismo: bloqueo pesimista mediante `SELECT ... FOR UPDATE` sobre las filas de inventario, con nivel de aislamiento `READ COMMITTED`. No se utiliza `SERIALIZABLE`.
- Orden de bloqueo: en todas las operaciones se bloquea primero la fila de la venta, la guía o la orden de recepción, y después las filas de inventario en orden ascendente de `id_inventario`. Este orden global evita interbloqueos entre procesos.
- Reserva de una venta:
  1. Bloquear la venta y verificar que se encuentra activa.
  2. Identificar las líneas con cantidad pendiente.
  3. Bloquear los inventarios candidatos en orden ascendente.
  4. Calcular el stock libre como cantidad física menos reservas activas.
  5. Crear reservas activas por la cantidad asignable en cada ubicación. La prioridad entre ubicaciones se define en el backend.
  6. Actualizar el estado de la venta y confirmar la transacción.
- Confirmación de despacho:
  1. Bloquear la guía. Si la salida ya se encuentra confirmada, no se generan movimientos y se devuelve el resultado existente.
  2. Verificar que todas las líneas de la venta se encuentran completamente reservadas.
  3. Bloquear los inventarios en orden ascendente.
  4. Crear los movimientos de salida, descontar la cantidad física y marcar las reservas como consumidas.
  5. Actualizar la guía y la venta, y confirmar la transacción.
- Confirmación de recepción: la misma secuencia aplicada a la orden de recepción, con movimientos de entrada. La orden de compra pasa a Parcial o Completada según las cantidades recibidas.
- Cancelación antes del despacho: bloquear la venta, registrar el motivo y el usuario autorizante, y liberar las reservas activas. No genera movimientos.
- Defensa final: la restricción de no negatividad de `cantidad_fisica` y los disparadores diferibles impiden que un error de código o una escritura directa violen las reglas. Si una verificación falla, la transacción completa se revierte (RNF-08).
- Reintentos: el backend reintenta una transacción ante un interbloqueo o un tiempo de espera de bloqueo, con un número limitado de intentos. Los tiempos de espera y el número de intentos se definen en el ADR del backend.
- Idempotencia: el cambio de estado y los movimientos se confirman en la misma transacción. Una solicitud repetida encuentra el estado ya cambiado y no duplica movimientos. La unicidad de `id_venta` en la guía impide generar una segunda guía para la misma venta.

### Semillas y datos de prueba

- Las semillas de desarrollo se ubican en `database/semillas/` (ubicación propuesta), separadas de `database/migraciones/`. Flyway no lee esa carpeta.
- Contienen únicamente datos ficticios. Las credenciales de prueba se configuran fuera del repositorio o en archivos de ejemplo con valores ficticios, conforme a la sección 11 de las [Reglas de contribución](../../CONTRIBUTING.md).
- Su carga requiere un perfil de desarrollo explícito y nunca se ejecuta en producción.

### Respaldo y recuperación

- Requisito: copias de respaldo periódicas (RNF-12), restauración verificada de forma periódica y capacidad de recuperación a un punto en el tiempo, para no perder operaciones de inventario confirmadas.
- La frecuencia, la retención, la ubicación, el cifrado y los procedimientos se definen en el ADR de despliegue, pendiente de redacción.

### Gestión de divergencias

- Ante una diferencia entre el DER y el diccionario, prevalece el DER por ser la versión más reciente, salvo que un ADR aceptado indique otra cosa.
- Toda modificación del modelo actualiza el diccionario y el DER en el mismo pull request, y actualiza la matriz de trazabilidad cuando cambian sus correspondencias.
- La fuente editable del DER se registra en el documento correspondiente, conforme a la sección 9.3 de las [Reglas de contribución](../../CONTRIBUTING.md).

## Justificación

**PostgreSQL.** Cumple RNF-01 y ofrece transacciones ACID, restricciones diferibles, disparadores, índices parciales y bloqueos de fila. Estas capacidades sostienen RNF-08 y RNF-16 y permiten trasladar las reglas críticas a la base de datos, como exige la arquitectura tecnológica en su sección de persistencia.

**Flyway con SQL plano.** El SQL plano permite revisar cada cambio por pares y usar las características de PostgreSQL sin una capa de abstracción, lo que favorece la mantenibilidad (RNF-14). La validación por suma de comprobación implementa la inmutabilidad de las migraciones integradas, y la integración con Spring Boot reduce la configuración. Liquibase ofrece reversiones, pero la reversión automática de cambios de datos no es confiable; por ello, no justifica su complejidad adicional.

**Claves bigint con identidad.** Ocupan 8 bytes frente a los 16 de un UUID, mejoran la localidad de los índices B-tree y reducen el costo de las uniones de las consultas de inventario (RNF-06). El sistema es centralizado y una sola base de datos genera los identificadores, por lo que no se requiere generación distribuida. El riesgo de enumeración de registros se mitiga con autorización por rol (RNF-09) y con códigos de negocio para los documentos visibles al usuario.

**Tipos numéricos exactos.** `numeric` evita los errores de redondeo de los tipos de punto flotante en importes y saldos. Las precisiones del diccionario cubren unidades fraccionarias como kilogramos y metros.

**Estados con CHECK.** Un tipo ENUM de PostgreSQL requiere modificaciones de tipo para agregar valores y no permite quitarlos con facilidad. Una restricción CHECK se modifica dentro de una migración transaccional. La lista de estados es corta y cerrada, por lo que no justifica una tabla de catálogo con uniones adicionales.

**Valores derivados.** Almacenar cantidades libres o totales duplicaría saldos que cambian en cada reserva y en cada movimiento, y exigiría mantenerlos sincronizados en todos los caminos de escritura (RF-COM-05, RF-COT-08). Calcularlos bajo bloqueo es correcto porque todas las escrituras de reservas de un inventario pasan por la misma fila bloqueada. La excepción de `cantidad_fisica` se justifica porque es el saldo físico que protege la restricción de no negatividad (RF-KAR-06, RF-VEN-16).

**Kardex de solo anexión.** Un registro inmutable de movimientos constituye evidencia verificable (RNF-10, RF-KAR-11). Además, las cancelaciones no requieren reversiones en el Kardex, como establece el ADR-003. Con un rol de aplicación sin privilegios de modificación, un error de código no altera el historial.

**Bloqueo pesimista con orden global.** Una venta con varias líneas reserva en la misma transacción filas de varios inventarios. Un control optimista con reintentos resulta costoso cuando varias ventas compiten por el mismo producto, y su complejidad aumenta con las reservas multilínea. El bloqueo pesimista con orden ascendente produce esperas acotadas y evita interbloqueos. El volumen de una ferretería de esta escala no justifica una estrategia más compleja.

**Defensa en capas.** El bloqueo garantiza la regla en el backend. Las restricciones y los disparadores impiden que un defecto de código o una escritura directa la violen, lo que cumple RNF-16.

**Idempotencia por estado.** Como el cambio de estado y los movimientos se confirman juntos, un reintento tras una falla de red no duplica movimientos y no requiere tablas adicionales de claves de idempotencia.

**Semillas separadas.** La separación evita que datos de prueba lleguen a producción y mantiene reproducibles las migraciones.

**Respaldo.** RNF-12 es obligatorio. La estrategia operativa depende de la infraestructura y se define en el ADR de despliegue.

## Alternativas consideradas

| Alternativa | Motivo de descarte |
| --- | --- |
| Otros motores relacionales (MySQL, MariaDB, SQL Server, Oracle) | Incumplen RNF-01, que exige PostgreSQL. |
| Base de datos documental o NoSQL | Incumple RNF-01. El modelo es relacional y requiere transacciones que abarcan varias entidades. |
| Liquibase | Ofrece reversiones y varios formatos de cambio, pero añade una capa de abstracción sin una ventaja decisiva para un equipo que trabaja con SQL. La reversión automática de datos no es confiable. |
| Generación automática del esquema desde el ORM o scripts manuales sin herramienta | No mantienen historial versionado ni revisión de cambios, y el esquema depende del modelo de clases y no de una revisión explícita. |
| Claves UUID v4 | Generan índices fragmentados, ocupan 16 bytes y no tienen orden temporal. No existe generación distribuida que lo justifique. Se reevalúa UUIDv7 si se exponen identificadores externos. |
| Claves con secuencia tradicional (serial) | Mecanismo anterior a la identidad estándar. La identidad evita la asignación manual de valores. |
| Tipo ENUM de PostgreSQL | Modificar sus valores requiere cambios de tipo y no permite quitar valores con facilidad. |
| Tabla de catálogo de estados | Garantiza integridad y permite extender los estados, pero añade uniones en consultas frecuentes para valores estables. Se reevalúa si la cantidad de estados crece. |
| Almacenar cantidades reservadas y libres | Introduce riesgo de desincronización y exige mantenimiento en cada escritura. Se reconsidera solo si una medición demuestra el incumplimiento de RNF-06. |
| Bloqueo optimista con columna de versión | Genera reintentos frecuentes cuando varias ventas compiten por el mismo producto, y complica las reservas multilínea. |
| Aislamiento SERIALIZABLE | Produce fallos de serialización frecuentes y reintentos de toda la transacción, con un costo mayor que los bloqueos explícitos. |
| Eliminación física de registros desactivados | Pierde el historial, lo que contradice RF-PROV-10, RF-CLI-08 y RNF-10. |

## Consecuencias

**Favorables:**

- La integridad del inventario queda protegida por la base de datos y no solo por la interfaz o el código de la aplicación.
- El Kardex es verificable y las cancelaciones no requieren movimientos de reversión.
- Las migraciones tienen historial en el repositorio y validación automática de integridad.
- Los valores derivados no pueden desincronizarse de sus fuentes.
- El orden global de bloqueo reduce los interbloqueos y hace predecible el comportamiento bajo concurrencia.

**Desfavorables:**

- Las reservas de productos de alta rotación pueden generar esperas. Mitigación: transacciones cortas, tiempos de espera de bloqueo configurados, reintentos acotados y medición de RNF-06 en pruebas de carga.
- Las vistas de disponibilidad calculan sumas en cada consulta. Mitigación: índices sobre las reservas activas por inventario y evaluación posterior de vistas materializadas.
- Los disparadores diferibles añaden lógica fuera de la aplicación. Mitigación: documentarlos en [database/README.md](../../database/README.md) y cubrirlos con pruebas específicas.
- Flyway Community no ofrece reversiones; una migración errónea se corrige con otra migración. Mitigación: revisión por pares y aplicación previa en una base de desarrollo.
- Los identificadores secuenciales permiten inferir volúmenes y enumerar registros. Mitigación: autorización por rol (RNF-09) y códigos de negocio para los documentos visibles.
- El esquema completo depende de la integración del ADR-003 y de los cambios pendientes en el DER y en el diccionario, entre ellos `id_usuario_autorizante`, `id_movimiento_corregido`, la notación de claves y los nombres de relaciones. Mientras no se integren, el esquema no puede implementarse de forma completa.
- Las cuentas separadas de migración y de aplicación aumentan la configuración del despliegue. Mitigación: definirlas en el ADR de despliegue.

## Referencias

- [Arquitectura tecnológica](../arquitectura-tecnologica.md)
- [Requerimientos](../requerimientos.md)
- [Modelo y diccionario de datos](../modelo-y-diccionario-datos.md)
- [Diagrama entidad-relación](../imagenes/MIGAL-DER-01-modelo-entidad-relacion.png)
- [Base de datos](../../database/README.md)
- [Migraciones](../../database/migraciones/README.md)
- [Reglas de contribución](../../CONTRIBUTING.md)
- ADR-003, Reglas de ventas, reservas, Kardex y despacho (pendiente de integración; citado como texto).
