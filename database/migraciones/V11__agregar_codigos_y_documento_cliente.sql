-- Migración V11: códigos de negocio y validación del documento de cliente.
-- Agrega la columna codigo a las entidades visibles para el usuario (cliente, cotizacion, venta, guia_despacho,
-- orden_compra y orden_recepcion). El valor se genera y se almacena en la base de datos a partir de la clave primaria,
-- de modo que no puede ser escrito por la aplicación. Cada código tiene el prefijo de su entidad, seguido del
-- identificador con ceros a la izquierda hasta cuatro dígitos (CLI-0001, V-0038). Un identificador de más de cuatro
-- dígitos se escribe completo y no se trunca.
-- La expresión utiliza únicamente funciones inmutables (repeat, greatest, length, ||) y el cast de bigint a texto,
-- por lo que es admisible en una columna generada de PostgreSQL 15 y 17.
-- El documento de cliente es opcional. Cuando existe, el tipo y el número se informan juntos, el tipo pertenece al
-- dominio comercial (dni o ruc) y el número respeta el formato del tipo. La unicidad se aplica solo a los documentos
-- informados, de modo que los clientes de mostrador sin documento no entran en conflicto.

-- Columnas generadas y restricciones de unicidad de los códigos de negocio.
ALTER TABLE cliente ADD COLUMN codigo varchar(30) GENERATED ALWAYS AS (
    'CLI-' || repeat('0', greatest(4 - length(id_cliente::text), 0)) || id_cliente::text) STORED NOT NULL;
ALTER TABLE cliente ADD CONSTRAINT uq_cliente_codigo UNIQUE (codigo);

ALTER TABLE cotizacion ADD COLUMN codigo varchar(30) GENERATED ALWAYS AS (
    'COT-' || repeat('0', greatest(4 - length(id_cotizacion::text), 0)) || id_cotizacion::text) STORED NOT NULL;
ALTER TABLE cotizacion ADD CONSTRAINT uq_cotizacion_codigo UNIQUE (codigo);

ALTER TABLE venta ADD COLUMN codigo varchar(30) GENERATED ALWAYS AS (
    'V-' || repeat('0', greatest(4 - length(id_venta::text), 0)) || id_venta::text) STORED NOT NULL;
ALTER TABLE venta ADD CONSTRAINT uq_venta_codigo UNIQUE (codigo);

ALTER TABLE guia_despacho ADD COLUMN codigo varchar(30) GENERATED ALWAYS AS (
    'D-' || repeat('0', greatest(4 - length(id_guia::text), 0)) || id_guia::text) STORED NOT NULL;
ALTER TABLE guia_despacho ADD CONSTRAINT uq_guia_despacho_codigo UNIQUE (codigo);

ALTER TABLE orden_compra ADD COLUMN codigo varchar(30) GENERATED ALWAYS AS (
    'OC-' || repeat('0', greatest(4 - length(id_orden::text), 0)) || id_orden::text) STORED NOT NULL;
ALTER TABLE orden_compra ADD CONSTRAINT uq_orden_compra_codigo UNIQUE (codigo);

ALTER TABLE orden_recepcion ADD COLUMN codigo varchar(30) GENERATED ALWAYS AS (
    'R-' || repeat('0', greatest(4 - length(id_recepcion::text), 0)) || id_recepcion::text) STORED NOT NULL;
ALTER TABLE orden_recepcion ADD CONSTRAINT uq_orden_recepcion_codigo UNIQUE (codigo);

-- Documento de cliente. Un mismo tipo y número no pueden pertenecer a dos clientes; los clientes sin documento se excluyen.
CREATE UNIQUE INDEX uq_cliente_documento ON cliente (tipo_documento, numero_documento)
    WHERE numero_documento IS NOT NULL;

-- Dominio de tipos de documento de uso comercial.
ALTER TABLE cliente ADD CONSTRAINT ck_cliente_tipo_documento CHECK (
    tipo_documento IS NULL OR tipo_documento IN ('dni', 'ruc'));
-- El tipo y el número se informan juntos o se omiten juntos.
ALTER TABLE cliente ADD CONSTRAINT ck_cliente_documento_completo CHECK (
    (tipo_documento IS NULL) = (numero_documento IS NULL));
-- Formato del número: el DNI tiene ocho dígitos y el RUC, once. Las comparaciones con el tipo se escriben con
-- IS NOT DISTINCT FROM y con IS NOT NULL para que un valor nulo no produzca un resultado nulo.
ALTER TABLE cliente ADD CONSTRAINT ck_cliente_formato_documento CHECK (
    (tipo_documento IS NULL AND numero_documento IS NULL)
    OR (tipo_documento IS NOT DISTINCT FROM 'dni'
        AND numero_documento IS NOT NULL AND numero_documento ~ '^[0-9]{8}$')
    OR (tipo_documento IS NOT DISTINCT FROM 'ruc'
        AND numero_documento IS NOT NULL AND numero_documento ~ '^[0-9]{11}$'));
