# ActionBar

Barra de resumen y acciones que acompaña a un flujo de confirmación. Reúne en un solo lugar el estado de lo que se va a confirmar, el motivo por el que no se puede confirmar todavía y las acciones. Es el patrón de cierre de recepciones, despachos, ventas y cotizaciones.

## Uso

- `summary` (insignias con el recuento por estado), `reason` (texto del bloqueo; se anuncia con `role="status"`), `children` (botones: cancelar a la izquierda y la acción principal a la derecha) y `aria-label`.
- La acción principal pasa a `size="lg"` y se deshabilita mientras exista un motivo de bloqueo; el motivo siempre es visible junto al botón, nunca solo un botón apagado.

## Reglas

- Bloquear únicamente lo que el negocio exige: una línea sin producto asociado impide confirmar; una cantidad menor a la pedida (recepción parcial, RF-COM-11) no bloquea, pero se resalta con "Faltan N".
- El resumen cuenta por estado con insignias que repiten forma y texto, no solo color.
- Una sola acción principal por barra.

## Comportamiento responsive

- Teléfono: la barra queda fija al pie con el área segura inferior y las acciones apiladas a ancho completo, con la principal arriba (zona del pulgar).
- Tablet y escritorio: barra fija al pie del contenido con las acciones a la derecha.

Equivalente en Mantine: `Paper` con `pos="sticky"` y `Group`.
