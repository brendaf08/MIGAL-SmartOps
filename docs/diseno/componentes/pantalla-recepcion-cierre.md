# PantallaRecepcionCierre

Cierre de una recepción (CU-13) con `Constancia`. Responde a la pregunta del usuario tras confirmar ("¿qué cambió?"): el sello del estado se estampa una sola vez, el resumen en doble línea contable muestra cada producto con su existencia antes y después, y la nota avisa de lo que queda pendiente de la orden de compra.

## Decisiones de diseño

- El sello solo aparece al ejecutar la acción, nunca al recargar, y queda fijo con movimiento reducido. En teléfono se agrega una vibración breve de 30 ms como confirmación táctil.
- "Antes y después" evita que el almacenero compruebe el resultado en otra pantalla; la acción principal lleva al Kardex.
- La recepción parcial (RF-COM-11) se explica en la nota, con la acción para registrar la siguiente.
