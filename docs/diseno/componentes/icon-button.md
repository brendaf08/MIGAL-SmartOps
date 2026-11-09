# IconButton

Botón cuadrado que muestra solo un ícono. Pensado para acciones de fila, barras de herramientas y cierre de avisos.

## Uso

- `icon` (nombre de ícono) y `label` son obligatorios: `label` se publica como `aria-label` y como `title` (texto de ayuda al pasar el cursor).
- `variant`: `subtle` (predeterminado), `outline`, `secondary`, `primary`, `danger`; `size`: `sm` 32 px, `md` 40 px, `lg` 48 px; `loading`, `disabled`.

## Reglas

- El `label` describe la acción y el objeto ("Editar producto"), no el ícono ("Lápiz").
- En tablas con varias acciones por fila, agrupar como máximo tres íconos; el resto se mueve a un menú "Más acciones".
- En tablets usar `size="lg"` o separar los botones al menos 8 px para evitar toques accidentales.
- Un ícono con significado ambiguo se complementa con `Button` y texto.

Equivalente en Mantine: `ActionIcon` con `aria-label` obligatorio.
