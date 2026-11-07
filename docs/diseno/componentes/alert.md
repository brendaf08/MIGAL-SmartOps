# Alert

Mensaje persistente dentro de la página que informa un estado, una advertencia o un error relacionado con el contenido cercano.

## Uso

- `tone` (`info`, `success`, `warning`, `danger`), `title`, contenido, `action` (nodo, normalmente un `Button` sm), `onClose`.
- `danger` y `warning` usan `role="alert"` (se anuncian de inmediato); `info` y `success` usan `role="status"`.

## Reglas

- El texto responde tres preguntas: qué pasó, por qué y qué hacer ahora.
- Se coloca junto al elemento afectado (encima del formulario o la tabla), no en un extremo de la pantalla.
- Un aviso por motivo; no apilar más de dos. Los resultados de una acción puntual (guardado, envío) usan `Notification`.
- Las reglas de negocio visibles, como la viabilidad de abastecimiento en ventas con stock insuficiente, se presentan como `warning` con la consecuencia explícita.

Equivalente en Mantine: `Alert` con `variant="light"` y `icon`.
