# Stat

Indicador con una cifra destacada, para alertas operativas del panel inicial (stock crítico, ventas pendientes de abastecimiento, recepciones por validar, despachos listos). La cifra es el foco: 32 px, peso 700, cifras tabulares; la etiqueta y la descripción quedan en un nivel menor.

## Uso

- `label`, `value` (texto ya formateado), `hint`, `tone` (`warning`, `danger`, `success`).
- El tono se expresa con el marcador de señalización junto a la etiqueta (triángulo, octógono o cuadrado), no con una caja de color ni con un ícono decorativo.
- Cada indicador enlaza al listado filtrado correspondiente.

## Reglas

- Máximo cuatro indicadores por fila. Si el mismo dato ya se ofrece como filtro con contador (por ejemplo, la pestaña "En alerta"), no se repite como indicador.
- Un indicador sin datos muestra un guion largo y no cero.
- Superficie con borde y sin sombra; los indicadores no compiten con la tabla que acompañan.
