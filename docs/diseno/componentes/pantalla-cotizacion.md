# PantallaCotizacion

Pantalla de cotización a un cliente (CU-09) con `PageHeader` y sello de estado, `Stepper`, `Table` con pie de totales, `Alert` y tarjetas de cliente y condiciones.

## Decisiones de diseño

- Los importes se leen como un libro contable: cifras tabulares en la fuente de rotulado, alineadas a la derecha, y totales en un pie con doble línea (subtotal, descuento y total), con el total en peso 700.
- El estado del documento se presenta como sello en el encabezado; el ciclo de vida (Emitida, Aceptada, Validada, Venta) se muestra en el `Stepper` (RF-COT-05).
- El aviso informativo aclara que una cotización no modifica ni reserva existencias (RF-COT-08), para evitar una expectativa errónea.
- El cliente es opcional (RF-CLI-04): la tarjeta permite cambiarlo y su ausencia no bloquea la emisión.
