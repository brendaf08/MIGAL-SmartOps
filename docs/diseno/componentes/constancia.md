# Constancia

Constancia que cierra una operación de inventario (recepción, despacho, ajuste, traslado). Contesta de inmediato "¿qué cambió?": el estado como sello, la existencia de cada producto antes y después, lo que queda pendiente y el siguiente paso. Es el final de los flujos de almacén.

## Uso

- `sello` (normalmente un `StatusBadge` con `variant="sello"`), `title`, `description`, `cambios` (`[{ producto, ubicacion, antes, despues, unidad }]`), `nota`, `actions` y `vibrate` (por omisión activa).
- Se monta como resultado de la acción de confirmar, nunca al cargar la página.

## Reglas

- El sello se estampa una sola vez (200 ms, de 1.04 a 1 de escala y opacidad), sin rebote; con movimiento reducido aparece fijo.
- En teléfono emite una vibración de 30 ms como confirmación táctil, salvo con movimiento reducido o si el dispositivo no la admite.
- El resumen usa la doble línea contable y cifras tabulares; la columna "Después" va en peso 700.
- El contenedor tiene `role="status"` para que el cierre se anuncie a los lectores de pantalla.
- Una única acción principal (ver el movimiento en el Kardex) y una secundaria para continuar el trabajo.

## Comportamiento responsive

- Teléfono: relleno de 16 px y acciones apiladas a ancho completo.
- Tablet y escritorio: ancho máximo de 560 px centrado.

Equivalente en Mantine: componente propio sobre `Paper` y `Table`.
