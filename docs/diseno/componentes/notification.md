# Notification

Mensaje flotante breve que confirma el resultado de una acción o informa un proceso en curso. Se muestra en la esquina superior derecha, sobre los modales.

## Uso

- `tone`, `title`, contenido, `loading` (indicador de progreso), `onClose`.
- Confirmaciones de éxito se cierran solas a los 5 s; los errores y los procesos en curso permanecen hasta que se resuelven o se cierran.

## Reglas

- `role="status"` y `aria-live="polite"`: el mensaje se anuncia sin interrumpir.
- No usar para información crítica que deba recordarse (preferir `Alert`) ni para solicitar decisiones (usar `ConfirmDialog`).
- Un solo mensaje por resultado; agrupar mensajes repetidos.

Equivalente en Mantine: `@mantine/notifications` con `notifications.show` y `loading`.

## Comportamiento responsive

- Teléfono: la notificación ocupa el ancho completo de la pantalla.
