# Base de datos

Este directorio contiene el esquema PostgreSQL de MIGAL SmartOps, administrado mediante migraciones Flyway en `migraciones/`. El proyecto Supabase `migal-smartops` tiene aplicadas la línea base 0 y las migraciones V1 a V15; el historial consultado el 15 de noviembre de 2026 muestra todas con ejecución exitosa.

El entorno local usa PostgreSQL en Docker Compose. El backend ejecuta Flyway al iniciar y crea el administrador inicial si no existe uno activo.

## Requisitos

- Docker Desktop o Docker Engine con Docker Compose v2.
- Espacio para la imagen `postgres:17.11-alpine` y el volumen local.

## Configurar PostgreSQL local

Desde la raíz, copia la plantilla:

```powershell
Copy-Item .env.example .env
```

En `.env`, define una contraseña local propia para `POSTGRES_PASSWORD`. Este archivo está excluido de Git. No uses aquí credenciales de Supabase.

| Variable | Uso | Valor de plantilla |
| --- | --- | --- |
| `POSTGRES_DB` | Base local | `migal_smartops` |
| `POSTGRES_USER` | Usuario propietario de la base local | `migal_app` |
| `POSTGRES_PASSWORD` | Contraseña local obligatoria | Reemplazar el ejemplo |
| `POSTGRES_PUERTO` | Puerto local opcional | `5432` |

Para que el backend se conecte a esta base, define además `SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/migal_smartops`, `SPRING_DATASOURCE_USERNAME=migal_app` y `SPRING_DATASOURCE_PASSWORD` con la misma contraseña local. Define también `MIGAL_JWT_SECRETO` (mínimo 32 caracteres), `MIGAL_ADMIN_USERNAME` y `MIGAL_ADMIN_CLAVE`. Usa valores locales sin comillas en el archivo `.env`; no publiques esos valores.

## Iniciar y consultar el entorno local

Desde la raíz:

```powershell
docker compose up -d --wait
docker compose ps
```

El servicio se llama `postgres`, el contenedor `migal-postgres` y el volumen persistente `migal-postgres-datos`. El puerto se publica solo en `127.0.0.1`. `docker compose ps` debe mostrar estado `healthy`.

Conecta con psql:

```powershell
docker compose exec postgres psql -U migal_app -d migal_smartops
```

Para detener el contenedor y conservar los datos:

```powershell
docker compose down
```

Para eliminar también el volumen y reconstruir la base local desde cero:

```powershell
docker compose down -v
```

Este último comando elimina permanentemente los datos del volumen local, incluidas las migraciones y los datos de demostración. No afecta Supabase.

## Migraciones

Las migraciones versionadas están en `database/migraciones/` y siguen el patrón `V<número>__<descripción>.sql`. No edites una migración que ya se aplicó: agrega una migración nueva. Flyway conserva el historial en `flyway_schema_history`.

En una base local nueva, inicia el backend desde `backend/`; Flyway aplicará V1–V15 y las migraciones posteriores pendientes.

## Estado de Supabase

La verificación del 15 de noviembre de 2026 encontró en el esquema `public` las tablas del sistema y `flyway_schema_history`, con línea base 0 y las migraciones V1–V15 exitosas. Por tanto, la base Supabase actual ya tiene el esquema aplicado.

La configuración `spring.flyway.baseline-on-migrate=true` y `spring.flyway.baseline-version=0` se usó en la primera aplicación, debido a una función propia presente en el esquema `public`. No vuelvas a activar estas opciones para el proyecto actual. Solo evalúalas al configurar otro proyecto Supabase y retíralas luego de su primera ejecución.

Para cambios futuros, aplica migraciones nuevas mediante Flyway desde el backend. No cambies el esquema con el editor SQL de Supabase ni con sus migraciones propias: se debe preservar un único historial.

La conexión a Supabase requiere credenciales administradas por el responsable del proyecto. No las guardes en Git ni las compartas en mensajes. Las reglas definitivas para separar el rol de migración del rol de aplicación siguen pendientes según ADR-007.

## Datos de demostración

`datos-demostracion/demo.sql` agrega ubicaciones, productos, clientes y proveedores ficticios para probar el entorno local con la API. No es una migración y no debe ejecutarse en Supabase ni en producción. El backend debe haber creado antes el administrador inicial.

Desde la raíz, en PowerShell:

```powershell
cmd.exe /c "docker exec -i migal-postgres psql -U migal_app -d migal_smartops -v ON_ERROR_STOP=1 < database\datos-demostracion\demo.sql"
```

El script es idempotente. El stock inicial se registra mediante los datos de demostración del entorno local; los datos reales de MIGAL no forman parte de esta carga.

