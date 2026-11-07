# Breadcrumbs

Ruta jerárquica de la página actual, con enlaces a los niveles superiores.

## Uso

- `items`: `[{ label, href, onClick }]`; el último elemento representa la página actual (`aria-current="page"`) y no es enlace.
- Se coloca sobre el título de la página (`PageHeader` lo incorpora mediante `breadcrumbs`).

## Reglas

- Solo se usa en vistas con profundidad de dos niveles o más (listado > detalle).
- Las etiquetas coinciden con los títulos de las páginas.

Equivalente en Mantine: `Breadcrumbs` con `Anchor`.
