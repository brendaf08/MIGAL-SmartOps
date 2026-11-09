-- Migración V2: tablas maestras.
-- Define las entidades de referencia del modelo: almacen, cliente, producto, proveedor y catalogo_proveedor.
-- La desactivación es lógica mediante estado_activo; la aplicación no elimina filas de estas tablas.
-- La restricción de que las nuevas órdenes de compra utilicen solo proveedores activos se aplica en la capa de negocio.

CREATE TABLE almacen (
    id_almacen          bigint        GENERATED ALWAYS AS IDENTITY,
    nombre              varchar(100)  NOT NULL,
    tipo_ubicacion      varchar(30)   NOT NULL,
    direccion_referencia varchar(255),
    estado_activo       boolean       NOT NULL DEFAULT true,

    CONSTRAINT pk_almacen PRIMARY KEY (id_almacen)
);

CREATE TABLE cliente (
    id_cliente          bigint        GENERATED ALWAYS AS IDENTITY,
    tipo_documento      varchar(10),
    numero_documento    varchar(20),
    nombre_razon_social varchar(200)  NOT NULL,
    telefono_contacto   varchar(30),
    direccion_referencia varchar(255),
    estado_activo       boolean       NOT NULL DEFAULT true,
    fecha_registro      timestamptz   NOT NULL DEFAULT now(),

    CONSTRAINT pk_cliente PRIMARY KEY (id_cliente)
);

CREATE TABLE producto (
    id_producto         bigint        GENERATED ALWAYS AS IDENTITY,
    sku                 varchar(50)   NOT NULL,
    nombre_oficial      varchar(200)  NOT NULL,
    descripcion         text,
    unidad_medida       varchar(30)   NOT NULL,
    precio_referencial  numeric(14,2) NOT NULL,
    stock_minimo        numeric(14,3) NOT NULL,
    estado_activo       boolean       NOT NULL DEFAULT true,
    categoria_producto  varchar(50)   NOT NULL,
    marca               varchar(100),
    presentacion        varchar(100),
    fecha_registro      timestamptz   NOT NULL DEFAULT now(),
    fecha_actualizacion timestamptz,

    CONSTRAINT pk_producto PRIMARY KEY (id_producto),
    -- El SKU es el código oficial y estandarizado del producto.
    CONSTRAINT uq_producto_sku UNIQUE (sku),
    CONSTRAINT ck_producto_precio_referencial CHECK (precio_referencial >= 0),
    -- El stock mínimo es un umbral de alerta y no admite valores negativos.
    CONSTRAINT ck_producto_stock_minimo CHECK (stock_minimo >= 0)
);

CREATE TABLE proveedor (
    id_proveedor        bigint        GENERATED ALWAYS AS IDENTITY,
    ruc                 varchar(11)   NOT NULL,
    razon_social        varchar(200)  NOT NULL,
    contacto            varchar(200)  NOT NULL,
    condiciones         text,
    estado_activo       boolean       NOT NULL DEFAULT true,
    fecha_registro      timestamptz   NOT NULL DEFAULT now(),

    CONSTRAINT pk_proveedor PRIMARY KEY (id_proveedor),
    -- El RUC identifica de forma única al proveedor.
    CONSTRAINT uq_proveedor_ruc UNIQUE (ruc)
);

-- Relación muchos a muchos entre proveedor y producto, con atributos propios de la relación.
CREATE TABLE catalogo_proveedor (
    id_proveedor                  bigint        NOT NULL,
    id_producto                   bigint        NOT NULL,
    precio_compra                 numeric(14,2) NOT NULL,
    plazo_entrega_referencial_dias integer,
    fecha_actualizacion_precio    timestamptz,

    CONSTRAINT pk_catalogo_proveedor PRIMARY KEY (id_proveedor, id_producto),
    CONSTRAINT fk_catalogo_proveedor_proveedor FOREIGN KEY (id_proveedor)
        REFERENCES proveedor (id_proveedor) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_catalogo_proveedor_producto FOREIGN KEY (id_producto)
        REFERENCES producto (id_producto) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_catalogo_proveedor_precio_compra CHECK (precio_compra >= 0),
    CONSTRAINT ck_catalogo_proveedor_plazo CHECK (plazo_entrega_referencial_dias >= 0)
);

-- La columna id_proveedor es la columna líder de la clave primaria y ya se encuentra indexada.
CREATE INDEX ix_catalogo_proveedor_id_producto ON catalogo_proveedor (id_producto);

-- Se habilita RLS sin definir políticas. Supabase expone las tablas de public mediante su API;
-- sin RLS, la clave anónima tendría acceso. El backend se conecta directamente a PostgreSQL.
ALTER TABLE almacen ENABLE ROW LEVEL SECURITY;
ALTER TABLE cliente ENABLE ROW LEVEL SECURITY;
ALTER TABLE producto ENABLE ROW LEVEL SECURITY;
ALTER TABLE proveedor ENABLE ROW LEVEL SECURITY;
ALTER TABLE catalogo_proveedor ENABLE ROW LEVEL SECURITY;
