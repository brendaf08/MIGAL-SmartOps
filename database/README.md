# Base de datos

Este directorio contiene el esquema PostgreSQL del proyecto MIGAL SmartOps, administrado mediante migraciones Flyway en `migraciones/`. El proyecto Supabase `migal-smartops` tiene aplicadas la línea base 0 y las migraciones V1 a V15; el historial consultado el 15 de noviembre de 2026 muestra todas con ejecución exitosa. El entorno local usa PostgreSQL en Docker Compose y el backend ejecuta Flyway al iniciar.

## Verificar los requisitos

- Docker Desktop o Docker Engine con el complemento Docker Compose v2.
- Espacio libre suficiente para la imagen `postgres:17.11-alpine` y el volumen de datos.

## Preparar el entorno

Desde la raíz del repositorio, copiar la plantilla de variables:

```text
copy .env.example .env
```

Reemplazar el valor de `POSTGRES_PASSWORD` por una credencial local. El archivo `.env` está excluido del control de versiones y no se publica. Las variables son las siguientes:

| Variable | Descripción | Valor de la plantilla |
| --- | --- | --- |
| `POSTGRES_DB` | Nombre de la base de datos. | `migal_smartops` |
| `POSTGRES_USER` | Usuario propietario de la base de datos. | `migal_app` (ficticio) |
| `POSTGRES_PASSWORD` | Contraseña del usuario. Obligatoria. | `cambiar-este-valor` |
| `POSTGRES_PUERTO` | Puerto publicado en `127.0.0.1`. Opcional. | `5432` |

Si alguna variable obligatoria no está definida, Compose interrumpe la ejecución e informa la variable faltante.

## Iniciar el entorno

Ejecutar el siguiente comando desde la raíz del repositorio:

```text
docker compose up -d
```

El contenedor se denomina `migal-postgres`. Los datos se almacenan en el volumen nombrado `migal-postgres-datos`. El contenedor se reinicia automáticamente, salvo que se detenga de forma explícita.

## Consultar el estado

```text
docker compose ps
```

El servicio `postgres` debe mostrar el estado `healthy` una vez superada la comprobación de `pg_isready`.

## Conectar con la base de datos

```text
docker compose exec postgres psql -U migal_app -d migal_smartops
```

El puerto publicado solo acepta conexiones desde el equipo local.

## Detener el entorno

```text
docker compose down
```

Este comando detiene y elimina el contenedor, pero conserva el volumen `migal-postgres-datos`. Los datos persisten en la siguiente ejecución.

## Eliminar los datos

```text
docker compose down -v
```

Advertencia: este comando elimina el contenedor y el volumen `migal-postgres-datos`. Se pierden todos los datos almacenados en la base de datos, de forma irreversible. Se recomienda ejecutarlo solo cuando se requiera reconstruir la base de datos desde cero.

## Ubicar las migraciones

Los cambios de esquema se almacenan en `database/migraciones/`. La herramienta es Flyway Community Edition con scripts SQL planos, y la convención de nombres (`V<número>__<descripción>.sql`) y las reglas de inmutabilidad se describen en el ADR-005. La ejecución de Flyway y la cuenta de migraciones, distinta de la cuenta de la aplicación, se definen en el ADR de arquitectura del backend. Estos mecanismos no forman parte de este entorno.

## Ubicar las semillas

Los datos de desarrollo se ubicarían en `database/semillas/`. Esta ubicación es una propuesta del ADR-005 y la carpeta aún no existe. Las semillas contienen únicamente datos ficticios, están separadas de las migraciones y no se ejecutan en producción.

## Datos de demostración (solo desarrollo local)

El script `datos-demostracion/demo.sql` carga ubicaciones, productos con stock inicial, clientes y proveedores para
probar las pantallas con la API real. No forma parte de las migraciones y no debe ejecutarse en Supabase ni en
producción. Requiere que el backend haya creado el administrador inicial y es idempotente.

Para cargarlo en el contenedor local de PostgreSQL, con las variables del archivo `.env` de la raíz:

```bash
docker exec -i migal-postgres psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v ON_ERROR_STOP=1 < database/datos-demostracion/demo.sql
```

## Estado de Supabase y migraciones

La verificación del 15 de noviembre de 2026 encontró en el esquema `public` las tablas del sistema y `flyway_schema_history`, con línea base 0 y migraciones V1 a V15, todas con `success = true`. El esquema del proyecto Supabase actual ya está aplicado. La migración V16 (`fecha_encolado` de `documento_digital`, ADR-012) se aplica en Supabase cuando el backend arranque por primera vez con una versión que la incluya; hasta entonces el proyecto permanece en V15.

Las migraciones versionadas de `database/migraciones/` son la fuente del esquema. Para cambios futuros, agrega una nueva migración Flyway; no edites migraciones ya ejecutadas ni modifiques el esquema mediante el editor SQL de Supabase o sus migraciones propias.

La configuración `spring.flyway.baseline-on-migrate=true` y `spring.flyway.baseline-version=0` se usó durante la primera aplicación, porque el esquema `public` contenía una función propia de la plataforma. **No vuelvas a activar estas opciones para el proyecto actual.** Si se aprovisiona otro proyecto Supabase y presenta la misma condición, evalúalas solo para esa primera ejecución y retíralas después.

Para conectarse a Supabase, el responsable debe proporcionar de forma segura la cadena de conexión y las credenciales. Se definen en el `.env` local mediante `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `MIGAL_JWT_SECRETO`, `MIGAL_ADMIN_USERNAME` y `MIGAL_ADMIN_CLAVE`. El backend lee el archivo al iniciar. No guardes estos valores en Git ni los compartas en mensajes.

No cargues `datos-demostracion/demo.sql` en Supabase ni en producción. La separación de los roles de migración y aplicación indicada en ADR-007 sigue pendiente de un cambio posterior.

