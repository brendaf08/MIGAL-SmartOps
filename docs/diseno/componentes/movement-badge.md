# MovementBadge

Indica si un movimiento de Kardex es de entrada o de salida y, opcionalmente, su cantidad con signo. Corresponde a la enumeración `TipoOperacion`.

## Uso

- `tipo`: `Entrada` (flecha hacia abajo, tono verde) o `Salida` (flecha hacia arriba, tono de marca).
- `cantidad`: número positivo; el componente antepone `+` o `−` y aplica cifras tabulares.

## Reglas

- La distinción se apoya en la palabra y la flecha, no solo en el color (RF-KAR-05).
- Se usa en las columnas "Tipo" y "Cantidad" del Kardex y del historial de movimientos; el origen (`Despacho`, `Recepcion`, `Ajuste`, `Traslado`) se muestra en una columna aparte como texto.
- Un traslado interno genera dos movimientos (salida en el origen y entrada en el destino); ambos se listan por separado y enlazan al mismo traslado.
