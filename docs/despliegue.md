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
