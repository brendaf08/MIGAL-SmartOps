# Contrato OpenAPI de la API REST

Este directorio contiene el contrato OpenAPI 3.1 de la API REST de MIGAL SmartOps. El contrato se define según el [ADR-006](../../docs/adr/adr-006-contrato-api.md) y se revisa antes de implementar el backend y el cliente del frontend.

## Estructura

| Ruta | Contenido |
| --- | --- |
| `componentes/seguridad.yaml` | Esquema `bearerAuth`, enumeración `RolUsuario` y convención de la extensión `x-roles`. |
| `componentes/errores.yaml` | Esquema `Problema` (RFC 9457), enumeración `CodigoError` y respuestas reutilizables. |
| `componentes/parametros.yaml` | Parámetros de paginación, ordenamiento, rango de fechas e `Idempotency-Key`. |
| `componentes/esquemas-comunes.yaml` | Sobre de paginación, importes, cantidades, identificadores y fechas. |
| `modulos/*.yaml` | Un documento OpenAPI independiente por módulo funcional. |
| `modulos/ventas.yaml` | Módulo de ventas (CU-10): listado, registro desde cotización, venta directa, cobertura y cancelación. |
| `modulos/recepciones.yaml` | Módulo de recepciones (CU-13): registro pendiente, precarga desde OCR, validación con movimientos de entrada y recepciones parciales. |
| `.redocly.yaml` | Configuración del lint y excepciones documentadas. |
| `openapi.yaml` | Documento unico generado mediante `join`. No se edita a mano. |

## Regla de un documento por módulo

Cada módulo se describe en un único archivo de `modulos/`. Un cambio en un módulo modifica únicamente su archivo, de modo que los cambios de módulos distintos no generan conflictos. Los archivos de `componentes/` se modifican en un cambio propio, porque afectan a todos los módulos.

Los documentos de módulo referencian los componentes comunes mediante `$ref` con rutas relativas, por ejemplo `../componentes/seguridad.yaml#/bearerAuth`.

## Concreción del ADR-006 sobre el archivo raíz

El ADR-006 describe un archivo raíz `openapi.yaml` que define la información general, los servidores, la seguridad global y los componentes compartidos. Este contrato concreta esa decisión de la siguiente manera: la raíz se obtiene por unión de los documentos de módulo mediante `join`; los componentes compartidos residen en `componentes/`; y `openapi.yaml` se genera y no se edita a mano.

## Validación

Se valida con Redocly CLI mediante Docker, sin instalaciones globales. En Git Bash se antepone `MSYS_NO_PATHCONV=1` al comando:

```text
docker run --rm -w /spec -v "<ruta absoluta de backend/api>:/spec" redocly/cli lint --config /spec/.redocly.yaml modulos/*.yaml
```

La opción `--config` es necesaria, porque Redocly CLI no localiza `.redocly.yaml` por descubrimiento automático y aplica entonces su configuración predeterminada. La versión verificada es `redocly/cli` 2.76.0.

## Generación del documento unico

Se genera el documento unico desde `backend/api`:

```text
npx @redocly/cli join modulos/*.yaml -o openapi.yaml --without-x-tag-groups
```

Se valida el resultado con `npx @redocly/cli lint openapi.yaml`. La unión presenta las limitaciones siguientes:

- Toma `info` y `servers` del primer documento de la lista.
- No conserva el campo raíz `security`. Cada operación declara su `security` de forma explícita; la regla `security-defined` del lint del documento unico verifica esta declaración.

## Convenciones que cumplen los módulos

- Las rutas comienzan con `/api/v1`. Un cambio incompatible requiere `/api/v2`.
- Los recursos se nombran en plural, en español, en minúsculas y con guiones (`ordenes-compra`).
- Los campos JSON usan camelCase. Las enumeraciones usan PascalCase sin acentos (`ListaDespacho`).
- Los errores se describen como `application/problem+json` mediante las respuestas de `componentes/errores.yaml`, con `code` tomado de `CodigoError`.
- Los listados se paginan con `page` y `pageSize` (máximo 100) y devuelven el sobre `Pagina`, salvo las excepciones documentadas en cada operación.
- Las operaciones que afectan inventario o reservas declaran el parámetro `Idempotency-Key`.
- Cada operación declara `x-roles` con valores de `RolUsuario`. Las operaciones públicas declaran `security: []`.
- La extensión `x-herramienta-agente: true` se usa solo en operaciones `GET`.
- Las transiciones de estado se modelan como sub-recursos `POST`. La activación y desactivación de maestros usa `PUT /{recurso}/{id}/estado`.

## Excepciones de lint

- La regla `info-license` se desactiva mientras la licencia de la API no esté definida (`.redocly.yaml`).
- Mientras no existan operaciones, el documento unico muestra una advertencia por el esquema `bearerAuth` sin uso. La advertencia desaparece al definir las primeras operaciones.
