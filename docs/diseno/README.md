# Sistema de diseño

Esta carpeta contiene la instantánea versionada del sistema de diseño de MIGAL SmartOps. Su finalidad es que el equipo y las herramientas de desarrollo dispongan del diseño dentro del repositorio, sin depender de un enlace externo ni de una sesión.

## Contenido

| Elemento | Descripción |
| --- | --- |
| [sistema-de-diseno.md](sistema-de-diseno.md) | Fundamentos, principios, color, tipografía, espaciado, iconografía, composición, integración con el stack y supuestos pendientes. |
| [tokens.json](tokens.json) | Valores normativos de color (temas claro y oscuro), tipografía, espaciado, radios, sombras, tamaños, puntos de quiebre y capas. |
| [componentes/](componentes/README.md) | Un documento por componente y por pantalla de referencia, con su uso, sus reglas y su equivalente en Mantine. |
| [pantallas/](pantallas/README.md) | Pantallas implementadas por módulo y matriz de trazabilidad entre casos de uso, pantallas y pruebas. |
| [prototipo/](prototipo/README.md) | Prototipo ejecutable de referencia, con las vistas previas de todos los componentes y pantallas. |

Los valores aplicados al código se encuentran en `frontend/src/theme/` (`tokens.css` y `theme.ts`) y el logo, en `frontend/src/assets/`.

## Procedencia

La instantánea se exportó del artefacto del sistema de diseño (versión 8, 07/11/2026). El artefacto es privado y solo es accesible para su propietario y las personas con acceso concedido.

## Fuente de verdad

| Etapa | Fuente de verdad | Procedimiento de cambio |
| --- | --- | --- |
| Diseño del prototipo | El artefacto del sistema de diseño. | Modificar el artefacto, volver a exportarlo a esta carpeta y abrir un pull request con el cambio. |
| Implementación estabilizada | El código de `frontend/` y los tokens de `frontend/src/theme/`. | Modificar el código y actualizar esta documentación en el mismo pull request. |

El paso de una etapa a la otra se registra en un nuevo ADR cuando los componentes reales del frontend reemplacen al prototipo.

## Equivalencia de rutas

Los documentos de esta carpeta conservan las referencias del artefacto. Su equivalente en el repositorio es el siguiente:

| En el artefacto | En el repositorio |
| --- | --- |
| `handoff/mantine-theme.ts` | `frontend/src/theme/theme.ts` |
| `tokens.css` | `frontend/src/theme/tokens.css` |
| `assets/Logos/progresol-logo.png` | `frontend/src/assets/progresol-logo.png` |
| `components/<Componente>/README.md` | `docs/diseno/componentes/<componente>.md` |
| `components/bundle.js`, `bundle.css`, `index.d.ts` | `docs/diseno/prototipo/` |

## Decisiones relacionadas

- [ADR-001: Stack tecnológico del frontend](../adr/adr-001-stack-frontend.md).
- [ADR-002: Sistema de diseño del frontend](../adr/adr-002-sistema-de-diseno.md).
