# ADR-004: Arquitectura y stack del backend

## Contexto

El [ADR-001](adr-001-stack-frontend.md) define el frontend como una Single Page Application que consume la API REST del backend. La [Arquitectura tecnológica](../arquitectura-tecnologica.md) define el backend principal como núcleo transaccional implementado en Java con Spring Boot, responsable de aplicar las reglas de negocio, de mantener la consistencia del inventario y de persistir la información en PostgreSQL.

Quedan pendientes la plataforma de ejecución, el gestor de construcción, la organización interna del código, el acceso a datos, la seguridad, la estrategia de pruebas y la integración continua del backend. Los requerimientos que intervienen son los siguientes:

| Requerimiento | Implicancia para el backend |
| --- | --- |
| RNF-01 | Persistencia en PostgreSQL como sistema gestor de base de datos relacional. |
| RNF-02 | Implementación en Java con Spring Boot y exposición de servicios mediante API REST. |
| RNF-03 | Procesamiento OCR y Fuzzy Matching delegado en el microservicio Python. |
| RNF-04 | Despliegue de los componentes mediante contenedores Docker. |
| RNF-08, RNF-15 | Ejecución transaccional de las operaciones que modifican inventario, reservas y confirmaciones. |
| RNF-09, RF-USR-01, RF-USR-02, RF-USR-03 | Autenticación personal y autorización según los roles Administrador, Vendedor, Almacenero y Reponedor. |
| RF-USR-04, RNF-10 | Registro del usuario responsable en cada operación y trazabilidad de los movimientos de Kardex. |
| RNF-14 | Módulos principales desacoplados mediante interfaces definidas. |
| RF-IA-03, RF-IA-04, RF-IA-09 | Consultas del Agente de IA mediante funciones autorizadas del backend, sin modificación de datos transaccionales. |

Los criterios para evaluar las opciones son el cumplimiento de los requerimientos citados, la integridad transaccional del inventario, el desacoplamiento entre módulos, la simplicidad y convención del ecosistema elegido, y la correspondencia con las clases definidas en el [Modelo y diccionario de clases](../modelo-y-diccionario-clases.md).

Las reglas de inventario, venta y despacho se establecen en el [ADR-003](adr-003-reglas-ventas-reservas-despacho.md). Los requerimientos RF-KAR-04 y RF-VEN-04 describen la salida de Kardex al confirmar la venta, RF-VEN-12 y RF-VEN-14 contemplan atención parcial, y RF-CLI-03 y RF-CLI-06 admiten ventas sin cliente. Estos puntos se resuelven en el ADR-003, y la actualización de los requerimientos se realiza en un cambio independiente.

## Decisión

Implementar el backend como un monolito modular con Java y Spring Boot, con los siguientes elementos:

| Elemento | Decisión |
| --- | --- |
| Plataforma | Java en su versión LTS vigente y Spring Boot en su versión estable vigente. Las versiones exactas se fijan en el pull request que genera el esqueleto del backend y se registran en el [README del backend](../../backend/README.md). |
| Gestor de construcción | Maven, con Maven Wrapper incluido en el repositorio. |
| Organización | Monolito modular con un paquete raíz por módulo funcional y, dentro de cada uno, las capas `controller`, `service`, `repository` y `model`. |
| Persistencia | Spring Data JPA con Hibernate sobre PostgreSQL. El esquema se gobierna mediante migraciones en `database/migraciones/` y Hibernate opera en modo `validate`. |
| Seguridad | Spring Security con autenticación mediante JWT de acceso y autorización por endpoint y por método. |
| Transacciones | Las operaciones sobre inventario se ejecutan en transacciones controladas por la capa de servicios. El detalle del bloqueo se define en el ADR-005. |
| Pruebas | JUnit 5, Mockito y Testcontainers con PostgreSQL real. |
| Integración continua | Flujo `verificar-backend` en GitHub Actions, análogo al flujo `verificar-frontend`. |
| Agente de IA | Expone funciones de consulta autorizadas. No accede a la base de datos. |
| Configuración | Parámetros externos mediante variables de entorno. El repositorio no contiene credenciales. |

