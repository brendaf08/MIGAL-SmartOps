# Pagination

Navegación entre páginas de un listado, con el resumen "Mostrando 41–60 de 238".

## Uso

- `page` (base 1), `pages`, `total`, `pageSize`, `onChange(page)`.
- Se muestran la primera y la última página y las adyacentes a la actual; el resto se abrevia con puntos suspensivos.

## Reglas

- Tamaño de página predeterminado: 20 registros. La paginación se ejecuta en el servidor para mantener la respuesta de consultas habituales por debajo de 3 segundos (RNF-06).
- Al cambiar de página, el foco vuelve al encabezado de la tabla y se conserva el orden y los filtros vigentes.
- Los botones anterior y siguiente se deshabilitan en los extremos.

Equivalente en Mantine: `Pagination` con `total` y `value`.
