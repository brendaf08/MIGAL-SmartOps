-- Datos de demostración para el entorno de desarrollo local. NO forma parte de las migraciones y no debe
-- ejecutarse en Supabase ni en producción. Es idempotente: si ya existe el almacén "Tienda", no hace nada.
--
-- Carga: 3 ubicaciones (tienda y dos almacenes), 14 productos con stock inicial (varios bajo el mínimo para
-- las alertas), 5 clientes y 2 proveedores con su catálogo. El stock inicial se registra como ajuste de Kardex
-- del usuario administrador inicial, de modo que el saldo siempre coincide con el historial.

DO $demo$
DECLARE
	v_admin bigint;
BEGIN
	IF EXISTS (SELECT 1 FROM almacen WHERE nombre = 'Tienda') THEN
		RAISE NOTICE 'Los datos de demostración ya existen; no se realiza ningún cambio.';
		RETURN;
	END IF;

	SELECT min(id_usuario) INTO v_admin FROM usuario WHERE rol = 'administrador';
	IF v_admin IS NULL THEN
		RAISE EXCEPTION 'No existe un usuario administrador. Iniciar el backend con MIGAL_ADMIN_USERNAME y MIGAL_ADMIN_CLAVE.';
	END IF;

	-- Ubicaciones. El orden de creación fija el orden de presentación: tienda, almacén 1, almacén 2.
	INSERT INTO almacen (nombre, tipo_ubicacion, direccion_referencia) VALUES
		('Tienda', 'tienda', 'Local principal, atención al público'),
		('Almacén 1', 'almacen', 'Depósito principal'),
		('Almacén 2', 'almacen', 'Depósito de materiales pesados');

	-- Productos: sku, nombre, unidad, precio, stock mínimo, categoría (por nombre).
	CREATE TEMP TABLE demo_producto (
		sku text, nombre text, unidad text, precio numeric, minimo numeric, categoria text,
		tienda numeric, alm1 numeric, alm2 numeric
	) ON COMMIT DROP;
	INSERT INTO demo_producto VALUES
		('CEM-001', 'Cemento Sol tipo I 42.5 kg', 'bolsa', 29.90, 100, 'Cemento y aglomerantes', 40, 150, 300),
		('CEM-002', 'Cemento Andino tipo V 42.5 kg', 'bolsa', 32.50, 60, 'Cemento y aglomerantes', 10, 30, 15),
		('LAD-001', 'Ladrillo King Kong 18 huecos', 'unidad', 1.35, 2000, 'Ladrillos y bloques', 500, 3000, 4000),
		('FIE-001', 'Fierro corrugado 3/8 pulgada x 9 m', 'varilla', 24.80, 150, 'Fierro y acero de construcción', 20, 60, 400),
		('FIE-002', 'Fierro corrugado 1/2 pulgada x 9 m', 'varilla', 43.90, 100, 'Fierro y acero de construcción', 5, 20, 10),
		('PIN-001', 'Pintura látex blanco 4 L', 'galón', 54.00, 30, 'Pinturas', 12, 25, 40),
		('PIN-002', 'Thinner acrílico 1 L', 'botella', 14.50, 40, 'Aerosoles y disolventes', 30, 55, 0),
		('ELE-001', 'Cable THW 14 AWG rollo 100 m', 'rollo', 118.00, 10, 'Conducción eléctrica', 6, 12, 0),
		('ELE-002', 'Tomacorriente doble con tierra', 'unidad', 6.90, 50, 'Tomas y controles', 70, 120, 0),
		('SAN-001', 'Tubo PVC desagüe 4 pulgadas x 3 m', 'unidad', 21.50, 40, 'Tuberías y conexiones', 15, 35, 80),
		('HER-001', 'Martillo de uña 16 oz', 'unidad', 28.00, 15, 'Herramientas manuales', 18, 20, 0),
		('HER-002', 'Taladro percutor 13 mm 650 W', 'unidad', 189.00, 5, 'Herramientas eléctricas y accesorios', 3, 2, 0),
		('SUJ-001', 'Clavo de acero 3 pulgadas caja 1 kg', 'caja', 9.80, 60, 'Sujetadores y fijaciones', 45, 90, 0),
		('LIM-001', 'Detergente industrial 5 L', 'bidón', 26.00, 20, 'Productos de limpieza', 22, 30, 0);

	INSERT INTO producto (sku, nombre_oficial, unidad_medida, precio_referencial, stock_minimo, id_categoria)
	SELECT d.sku, d.nombre, d.unidad, d.precio, d.minimo, c.id_categoria
	  FROM demo_producto d JOIN categoria c ON c.nombre = d.categoria;

	-- Existencias y Kardex de carga inicial (solo donde la cantidad es positiva).
	INSERT INTO inventario (id_producto, id_almacen, cantidad_fisica)
	SELECT p.id_producto, a.id_almacen, s.cantidad
	  FROM demo_producto d
	  JOIN producto p ON p.sku = d.sku
	  CROSS JOIN LATERAL (VALUES ('Tienda', d.tienda), ('Almacén 1', d.alm1), ('Almacén 2', d.alm2)) AS s(ubicacion, cantidad)
	  JOIN almacen a ON a.nombre = s.ubicacion
	 WHERE s.cantidad > 0;

	INSERT INTO movimiento_kardex (id_inventario, fecha_hora, tipo_operacion, cantidad, motivo, id_usuario_responsable, origen_movimiento)
	SELECT i.id_inventario, now(), 'entrada', i.cantidad_fisica, 'Carga inicial de demostración', v_admin, 'ajuste'
	  FROM inventario i JOIN producto p ON p.id_producto = i.id_producto
	 WHERE p.sku IN (SELECT sku FROM demo_producto);

	-- Clientes.
	INSERT INTO cliente (tipo_documento, numero_documento, nombre_razon_social, telefono_contacto, direccion_referencia) VALUES
		('ruc', '20512345678', 'Constructora Andina S.A.C.', '987654321', 'Av. Los Constructores 120'),
		('ruc', '20498765432', 'Inmobiliaria Pacífico S.A.', '956321478', 'Jr. Las Flores 45'),
		('dni', '45871236', 'Luis Rivera Campos', '945123678', 'Calle Los Pinos 310'),
		('dni', '71234568', 'María Quispe Huamán', '921456789', 'Psj. San Martín 22'),
		(NULL, NULL, 'Cliente de mostrador', NULL, NULL);

	-- Proveedores y su catálogo de compra.
	INSERT INTO proveedor (ruc, razon_social, contacto, condiciones) VALUES
		('20100070970', 'Distribuidora Cementera del Sur S.A.', 'Rosa Medina; 987001122; ventas@cementerasur.example', 'Pago a 30 días; despacho en 3 días.'),
		('20551122334', 'Ferretería Industrial Lima S.A.C.', 'Jorge Salas; 987003344; pedidos@ferrilima.example', 'Pago contra entrega; despacho en 5 días.');

	INSERT INTO catalogo_proveedor (id_proveedor, id_producto, precio_compra, plazo_entrega_referencial_dias)
	SELECT pr.id_proveedor, p.id_producto, x.precio, x.plazo
	  FROM (VALUES
		('20100070970', 'CEM-001', 26.50, 3), ('20100070970', 'CEM-002', 29.00, 3),
		('20100070970', 'LAD-001', 1.10, 4), ('20100070970', 'FIE-001', 21.90, 5),
		('20100070970', 'FIE-002', 39.50, 5),
		('20551122334', 'PIN-001', 46.00, 5), ('20551122334', 'ELE-001', 98.00, 6),
		('20551122334', 'SAN-001', 17.80, 5), ('20551122334', 'HER-002', 160.00, 7)
	  ) AS x(ruc, sku, precio, plazo)
	  JOIN proveedor pr ON pr.ruc = x.ruc
	  JOIN producto p ON p.sku = x.sku;

	RAISE NOTICE 'Datos de demostración cargados.';
END
$demo$;
