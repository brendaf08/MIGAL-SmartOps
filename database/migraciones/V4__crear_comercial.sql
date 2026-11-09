-- Migración V4: tablas comerciales.
-- Define cotizacion, detalle_cotizacion, venta y detalle_venta según el ADR-003 y el diccionario de datos.
-- La tabla reserva_stock no forma parte de esta migración; se incorpora en una migración posterior.
-- Los valores derivados (subtotales, totales y cantidades reservadas o pendientes) no se almacenan.
-- Las transiciones de estado no se verifican en la base de datos; la capa de negocio las aplica.

CREATE TABLE cotizacion (
    id_cotizacion            bigint        GENERATED ALWAYS AS IDENTITY,
    id_cliente               bigint        NOT NULL,
    id_usuario_responsable   bigint        NOT NULL,
    fecha_emision            timestamptz   NOT NULL DEFAULT now(),
    fecha_vencimiento        date          NOT NULL,
    condiciones_comerciales  text,
    descuento                numeric(14,2) NOT NULL,
    estado                   varchar(20)   NOT NULL,
    fecha_aceptacion_cliente timestamptz,
    fecha_validacion_migal   timestamptz,

    CONSTRAINT pk_cotizacion PRIMARY KEY (id_cotizacion),
    CONSTRAINT fk_cotizacion_cliente FOREIGN KEY (id_cliente)
        REFERENCES cliente (id_cliente) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_cotizacion_usuario_responsable FOREIGN KEY (id_usuario_responsable)
        REFERENCES usuario (id_usuario) ON DELETE RESTRICT ON UPDATE RESTRICT,
    -- Clave referenciada por la clave foránea compuesta de venta, que garantiza que el cliente
    -- de la venta coincide con el de su cotización.
    CONSTRAINT uq_cotizacion_id_cotizacion_cliente UNIQUE (id_cotizacion, id_cliente),
    CONSTRAINT ck_cotizacion_descuento CHECK (descuento >= 0),
    CONSTRAINT ck_cotizacion_estado CHECK (estado IN ('emitida', 'aceptada', 'validada', 'rechazada', 'vencida'))
);

CREATE INDEX ix_cotizacion_id_cliente ON cotizacion (id_cliente);
CREATE INDEX ix_cotizacion_id_usuario_responsable ON cotizacion (id_usuario_responsable);

-- Relación muchos a muchos entre cotizacion y producto. La columna id_cotizacion es líder de la clave primaria.
CREATE TABLE detalle_cotizacion (
    id_cotizacion     bigint        NOT NULL,
    id_producto       bigint        NOT NULL,
    cantidad_cotizada numeric(14,3) NOT NULL,
    precio_unitario   numeric(14,2) NOT NULL,

    CONSTRAINT pk_detalle_cotizacion PRIMARY KEY (id_cotizacion, id_producto),
    CONSTRAINT fk_detalle_cotizacion_cotizacion FOREIGN KEY (id_cotizacion)
        REFERENCES cotizacion (id_cotizacion) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_detalle_cotizacion_producto FOREIGN KEY (id_producto)
        REFERENCES producto (id_producto) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_detalle_cotizacion_cantidad CHECK (cantidad_cotizada > 0),
    CONSTRAINT ck_detalle_cotizacion_precio CHECK (precio_unitario >= 0)
);

CREATE INDEX ix_detalle_cotizacion_id_producto ON detalle_cotizacion (id_producto);

