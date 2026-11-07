import type {
  Usuario, Cliente, Producto, Almacen, Inventario, Proveedor, CatalogoProveedor,
  Venta, DetalleVenta, Cotizacion, DetalleCotizacion, OrdenCompra, DetalleCompra,
} from '../tipos';

// Contraseña de los 4 usuarios de prueba: migal123
export const usuarios: Usuario[] = [
  { id_usuario: 'USR-001', nombres: 'Ana', apellidos: 'Torres', username: 'admin', rol: 'administrador', estado_activo: true, fecha_registro: '2026-01-10' },
  { id_usuario: 'USR-002', nombres: 'Luis', apellidos: 'Quispe', username: 'vendedor', rol: 'vendedor', estado_activo: true, fecha_registro: '2026-02-03' },
  { id_usuario: 'USR-003', nombres: 'Rosa', apellidos: 'Huamán', username: 'almacenero', rol: 'almacenero', estado_activo: true, fecha_registro: '2026-02-15' },
  { id_usuario: 'USR-004', nombres: 'Carlos', apellidos: 'Ramos', username: 'reponedor', rol: 'reponedor', estado_activo: true, fecha_registro: '2026-03-01' },
];

export const almacenes: Almacen[] = [
  { id_almacen: 'ALM-001', nombre: 'Tienda principal', tipo_ubicacion: 'tienda', direccion_referencia: 'Av. Principal 100', estado_activo: true },
  { id_almacen: 'ALM-002', nombre: 'Almacén 1', tipo_ubicacion: 'almacen', estado_activo: true },
  { id_almacen: 'ALM-003', nombre: 'Almacén 2', tipo_ubicacion: 'almacen', estado_activo: true },
];

export const productos: Producto[] = [
  { id_producto: 'PRD-001', sku: 'CEM-001', nombre_oficial: 'Cemento Portland tipo I', descripcion: 'Cemento de uso general', unidad_medida: 'bolsa', precio_referencial: 32.5, stock_minimo: 20, estado_activo: true, categoria_producto: 'Construcción', marca: 'Sol', presentacion: 'Bolsa de 42.5 kg', fecha_registro: '2026-01-15' },
  { id_producto: 'PRD-002', sku: 'FIE-012', nombre_oficial: 'Fierro corrugado 1/2"', descripcion: 'Varilla de construcción', unidad_medida: 'varilla', precio_referencial: 38.9, stock_minimo: 50, estado_activo: true, categoria_producto: 'Construcción', marca: 'Aceros Arequipa', presentacion: 'Varilla de 9 m', fecha_registro: '2026-01-15' },
  { id_producto: 'PRD-003', sku: 'PIN-004', nombre_oficial: 'Pintura látex blanco', descripcion: 'Pintura para interiores', unidad_medida: 'galón', precio_referencial: 55, stock_minimo: 10, estado_activo: true, categoria_producto: 'Pinturas', marca: 'CPP', presentacion: 'Galón de 4 L', fecha_registro: '2026-01-20' },
  { id_producto: 'PRD-004', sku: 'TUB-020', nombre_oficial: 'Tubo PVC 1/2" agua', descripcion: 'Tubo para agua fría', unidad_medida: 'unidad', precio_referencial: 8.5, stock_minimo: 30, estado_activo: true, categoria_producto: 'Gasfitería', marca: 'Pavco', presentacion: 'Tubo de 5 m', fecha_registro: '2026-02-01' },
  { id_producto: 'PRD-005', sku: 'CLA-003', nombre_oficial: 'Clavo de 3"', descripcion: 'Clavo para madera', unidad_medida: 'kg', precio_referencial: 6.8, stock_minimo: 15, estado_activo: true, categoria_producto: 'Ferretería', marca: 'Genérica', presentacion: 'Kilo', fecha_registro: '2026-02-01' },
];

