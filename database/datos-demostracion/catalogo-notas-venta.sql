-- Catálogo ampliado para probar el OCR con notas de venta reales (fierro, agregados, gasfitería, electricidad). Solo
-- para el entorno de desarrollo local: NO forma parte de las migraciones y no debe ejecutarse en Supabase ni en
-- producción. Ejecutar después de demo.sql. Es idempotente: omite los SKU que ya existen.
--
-- Cada producto nuevo recibe 200 unidades en el "Almacén 1", registradas como ajuste de Kardex del administrador
-- inicial, para que también pueda cotizarse, venderse y despacharse.

DO $catalogo$
DECLARE
	v_admin bigint;
	v_almacen bigint;
BEGIN
	SELECT min(id_usuario) INTO v_admin FROM usuario WHERE rol = 'administrador';
	SELECT id_almacen INTO v_almacen FROM almacen WHERE nombre = 'Almacén 1';
	IF v_admin IS NULL OR v_almacen IS NULL THEN
		RAISE EXCEPTION 'Cargar primero datos-demostracion/demo.sql.';
	END IF;

	-- sku, nombre, marca, unidad, precio, stock mínimo, categoría (por nombre).
	CREATE TEMP TABLE catalogo_notas (
		sku text, nombre text, marca text, unidad text, precio numeric, minimo numeric, categoria text
	) ON COMMIT DROP;
	INSERT INTO catalogo_notas VALUES
		('FIE-003', 'Fierro corrugado 5/8 pulgada x 9 m', 'Siderperú', 'varilla', 50.60, 80, 'Fierro y acero de construcción'),
		('FIE-004', 'Fierro corrugado 3/4 pulgada x 9 m', 'Siderperú', 'varilla', 75.00, 40, 'Fierro y acero de construcción'),
		('FIE-005', 'Fierro corrugado 8 mm x 9 m', 'Siderperú', 'varilla', 13.90, 80, 'Fierro y acero de construcción'),
		('FIE-006', 'Fierro liso 1/4 pulgada x 9 m', 'Siderperú', 'varilla', 8.00, 60, 'Fierro y acero de construcción'),
		('FIE-007', 'Alambre negro recocido N° 16', NULL, 'kg', 5.50, 50, 'Fierro y acero de construcción'),
		('FIE-008', 'Alambre negro recocido N° 8', NULL, 'kg', 5.50, 50, 'Fierro y acero de construcción'),
		('SUJ-002', 'Clavo para construcción con cabeza 3 pulgadas', NULL, 'kg', 6.50, 30, 'Sujetadores y fijaciones'),
		('AGR-001', 'Arena gruesa', NULL, 'm3', 65.00, 20, 'Agregados'),
		('AGR-002', 'Arena fina', NULL, 'm3', 63.00, 20, 'Agregados'),
		('AGR-003', 'Piedra chancada 1/2 pulgada', NULL, 'm3', 80.00, 20, 'Agregados'),
		('LAD-002', 'Ladrillo de techo 15 x 30 cm', NULL, 'unidad', 3.00, 500, 'Ladrillos y bloques'),
		('LAD-003', 'Ladrillo pandereta', NULL, 'unidad', 0.74, 1000, 'Ladrillos y bloques'),
		('ELE-003', 'Tubo PVC luz 3/4 pulgada x 3 m', 'Nicoll', 'unidad', 5.70, 50, 'Conducción eléctrica'),
		('ELE-004', 'Curva PVC luz 3/4 pulgada', 'Nicoll', 'unidad', 0.70, 100, 'Conducción eléctrica'),
		('ELE-005', 'Caja octogonal PVC luz', NULL, 'unidad', 2.50, 50, 'Conducción eléctrica'),
		('ELE-006', 'Caja rectangular PVC luz', NULL, 'unidad', 2.50, 50, 'Conducción eléctrica'),
		('SAN-002', 'Tubo PVC desagüe 6 pulgadas x 3 m', NULL, 'unidad', 150.00, 5, 'Tuberías y conexiones'),
		('SAN-003', 'Tubo PVC desagüe 2 pulgadas x 3 m', NULL, 'unidad', 17.00, 20, 'Tuberías y conexiones'),
		('SAN-004', 'Codo PVC desagüe 2 pulgadas', NULL, 'unidad', 2.70, 30, 'Tuberías y conexiones'),
		('SAN-005', 'Yee PVC desagüe 4" x 2"', NULL, 'unidad', 13.50, 10, 'Tuberías y conexiones'),
		('SAN-006', 'Codo PVC desagüe reducción 4" x 2"', NULL, 'unidad', 13.50, 10, 'Tuberías y conexiones'),
		('SAN-007', 'Tee PVC desagüe 4 pulgadas', NULL, 'unidad', 14.00, 10, 'Tuberías y conexiones'),
		('SAN-008', 'Pegamento para PVC 1/4 galón', NULL, 'unidad', 56.00, 10, 'Adhesivos y cintas'),
		('SAN-009', 'Tubo PVC agua 1/2 pulgada x 5 m', NULL, 'unidad', 23.00, 30, 'Tuberías y conexiones'),
		('SAN-010', 'Tubo PVC agua 1 pulgada x 5 m', NULL, 'unidad', 43.00, 10, 'Tuberías y conexiones'),
		('SAN-011', 'Llave de paso 1/2 pulgada', NULL, 'unidad', 29.00, 10, 'Tuberías y conexiones'),
		('SAN-012', 'Codo PVC agua 1/2 pulgada', NULL, 'unidad', 2.50, 50, 'Tuberías y conexiones'),
		('SAN-013', 'Adaptador PVC agua 1/2 pulgada', NULL, 'unidad', 2.00, 50, 'Tuberías y conexiones'),
		('SAN-014', 'Tee PVC agua 1/2 pulgada', NULL, 'unidad', 3.50, 50, 'Tuberías y conexiones'),
		('SAN-015', 'Codo de bronce 1/2 pulgada', NULL, 'unidad', 8.00, 20, 'Tuberías y conexiones'),
		('SAN-016', 'Pegamento para CPVC 1/32 galón', NULL, 'unidad', 9.80, 10, 'Adhesivos y cintas'),
		('SAN-017', 'Cinta teflón', NULL, 'unidad', 1.00, 50, 'Sellado e impermeabilización'),
		('SAN-018', 'Mezcladora para ducha', NULL, 'unidad', 290.00, 3, 'Aparatos sanitarios'),
		('SAN-019', 'Tubo CPVC agua caliente 1/2 pulgada x 5 m', NULL, 'unidad', 33.00, 10, 'Tuberías y conexiones'),
		('SAN-020', 'Codo CPVC 1/2 pulgada', NULL, 'unidad', 2.50, 30, 'Tuberías y conexiones'),
		('SAN-021', 'Tee CPVC 1/2 pulgada', NULL, 'unidad', 2.80, 30, 'Tuberías y conexiones'),
		('SAN-022', 'Adaptador CPVC 1/2 pulgada', NULL, 'unidad', 2.50, 30, 'Tuberías y conexiones'),
		('SAN-023', 'Unión CPVC 1/2 pulgada', NULL, 'unidad', 1.80, 30, 'Tuberías y conexiones'),
		('SAN-024', 'Juego de baño inodoro y lavatorio', NULL, 'juego', 806.20, 2, 'Aparatos sanitarios');

	CREATE TEMP TABLE catalogo_nuevo ON COMMIT DROP AS
	SELECT n.* FROM catalogo_notas n WHERE NOT EXISTS (SELECT 1 FROM producto p WHERE p.sku = n.sku);

	INSERT INTO producto (sku, nombre_oficial, marca, unidad_medida, precio_referencial, stock_minimo, id_categoria)
	SELECT n.sku, n.nombre, n.marca, n.unidad, n.precio, n.minimo, c.id_categoria
	  FROM catalogo_nuevo n JOIN categoria c ON c.nombre = n.categoria;

	INSERT INTO inventario (id_producto, id_almacen, cantidad_fisica)
	SELECT p.id_producto, v_almacen, 200
	  FROM catalogo_nuevo n JOIN producto p ON p.sku = n.sku;

	INSERT INTO movimiento_kardex (id_inventario, fecha_hora, tipo_operacion, cantidad, motivo, id_usuario_responsable, origen_movimiento)
	SELECT i.id_inventario, now(), 'entrada', i.cantidad_fisica, 'Carga inicial del catálogo de notas', v_admin, 'ajuste'
	  FROM inventario i JOIN producto p ON p.id_producto = i.id_producto
	 WHERE p.sku IN (SELECT sku FROM catalogo_nuevo) AND i.id_almacen = v_almacen;

	RAISE NOTICE 'Productos nuevos del catálogo de notas: %', (SELECT count(*) FROM catalogo_nuevo);
END
$catalogo$;