-- Una cotización origina como máximo una venta (uq_venta_id_cotizacion).
-- La clave foránea compuesta (id_cotizacion, id_cliente) impide que el cliente de la venta
-- difiera del cliente de su cotización. Se prefiere a un disparador porque es declarativa,
-- verificable por el motor y no requiere lógica adicional en cada escritura.
CREATE TABLE venta (
    id_venta               bigint        GENERATED ALWAYS AS IDENTITY,
    id_cotizacion          bigint        NOT NULL,
    id_cliente             bigint        NOT NULL,
    id_usuario_responsable bigint        NOT NULL,
    fecha_venta            timestamptz   NOT NULL DEFAULT now(),
    descuento              numeric(14,2) NOT NULL,
    estado                 varchar(30)   NOT NULL,
    fecha_cancelacion      timestamptz,
    motivo_cancelacion     text,
    id_usuario_autorizante bigint,

    CONSTRAINT pk_venta PRIMARY KEY (id_venta),
    CONSTRAINT uq_venta_id_cotizacion UNIQUE (id_cotizacion),
    CONSTRAINT fk_venta_cotizacion_cliente FOREIGN KEY (id_cotizacion, id_cliente)
        REFERENCES cotizacion (id_cotizacion, id_cliente) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_venta_cliente FOREIGN KEY (id_cliente)
        REFERENCES cliente (id_cliente) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_venta_usuario_responsable FOREIGN KEY (id_usuario_responsable)
        REFERENCES usuario (id_usuario) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_venta_usuario_autorizante FOREIGN KEY (id_usuario_autorizante)
        REFERENCES usuario (id_usuario) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_venta_descuento CHECK (descuento >= 0),
    CONSTRAINT ck_venta_estado CHECK (estado IN (
        'pendiente_abastecimiento', 'pendiente_despacho', 'lista_para_despacho', 'despachada', 'cancelada')),
    -- Cancelación condicional: los tres datos son obligatorios si la venta está cancelada y nulos en otro caso.
    -- Cada comparación con el motivo se escribe con IS NOT NULL para que un valor nulo no produzca un resultado nulo,
    -- que el CHECK aceptaría.
    CONSTRAINT ck_venta_cancelacion CHECK (
        (estado = 'cancelada'
            AND motivo_cancelacion IS NOT NULL AND btrim(motivo_cancelacion) <> ''
            AND fecha_cancelacion IS NOT NULL
            AND id_usuario_autorizante IS NOT NULL)
        OR
        (estado <> 'cancelada'
            AND motivo_cancelacion IS NULL
            AND fecha_cancelacion IS NULL
            AND id_usuario_autorizante IS NULL)
    )
);

CREATE INDEX ix_venta_id_cliente ON venta (id_cliente);
CREATE INDEX ix_venta_id_usuario_responsable ON venta (id_usuario_responsable);
CREATE INDEX ix_venta_id_usuario_autorizante ON venta (id_usuario_autorizante);

CREATE TABLE detalle_venta (
    id_detalle_venta  bigint        GENERATED ALWAYS AS IDENTITY,
    id_venta          bigint        NOT NULL,
    id_producto       bigint        NOT NULL,
    cantidad_vendida  numeric(14,3) NOT NULL,
    precio_unitario   numeric(14,2) NOT NULL,

    CONSTRAINT pk_detalle_venta PRIMARY KEY (id_detalle_venta),
    CONSTRAINT fk_detalle_venta_venta FOREIGN KEY (id_venta)
        REFERENCES venta (id_venta) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_detalle_venta_producto FOREIGN KEY (id_producto)
        REFERENCES producto (id_producto) ON DELETE RESTRICT ON UPDATE RESTRICT,
    -- Clave referenciada por las claves foráneas compuestas de reserva_stock, que incluyen id_producto.
    CONSTRAINT uq_detalle_venta_id_detalle_venta_producto UNIQUE (id_detalle_venta, id_producto),
    CONSTRAINT ck_detalle_venta_cantidad CHECK (cantidad_vendida > 0),
    CONSTRAINT ck_detalle_venta_precio CHECK (precio_unitario >= 0)
);

CREATE INDEX ix_detalle_venta_id_venta ON detalle_venta (id_venta);
CREATE INDEX ix_detalle_venta_id_producto ON detalle_venta (id_producto);

-- Se habilita RLS sin definir políticas. Supabase expone las tablas de public mediante su API;
-- sin RLS, la clave anónima tendría acceso. El backend se conecta directamente a PostgreSQL.
ALTER TABLE cotizacion ENABLE ROW LEVEL SECURITY;
ALTER TABLE detalle_cotizacion ENABLE ROW LEVEL SECURITY;
ALTER TABLE venta ENABLE ROW LEVEL SECURITY;
ALTER TABLE detalle_venta ENABLE ROW LEVEL SECURITY;
