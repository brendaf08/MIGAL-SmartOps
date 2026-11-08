# Columns

Disposición en columnas con separación de la escala `space-*`. Es la pieza que hace responsive a los formularios, los detalles y las pantallas con panel lateral: en escritorio muestra las columnas y, al reducirse el ancho, las apila en una sola.

## Uso

- `layout`: `two` (dos columnas iguales), `three` (tres iguales), `main-aside` (contenido y panel de 300 px) y `aside-main` (panel de 300 px y contenido); `gap` (1 a 8, equivalente a `space-N`; 5 por omisión); `as`.
- El orden del código es el orden de lectura en teléfono: escribir primero lo que debe verse primero.

## Comportamiento responsive

- Escritorio (1024 px o más): columnas según el `layout`.
- Tablet (768 a 1023 px): `main-aside` y `aside-main` se apilan; `three` pasa a dos columnas.
- Teléfono (menos de 768 px): una sola columna en todos los casos.

## Reglas

- No fijar anchos en píxeles dentro de las columnas; usar el espacio que `Columns` reparte.
- El panel lateral no contiene acciones críticas que deban verse antes del contenido en teléfono.

Equivalente en Mantine: `SimpleGrid` y `Grid` con `cols` responsive.
