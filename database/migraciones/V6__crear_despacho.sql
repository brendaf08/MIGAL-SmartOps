-- Migración V6: tabla guia_despacho.
-- Define la guía de despacho según el modelo y diccionario de datos y el ADR-003.
-- Cada venta origina como máximo una guía (uq_guia_despacho_id_venta). El despacho es único y completo.
-- La emisión de la guía no modifica el inventario. La salida física se confirma con fecha_salida_real y el
-- usuario responsable; los movimientos de Kardex correspondientes se registran en la migración V7.
-- Las transiciones de estado y la verificación de que la venta esté lista para despacho se aplican en la capa de negocio.

CREATE TABLE guia_despacho (
    id_guia                bigint       GENERATED ALWAYS AS IDENTITY,
    id_venta               bigint       NOT NULL,
    direccion_entrega      varchar(255) NOT NULL,
    fecha_despacho         date,
    fecha_emision          timestamptz  NOT NULL DEFAULT now(),
    estado                 varchar(20)  NOT NULL,
    fecha_salida_real      timestamptz,
    id_usuario_responsable bigint,

    CONSTRAINT pk_guia_despacho PRIMARY KEY (id_guia),
    CONSTRAINT fk_guia_despacho_venta FOREIGN KEY (id_venta)
        REFERENCES venta (id_venta) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_guia_despacho_usuario_responsable FOREIGN KEY (id_usuario_responsable)
        REFERENCES usuario (id_usuario) ON DELETE RESTRICT ON UPDATE RESTRICT,
    -- Una venta origina como máximo una guía de despacho.
    CONSTRAINT uq_guia_despacho_id_venta UNIQUE (id_venta),
    -- Situación del documento: preparacion, emitida, salida_confirmada o entregada.
    CONSTRAINT ck_guia_despacho_estado CHECK (estado IN ('preparacion', 'emitida', 'salida_confirmada', 'entregada')),
    -- Condicional: la fecha de salida real y el usuario que confirma la salida son obligatorios cuando la salida
    -- física se confirmó (salida_confirmada o entregada). Antes de la confirmación, ambos campos son nulos.
    CONSTRAINT ck_guia_despacho_salida_confirmada CHECK (
        (estado IN ('salida_confirmada', 'entregada')
            AND fecha_salida_real IS NOT NULL
            AND id_usuario_responsable IS NOT NULL)
        OR
        (estado IN ('preparacion', 'emitida')
            AND fecha_salida_real IS NULL
            AND id_usuario_responsable IS NULL)
    )
);

-- La columna id_venta se encuentra indexada por uq_guia_despacho_id_venta.
CREATE INDEX ix_guia_despacho_id_usuario_responsable ON guia_despacho (id_usuario_responsable);

-- Se habilita RLS sin definir políticas. Supabase expone las tablas de public mediante su API;
-- sin RLS, la clave anónima tendría acceso. El backend se conecta directamente a PostgreSQL.
ALTER TABLE guia_despacho ENABLE ROW LEVEL SECURITY;
