# StatusBadge

Insignia de estado para cada entidad del modelo de dominio. Traduce los valores de las enumeraciones del backend (`EstadoCotizacion`, `EstadoVenta`, `EstadoGuia`, `EstadoCompra`, `EstadoRecepcion`, `EstadoDocumento`, `EstadoReserva`) a un tono, un texto legible y un marcador, de modo que el mismo estado se vea igual en todas las pantallas.

## Señalización

Los cuatro tonos semánticos usan la forma de la señalización industrial de una ferretería y de un almacén, de modo que el estado se distingue por forma y no solo por color (útil para quienes no distinguen rojo de verde):

- Cuadrado con marca (`status-ok`): correcto, completado, existencia suficiente.
- Triángulo (`alert-triangle`): atención, pendiente de una acción o stock en el mínimo.
- Octógono (`status-danger`): crítico, cancelado o rechazado.
- Círculo (`info`): información de proceso.

Los estados de marca (listo para el siguiente paso) y neutrales (inactivo, vencido, en preparación) conservan el ícono propio de la entidad.

## Sello

En el encabezado de un documento (cotización, venta, guía) el estado se presenta como sello: `variant="sello"`, con doble contorno y la fuente de rotulado, sin ícono (la forma ya la aporta el contorno). Se usa una sola vez por página, en `PageHeader`. En listados y tablas se mantiene la insignia normal.

## Uso

- `entity`: `cotizacion`, `venta`, `guia`, `compra`, `recepcion`, `documento`, `reserva`, `stock` o `registro`.
- `value`: valor exacto de la enumeración (`AceptadaCliente`, `PendienteAbastecimiento`, `Procesado_OCR`, entre otros).
- `label`: texto alternativo opcional; por omisión se usa el texto del mapa central.

## Reglas

- Todo estado se representa con `StatusBadge`; no se arman insignias manuales con colores sueltos.
- Mapa de tonos: espera o pendiente de acción externa = `warning`; en proceso o informativo = `info`; listo para el siguiente paso = `brand`; completado = `success`; rechazado, cancelado o crítico = `danger`; inactivo o vencido = `neutral`.
- Un valor nuevo en el backend se agrega una sola vez en el mapa `STATUS`; un valor desconocido se muestra como insignia neutral con el texto original.
- Los nombres definitivos de los estados de cotización, venta y guía están pendientes de armonización con los casos de uso; el mapa debe actualizarse cuando se confirmen.

Equivalente en la implementación con Mantine: componente propio sobre `Badge` con el mapa en `frontend/src/features/shared/status.ts`.
