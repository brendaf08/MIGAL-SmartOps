# Icon

Conjunto de íconos de trazo para toda la interfaz, dibujado sobre una rejilla de 24 px con trazo de 1.75 px, terminaciones redondeadas y color heredado (`currentColor`).

## Uso

- `name`: identificador del ícono. Los nombres de dominio usan sustantivos del negocio (`kardex`, `almacen`, `despacho`, `recepcion`, `reserva`, `cotizacion`, `orden-compra`, `proveedor`, `scan`, `sku`, `rol`) y los genéricos usan nombres de acción (`plus`, `edit`, `trash`, `search`). Los íconos de estado siguen la señalización industrial: `status-ok` (cuadrado), `alert-triangle` (triángulo), `status-danger` (octógono) e `info` (círculo).
- `size`: 16 (`icon-sm`) en insignias y botones sm, 20 (`icon-md`) en botones, campos y navegación, 24 (`icon-lg`) en botones lg y avisos.
- `title`: obligatorio solo cuando el ícono comunica algo sin texto adyacente; en caso contrario el ícono queda oculto a tecnologías de asistencia (`aria-hidden`).

## Reglas

- Un ícono acompaña a un texto; nunca lo reemplaza, salvo en `IconButton`, que exige `label`.
- El color proviene del texto contenedor. Los estados usan los tokens `success`, `warning`, `danger` e `info`, siempre junto a una palabra.
- Un mismo concepto utiliza siempre el mismo ícono en todo el sistema (por ejemplo, `reserva` para reservas de stock y `despacho` para salidas físicas).
- Los íconos nuevos respetan la rejilla de 24 px, el trazo de 1.75 px y un margen interior de 3 px.

Equivalente en la implementación con Mantine: cada nombre se publica como componente de `frontend/src/components/ui/icons` con la misma API (`size`, `title`).
