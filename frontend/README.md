# Frontend

Aplicación web de MIGAL SmartOps (SPA en React). Las decisiones de stack y diseño visual están en [ADR-001](../docs/adr/adr-001-stack-frontend.md) y [ADR-002](../docs/adr/adr-002-sistema-de-diseno.md).

## Estado

Incluye el tema claro y oscuro, layout responsive, componentes del sistema de diseño, rutas protegidas por rol y pantallas para disponibilidad, catálogo, alertas, Kardex, despachos, recepciones, OCR, cotizaciones, ventas, clientes, proveedores, compras, agente de IA y usuarios. Las pantallas se documentan en [docs/diseno/pantallas](../docs/diseno/pantallas/README.md).

El backend Spring Boot está implementado en `backend/`. En desarrollo, Vite usa la API real por defecto y reenvía las rutas `/api` a `http://localhost:8080`, según `vite.config.ts`. Para ejecutar la interfaz conectada, inicia PostgreSQL y el backend antes de Vite.

El Dockerfile del frontend sirve los archivos estáticos mediante nginx. La configuración actual de nginx **no** reenvía `/api` al backend; para publicar la aplicación conectada con Docker, hay que completar ese proxy. En compilaciones de producción, establece `VITE_API_REAL=true`; sin ello la aplicación selecciona los datos simulados.

## Requisitos

- Node.js 22.22 o posterior.
- npm 10 o posterior.

## Instalación y ejecución local

Desde `frontend/`:

```powershell
npm ci
npm run dev
```

Vite sirve la aplicación en `http://localhost:5173` y reenvía `/api` al backend local en el puerto 8080.

## Scripts

| Comando | Función |
| --- | --- |
| `npm run dev` | Inicia Vite en desarrollo. |
| `npm run build` | Comprueba tipos y genera `dist/`. |
| `npm run preview` | Previsualiza la compilación. |
| `npm run typecheck` | Comprueba los tipos TypeScript. |
| `npm run lint` | Ejecuta ESLint. |
| `npm run format` | Aplica Prettier. |
| `npm run format:check` | Comprueba el formato. |
| `npm test` | Ejecuta Vitest y Testing Library. |
| `npm run test:watch` | Ejecuta pruebas interactivas. |
| `npm run storybook` | Inicia Storybook en el puerto 6006. |
| `npm run build-storybook` | Genera Storybook estático. |

Antes de integrar un cambio, seguir el flujo de revisión y verificación definido en el repositorio.

## Tecnologías

| Elemento | Tecnología |
| --- | --- |
| Construcción | Vite 8 |
| Lenguaje | TypeScript 6, modo estricto |
| Interfaz | React 19, Mantine 9 |
| Navegación | React Router 8 |
| Estado del servidor | TanStack Query 5 |
| Validación | Zod 4 |
| Pruebas | Vitest 5, Testing Library y jsdom |
| Calidad | ESLint 9 y jsx-a11y |

## Estructura

```text
frontend/
├── Dockerfile
├── nginx.conf
├── vite.config.ts
└── src/
    ├── app/
    ├── components/
    ├── features/
    ├── routes/
    ├── api/
    ├── lib/
    └── assets/
```

El alias `@/` equivale a `src/`. Las pantallas y consultas por módulo están en `src/features/`; el cliente HTTP está en `src/api/`.

## Rutas y roles

La navegación y las rutas protegidas se definen en `src/routes/navigation.ts` y `src/routes/AppRoutes.tsx`. Ocultar una opción según el rol no sustituye la autorización del backend, que es responsable de validar el acceso a cada operación.

## API real y datos simulados

`API_REAL` se activa en el servidor de desarrollo por defecto. Se puede forzar con `VITE_API_REAL=true` o `false`. Las pruebas usan datos simulados. En modo simulado, las operaciones modificadoras solo cambian datos en memoria y se pierden al recargar.

Las credenciales `admin`, `vendedor`, `rquispe` y `reponedor` con clave `demo1234` corresponden únicamente al modo simulado definido en `src/features/seguridad/fixtures.ts`. No son las credenciales del backend. El administrador inicial del backend se define localmente mediante `MIGAL_ADMIN_USERNAME` y `MIGAL_ADMIN_CLAVE`; no guardes esos secretos en el repositorio.

## Docker

El Dockerfile construye la SPA con Node y la sirve con nginx. La configuración actual permite las rutas de la SPA, pero no proxifica `/api`. La publicación conectada al backend requiere agregar dicho proxy y definir `VITE_API_REAL=true` durante la compilación.

## Diseño y accesibilidad

- Sistema de diseño: [docs/diseno/sistema-de-diseno.md](../docs/diseno/sistema-de-diseno.md).
- Componentes y pantallas: [docs/diseno/componentes/README.md](../docs/diseno/componentes/README.md).
- Prototipo: [docs/diseno/prototipo/README.md](../docs/diseno/prototipo/README.md).

La accesibilidad se apoya en ESLint, pruebas de Testing Library y axe, y revisión interactiva en Storybook. Las pruebas automatizadas no validan por completo la composición responsive; revisar manualmente en anchos de 390, 768 y 1280 px, ambos temas y zoom de 200 % antes de integrar cambios visuales.

