# EmptyState

Contenido que se muestra cuando una lista, tabla o panel no tiene datos. Orienta sobre el motivo y ofrece el siguiente paso.

## Uso

- `icon`, `title`, `description`, `action` (nodo, normalmente un `Button`).
- Tipos: sin resultados (acción: limpiar filtros), primer uso (acción: crear el primer registro), sin permiso (explicar qué rol tiene acceso).

## Reglas

- Título corto y descripción de una o dos frases; sin tono de reproche ni humor.
- La acción es la que permite salir del estado vacío; si el usuario no tiene permiso para ejecutarla, se omite y se explica el motivo.
- No se usa para errores de carga: esos casos usan `Alert` con la opción de reintentar.
