# StockLocations

Plano esquemático de dónde se encuentra un producto: una celda por ubicación (tienda principal y los dos almacenes), con la cantidad libre, una barra de la parte libre frente a la reservada y el ancho de la celda proporcional a la existencia. Responde a la pregunta de quien consulta: "¿hay y dónde?".

## Uso

- `ubicaciones`: `[{ nombre, cantidad (físico), reservado }]`; `unidad`; `escala` (existencia a partir de la cual una celda alcanza el ancho máximo); `compact`; `label`.
- La cifra de cada celda es la cantidad libre de esa ubicación (física menos reservada). Una ubicación sin existencia se muestra con borde punteado y el texto "Sin stock", para que el vacío también se vea.

## Reglas

- Siempre en el orden Tienda, Almacén 1, Almacén 2, para que la posición se aprenda.
- No sustituye al detalle numérico: el físico y el reservado exactos se consultan en el detalle del producto.
- Se usa en fichas de teléfono, detalle de producto y respuestas de disponibilidad; en la tabla de escritorio se prefieren las columnas por ubicación.

## Comportamiento responsive

- Teléfono: variante `compact`, tres celdas en una fila dentro de la ficha del producto.
- Tablet y escritorio: celdas con ancho proporcional.

Equivalente en la implementación con Mantine: componente propio con `Group` y `Progress`.
