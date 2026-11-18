-- Registros funcionales mínimos requeridos por el frontend y las reglas de negocio en PostgreSQL.
-- Es idempotente: puede ejecutarse múltiples veces sin duplicar registros.

DO $funcional$
BEGIN
	-- 1. Ubicaciones de inventario (deben coincidir exactamente con UBICACIONES en frontend/src/features/inventario/movimientos/esquema.ts
	--    y ALMACEN = 'Almacén 1' en frontend/src/features/recepcion/api.ts).
	INSERT INTO almacen (nombre, tipo_ubicacion, direccion_referencia)
	SELECT v.nombre, v.tipo_ubicacion, v.direccion_referencia
	FROM (VALUES
		('Tienda principal', 'tienda', 'Local principal, atención al público'),
		('Almacén 1', 'almacen', 'Depósito principal'),
		('Almacén 2', 'almacen', 'Depósito de materiales pesados')
	) AS v(nombre, tipo_ubicacion, direccion_referencia)
	WHERE NOT EXISTS (
		SELECT 1 FROM almacen a WHERE a.nombre = v.nombre
	);

	-- 2. Cliente funcional por defecto para mostrador / cotizaciones rápidas (CotizacionService exige un cliente).
	INSERT INTO cliente (tipo_documento, numero_documento, nombre_razon_social, telefono_contacto, direccion_referencia)
	SELECT NULL, NULL, 'Cliente de mostrador', NULL, NULL
	WHERE NOT EXISTS (
		SELECT 1 FROM cliente c WHERE c.nombre_razon_social = 'Cliente de mostrador'
	);

	RAISE NOTICE 'Registros funcionales verificados/creados correctamente.';
END
$funcional$;
