# Despliegue del frontend en Vercel

## Alcance

Vercel aloja únicamente la SPA de `frontend/`. El backend, PostgreSQL y el servicio de OCR se ejecutan con Docker Compose (`compose.yaml`) y no se despliegan en Vercel.

Mientras no se defina un backend público, la compilación de producción utiliza los datos simulados (`VITE_API_REAL` no definido). Para conectarla a un backend, definir `VITE_API_REAL=true` en las variables de entorno del proyecto de Vercel y agregar en `frontend/vercel.json` un rewrite de `/api/:path*` hacia la URL del backend, de modo que el navegador siga operando en el mismo origen (la política CSP usa `connect-src 'self'`).

## Configuración inicial

1. Crear el proyecto en Vercel con el directorio raíz `frontend`.
2. Registrar en GitHub los secretos `VERCEL_TOKEN`, `VERCEL_ORG_ID` y `VERCEL_PROJECT_ID`.
3. Crear en GitHub los entornos `production` y `preview` (opcionalmente con revisores obligatorios para producción).

## Flujos de trabajo

| Archivo | Función |
| --- | --- |
| `verificar-*.yml` | Lint, tipos, pruebas y compilación por componente en cada PR. |
| `desplegar-frontend.yml` | Vista previa en cada PR y producción al integrar en `main`, tras superar la verificación. Se omite si faltan los secretos. |
| `analisis-codeql.yml` | Análisis estático de seguridad en PR, `main` y semanalmente. |
| `dependabot.yml` | Actualizaciones semanales de dependencias y acciones. |

## Despliegue del backend en Render

1. En Supabase, copiar los datos de conexión del pooler en modo sesión (puerto 5432).
2. En Render, elegir New > Blueprint, seleccionar el repositorio y la rama `main`; se lee `render.yaml`.
3. Completar las variables solicitadas: `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` y `MIGAL_ADMIN_CLAVE` (letras y números, mínimo 8 caracteres).
4. En la primera ejecución contra Supabase, agregar `spring.flyway.baseline-on-migrate=true` y `spring.flyway.baseline-version=0`, y retirarlas después (ver `.env.example`).
5. Verificar `https://<servicio>.onrender.com/actuator/health`.
6. En `frontend/vercel.json`, agregar antes del rewrite de la SPA `{ "source": "/api/:path*", "destination": "https://<servicio>.onrender.com/api/:path*" }` y definir `VITE_API_REAL=true` en Vercel.

Limitaciones del plan gratuito: el servicio se suspende por inactividad y los documentos subidos no persisten. El servicio de OCR no se incluye; el backend usa el proveedor simulado hasta que se despliegue por separado.
