-- Migración V19: fecha en que el cliente requiere la entrega de una venta.
-- Se registra en la venta (no en la cotización) porque es la venta la que se abastece. Es opcional y sirve para planificar
-- las órdenes de compra de las ventas pendientes de abastecimiento; la trazabilidad hacia la cotización de origen ya la da
-- la columna id_cotizacion de la venta.
ALTER TABLE venta ADD COLUMN fecha_entrega_requerida date;

COMMENT ON COLUMN venta.fecha_entrega_requerida IS 'Fecha en que el cliente requiere la entrega; nula si no se indicó.';
