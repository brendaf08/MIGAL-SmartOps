-- Migración V21: tipo de entrega de la guía de despacho.
-- Una guía se entrega por envío a una dirección o por retiro en tienda (venta de mostrador). El tipo decide quién puede
-- gestionar el despacho: las guías de retiro en tienda las puede despachar también el Vendedor; las de envío, solo el
-- Almacenero (y el Administrador en consulta). Las guías existentes con dirección "Retiro en tienda" pasan a retiro.

ALTER TABLE guia_despacho ADD COLUMN tipo_entrega varchar(20) NOT NULL DEFAULT 'envio';

ALTER TABLE guia_despacho ADD CONSTRAINT ck_guia_despacho_tipo_entrega CHECK (tipo_entrega IN ('envio', 'retiro_tienda'));

UPDATE guia_despacho SET tipo_entrega = 'retiro_tienda' WHERE direccion_entrega ILIKE 'retiro en tienda%';

