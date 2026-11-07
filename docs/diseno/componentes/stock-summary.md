# StockSummary

Resumen de la existencia de un producto en una ubicación o de forma consolidada. La cantidad **libre** (lo que puede reservarse o venderse) es el foco: 28 px, peso 700, a la izquierda y separada por un filete; el stock físico y el reservado quedan como contexto en un nivel menor.

## Uso

- `fisico`, `reservado`, `libre` (opcional; se calcula como físico menos reservado), `minimo`, `unidad`, `compact` (variante de tabla), `hideLevel`.
- Nivel de alerta con `StatusBadge`: `Normal` (libre mayor al mínimo), `Minimo` (libre menor o igual al mínimo, RF-KAR-09) y `Critico` (libre igual a cero).

## Reglas

- Las tres cantidades siempre se rotulan (Libre, Físico, Reservado); la interfaz de ventas y cotizaciones debe mostrarlas por ubicación y consolidadas.
- El stock mínimo es una alerta de reposición: no impide reservar unidades disponibles.
- Supuesto: el nivel se calcula sobre el stock libre. Confirmar con el negocio si debe calcularse sobre el físico.
- Las cifras de cada ubicación (tienda principal, Almacén 1, Almacén 2) se presentan en filas de tabla con la variante `compact`.
