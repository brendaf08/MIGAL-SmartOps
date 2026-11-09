# PageHeader

Cabecera estándar de cada página: ruta, título (`h1`), estado de la entidad, descripción y acciones principales.

## Uso

- `title`, `description`, `breadcrumbs`, `status` (un `StatusBadge` con `variant="sello"` en documentos), `actions`.
- Una página tiene un solo `PageHeader`; las acciones se ordenan de menor a mayor énfasis, y la principal (`primary`) queda a la derecha.

## Reglas

- El título nombra el recurso o la tarea ("Inventario por almacén", "Venta V-0042").
- Las acciones no disponibles por estado o rol se ocultan; si se muestran deshabilitadas, se explica el motivo en la descripción o en un aviso.
- En tablet las acciones pasan debajo del título (diseño envolvente).

## Comportamiento responsive

- Teléfono: el título pasa a 24 px, las acciones se apilan debajo y se expanden a ancho completo.
- Tablet y escritorio: acciones a la derecha del título, con envoltura si no caben.
