-- Migración V9: tablas de documentos OCR.
-- Define documento_digital y detalle_extraido según el modelo y diccionario de datos.
-- Un documento se utiliza como fuente de una cotización o como apoyo de una recepción, pero no de ambas
-- (restricción de exclusividad documental). Ambas referencias pueden permanecer nulas mientras el archivo está pendiente.
-- La información extraída constituye una precarga sujeta a validación humana; esta migración no genera operaciones de inventario.

CREATE TABLE documento_digital (
    id_documento          bigint       GENERATED ALWAYS AS IDENTITY,
    ruta_archivo          varchar(500) NOT NULL,
    fecha_subida          timestamptz  NOT NULL DEFAULT now(),
    estado_procesamiento  varchar(20)  NOT NULL,
    tipo_documento        varchar(50)  NOT NULL,
    id_usuario            bigint       NOT NULL,
    id_cotizacion         bigint,
    id_recepcion          bigint,

    CONSTRAINT pk_documento_digital PRIMARY KEY (id_documento),
    CONSTRAINT fk_documento_digital_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuario (id_usuario) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_documento_digital_cotizacion FOREIGN KEY (id_cotizacion)
        REFERENCES cotizacion (id_cotizacion) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_documento_digital_recepcion FOREIGN KEY (id_recepcion)
        REFERENCES orden_recepcion (id_recepcion) ON DELETE RESTRICT ON UPDATE RESTRICT,
    -- Estado de procesamiento: pendiente, procesado_ocr o validado.
    CONSTRAINT ck_documento_digital_estado CHECK (estado_procesamiento IN ('pendiente', 'procesado_ocr', 'validado')),
    -- Exclusividad documental: el documento no puede apoyar a la vez una cotización y una recepción.
    CONSTRAINT ck_documento_digital_exclusividad CHECK (id_cotizacion IS NULL OR id_recepcion IS NULL)
);

CREATE INDEX ix_documento_digital_id_usuario ON documento_digital (id_usuario);
CREATE INDEX ix_documento_digital_id_cotizacion ON documento_digital (id_cotizacion);
CREATE INDEX ix_documento_digital_id_recepcion ON documento_digital (id_recepcion);

CREATE TABLE detalle_extraido (
    id_detalle_extraido bigint        GENERATED ALWAYS AS IDENTITY,
    id_documento        bigint        NOT NULL,
    texto_crudo         text          NOT NULL,
    cantidad_leida      numeric(14,3),
    id_producto         bigint,
    texto_corregido     text,

    CONSTRAINT pk_detalle_extraido PRIMARY KEY (id_detalle_extraido),
    CONSTRAINT fk_detalle_extraido_documento_digital FOREIGN KEY (id_documento)
        REFERENCES documento_digital (id_documento) ON DELETE RESTRICT ON UPDATE RESTRICT,
    -- La asociación con el producto puede permanecer vacía hasta que se confirme una coincidencia.
    CONSTRAINT fk_detalle_extraido_producto FOREIGN KEY (id_producto)
        REFERENCES producto (id_producto) ON DELETE RESTRICT ON UPDATE RESTRICT
);

CREATE INDEX ix_detalle_extraido_id_documento ON detalle_extraido (id_documento);
CREATE INDEX ix_detalle_extraido_id_producto ON detalle_extraido (id_producto);

-- Se habilita RLS sin definir políticas. Supabase expone las tablas de public mediante su API;
-- sin RLS, la clave anónima tendría acceso. El backend se conecta directamente a PostgreSQL.
ALTER TABLE documento_digital ENABLE ROW LEVEL SECURITY;
ALTER TABLE detalle_extraido ENABLE ROW LEVEL SECURITY;