### Organización modular

Los paquetes raíz corresponden a los módulos funcionales definidos en [PAQ-02](../diagramas-paquetes.md#paq-02): `seguridad`, `catalogo`, `inventario`, `documentosocr`, `comercial`, `despacho`, `abastecimiento` y `agenteia`. Los nombres de los paquetes Java se escriben sin guiones, conforme a la convención del lenguaje.

Las dependencias entre módulos se limitan a las declaradas en PAQ-02. Se establecen las siguientes reglas:

- Un módulo accede a los datos de otro únicamente mediante la capa `service` del módulo propietario. Los repositorios son de uso interno de su módulo.
- Las dependencias entre capas siguen una sola dirección: `controller` utiliza `service`, `service` utiliza `repository` del mismo módulo y los servicios de los módulos declarados, y todas las capas utilizan `model`. Ningún `controller` accede a un `repository`.
- Solo el módulo `inventario` modifica existencias físicas, reservas y movimientos de Kardex. Los demás módulos invocan sus servicios.
- El módulo `seguridad` es utilizado por todos los módulos para autenticación, autorización y registro del usuario responsable, según PAQ-02.
- La comunicación con el microservicio Python se encapsula en el módulo `documentosocr`.
- La capa `model` no depende de ninguna otra capa, y corresponde a la capa de dominio descrita en el modelo de clases. Las entidades se implementan como entidades JPA.

Las dependencias se verifican mediante pruebas automatizadas de la estructura de paquetes que se ejecutan en la integración continua.

### Persistencia

Los servicios utilizan repositorios de Spring Data JPA sobre PostgreSQL. Los cambios de esquema se registran como migraciones versionadas en `database/migraciones/`, según lo establecido en las [Reglas de contribución](../../CONTRIBUTING.md). Hibernate únicamente valida que el mapeo coincida con el esquema y no modifica la base de datos. La herramienta de migraciones se define en el pull request del esqueleto del backend.

### Seguridad

- La autenticación se realiza con credenciales personales. Las contraseñas se almacenan como hash en el atributo `passwordHash` de la entidad `Usuario`.
- Tras una autenticación correcta, el backend emite un JWT de acceso firmado. Su duración y la clave de firma se configuran mediante variables de entorno. Esta decisión no incorpora un token de refresco.
- Los roles corresponden a la enumeración `RolUsuario`: `Administrador`, `Vendedor`, `Almacenero` y `Reponedor`.
- La autorización se aplica en dos niveles: por endpoint, mediante las reglas de `SecurityFilterChain`, y por método, mediante anotaciones sobre los métodos de la capa `service`. La matriz de permisos por rol se deriva de las [Especificaciones de casos de uso](../especificaciones-casos-de-uso.md).
- El usuario responsable de cada operación se obtiene del contexto de seguridad autenticado y no de los datos enviados en la solicitud.

### Transacciones y concurrencia

Los límites transaccionales se definen en la capa `service`, nunca en los controladores. Las operaciones que se ejecutan de forma transaccional son las siguientes:

- Reserva de existencias para una venta.
- Confirmación de una recepción de mercadería.
- Traslado interno entre almacenes.
- Confirmación de la salida física en el despacho.
- Cancelación autorizada antes del despacho.

El mecanismo de bloqueo que evita reservas duplicadas, cantidades negativas y movimientos de Kardex repetidos se define en el ADR-005. El presente registro solo establece que dichas operaciones son transaccionales.

### Reglas de negocio fijadas por el ADR-003

Se mencionan a continuación las reglas establecidas en el [ADR-003](adr-003-reglas-ventas-reservas-despacho.md), que el backend debe respetar:

- La salida de Kardex se registra únicamente al confirmar el despacho.
- La venta solo reserva existencias y no genera movimientos de salida.
- El cliente es obligatorio en la venta.
- El despacho es único y completo, sin entregas parciales.

### Agente de IA

El módulo `agenteia` expone al Agente de IA un conjunto de funciones de consulta sobre los módulos `inventario` y `catalogo`. Cada función valida los permisos del usuario autenticado antes de ejecutarse. El conjunto de funciones no incluye operaciones de escritura. El agente no accede a la base de datos y no modifica datos transaccionales.

### Configuración

Las credenciales, las cadenas de conexión, la clave de firma de los JWT y la dirección del microservicio Python se leen desde variables de entorno. Los archivos de configuración del repositorio contienen únicamente valores ficticios o referencias a dichas variables, según las [Reglas de contribución](../../CONTRIBUTING.md).

### Integración continua

El flujo `verificar-backend` se activa en los pull requests hacia `main` que modifican `backend/` o su propio archivo de flujo. Ejecuta la compilación y las pruebas del backend con Maven. Testcontainers requiere Docker, disponible en el runner `ubuntu-latest`.

## Justificación

**Monolito modular.** La arquitectura tecnológica establece un núcleo transaccional único, y RNF-08 y RNF-15 exigen actualizaciones atómicas de inventario, reservas y movimientos de Kardex. Un monolito permite que estas operaciones se ejecuten dentro de una misma transacción de base de datos. Los módulos de PAQ-02 aportan el desacoplamiento que exige RNF-14 y permiten asignar cada clase a un único responsable.

**Java con Spring Boot.** Lo exigen RNF-02 y la arquitectura tecnológica. Spring ofrece la integración de la API REST, la persistencia, la seguridad y las pruebas con un conjunto de convenciones maduras.

**Versiones LTS y estables.** Una versión LTS recibe soporte prolongado, lo que reduce las actualizaciones no planificadas del runtime y favorece la disponibilidad exigida por RNF-11. La versión exacta se fija en el esqueleto para que quede registrada en el repositorio.

**Maven.** Es la herramienta de construcción de referencia del ecosistema Spring y su modelo declarativo (`pom.xml`) facilita la lectura y el mantenimiento por parte de todo el equipo. Sus convenciones de ciclo de vida (`compile`, `test`, `verify`) se utilizan directamente en el flujo de integración continua. El Maven Wrapper garantiza una versión uniforme de la herramienta en cada entorno.

**Spring Data JPA con Hibernate.** Las clases de dominio se describen en el modelo como entidades JPA, y las relaciones entre ellas (por ejemplo, `DetalleVenta`, `ReservaStock` e `Inventario`) se expresan de forma natural mediante asociaciones. Esta correspondencia reduce el código de mapeo frente a un acceso JDBC directo. Las migraciones en `database/migraciones/` registran la evolución del esquema de forma versionada, según las [Reglas de contribución](../../CONTRIBUTING.md). El modo `validate` impide que el esquema se modifique en silencio por el ORM.

**Spring Security con JWT de acceso.** RNF-09 exige autenticación y autorización basadas en roles. Un JWT no almacena sesión en el servidor, lo que se ajusta a una API REST consumida por una SPA (ADR-001). La autorización por endpoint y por método cubre RF-USR-02 y RF-USR-03 en las capas de entrada y de negocio. Tomar el usuario responsable del contexto de seguridad asegura RF-USR-04 y la trazabilidad de RNF-10.

**Transacciones en la capa de servicios.** Los controladores no ejecutan reglas de negocio (modelo de clases, capa de presentación). Concentrar los límites transaccionales en `service` garantiza que cada operación de inventario se ejecute completa o no se ejecute, según RNF-08.

**Reglas del ADR-003.** La separación entre reserva y salida física se describe en la [Arquitectura tecnológica](../arquitectura-tecnologica.md) (sección 2) y en el [Modelo de clases](../modelo-y-diccionario-clases.md), y RF-VEN-15 establece que una venta pendiente no genera movimientos de salida. La entrega completa corresponde al proceso de despacho (CU-06). Su texto completo se encuentra en el ADR-003.

**Agente de IA sin acceso a la base de datos.** RF-IA-03 y RF-IA-04 exigen que el agente utilice funciones autorizadas del backend y no modifique datos. La validación de permisos en cada función cumple RF-IA-09.

**Pruebas.** JUnit 5 y Mockito permiten verificar las reglas de los servicios de forma aislada. Testcontainers con PostgreSQL real asegura que las consultas, las restricciones de integridad y las transacciones se comporten igual que en producción, lo que RNF-08 y RNF-15 requieren. El flujo `verificar-backend` reproduce el esquema del flujo `verificar-frontend`, con lo que ambos componentes se validan del mismo modo.

**Configuración externa.** RNF-04 exige contenedores reproducibles, y la arquitectura tecnológica (sección 5) exige que las credenciales no se incorporen en las imágenes ni en el código fuente. Las variables de entorno cumplen ambos requisitos.

## Alternativas consideradas

| Alternativa | Motivo de descarte |
| --- | --- |
| Gradle (Groovy o Kotlin DSL) | Ofrece una flexibilidad de scripting que el proyecto no requiere. Los scripts propios aumentan el costo de mantenimiento frente a la convención de Maven. |
| Microservicios por módulo | Las operaciones de inventario, reservas y Kardex requerirían transacciones distribuidas para cumplir RNF-08 y RNF-15. Además, agregarían despliegues y comunicaciones de red que la arquitectura tecnológica no contempla. |
| Sesiones de servidor (`HttpSession`) | Acoplan el backend a un estado de sesión por instancia y no se ajustan a una API REST consumida por una SPA. |
| Hibernate con `ddl-auto=update` | Modifica el esquema sin registro versionado. Impide la trazabilidad de cambios exigida por las reglas de contribución. |
| JDBC sin ORM | Requiere escribir y mantener manualmente el mapeo de cada entidad del modelo de clases, sin ventaja funcional para el alcance del sistema. |
| Paquetes por capa en la raíz (`controller`, `service`, `repository`, `model` globales) | Contradice la organización modular de PAQ-02 y dificulta el desacoplamiento exigido por RNF-14. |
| Agente de IA con acceso directo a la base de datos | Contradice RF-IA-04 y la arquitectura tecnológica (sección 2), que prohíben que el agente acceda a la base de datos o modifique inventario. |

## Consecuencias

**Favorables:**

- El equipo utiliza una convención de construcción, persistencia, seguridad y pruebas ampliamente documentada.
- Las operaciones de inventario se ejecutan en una única transacción de base de datos, sin coordinación distribuida.
- La estructura modular permite asignar cada clase del modelo a un módulo y verificar sus dependencias.
- El agente de IA opera solo con funciones de consulta, lo que limita el alcance de un eventual error de interpretación.

**Desfavorables:**

- La verificación de dependencias entre paquetes requiere pruebas adicionales que deben mantenerse. Mitigación: se incorporan en el flujo `verificar-backend` desde el esqueleto.
- Testcontainers requiere Docker tanto en los equipos de desarrollo como en el runner de integración continua. Mitigación: el runner `ubuntu-latest` incluye Docker de forma nativa.
- Hibernate en modo `validate` exige que las migraciones y las entidades se actualicen en el mismo cambio. Mitigación: la revisión de los pull requests verifica la correspondencia entre ambos.
- Un monolito se despliega como una sola unidad. Mitigación: la separación por módulos permite extraer un servicio en el futuro si los requerimientos lo justifican.
- La ausencia de token de refresco obliga a iniciar sesión nuevamente al expirar el JWT. Mitigación: la duración del token se configura mediante una variable de entorno.
- Los integrantes que no conocen Spring Security, JWT o Testcontainers requieren un periodo de aprendizaje inicial.

## Referencias

- [ADR-001: Stack tecnológico del frontend](adr-001-stack-frontend.md)
- [ADR-003: Reglas de ventas, reservas, Kardex y despacho](adr-003-reglas-ventas-reservas-despacho.md)
- [Arquitectura tecnológica](../arquitectura-tecnologica.md)
- [Diagramas de paquetes](../diagramas-paquetes.md)
- [Modelo y diccionario de clases](../modelo-y-diccionario-clases.md)
- [Requerimientos](../requerimientos.md)
- [Especificaciones de casos de uso](../especificaciones-casos-de-uso.md)
- [Reglas de contribución](../../CONTRIBUTING.md)
- [README del backend](../../backend/README.md)
- [Flujo de verificación del frontend](../../.github/workflows/verificar-frontend.yml)
