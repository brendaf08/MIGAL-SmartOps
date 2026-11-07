# Tabs

Pestañas para alternar vistas de un mismo contexto, por ejemplo "Todos", "Stock crítico" y "Movimientos" dentro de inventario.

## Uso

- `items`: `[{ value, label, count, icon, disabled }]`, `value`, `onChange(value)`, `idPrefix` (enlaza `aria-controls` con el panel), `aria-label` de la lista.
- El panel asociado lo presenta el consumidor con `role="tabpanel"`.

## Reglas

- Flechas izquierda y derecha, Inicio y Fin mueven el foco y la selección; solo la pestaña activa está en el orden de tabulación.
- Máximo cinco pestañas visibles; etiquetas de una o dos palabras; el contador es opcional.
- Las pestañas no sustituyen a la navegación principal ni representan pasos secuenciales (usar `Stepper`).

Equivalente en Mantine: `Tabs`.
