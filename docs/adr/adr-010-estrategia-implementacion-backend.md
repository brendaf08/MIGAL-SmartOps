# ADR-010: Estrategia de implementación del backend

## Contexto

El esquema de la base de datos (V1 a V14) y el contrato de la API (diez documentos de módulo, [ADR-006](adr-006-contrato-api.md), [ADR-008](adr-008-ajustes-contrato-api.md) y [ADR-009](adr-009-ajustes-seguridad-y-componentes-contrato.md)) están definidos. El [ADR-004](adr-004-arquitectura-backend.md) fija la arquitectura del backend como monolito modular con Spring Boot. Falta decidir cómo se implementa, en qué orden y con qué verificaciones, de forma que cada pull request sea pequeño y el contrato se cumpla de manera comprobable.

## Decisión

1. **Orden de módulos.** Se implementan según sus dependencias: `seguridad`, `catalogo`, `inventario`, `comercial` (clientes y cotizaciones), ventas (dentro de `comercial`), `despacho`, `abastecimiento` (incluye recepciones), `documentosocr` y `agenteia`.
2. **Un pull request por capa.** Cada módulo se divide en tres cambios sucesivos: (a) entidades JPA y repositorios, (b) servicios con las reglas de negocio y sus pruebas, (c) controladores, objetos de transferencia y prueba de contrato. Ningún cambio supera las 800 líneas.
3. **Objetos de transferencia escritos a mano.** Los DTO son `record` de Java que reflejan el contrato. No se genera código a partir del contrato, para conservar el control del diseño y no depender de la compatibilidad del generador con la versión de Spring Boot.
4. **Verificación del contrato.** Las pruebas de los controladores validan cada solicitud y respuesta contra el documento OpenAPI del módulo (biblioteca `swagger-request-validator` con MockMvc). Una divergencia entre el código y el contrato hace fallar la prueba.
5. **Errores.** Una clase `@RestControllerAdvice` convierte las excepciones de dominio en `ProblemDetail` (RFC 9457) con el campo `code` del catálogo `CodigoError`. Cada código del catálogo corresponde a una excepción de dominio tipada.
6. **Seguridad.** Spring Security con JWT firmado, según el ADR-009: duración de 8 horas, rol como reclamación en minúsculas y verificación del estado del usuario en cada solicitud. La clave de firma se lee de una variable de entorno.
7. **Concurrencia.** Las operaciones de reserva, recepción, traslado, despacho y cancelación bloquean las filas de inventario con `PESSIMISTIC_WRITE` en orden ascendente de identificador, según el ADR-005, y reintentan ante interbloqueo con un número limitado de intentos.
8. **Idempotencia.** Las operaciones con `Idempotency-Key` guardan la clave, el usuario, la operación, la huella de la solicitud y la respuesta en una tabla `idempotencia` (migración V15), con una retención de 24 horas. Una clave repetida con la misma huella devuelve la respuesta guardada; con otra huella responde `IDEMPOTENCIA_CONFLICTO`.
9. **Pruebas.** Pruebas unitarias con Mockito para los servicios; pruebas de integración con Testcontainers (PostgreSQL 17.11) y MockMvc para controladores y repositorios. Se prueban explícitamente los casos de concurrencia de reservas y despachos con dos transacciones.
10. **Reloj y tiempo.** Los servicios reciben un `Clock` inyectado para fijar fechas de forma determinista en las pruebas.

## Justificación

- El orden por dependencias permite probar cada módulo con sus requisitos ya implementados.
- Tres cambios por módulo mantienen la revisión manejable y permiten fallar rápido ante errores de modelado en la capa de entidades.
- Validar contra el OpenAPI convierte el contrato en una prueba ejecutable sin acoplar el código al generador.
- Una tabla de idempotencia es la forma más simple de cumplir el ADR-009 sin infraestructura adicional.

## Alternativas consideradas

| Alternativa | Motivo de descarte |
| --- | --- |
| Generar interfaces y modelos desde el OpenAPI | Acopla el código al generador y a su compatibilidad con Spring Boot 4. |
| Un pull request por módulo | Supera el límite de tamaño y dificulta la revisión. |
| Idempotencia en memoria o con caché externa | No sobrevive a reinicios ni a varias instancias, y añade infraestructura. |
| Implementar por capa horizontal para todos los módulos | Retrasa la primera funcionalidad completa y mezcla dominios en cada cambio. |

## Consecuencias

**Favorables:**

- Cada módulo se entrega y se verifica de extremo a extremo con el contrato.
- Los cambios al contrato rompen pruebas de forma inmediata.

**Desfavorables:**

- Mantener los DTO a mano exige disciplina. Se mitiga con la validación contra el contrato en las pruebas.
- La tabla de idempotencia añade una migración y una limpieza periódica de registros vencidos.

## Referencias

- [ADR-004: Arquitectura y stack del backend](adr-004-arquitectura-backend.md)
- [ADR-005: Base de datos, migraciones y concurrencia](adr-005-base-de-datos.md)
- [ADR-009: Ajustes de seguridad, sesión y componentes del contrato](adr-009-ajustes-seguridad-y-componentes-contrato.md)
- [Contrato de la API](../../backend/api/README.md)
