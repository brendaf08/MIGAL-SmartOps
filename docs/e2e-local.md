# Verificación E2E local

El verificador recorre MIGAL SmartOps con Playwright desde un navegador Chromium local. No usa GitHub Actions. Ejecuta una base PostgreSQL 17.11 nueva en un contenedor temporal, sin volumen Docker, publicada únicamente en `127.0.0.1` y separada de los datos de demostración persistentes. La base temporal se elimina al terminar; los servicios preexistentes y sus volúmenes no se detienen ni se limpian.

## Requisitos

- Windows, PowerShell 7 (`pwsh`), Node.js 22 o posterior y npm.
- JDK 21 o posterior.
- Docker Desktop o Docker Engine iniciado y acceso para descargar `postgres:17.11-alpine` si no está en caché.
- Acceso al registro npm y a la descarga de Chromium la primera vez.

Desde la raíz del repositorio:

```powershell
pwsh -File scripts/verificar-e2e-local.ps1
```

El script prepara dependencias si Playwright no está instalado, valida las herramientas, genera credenciales aleatorias solo en memoria, fija la conexión del backend a la base local temporal, inicia frontend y backend en puertos libres y detiene únicamente esos procesos y su contenedor al finalizar. No lee `.env`, no usa Supabase y no ejecuta comandos de borrado de volúmenes.

## Cobertura y clasificación

- `VITE_API_REAL=true` hace que los adaptadores de sesión, catálogo, disponibilidad, comercial, abastecimiento, despacho, recepción, OCR y agente consulten la API local; `false` usa `mockRequest` y fixtures en memoria. En Vite desarrollo el valor predeterminado es real, y el lanzador lo fija explícitamente.
- Las consultas de catálogo, disponibilidad y órdenes de compra verifican pantallas con datos del backend. Las pantallas de cotizaciones/ventas y órdenes de compra también usan endpoints reales para sus operaciones, pero los selectores de producto de cotización, venta y compra se alimentan de `inventario/fixtures.ts`; la prevalidación de stock del formulario de venta también usa esos fixtures. Por eso sus recorridos están etiquetados **SIMULADO PARCIAL**, aunque las mutaciones posteriores lleguen a la base aislada.
- Recepción, despacho y sus resultados operativos llegan al backend local; su base transaccional es la temporal del verificador.
- El login se valida contra `/api/v1/sesiones`, no con los usuarios mock del frontend.
- Inicio de sesión por API local real y consultas de catálogo, disponibilidad y órdenes de compra.
- **SIMULADO PARCIAL:** cotización → aceptación → validación → venta/reserva → despacho, y compra → recepción → inventario. Las operaciones llegan a la API local, pero los formularios obtienen los productos y parte de sus cifras de [`inventario/fixtures.ts`](../frontend/src/features/inventario/fixtures.ts): `opcionesProducto()` alimenta los selectores y el cálculo previo de stock de venta usa esos fixtures. El reporte etiqueta estos casos para que no se interpreten como una integración completa de datos de punta a punta.
- **SIMULADO:** OCR y agente consultivo llaman al backend local, pero `MIGAL_OCR_PROVEEDOR` y `MIGAL_AGENTE_PROVEEDOR` se fijan en `simulado`. No prueban proveedores OCR ni modelos de IA externos; el OCR retorna una línea determinista sin coincidencia y el agente usa el proveedor de reglas simulado.

Las pruebas de Vitest existentes siguen cubriendo componentes, pantallas y contratos; Playwright cubre recorridos de navegador en la configuración API real y una base aislada, dejando visible la dependencia de fixtures donde existe. No se cambia el código de la aplicación.

## Reportes

Cada ejecución crea `artifacts/e2e-local/<fecha-hora>/`, ignorado por Git:

- `index.html`: resumen con fecha, commit, estado, duración y escenarios.
- `resumen.md`: mismo resumen en Markdown.
- `html/index.html`: reporte detallado de Playwright con capturas acotadas a los encabezados de pantalla.
- `junit.xml`: resultados por escenario.
- `api-estados.txt`: método, ruta local y código HTTP observados, sin cuerpos, cabeceras ni parámetros.

Un escenario fallido conserva sus resultados y los demás continúan cuando se pueden ejecutar con seguridad. Los reportes no incluyen credenciales, tokens, variables de entorno, cuerpos o cabeceras HTTP ni videos. Los datos de negocio se preparan en una base efímera aislada. El código HTTP ayuda a distinguir una denegación de acceso (403) de errores de aplicación; revisa el reporte de Playwright para el paso de interfaz asociado.

## Reinicio y conservación de datos

Al finalizar, la base temporal desaparece junto con el contenedor porque se crea sin volumen. Para repetir, ejecuta otra vez el mismo comando. La base de demostración del proyecto y su volumen nombrado no se usan ni se modifican. Para detener manualmente un proceso interrumpido, revisa primero el nombre único `migal-e2e-<PID>` con `docker ps`; detener únicamente ese contenedor con `docker stop <nombre>`. No se requiere `docker compose down` y nunca se debe ejecutar `docker compose down -v` para este verificador.
