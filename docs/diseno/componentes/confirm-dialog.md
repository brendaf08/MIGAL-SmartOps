# ConfirmDialog

Diálogo de confirmación para acciones que modifican inventario o no se pueden deshacer: cancelar una venta, confirmar el despacho, confirmar una recepción, desactivar un proveedor. Aplica la heurística de prevención de errores.

## Uso

- `open`, `title`, `description`, `consequences` (lista de efectos), `tone` (`danger` o `primary`), `confirmLabel`, `confirmIcon`, `cancelLabel`, `loading`, `onConfirm`, `onCancel`.
- Usa `role="alertdialog"`; no se cierra al hacer clic fuera del diálogo.

## Reglas

- El foco inicial recae en la acción segura (cancelar), no en la destructiva.
- El título nombra la acción y el objeto ("Cancelar la venta V-0042"); las consecuencias enumeran lo que cambiará en inventario, reservas y Kardex.
- El botón de confirmación repite el verbo de la acción ("Cancelar venta"), nunca "Aceptar" o "Sí".
- Mientras se procesa, el botón muestra `loading` y el diálogo no se puede cerrar.

Equivalente en Mantine: `modals.openConfirmModal` de `@mantine/modals` con contenido propio.
