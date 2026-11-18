-- Índices para las consultas ordenadas más frecuentes de los listados (RNF-06) y unicidad del documento OCR que apoya
-- una recepción. Solo agrega índices: no modifica datos ni columnas.

-- Una recepción se apoya en un solo documento vigente: DocumentoDigitalRepository.idDocumentoDeRecepcion devuelve un
-- único valor, y sin esta restricción dos recepciones simultáneas podían asociar el mismo documento o una recepción
-- quedar con dos documentos. Los documentos eliminados lógicamente no cuentan.
CREATE UNIQUE INDEX uq_documento_digital_recepcion_vigente ON documento_digital (id_recepcion)
    WHERE id_recepcion IS NOT NULL AND fecha_eliminacion IS NULL;

-- Kardex: se consulta del movimiento más reciente al más antiguo.
CREATE INDEX ix_movimiento_kardex_fecha_hora ON movimiento_kardex (fecha_hora DESC, id_movimiento DESC);

-- Listados por estado, del más reciente al más antiguo.
CREATE INDEX ix_venta_estado_fecha ON venta (estado, fecha_venta DESC);
CREATE INDEX ix_cotizacion_estado_fecha ON cotizacion (estado, fecha_emision DESC);
CREATE INDEX ix_orden_compra_estado_fecha ON orden_compra (estado, fecha_emision DESC);

-- Vencimiento programado de cotizaciones (VencimientoCotizaciones): solo las que todavía pueden vencer.
CREATE INDEX ix_cotizacion_vencimiento_vigentes ON cotizacion (fecha_vencimiento)
    WHERE estado IN ('emitida', 'aceptada', 'validada');

CREATE INDEX ix_guia_despacho_fecha_emision ON guia_despacho (fecha_emision DESC);
CREATE INDEX ix_orden_recepcion_fecha_recepcion ON orden_recepcion (fecha_recepcion DESC);

-- Documentos OCR vigentes filtrados por tipo y estado.
CREATE INDEX ix_documento_digital_tipo_estado_fecha ON documento_digital (tipo_documento, estado_procesamiento, fecha_subida DESC)
    WHERE fecha_eliminacion IS NULL;
