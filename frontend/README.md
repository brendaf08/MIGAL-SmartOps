# Frontend

Aplicación web de MIGAL SmartOps (SPA en React). La decisión de stack se encuentra en [ADR-001](../docs/adr/adr-001-stack-frontend.md) y la del sistema de diseño en [ADR-002](../docs/adr/adr-002-sistema-de-diseno.md).

## Estado

El proyecto aún no está montado. Esta carpeta contiene únicamente los recursos de diseño aplicables al código:

| Ruta | Contenido |
| --- | --- |
| `src/theme/tokens.css` | Variables CSS de los temas claro y oscuro, derivadas de [tokens.json](../docs/diseno/tokens.json). |
| `src/theme/theme.ts` | Tema de Mantine con las paletas, la tipografía, los radios, el espaciado y los valores predeterminados de los componentes. |
| `src/assets/progresol-logo.png` | Logo de Progresol. |
| `CLAUDE.md` | Instrucciones de trabajo para las herramientas de desarrollo asistido. |

## Documentación de diseño

- [Sistema de diseño](../docs/diseno/sistema-de-diseno.md).
- [Componentes y pantallas de referencia](../docs/diseno/componentes/README.md).
- [Prototipo de referencia](../docs/diseno/prototipo/README.md).

## Dependencias de diseño previstas

Además de las definidas en ADR-001: `@fontsource-variable/bricolage-grotesque`, `@fontsource-variable/instrument-sans`, `@fontsource/barlow-semi-condensed`, `@fontsource/ibm-plex-mono`, Storybook y una librería de pruebas de accesibilidad con `axe`.

## Instalación y ejecución

Pendiente de definir al montar el proyecto (Vite). Al hacerlo, documentar en este archivo las versiones, las dependencias, los comandos de desarrollo, prueba y construcción, y la imagen Docker.
