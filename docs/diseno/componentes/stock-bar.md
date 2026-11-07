# StockBar

Barra segmentada que muestra de un vistazo cómo se reparte el stock de un producto: libre, reservado y, cuando existe, la cantidad solicitada sin cobertura (pendiente de abastecimiento). Bajo la barra se dibuja una regla graduada, como una cinta métrica, con la escala de 0 al total.

## Uso

- `libre`, `reservado`, `pendiente`, `unidad`, `label` (texto accesible), `legend` (`false` oculta la leyenda).
- Cada segmento se identifica con un color y con la leyenda numérica; el segmento pendiente se distingue además con un patrón de rayas, de modo que no dependa solo del color.

## Reglas

- La parte pendiente no es stock físico: representa cantidades vendidas aún sin existencia y no genera salidas de Kardex.
- La barra tiene `role="img"` con un resumen textual; la leyenda es la fuente de lectura exacta y la regla es decorativa (`aria-hidden`).
- Se usa en el detalle de producto y de venta; en tablas se prefiere `StockSummary` compacto.

Equivalente en Mantine: `Progress.Root` con tres `Progress.Section`.
