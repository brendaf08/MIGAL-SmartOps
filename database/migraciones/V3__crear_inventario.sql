-- Migración V3: tabla inventario.
-- Resuelve la relación muchos a muchos entre producto y almacen. Cada combinación producto-almacén es única.
-- cantidad_fisica es el saldo físico almacenado; cantidad_reservada y cantidad_libre son valores derivados
-- que se obtienen mediante consultas o vistas y no se almacenan como columnas.

CREATE TABLE inventario (
    id_inventario   bigint        GENERATED ALWAYS AS IDENTITY,
    id_producto     bigint        NOT NULL,
    id_almacen      bigint        NOT NULL,
    cantidad_fisica numeric(14,3) NOT NULL,

    CONSTRAINT pk_inventario PRIMARY KEY (id_inventario),
    CONSTRAINT fk_inventario_producto FOREIGN KEY (id_producto)
        REFERENCES producto (id_producto) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_inventario_almacen FOREIGN KEY (id_almacen)
        REFERENCES almacen (id_almacen) ON DELETE RESTRICT ON UPDATE RESTRICT,
    -- Unicidad de la combinación producto-almacén. La columna id_producto es líder de este índice.
    CONSTRAINT uq_inventario_producto_almacen UNIQUE (id_producto, id_almacen),
    -- Clave referenciada por las claves foráneas compuestas de reserva_stock, que incluyen id_producto
    -- para garantizar que una reserva corresponde al producto de su línea de venta.
    CONSTRAINT uq_inventario_id_inventario_producto UNIQUE (id_inventario, id_producto),
    -- El saldo físico no puede ser negativo (RNF-16).
    CONSTRAINT ck_inventario_cantidad_fisica CHECK (cantidad_fisica >= 0)
);

-- Índice sobre la clave foránea id_almacen. La clave foránea id_producto está cubierta por uq_inventario_producto_almacen.
CREATE INDEX ix_inventario_id_almacen ON inventario (id_almacen);

-- Se habilita RLS sin definir políticas. Supabase expone las tablas de public mediante su API;
-- sin RLS, la clave anónima tendría acceso. El backend se conecta directamente a PostgreSQL.
ALTER TABLE inventario ENABLE ROW LEVEL SECURITY;