// cantidad_libre = cantidad_fisica - cantidad_reservada
// PRD-003 y PRD-005 quedan bajo su stock mínimo (para probar alertas).
export const inventario: Inventario[] = [
  { id_inventario: 'INV-001', id_producto: 'PRD-001', id_almacen: 'ALM-001', cantidad_fisica: 30, cantidad_reservada: 5, cantidad_libre: 25 },
  { id_inventario: 'INV-002', id_producto: 'PRD-001', id_almacen: 'ALM-002', cantidad_fisica: 120, cantidad_reservada: 0, cantidad_libre: 120 },
  { id_inventario: 'INV-003', id_producto: 'PRD-002', id_almacen: 'ALM-001', cantidad_fisica: 40, cantidad_reservada: 10, cantidad_libre: 30 },
  { id_inventario: 'INV-004', id_producto: 'PRD-002', id_almacen: 'ALM-003', cantidad_fisica: 200, cantidad_reservada: 0, cantidad_libre: 200 },
  { id_inventario: 'INV-005', id_producto: 'PRD-003', id_almacen: 'ALM-001', cantidad_fisica: 4, cantidad_reservada: 1, cantidad_libre: 3 },
  { id_inventario: 'INV-006', id_producto: 'PRD-003', id_almacen: 'ALM-002', cantidad_fisica: 2, cantidad_reservada: 0, cantidad_libre: 2 },
  { id_inventario: 'INV-007', id_producto: 'PRD-004', id_almacen: 'ALM-001', cantidad_fisica: 25, cantidad_reservada: 0, cantidad_libre: 25 },
  { id_inventario: 'INV-008', id_producto: 'PRD-004', id_almacen: 'ALM-003', cantidad_fisica: 60, cantidad_reservada: 0, cantidad_libre: 60 },
  { id_inventario: 'INV-009', id_producto: 'PRD-005', id_almacen: 'ALM-001', cantidad_fisica: 8, cantidad_reservada: 0, cantidad_libre: 8 },
];

export const clientes: Cliente[] = [
  { id_cliente: 'CLI-001', tipo_documento: 'DNI', numero_documento: '45678912', nombre_razon_social: 'Juan Pérez Quispe', telefono_contacto: '987654321', direccion_referencia: 'Av. Los Olivos 123', estado_activo: true, fecha_registro: '2026-03-05' },
  { id_cliente: 'CLI-002', tipo_documento: 'RUC', numero_documento: '20600000001', nombre_razon_social: 'Constructora Los Andes S.A.C.', telefono_contacto: '014567890', direccion_referencia: 'Jr. Lima 456', estado_activo: true, fecha_registro: '2026-03-10' },
  { id_cliente: 'CLI-003', tipo_documento: 'DNI', numero_documento: '70123456', nombre_razon_social: 'María Flores Rojas', telefono_contacto: '912345678', direccion_referencia: 'Calle Las Flores 789', estado_activo: true, fecha_registro: '2026-04-02' },
];

export const proveedores: Proveedor[] = [
  { id_proveedor: 'PRV-001', ruc: '20600000002', razon_social: 'Distribuidora Ferretera del Sur S.A.C.', contacto: 'Marta Díaz – 999111222', condiciones: 'Crédito a 30 días', estado_activo: true, fecha_registro: '2026-01-12' },
  { id_proveedor: 'PRV-002', ruc: '20600000003', razon_social: 'Aceros y Más E.I.R.L.', contacto: 'Jorge Silva – 999333444', condiciones: 'Contado', estado_activo: true, fecha_registro: '2026-01-18' },
];

export const catalogoProveedor: CatalogoProveedor[] = [
  { id_proveedor: 'PRV-001', id_producto: 'PRD-001', precio_compra: 27, plazo_entrega_referencial_dias: 3 },
  { id_proveedor: 'PRV-001', id_producto: 'PRD-003', precio_compra: 45, plazo_entrega_referencial_dias: 5 },
  { id_proveedor: 'PRV-002', id_producto: 'PRD-002', precio_compra: 35, plazo_entrega_referencial_dias: 7 },
];

// ─── Semillas que cruzan módulos ─────────────────────────────────────
// Cotización: la lee Pinedo en P20 (venta desde cotización Aceptada)
export const cotizaciones: Cotizacion[] = [
  { id_cotizacion: 'COT-001', id_cliente: 'CLI-002', id_usuario_responsable: 'USR-002', fecha_emision: '2026-11-02', fecha_vencimiento: '2026-11-16', condiciones_comerciales: 'Entrega en obra', descuento: 61, estado: 'Aceptada', total_cotizacion: 1300, fecha_aceptacion_cliente: '2026-11-03' },
  { id_cotizacion: 'COT-002', id_cliente: 'CLI-001', id_usuario_responsable: 'USR-002', fecha_emision: '2026-11-04', fecha_vencimiento: '2026-11-18', descuento: 0, estado: 'Emitida', total_cotizacion: 110 },
  { id_cotizacion: 'COT-003', id_cliente: 'CLI-003', id_usuario_responsable: 'USR-002', fecha_emision: '2026-10-01', fecha_vencimiento: '2026-10-15', descuento: 0, estado: 'Vencida', total_cotizacion: 65 },
];
export const detallesCotizacion: DetalleCotizacion[] = [
  { id_cotizacion: 'COT-001', id_producto: 'PRD-001', cantidad_cotizada: 20, precio_unitario: 32.5, subtotal_detalle: 650 },
  { id_cotizacion: 'COT-001', id_producto: 'PRD-002', cantidad_cotizada: 18, precio_unitario: 39.5, subtotal_detalle: 711 },
  { id_cotizacion: 'COT-002', id_producto: 'PRD-003', cantidad_cotizada: 2, precio_unitario: 55, subtotal_detalle: 110 },
  { id_cotizacion: 'COT-003', id_producto: 'PRD-001', cantidad_cotizada: 2, precio_unitario: 32.5, subtotal_detalle: 65 },
];

