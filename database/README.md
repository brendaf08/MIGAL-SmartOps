# Base de datos

Este directorio define la base de datos del proyecto. El motor es PostgreSQL, según el [ADR-005](../docs/adr/adr-005-base-de-datos.md), y la imagen de referencia se fija en el archivo `compose.yaml` de la raíz. No se incluye un esquema SQL validado. Las migraciones se incorporan en `migraciones/` después de resolver y aprobar el modelo de datos.

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
