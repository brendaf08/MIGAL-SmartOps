-- Migración V13: completar documentos OCR con coincidencias y eliminación lógica.
-- Amplía documento_digital con el nombre y el tamaño del archivo, el código de error del procesamiento y la fecha de
-- eliminación lógica. Amplía detalle_extraido con la sugerencia de SKU, la confianza del reconocimiento y los indicadores
-- de revisión y de descarte. Agrega coincidencia_detalle, que conserva los productos candidatos de cada elemento
-- extraído, con su similitud y su posición en la lista de sugerencias.
-- La eliminación lógica registra fecha_eliminacion y excluye el documento de las consultas habituales. La aplicación no
-- elimina filas de documentos ni de detalles.
-- Las transiciones de estado y el uso de las coincidencias se aplican en la capa de negocio, como en las migraciones
-- anteriores. Esta migración no genera operaciones de inventario.

-- Documento digital: nombre, tamaño, código de error y eliminación lógica.
-- La tabla no contiene filas en ningún entorno, por lo que las columnas obligatorias se agregan sin valor temporal.
ALTER TABLE documento_digital
    ADD COLUMN nombre_archivo     varchar(255) NOT NULL,
    ADD COLUMN tamano_bytes       bigint       NOT NULL,
    ADD COLUMN codigo_error       varchar(40),
    ADD COLUMN fecha_eliminacion  timestamptz;

ALTER TABLE documento_digital ADD CONSTRAINT ck_documento_digital_nombre_archivo CHECK (btrim(nombre_archivo) <> '');
ALTER TABLE documento_digital ADD CONSTRAINT ck_documento_digital_tamano CHECK (tamano_bytes > 0);

-- Clasificación funcional del archivo: solicitud del cliente o documento de recepción.
ALTER TABLE documento_digital ADD CONSTRAINT ck_documento_digital_tipo CHECK (
    tipo_documento IN ('solicitud_cliente', 'documento_recepcion'));

-- Código de error del procesamiento. Solo admite los códigos del catálogo de errores del contrato.
ALTER TABLE documento_digital ADD CONSTRAINT ck_documento_digital_codigo_error CHECK (
    codigo_error IS NULL OR codigo_error IN ('DOCUMENTO_ILEGIBLE', 'SERVICIO_NO_DISPONIBLE'));
-- El código de error solo se informa mientras el documento se encuentra pendiente de procesamiento.
ALTER TABLE documento_digital ADD CONSTRAINT ck_documento_digital_codigo_error_pendiente CHECK (
    codigo_error IS NULL OR estado_procesamiento = 'pendiente');

-- La eliminación lógica no puede anteceder a la carga del documento.
ALTER TABLE documento_digital ADD CONSTRAINT ck_documento_digital_fecha_eliminacion CHECK (
    fecha_eliminacion IS NULL OR fecha_eliminacion >= fecha_subida);

-- Consulta habitual de los documentos vigentes de un usuario. Excluye los documentos eliminados lógicamente.
CREATE INDEX ix_documento_digital_usuario_activos ON documento_digital (id_usuario, fecha_subida DESC)
    WHERE fecha_eliminacion IS NULL;

-- Detalle extraído: sugerencia de SKU, confianza, cantidad corregida, revisión y descarte.
-- cantidad_leida conserva la lectura original del OCR; cantidad_corregida registra la cantidad fijada por el usuario.
-- Los indicadores de revisión y de descarte son obligatorios y su valor inicial es falso.
ALTER TABLE detalle_extraido
    ADD COLUMN sku_sugerido        varchar(50),
    ADD COLUMN confianza           numeric(4,3),
    ADD COLUMN cantidad_corregida  numeric(14,3),
    ADD COLUMN requiere_revision   boolean NOT NULL DEFAULT false,
    ADD COLUMN descartado          boolean NOT NULL DEFAULT false;

ALTER TABLE detalle_extraido ADD CONSTRAINT ck_detalle_extraido_sku_sugerido CHECK (
    sku_sugerido IS NULL OR btrim(sku_sugerido) <> '');
-- La cantidad leída, cuando existe, no puede ser negativa (CantidadNoNegativa del contrato de documentos OCR).
ALTER TABLE detalle_extraido ADD CONSTRAINT ck_detalle_extraido_cantidad_leida CHECK (
    cantidad_leida IS NULL OR cantidad_leida >= 0);
-- La cantidad corregida, cuando existe, no puede ser negativa. El contrato exige una cantidad positiva en la corrección;
-- esa regla de positividad se aplica en la capa de negocio.
ALTER TABLE detalle_extraido ADD CONSTRAINT ck_detalle_extraido_cantidad_corregida CHECK (
    cantidad_corregida IS NULL OR cantidad_corregida >= 0);
-- La confianza es una proporción entre cero y uno.
ALTER TABLE detalle_extraido ADD CONSTRAINT ck_detalle_extraido_confianza CHECK (
    confianza IS NULL OR (confianza >= 0 AND confianza <= 1));

-- Coincidencias de un elemento extraído con los productos del catálogo maestro.
-- Cada coincidencia conserva la similitud calculada y la posición en la lista de sugerencias.
-- La clave única (id_detalle_extraido, posicion) cubre el índice de la clave foránea hacia detalle_extraido.
CREATE TABLE coincidencia_detalle (
    id_coincidencia      bigint        GENERATED ALWAYS AS IDENTITY,
    id_detalle_extraido  bigint        NOT NULL,
    id_producto          bigint        NOT NULL,
    similitud            numeric(4,3)  NOT NULL,
    posicion             smallint      NOT NULL,

    CONSTRAINT pk_coincidencia_detalle PRIMARY KEY (id_coincidencia),
    CONSTRAINT fk_coincidencia_detalle_detalle_extraido FOREIGN KEY (id_detalle_extraido)
        REFERENCES detalle_extraido (id_detalle_extraido) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_coincidencia_detalle_producto FOREIGN KEY (id_producto)
        REFERENCES producto (id_producto) ON DELETE RESTRICT ON UPDATE RESTRICT,
    -- La similitud es una proporción entre cero y uno.
    CONSTRAINT ck_coincidencia_detalle_similitud CHECK (similitud >= 0 AND similitud <= 1),
    -- La posición comienza en uno.
    CONSTRAINT ck_coincidencia_detalle_posicion CHECK (posicion > 0),
    -- Una posición no se repite dentro de un mismo elemento extraído.
    CONSTRAINT uq_coincidencia_detalle_posicion UNIQUE (id_detalle_extraido, posicion),
    -- Un producto aparece como máximo una vez entre las coincidencias de un mismo elemento extraído.
    CONSTRAINT uq_coincidencia_detalle_producto UNIQUE (id_detalle_extraido, id_producto)
);

CREATE INDEX ix_coincidencia_detalle_id_producto ON coincidencia_detalle (id_producto);

-- Se habilita RLS sin definir políticas. Supabase expone las tablas de public mediante su API;
-- sin RLS, la clave anónima tendría acceso. El backend se conecta directamente a PostgreSQL.
ALTER TABLE coincidencia_detalle ENABLE ROW LEVEL SECURITY;
