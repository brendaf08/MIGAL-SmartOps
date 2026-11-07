# Modal

Ventana modal que concentra una tarea corta (registrar un movimiento manual, completar datos de un cliente) sin salir de la página.

## Uso

- `open`, `title`, `description`, `onClose`, `size` (`sm` 440 px, `md` 560 px, `lg` 760 px), `children` (cuerpo), `footer` (acciones).
- `inline` representa el diálogo dentro del documento, sin velo fijo, para vistas previas y pruebas.
- El primer control del cuerpo o el elemento con `data-autofocus` recibe el foco al abrir; al cerrar el foco vuelve al elemento que lo abrió.

## Reglas

- Atrapa el foco (Tab y Mayús+Tab circulan dentro) y se cierra con Escape o con el botón de cierre.
- Formularios de más de seis campos o con subtareas van en una página, no en un modal.
- Acciones en el pie: confirmar (`primary`) a la derecha y cancelar (`outline`) a su izquierda.
- Se muestra encima de la barra lateral y por debajo de las notificaciones (`z-modal` < `z-toast`).

Equivalente en Mantine: `Modal` de `@mantine/core`.
