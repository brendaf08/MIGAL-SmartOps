# Prototipo de referencia

Este prototipo reproduce los componentes y las pantallas del sistema de diseño en HTML, CSS y JavaScript. Sirve como especificación visual y de comportamiento para la implementación del frontend. No es código de producción.

## Visualización

1. Abrir una terminal en esta carpeta.
2. Iniciar un servidor estático, por ejemplo:

```sh
python -m http.server 8765
```

3. Abrir `http://localhost:8765/` y seleccionar una vista previa.

Las vistas previas cargan React 18 desde un CDN, por lo que requieren conexión a internet, y las fuentes desde Google Fonts. Sin conexión se aplican las fuentes de respaldo.

## Contenido

| Archivo | Descripción |
| --- | --- |
| `index.html` | Listado de vistas previas. |
| `previews/` | Una vista previa por componente y por pantalla de referencia. |
| `tokens.css` | Variables CSS de los temas claro y oscuro. Equivale a `frontend/src/theme/tokens.css`. |
| `bundle.css` | Estilos de los componentes, construidos sobre los tokens. |
| `bundle.js` | Componentes en React sin JSX, publicados en el objeto global `Migal`. |
| `index.d.ts` | Tipos de las propiedades de cada componente, como documentación. |
| `progresol-logo.png` | Logo de Progresol. |

## Uso previsto

- Consultar el comportamiento esperado de cada componente: estados, teclado, atributos de accesibilidad y variantes.
- Comparar la implementación real con su vista previa durante la revisión visual.
- Reutilizar las decisiones de diseño, no el código: el frontend se implementa con TypeScript, Mantine y CSS Modules, según [ADR-001](../../adr/adr-001-stack-frontend.md) y [ADR-002](../../adr/adr-002-sistema-de-diseno.md).
- No importar `bundle.js` ni `bundle.css` desde `frontend/`.