// Venta: la lee Nuñez en P15 (ventas PendienteDespacho y ListaParaDespacho)
export const ventas: Venta[] = [
  { id_venta: 'VEN-001', id_usuario_responsable: 'USR-002', id_cotizacion: null, id_cliente: 'CLI-001', fecha_venta: '2026-11-03', descuento: 0, total_venta: 77.8, estado: 'Despachada' },
  { id_venta: 'VEN-002', id_usuario_responsable: 'USR-002', id_cotizacion: 'COT-001', id_cliente: 'CLI-002', fecha_venta: '2026-11-04', descuento: 61, total_venta: 1300, estado: 'PendienteDespacho' },
  { id_venta: 'VEN-003', id_usuario_responsable: 'USR-002', id_cotizacion: null, id_cliente: 'CLI-003', fecha_venta: '2026-11-05', descuento: 0, total_venta: 550, estado: 'PendienteAbastecimiento' },
  { id_venta: 'VEN-004', id_usuario_responsable: 'USR-002', id_cotizacion: null, id_cliente: 'CLI-001', fecha_venta: '2026-11-05', descuento: 5, total_venta: 80, estado: 'PendienteDespacho' },
];
export const detallesVenta: DetalleVenta[] = [
  { id_detalle_venta: 'DVE-001', id_venta: 'VEN-001', id_producto: 'PRD-002', cantidad_vendida: 2, precio_unitario: 38.9, subtotal_detalle: 77.8, cantidad_reservada_actual: 0, cantidad_pendiente_abastecimiento: 0 },
  { id_detalle_venta: 'DVE-002', id_venta: 'VEN-002', id_producto: 'PRD-001', cantidad_vendida: 20, precio_unitario: 32.5, subtotal_detalle: 650, cantidad_reservada_actual: 5, cantidad_pendiente_abastecimiento: 0 },
  { id_detalle_venta: 'DVE-003', id_venta: 'VEN-002', id_producto: 'PRD-002', cantidad_vendida: 18, precio_unitario: 39.5, subtotal_detalle: 711, cantidad_reservada_actual: 10, cantidad_pendiente_abastecimiento: 0 },
  { id_detalle_venta: 'DVE-004', id_venta: 'VEN-003', id_producto: 'PRD-003', cantidad_vendida: 10, precio_unitario: 55, subtotal_detalle: 550, cantidad_reservada_actual: 1, cantidad_pendiente_abastecimiento: 7 },
  { id_detalle_venta: 'DVE-005', id_venta: 'VEN-004', id_producto: 'PRD-004', cantidad_vendida: 10, precio_unitario: 8.5, subtotal_detalle: 85, cantidad_reservada_actual: 0, cantidad_pendiente_abastecimiento: 0 },
];

// Orden de compra: la lee Alexander en P27 (recepción asociada a una orden)
export const ordenesCompra: OrdenCompra[] = [
  { id_orden: 'OC-001', id_proveedor: 'PRV-001', id_usuario_responsable: 'USR-004', fecha_emision: '2026-11-01', estado: 'Pendiente', fecha_entrega_estimada: '2026-11-08', modalidad_pago: 'Crédito 30 días' },
  { id_orden: 'OC-002', id_proveedor: 'PRV-002', id_usuario_responsable: 'USR-004', fecha_emision: '2026-10-28', estado: 'Parcial', fecha_entrega_estimada: '2026-11-04', modalidad_pago: 'Contado' },
  { id_orden: 'OC-003', id_proveedor: 'PRV-001', id_usuario_responsable: 'USR-004', fecha_emision: '2026-10-15', estado: 'Completada' },
];
export const detallesCompra: DetalleCompra[] = [
  { id_orden: 'OC-001', id_producto: 'PRD-003', cantidad_comprada: 20, costo_unitario: 45, subtotal_detalle: 900, cantidad_pendiente_recepcion: 20 },
  { id_orden: 'OC-002', id_producto: 'PRD-002', cantidad_comprada: 100, costo_unitario: 35, subtotal_detalle: 3500, cantidad_pendiente_recepcion: 40 },
  { id_orden: 'OC-003', id_producto: 'PRD-005', cantidad_comprada: 100, costo_unitario: 6, subtotal_detalle: 600, cantidad_pendiente_recepcion: 0 },
];
