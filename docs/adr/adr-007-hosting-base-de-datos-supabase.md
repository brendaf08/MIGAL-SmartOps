# ADR-007: Hosting de la base de datos en Supabase

## Contexto

El [ADR-005](adr-005-base-de-datos.md) define PostgreSQL como motor, Flyway como herramienta de migraciones y un entorno local con Docker Compose, y difiere la versión del motor y el despliegue a decisiones posteriores. La [Arquitectura tecnológica](../arquitectura-tecnologica.md) (sección 5) contempla contenedores para todos los componentes. El equipo dispone de un proyecto de Supabase (`migal-smartops`, región us-east-1) que ejecuta PostgreSQL 17.11 y que alojará la base de datos del sistema.

Supabase es una plataforma que ofrece PostgreSQL gestionado junto con servicios adicionales (API REST automática, autenticación, almacenamiento de archivos). Algunos de estos servicios tienen efectos sobre el diseño: la API REST expone por defecto las tablas del esquema `public` a cualquier cliente que posea la clave anónima del proyecto, salvo que cada tabla tenga habilitada la seguridad a nivel de fila (RLS).

Los criterios de evaluación son: compatibilidad con el ADR-005 y con el backend de [ADR-004](adr-004-arquitectura-backend.md), seguridad de los datos (RNF-09), disponibilidad y respaldo (RNF-11, RNF-12), reproducibilidad del esquema y costo.

## Decisión

1. **Motor y versión.** La base de datos del sistema es el proyecto `migal-smartops` de Supabase, con PostgreSQL 17. El entorno local de Docker Compose y las pruebas con Testcontainers utilizan la misma versión mayor y menor (`postgres:17.11-alpine`). Esta decisión reemplaza la versión 18.6 definida en el ADR-005.
2. **Uso limitado de la plataforma.** El sistema utiliza de Supabase únicamente PostgreSQL. El backend es el único cliente de la base de datos. No se utilizan la API REST automática, la autenticación de Supabase ni sus funciones de tiempo real. La autenticación del sistema es la definida en el ADR-004.
3. **Seguridad a nivel de fila.** Toda tabla del esquema `public` habilita RLS sin políticas, y toda vista se crea con `security_invoker = true`. Así, la API pública de Supabase no devuelve datos, mientras que el backend accede con un rol de base de datos propio.
4. **Migraciones.** Las migraciones de Flyway de `database/migraciones/` son la única fuente del esquema. Se ejecutan desde el backend o la línea de comandos de Flyway contra Supabase, y no mediante el editor SQL ni las migraciones propias de la plataforma, para mantener un único historial (`flyway_schema_history`). Toda aplicación de migraciones a Supabase requiere confirmación explícita del responsable.
5. **Conexión.** El backend y Flyway se conectan por conexión directa o por el pooler en modo sesión. No se utiliza el pooler en modo transacción, pues interfiere con las sentencias preparadas y con los bloqueos de sesión. Las cadenas de conexión y las credenciales se mantienen en variables de entorno y en archivos `.env` excluidos del repositorio.
6. **Roles.** Se definen dos roles de base de datos propios: uno para migraciones, con privilegios de definición, y otro para la aplicación, con privilegios de consulta y manipulación, sin permiso de `UPDATE` ni `DELETE` sobre `movimiento_kardex`, según el ADR-005. La definición de los roles se incorpora en un cambio posterior.
7. **Entornos.** Las pruebas automáticas y el desarrollo local utilizan la base de datos de Docker. El proyecto de Supabase se utiliza como entorno compartido hasta que exista un entorno de producción separado. Antes de la puesta en producción se crea un proyecto distinto para cada entorno.
8. **Fuente del modelo.** El diccionario de datos es la fuente del contenido del modelo, y el diagrama entidad-relación se genera desde su fuente PlantUML. Ante una diferencia, se corrige el diagrama. Esto actualiza la política de divergencias del ADR-005.

## Justificación

- **Supabase:** ofrece PostgreSQL estándar, sin dependencia de extensiones propias, lo que mantiene la portabilidad y permite probar localmente con la misma versión del motor. Evita administrar el servidor, las actualizaciones y el acceso remoto.
- **Uso limitado:** cada servicio adicional amplía la superficie de ataque y la dependencia de la plataforma. El ADR-004 ya define la autenticación y centraliza las reglas de negocio en el backend (RNF-14).
- **RLS sin políticas:** convierte la exposición automática de la API en un rechazo por defecto, sin modificar el modelo, y es compatible con una conexión directa del backend con un rol propietario o con permisos explícitos.
- **Flyway como único historial:** conserva la inmutabilidad verificable por suma de comprobación y la revisión por pull request exigidas por el ADR-005. Un segundo historial generaría divergencias entre entornos.
- **Pooler en modo sesión:** conserva las propiedades de sesión que requieren `SELECT ... FOR UPDATE` y los disparadores diferibles del modelo de reservas.
- **Misma versión local y remota:** las pruebas verifican el comportamiento que tendrá la base de datos desplegada.

## Alternativas consideradas

| Alternativa | Motivo de descarte |
| --- | --- |
| PostgreSQL autoadministrado en Docker | Exige administrar servidor, respaldo, actualización y acceso, sin ventaja funcional para el alcance del proyecto. |
| Otro servicio gestionado (por ejemplo, Amazon RDS) | Mayor costo y configuración; el equipo ya dispone del proyecto de Supabase. |
| Usar la API REST de Supabase desde el frontend | Duplica las reglas de negocio fuera del backend y contradice RNF-14 y el ADR-004. |
| Aplicar las migraciones con el editor o la herramienta de Supabase | Genera un segundo historial de migraciones con nombres distintos al de Flyway. |
| Pooler en modo transacción | Incompatible con las sentencias preparadas y con las sesiones que sostienen bloqueos. |

## Consecuencias

**Favorables:**

- El equipo no administra infraestructura de base de datos.
- El mismo esquema se prueba localmente, en integración continua y en Supabase.
- La exposición accidental de datos por la API pública queda cerrada por defecto.

**Desfavorables:**

- Dependencia de un proveedor externo. Mitigación: se utiliza PostgreSQL estándar y migraciones en SQL plano, lo que permite trasladar la base de datos a otro servidor.
- El plan gratuito tiene limitaciones de disponibilidad y respaldo (como la pausa de proyectos inactivos y la ausencia de recuperación a un punto en el tiempo). Mitigación: antes de la puesta en producción se verifica el plan vigente frente a RNF-11 y RNF-12 y se define el respaldo en un ADR de despliegue.
- La conexión directa utiliza IPv6 en el plan gratuito. Mitigación: se utiliza el pooler en modo sesión en redes sin IPv6.
- Las políticas de RLS no se definen, por lo que la seguridad depende de que el backend sea el único cliente. Mitigación: la clave anónima y la clave de servicio no se distribuyen en el frontend.

## Referencias

- [ADR-004: Arquitectura y stack del backend](adr-004-arquitectura-backend.md)
- [ADR-005: Base de datos, migraciones y concurrencia](adr-005-base-de-datos.md)
- [Arquitectura tecnológica](../arquitectura-tecnologica.md)
- [Modelo y diccionario de datos](../modelo-y-diccionario-datos.md)
- [Reglas de contribución](../../CONTRIBUTING.md)
