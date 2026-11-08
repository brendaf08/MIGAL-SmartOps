# StockBar

Barra segmentada que muestra de un vistazo cómo se reparte el stock de un producto: libre, reservado y, cuando existe, la cantidad solicitada sin cobertura (pendiente de abastecimiento). Bajo la barra se dibuja una regla graduada, como una cinta métrica, con la escala de 0 al total.

## Uso

- `libre`, `reservado`, `pendiente`, `unidad`, `label` (texto accesible), `legend` (`false` oculta la leyenda).
- Cada segmento se identifica con un color y con la leyenda numérica; el segmento pendiente se distingue además con un patrón de rayas, de modo que no dependa solo del color.

## Cinta por empaque

- `empaque` (`{ paso, unidad }`) hace que las marcas de la regla caigan en múltiplos de la unidad de venta (cada 100 bolsas, cada millar, cada varilla) en lugar de en décimas arbitrarias; `minimo` y `reposicion` agregan dos marcas con forma de marcador de estante (cuadrada para el mínimo, circular para la reposición) sobre la barra, que también figuran en la leyenda con su valor.
- `compact` reduce la barra a un trazo de 8 px sin regla ni escala, para celdas de tabla; la lectura exacta se conserva en el texto accesible de la barra.

## Reglas

- La parte pendiente no es stock físico: representa cantidades vendidas aún sin existencia y no genera salidas de Kardex.
- La barra tiene `role="img"` con un resumen textual; la leyenda es la fuente de lectura exacta y la regla es decorativa (`aria-hidden`).
- Se usa en el detalle de producto y de venta; en tablas se prefiere `StockSummary` compacto.

Equivalente en Mantine: `Progress.Root` con tres `Progress.Section`.
