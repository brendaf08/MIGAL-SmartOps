# Migraciones

Esquema de PostgreSQL administrado con Flyway (ADR-005). Cada archivo `V<número>__<descripción>.sql` se aplica una sola
vez, en orden, al iniciar el backend. Las migraciones ya aplicadas no se editan: un cambio del esquema es una migración
nueva.

| Versiones | Contenido |
| --- | --- |
| V1 a V8 | Usuarios, maestros, inventario, comercial, abastecimiento, despacho, Kardex y reservas. |
| V9 a V13 | Documentos OCR, categorías normalizadas, códigos de cliente, rechazo de cotizaciones y guías anuladas. |
| V14 y V15 | Conversaciones del agente de IA e idempotencia. |
| V16 | Fecha de encolado de los documentos OCR (vigilancia de 60 s, ADR-012). |
| V17 | Índices de los listados ordenados y unicidad del documento que apoya una recepción. |

Para verificar una migración nueva, ejecutar las pruebas del backend: cada prueba de integración aplica todas las
migraciones sobre un PostgreSQL de Testcontainers.
