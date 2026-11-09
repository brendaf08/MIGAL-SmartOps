# Backend (API REST Java / Spring Boot)

Backend principal de MIGAL SmartOps. Implementa el núcleo transaccional descrito en el [ADR-004](../docs/adr/adr-004-arquitectura-backend.md) y persiste en PostgreSQL según el [ADR-005](../docs/adr/adr-005-base-de-datos.md). El contrato de la API se define en el [ADR-006](../docs/adr/adr-006-contrato-api.md).

## Versiones

| Componente | Versión |
| --- | --- |
| Java | 21 (LTS) |
| Spring Boot | 4.1.2 |
| Maven | Gestionado por Maven Wrapper (`mvnw`) |
| PostgreSQL | 17.11 (imagen `postgres:17.11-alpine`), la versión del proyecto en Supabase |

## Requisitos

- JDK 21.
- Docker en ejecución. Las pruebas utilizan Testcontainers con PostgreSQL real.

No se requiere instalar Maven; el Maven Wrapper lo descarga en la primera ejecución.

## Ejecución

Todos los comandos se ejecutan desde la carpeta `backend/`.

```bash
./mvnw verify          # Compilar y ejecutar las pruebas
./mvnw spring-boot:run # Iniciar la aplicación
```

En Windows, utilizar `mvnw.cmd` en lugar de `./mvnw`.

Para iniciar la aplicación contra la base de datos de desarrollo, levantar primero PostgreSQL con Docker Compose según [database/README.md](../database/README.md) y definir las variables de entorno indicadas a continuación.

## Configuración

La configuración se lee desde variables de entorno, con los valores predeterminados definidos en `src/main/resources/application.yaml`. El repositorio no contiene credenciales.

| Variable | Descripción | Valor predeterminado |
| --- | --- | --- |
| `SPRING_DATASOURCE_URL` | URL JDBC de la base de datos. | `jdbc:postgresql://localhost:5432/migal_smartops` |
| `SPRING_DATASOURCE_USERNAME` | Usuario de la aplicación. | `migal_app` |
| `SPRING_DATASOURCE_PASSWORD` | Contraseña del usuario. | Sin valor |
| `FLYWAY_LOCATIONS` | Ubicación de las migraciones. | `filesystem:../database/migraciones` |

Hibernate opera en modo `validate`: el esquema se gobierna únicamente mediante las migraciones de `database/migraciones/`.

## Estructura

El código se organiza como un monolito modular. Cada módulo funcional es un paquete bajo `com.migal.smartops` y agrupa sus capas `controller`, `service`, `repository` y `model`.

| Paquete | Responsabilidad |
| --- | --- |
| `seguridad` | Autenticación, autorización y usuario responsable. |
| `catalogo` | Catálogo maestro de productos y categorías. |
| `inventario` | Existencias, reservas y Kardex. |
| `documentosocr` | Documentos digitales y comunicación con el servicio de OCR. |
| `comercial` | Clientes, cotizaciones y ventas. |
| `despacho` | Guías de despacho y salida física. |
| `abastecimiento` | Proveedores, órdenes de compra y recepciones. |
| `agenteia` | Funciones de consulta para el agente de IA. |

Las reglas de dependencia entre módulos se describen en el ADR-004.
