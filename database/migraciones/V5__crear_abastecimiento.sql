-- Migración V5: tablas de abastecimiento.
-- Define orden_compra, detalle_compra, orden_recepcion y detalle_recepcion según el modelo y diccionario de datos.
-- Los valores derivados (subtotal_detalle y cantidad_pendiente_recepcion) no se almacenan.
-- La validación de que una nueva orden utilice solo proveedores activos y la actualización del estado de la orden
-- según las cantidades recibidas se aplican en la capa de negocio.
-- Las recepciones validadas generan movimientos de Kardex en la migración V7; esta migración no genera movimientos.

CREATE TABLE orden_compra (
    id_orden               bigint        GENERATED ALWAYS AS IDENTITY,
    id_proveedor           bigint        NOT NULL,
    id_usuario_responsable bigint        NOT NULL,
    fecha_emision          timestamptz   NOT NULL DEFAULT now(),
    estado                 varchar(20)   NOT NULL,
    fecha_entrega_estimada date,
    observaciones          text,
    modalidad_pago         varchar(100),

    CONSTRAINT pk_orden_compra PRIMARY KEY (id_orden),
    CONSTRAINT fk_orden_compra_proveedor FOREIGN KEY (id_proveedor)
        REFERENCES proveedor (id_proveedor) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_orden_compra_usuario_responsable FOREIGN KEY (id_usuario_responsable)
        REFERENCES usuario (id_usuario) ON DELETE RESTRICT ON UPDATE RESTRICT,
    -- Estado de cumplimiento de la orden: pendiente, parcial o completada.
    CONSTRAINT ck_orden_compra_estado CHECK (estado IN ('pendiente', 'parcial', 'completada'))
);

CREATE INDEX ix_orden_compra_id_proveedor ON orden_compra (id_proveedor);
CREATE INDEX ix_orden_compra_id_usuario_responsable ON orden_compra (id_usuario_responsable);

-- Relación muchos a muchos entre orden_compra y producto. La columna id_orden es líder de la clave primaria.
CREATE TABLE detalle_compra (
    id_orden            bigint        NOT NULL,
    id_producto         bigint        NOT NULL,
    cantidad_comprada   numeric(14,3) NOT NULL,
    costo_unitario      numeric(14,2) NOT NULL,

    CONSTRAINT pk_detalle_compra PRIMARY KEY (id_orden, id_producto),
    CONSTRAINT fk_detalle_compra_orden_compra FOREIGN KEY (id_orden)
        REFERENCES orden_compra (id_orden) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_detalle_compra_producto FOREIGN KEY (id_producto)
        REFERENCES producto (id_producto) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_detalle_compra_cantidad CHECK (cantidad_comprada > 0),
    CONSTRAINT ck_detalle_compra_costo CHECK (costo_unitario >= 0)
);

CREATE INDEX ix_detalle_compra_id_producto ON detalle_compra (id_producto);

CREATE TABLE orden_recepcion (
    id_recepcion           bigint        GENERATED ALWAYS AS IDENTITY,
    id_orden               bigint        NOT NULL,
    id_usuario_responsable bigint        NOT NULL,
    fecha_recepcion        date          NOT NULL,
    estado                 varchar(20)   NOT NULL,
    fecha_validacion       timestamptz,
    observaciones          text,

    CONSTRAINT pk_orden_recepcion PRIMARY KEY (id_recepcion),
    CONSTRAINT fk_orden_recepcion_orden_compra FOREIGN KEY (id_orden)
        REFERENCES orden_compra (id_orden) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_orden_recepcion_usuario_responsable FOREIGN KEY (id_usuario_responsable)
        REFERENCES usuario (id_usuario) ON DELETE RESTRICT ON UPDATE RESTRICT,
    -- Estado de la recepción: pendiente o validada.
    CONSTRAINT ck_orden_recepcion_estado CHECK (estado IN ('pendiente', 'validada')),
    -- Condicional: la fecha de validación es obligatoria si la recepción está validada y nula en otro caso.
    CONSTRAINT ck_orden_recepcion_validacion CHECK (
        (estado = 'validada' AND fecha_validacion IS NOT NULL)
        OR
        (estado <> 'validada' AND fecha_validacion IS NULL)
    )
);

CREATE INDEX ix_orden_recepcion_id_orden ON orden_recepcion (id_orden);
CREATE INDEX ix_orden_recepcion_id_usuario_responsable ON orden_recepcion (id_usuario_responsable);

-- Relación entre orden_recepcion, producto y almacen con cantidad recibida por ubicación.
-- La columna id_recepcion es líder de la clave primaria compuesta y ya se encuentra indexada.
CREATE TABLE detalle_recepcion (
    id_recepcion        bigint        NOT NULL,
    id_producto         bigint        NOT NULL,
    id_almacen_destino  bigint        NOT NULL,
    cantidad_recibida   numeric(14,3) NOT NULL,

    CONSTRAINT pk_detalle_recepcion PRIMARY KEY (id_recepcion, id_producto, id_almacen_destino),
    CONSTRAINT fk_detalle_recepcion_orden_recepcion FOREIGN KEY (id_recepcion)
        REFERENCES orden_recepcion (id_recepcion) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_detalle_recepcion_producto FOREIGN KEY (id_producto)
        REFERENCES producto (id_producto) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_detalle_recepcion_almacen_destino FOREIGN KEY (id_almacen_destino)
        REFERENCES almacen (id_almacen) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_detalle_recepcion_cantidad CHECK (cantidad_recibida >= 0)
);

CREATE INDEX ix_detalle_recepcion_id_producto ON detalle_recepcion (id_producto);
CREATE INDEX ix_detalle_recepcion_id_almacen_destino ON detalle_recepcion (id_almacen_destino);

-- Se habilita RLS sin definir políticas. Supabase expone las tablas de public mediante su API;
-- sin RLS, la clave anónima tendría acceso. El backend se conecta directamente a PostgreSQL.
ALTER TABLE orden_compra ENABLE ROW LEVEL SECURITY;
ALTER TABLE detalle_compra ENABLE ROW LEVEL SECURITY;
ALTER TABLE orden_recepcion ENABLE ROW LEVEL SECURITY;
ALTER TABLE detalle_recepcion ENABLE ROW LEVEL SECURITY;
