# SearchInput

Campo de búsqueda con ícono de lupa y botón "Limpiar búsqueda" visible cuando hay texto. Es el punto de entrada de las consultas de disponibilidad, catálogo y Kardex.

## Uso

- `value`, `onChange(valor)`, `onSubmit(valor)` (Enter), `placeholder`, `size`, `aria-label` cuando no hay `label`.
- Aplicar una espera (debounce) de unos 300 ms antes de consultar al servidor.

## Reglas

- El texto de ayuda indica por qué campos se busca ("nombre o SKU").
- Una búsqueda sin resultados muestra `EmptyState` con la opción de limpiar filtros; no deja la tabla en blanco.
- Los términos coloquiales del cliente se resuelven en el backend mediante Fuzzy Matching, no en el campo.

Equivalente en Mantine: `TextInput` con `leftSection` y `rightSection`, y `useDebouncedValue`.
