# Frontend

Aplicación web de MIGAL SmartOps (SPA en React). La decisión de stack se encuentra en [ADR-001](../docs/adr/adr-001-stack-frontend.md) y la del sistema de diseño en [ADR-002](../docs/adr/adr-002-sistema-de-diseno.md).

## Estado

El proyecto se encuentra montado con la configuración base, el tema claro y oscuro, los proveedores globales y las herramientas de verificación. Los componentes de interfaz y las pantallas se incorporan siguiendo el orden definido en [CLAUDE.md](CLAUDE.md): primitivos, componentes de dominio y pantallas por módulo.

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
├── vitest.setup.ts
├── Dockerfile
├── nginx.conf
└── src/
    ├── main.tsx              Punto de entrada y estilos globales.
    ├── App.tsx
    ├── app/                  Proveedores globales y cliente de consultas.
    ├── theme/                Tokens, tema de Mantine, fuentes y modo claro u oscuro.
    └── assets/               Logo y recursos estáticos.
```

Las carpetas `components/`, `features/`, `api/` y `routes/` se crean al incorporar los primeros elementos de cada una. Su organización se describe en [CLAUDE.md](CLAUDE.md). El alias `@/` equivale a `src/`.

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

## Dependencias de diseño pendientes

Storybook y una librería de pruebas de accesibilidad con `axe` se incorporan junto con los primeros componentes, conforme a ADR-002.
