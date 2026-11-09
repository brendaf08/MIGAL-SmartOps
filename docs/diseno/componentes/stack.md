# Stack

Contenedor flexible que ordena a sus hijos en columna o fila con un espaciado tomado de la escala `space-*`.

## Uso

- `gap` (1, 2, 3, 4, 5, 6, 8, 10 o 12, equivalentes a `space-N`), `direction` (`column` o `row`), `align`, `justify`, `wrap`, `as`.
- Sustituye los márgenes sueltos: el espaciado entre elementos pertenece al contenedor, no a cada hijo.

## Reglas

- Entre campos de un formulario usar `gap` 4 (16 px); entre tarjetas, `gap` 6 (24 px).
- Para cuadrículas responsive usar `SimpleGrid` de Mantine o CSS Grid con `minmax`, no filas con anchos fijos.

Equivalente en Mantine: `Stack` y `Group`.
