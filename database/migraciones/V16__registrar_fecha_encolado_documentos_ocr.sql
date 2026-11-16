-- Migración V16: fecha del último encolado de un documento OCR (ADR-012, punto 4).
-- fecha_subida registra la carga y no cambia al reprocesar. fecha_encolado registra el último envío al servicio de OCR:
-- coincide con fecha_subida en la carga y se actualiza en cada reprocesamiento. La vigilancia de documentos pendientes
-- la utiliza para registrar SERVICIO_NO_DISPONIBLE cuando el resultado no llega dentro del plazo.
-- Esta migración no genera operaciones de inventario.

-- Las filas existentes reciben su fecha de carga, que corresponde a su primer encolado.
ALTER TABLE documento_digital ADD COLUMN fecha_encolado timestamptz;
UPDATE documento_digital SET fecha_encolado = fecha_subida;
ALTER TABLE documento_digital ALTER COLUMN fecha_encolado SET NOT NULL;
ALTER TABLE documento_digital ALTER COLUMN fecha_encolado SET DEFAULT now();

-- Un encolado no puede anteceder a la carga del documento.
ALTER TABLE documento_digital ADD CONSTRAINT ck_documento_digital_fecha_encolado CHECK (fecha_encolado >= fecha_subida);

-- Consulta de la vigilancia: documentos pendientes, vigentes y sin error, por antigüedad del último encolado.
CREATE INDEX ix_documento_digital_pendientes_encolado ON documento_digital (fecha_encolado)
    WHERE estado_procesamiento = 'pendiente' AND codigo_error IS NULL AND fecha_eliminacion IS NULL;
