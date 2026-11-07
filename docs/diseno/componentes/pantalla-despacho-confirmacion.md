# PantallaDespachoConfirmacion

Confirmación de la salida física de mercadería (CU-06). Compone `PageHeader`, `Alert` y `ConfirmDialog`.

## Decisiones de diseño

- La salida de Kardex se registra al confirmar la salida física, no al aprobar la venta ni al imprimir la guía; el diálogo lo enuncia como consecuencia explícita.
- El foco inicial queda en "Volver" para evitar confirmaciones accidentales.
