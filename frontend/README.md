# Frontend

Aplicación web de MIGAL SmartOps (SPA en React). La decisión de stack se encuentra en [ADR-001](../docs/adr/adr-001-stack-frontend.md) y la del sistema de diseño en [ADR-002](../docs/adr/adr-002-sistema-de-diseno.md).

## Estado

Incluye la configuración base, el tema claro y oscuro, el layout responsive (teléfono, tablet y escritorio), los primitivos y componentes de dominio del sistema de diseño, la sesión con rutas protegidas por rol y las pantallas de todos los módulos de la navegación: disponibilidad, catálogo, Kardex, despachos, recepciones con OCR, documentos, cotizaciones, proveedores y compras, agente de IA y usuarios. Incorpora además la barra de acciones con diferencias y bloqueo con motivo en la recepción, las ubicaciones de stock (`StockLocations`), la constancia de cierre de recepciones y salidas, y el modo almacén por equipo. Las pantallas de referencia del diseño (acceso, disponibilidad, cotización, recepción con OCR, despacho y agente de IA) tienen implementación completa; el resto son listados.

El backend aún no existe (la carpeta `backend/` solo contiene su README). Los datos provienen de una capa simulada, descrita en [Datos simulados](#datos-simulados-y-usuarios-de-prueba).

## Requisitos

- Node.js 22.22 o posterior (se utiliza la versión 24 en la imagen Docker).
- npm 10 o posterior.

## Instalación y ejecución

Abrir una terminal en esta carpeta y ejecutar:

```sh
npm install
npm run dev
```

La aplicación queda disponible en `http://localhost:5173`.

## Scripts

| Comando | Función |
| --- | --- |
| `npm run dev` | Inicia el servidor de desarrollo. |
| `npm run build` | Comprueba los tipos y genera los archivos estáticos en `dist/`. |
| `npm run preview` | Sirve localmente el resultado de `build`. |
| `npm run typecheck` | Comprueba los tipos de TypeScript. |
| `npm run lint` | Ejecuta ESLint, incluidas las reglas de accesibilidad de `jsx-a11y`. |
| `npm run format` | Aplica el formato de Prettier. |
| `npm run format:check` | Verifica el formato sin modificar archivos. |
| `npm test` | Ejecuta las pruebas con Vitest y Testing Library. |
| `npm run test:watch` | Ejecuta las pruebas en modo interactivo. |
| `npm run storybook` | Inicia el catálogo de componentes en `http://localhost:6006`. |
| `npm run build-storybook` | Genera el catálogo estático en `storybook-static/`. |

Antes de abrir un pull request, ejecutar `npm run lint`, `npm run typecheck`, `npm test` y `npm run build`, y registrar los resultados en la descripción.

## Tecnologías y versiones

| Elemento | Tecnología |
| --- | --- |
| Construcción | Vite 8 |
| Lenguaje | TypeScript 6 en modo estricto |
| Interfaz | React 19 y Mantine 9 (formularios, modales, notificaciones y carga de archivos) |
| Navegación | React Router 8 |
| Estado del servidor | TanStack Query 5 |
| Validación | Zod 4 |
| Pruebas | Vitest 5, Testing Library y jsdom |
| Calidad | ESLint 9 con `eslint-plugin-jsx-a11y` y Prettier |

TypeScript se mantiene en la serie 6 y ESLint en la serie 9 porque `typescript-eslint` y `eslint-plugin-jsx-a11y` aún no admiten TypeScript 7 ni ESLint 10. Actualizar ambas series cuando los paquetes mencionados las admitan.

## Estructura

```text
frontend/
├── index.html
├── vite.config.ts
├── postcss.config.js         Mixins de Mantine y puntos de quiebre para CSS Modules.
├── vitest.setup.ts
├── Dockerfile
├── nginx.conf
└── src/
    ├── main.tsx              Punto de entrada, estilos globales y enrutador.
    ├── App.tsx
    ├── app/                  Proveedores globales y cliente de consultas.
    ├── theme/                Tokens, tema de Mantine, fuentes y modo claro u oscuro.
    ├── components/
    │   ├── ui/               Primitivos genéricos (DataTable, Modal, Tabs, Stepper, Dropzone, ...).
    │   ├── domain/           StatusBadge, StockSummary, StockBar, MatchConfidence y chat.
    │   └── layout/           AppShell y PageHeader.
    ├── features/             Un directorio por módulo funcional (pantallas, consultas y tipos).
    ├── routes/               Rutas, navegación por rol y marco de las pantallas internas.
    ├── api/                  Utilidades de la capa de datos simulada.
    ├── lib/                  Formatos es-PE y utilidades.
    ├── test/                 Utilidades de prueba.
    └── assets/               Logo y recursos estáticos.
```

El alias `@/` equivale a `src/`. La organización y las reglas de dependencia se describen en [CLAUDE.md](CLAUDE.md).

## Rutas y roles

La navegación y las rutas protegidas se definen una sola vez en `src/routes/navigation.ts`. Un módulo oculto para un rol equivale a una ruta no accesible; la autorización real corresponde al backend (RNF-09). La clave de cada ítem es su ruta (`/disponibilidad`, `/ventas`, `/despachos`, etc.) y las pantallas secundarias se registran en `src/routes/AppRoutes.tsx` (`/ventas/:id`, `/despachos/:id`, `/recepciones/nueva`).

## Datos simulados y usuarios de prueba

Cada módulo expone sus consultas en un archivo `api.ts` con firmas tipadas que devuelven datos de prueba mediante `mockRequest` (`src/api/mock.ts`), con latencia simulada. Al disponer de la API REST, cada función se sustituye por una llamada HTTP sin modificar su firma ni las pantallas. Las operaciones que modifican datos (aceptar una cotización, confirmar una salida o una recepción) actualizan los datos en memoria y se reinician al recargar la página.

Usuarios de prueba para desarrollo (contraseña `demo1234` en todos los casos):

| Usuario | Rol |
| --- | --- |
| `admin` | Administrador |
| `vendedor` | Vendedor |
| `rquispe` | Almacenero |
| `reponedor` | Reponedor |

Estos usuarios se eliminan al integrar la autenticación del backend (`src/features/seguridad/fixtures.ts`).

## Tema y modo claro u oscuro

| Archivo | Contenido |
| --- | --- |
| `src/theme/tokens.css` | Variables CSS de los temas claro y oscuro, derivadas de [tokens.json](../docs/diseno/tokens.json). Archivo generado: no se edita a mano. |
| `src/theme/theme.ts` | Tema de Mantine con las paletas, la tipografía, los radios, el espaciado y los valores predeterminados de los componentes. |
| `src/theme/cssVariablesResolver.ts` | Enlace de las variables de color y de fuente de Mantine con los tokens. |
| `src/theme/global.css` | Estilos globales que consumen únicamente tokens. |
| `src/theme/fonts.ts` | Fuentes autoalojadas mediante paquetes `@fontsource`. |
| `src/theme/ThemeSync.tsx` | Sincronización del atributo `data-theme` con el esquema de color de Mantine. |
| `src/theme/useThemeMode.ts` | Hook para consultar y alternar el modo. |

El modo inicial proviene de `prefers-color-scheme` y la elección del usuario se conserva en `localStorage` con la clave `migal-color-scheme`. Un script en línea de `index.html` aplica el modo antes de renderizar para evitar un destello de tema.

Los paquetes `@fontsource-variable` registran las familias "Bricolage Grotesque Variable" e "Instrument Sans Variable". `global.css` las antepone a los nombres definidos en `tokens.css`, que se conserva sin cambios.

## Docker

La imagen se construye en dos etapas: compilación con Node y servicio de los archivos estáticos con nginx, que redirige las rutas de la SPA a `index.html`.

```sh
docker build -t migal-frontend .
docker run --rm -p 8080:80 migal-frontend
```

La aplicación queda disponible en `http://localhost:8080`.

## Documentación de diseño

- [Sistema de diseño](../docs/diseno/sistema-de-diseno.md).
- [Componentes y pantallas de referencia](../docs/diseno/componentes/README.md).
- [Prototipo de referencia](../docs/diseno/prototipo/README.md).

## Catálogo de componentes y accesibilidad

El catálogo vive en Storybook (`.storybook/`) y contiene una historia por componente de `components/ui/`, `components/domain/` y `components/layout/`, en archivos `*.stories.tsx` junto a cada componente. La barra de herramientas permite alternar entre el tema claro y el oscuro tenue, y el complemento de accesibilidad de Storybook ejecuta `axe` sobre cada historia. La telemetría de Storybook está desactivada.

La accesibilidad se verifica en cuatro niveles:

- `eslint-plugin-jsx-a11y` durante `npm run lint`.
- Pruebas de Testing Library con roles, nombres accesibles y teclado.
- `vitest-axe` por pantalla (`src/a11y.test.tsx`) y por historia (`src/historias.a11y.test.tsx`), sin la regla de contraste, que depende de los tokens y no de jsdom, ni la de regiones, que corresponde a páginas completas.
- El complemento de accesibilidad de Storybook, de forma interactiva.

Una historia nueva se agrega junto al componente y la prueba de `src/historias.a11y.test.tsx` la recoge sin cambios adicionales.

## Verificación responsive pendiente

Las composiciones de teléfono, tablet y escritorio se implementan con CSS Modules y los puntos de quiebre del tema, y las pruebas automatizadas no evalúan la maquetación. Antes de integrar, comprobar manualmente cada pantalla en 390, 768 y 1280 px, en tema claro y oscuro y con el zoom al 200 %.
