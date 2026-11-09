# NumberInput

Entrada de cantidades con botones de disminuir y aumentar, soporte de teclado (flechas arriba y abajo), límites y unidad de medida visible.

## Uso

- `value` / `defaultValue`, `onChange(number)`, `min`, `max`, `step`, `unit`, más los props de campo (`label`, `description`, `error`, `required`, `size`).
- La unidad (`bolsas`, `m3`, `unid.`, `kg`) proviene de la unidad de medida del producto (RF-PRO-02).

## Reglas

- La cantidad nunca es negativa; `min` refleja el stock disponible en salidas y traslados (RF-KAR-06) y `max` la cantidad solicitada en recepciones y despachos.
- Los botones de paso son de 40 px (48 px en `lg`), aptos para uso táctil.
- Las cifras usan formato tabular; los valores decimales usan punto.

Equivalente en Mantine: `NumberInput` con `min`, `max`, `step` y `suffix`.
