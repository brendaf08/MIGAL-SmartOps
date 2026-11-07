import type { ReactNode } from 'react';

// ─── Roles y pantallas ───────────────────────────────────────────────
export type Rol = 'administrador' | 'vendedor' | 'almacenero' | 'reponedor';

export type CodigoPantalla =
  | 'P01' | 'P02' | 'P03' | 'P04' | 'P05' | 'P06' | 'P07' | 'P08' | 'P09'
  | 'P10' | 'P11' | 'P12' | 'P13' | 'P14' | 'P15' | 'P16' | 'P17' | 'P18'
  | 'P19' | 'P20' | 'P21' | 'P22' | 'P23' | 'P24' | 'P25' | 'P26' | 'P27';

// Contrato de cada módulo: src/modules/<modulo>/index.tsx exporta `rutas: RutaModulo[]`
export interface RutaModulo {
  codigo: CodigoPantalla;
  path: string;
  titulo: string;
  elemento: ReactNode;
  enMenu: boolean;
}

// ─── Estados y catálogos (nombres exactos) ───────────────────────────
// Venta: la venta solo RESERVA stock. El stock baja (salida de Kardex) al confirmar el despacho.
// PendienteAbastecimiento: falta stock · PendienteDespacho: reservado completo, sin guía
// ListaParaDespacho: guía emitida · Despachada: salida confirmada · Cancelada: con motivo
export type EstadoVenta = 'PendienteAbastecimiento' | 'PendienteDespacho' | 'ListaParaDespacho'
  | 'Despachada' | 'Cancelada';
export type EstadoCotizacion = 'Emitida' | 'Aceptada' | 'Rechazada' | 'Vencida';
export type EstadoOrdenCompra = 'Pendiente' | 'Parcial' | 'Completada';
export type EstadoRecepcion = 'Pendiente' | 'Validada';
export type EstadoDocumento = 'Pendiente' | 'Procesado OCR' | 'Validado';
// Guía de despacho. Al pasar a SalidaConfirmada se registran las salidas de Kardex (origen Despacho).
export type EstadoDespacho = 'EnPreparacion' | 'Emitida' | 'SalidaConfirmada' | 'Entregada';
export type EstadoReserva = 'Activa' | 'Liberada' | 'Consumida';
export type TipoOperacion = 'Entrada' | 'Salida';
// Una merma se registra como Ajuste (Salida) con motivo «Merma».
export type OrigenMovimiento = 'Despacho' | 'Recepcion' | 'Ajuste' | 'Traslado';
export type NivelCoincidencia = 'Alta' | 'Media' | 'Baja' | 'No identificado';

// ─── Entidades: diccionario de datos del informe PC2 (Tablas 16 y 17) ─
// Fechas: texto 'aaaa-mm-dd' (o 'aaaa-mm-ddThh:mm' si lleva hora).
// En pantalla se muestran con formatearFecha(). password_hash no existe en el frontend.
// Campos con ? son opcionales en el diccionario.

export interface Usuario {
  id_usuario: string; nombres: string; apellidos: string; username: string;
  rol: Rol; estado_activo: boolean; fecha_registro: string; fecha_actualizacion?: string;
}

export interface Cliente {
  id_cliente: string; tipo_documento?: 'DNI' | 'RUC'; numero_documento?: string;
  nombre_razon_social: string; telefono_contacto?: string; direccion_referencia?: string;
  estado_activo: boolean; fecha_registro: string;
}

export interface Almacen {
  id_almacen: string; nombre: string; tipo_ubicacion: string;
  direccion_referencia?: string; estado_activo: boolean;
}

export interface Producto {
  id_producto: string; sku: string; nombre_oficial: string; descripcion?: string;
  unidad_medida: string; precio_referencial: number; stock_minimo: number;
  estado_activo: boolean; categoria_producto: string; marca?: string; presentacion?: string;
  fecha_registro: string; fecha_actualizacion?: string;
}

// Tabla 17 · Producto + Almacen. cantidad_libre = cantidad_fisica - cantidad_reservada
export interface Inventario {
  id_inventario: string; id_producto: string; id_almacen: string;
  cantidad_fisica: number; cantidad_reservada: number; cantidad_libre: number;
}

export interface Proveedor {
  id_proveedor: string; ruc: string; razon_social: string; contacto: string;
  condiciones?: string; estado_activo: boolean; fecha_registro: string;
}

// Tabla 17 · Proveedor + Producto
export interface CatalogoProveedor {
  id_proveedor: string; id_producto: string; precio_compra: number;
  plazo_entrega_referencial_dias?: number; fecha_actualizacion_precio?: string;
}

// ─── Ventas (Pinedo) ─────────────────────────────────────────────────
export interface Venta {
  id_venta: string; id_usuario_responsable: string; id_cotizacion: string | null;
  id_cliente: string; // obligatorio por decisión del equipo
  // id_cotizacion es null en una venta directa (decisión del equipo)
  fecha_venta: string; descuento: number; total_venta: number; estado: EstadoVenta;
  fecha_cancelacion?: string; motivo_cancelacion?: string;
}

// Tabla 17 · Venta + Producto
export interface DetalleVenta {
  id_detalle_venta: string; id_venta: string; id_producto: string;
  cantidad_vendida: number; precio_unitario: number; subtotal_detalle: number;
  cantidad_reservada_actual: number; cantidad_pendiente_abastecimiento: number;
}

// Tabla 17 · Detalle_Venta + Inventario
export interface ReservaStock {
  id_reserva: string; id_detalle_venta: string; id_inventario: string;
  cantidad_reservada: number; estado: EstadoReserva; fecha_reserva: string;
  fecha_cierre?: string; id_usuario_responsable: string;
}

// ─── Cotizaciones (Alexander) ────────────────────────────────────────
export interface Cotizacion {
  id_cotizacion: string; id_cliente: string; id_usuario_responsable: string;
  fecha_emision: string; fecha_vencimiento: string; condiciones_comerciales?: string;
  descuento: number; estado: EstadoCotizacion; total_cotizacion: number;
  fecha_aceptacion_cliente?: string; fecha_validacion_migal?: string;
}

// Tabla 17 · Cotizacion + Producto
export interface DetalleCotizacion {
  id_cotizacion: string; id_producto: string;
  cantidad_cotizada: number; precio_unitario: number; subtotal_detalle: number;
}

// ─── Despacho (Nuñez) ────────────────────────────────────────────────
export interface GuiaDespacho {
  id_guia: string; id_venta: string; fecha_despacho?: string; direccion_entrega: string;
  estado: EstadoDespacho; fecha_emision: string;
  fecha_salida_real?: string; id_usuario_responsable?: string;
}

// ─── Kardex (Nuñez) ──────────────────────────────────────────────────
export interface MovimientoKardex {
  id_movimiento: string; id_inventario: string; fecha_hora: string;
  tipo_operacion: TipoOperacion; cantidad: number; motivo?: string;
  id_usuario_responsable: string; origen_movimiento: OrigenMovimiento;
  id_guia?: string; id_recepcion?: string; referencia_traslado?: string;
}

// ─── Compras (Brenda) ────────────────────────────────────────────────
export interface OrdenCompra {
  id_orden: string; id_proveedor: string; id_usuario_responsable: string;
  fecha_emision: string; estado: EstadoOrdenCompra;
  fecha_entrega_estimada?: string; observaciones?: string; modalidad_pago?: string;
}

// Tabla 17 · Orden_Compra + Producto
export interface DetalleCompra {
  id_orden: string; id_producto: string; cantidad_comprada: number;
  costo_unitario: number; subtotal_detalle: number; cantidad_pendiente_recepcion: number;
}

// ─── Recepción (Alexander) ───────────────────────────────────────────
export interface OrdenRecepcion {
  id_recepcion: string; id_orden: string; id_usuario_responsable: string;
  fecha_recepcion: string; estado: EstadoRecepcion;
  fecha_validacion?: string; observaciones?: string;
}

// Tabla 17 · Orden_Recepcion + Producto (+ almacén destino)
export interface DetalleRecepcion {
  id_recepcion: string; id_producto: string; id_almacen_destino: string; cantidad_recibida: number;
}

// ─── OCR (Alexander) ─────────────────────────────────────────────────
export interface DocumentoDigital {
  id_documento: string; ruta_archivo: string; fecha_subida: string;
  estado_procesamiento: EstadoDocumento;
  tipo_documento: 'SolicitudCliente' | 'DocumentoRecepcion';
  id_usuario: string; id_cotizacion?: string; id_recepcion?: string;
}

export interface DetalleExtraido {
  id_detalle_extraido: string; id_documento: string; texto_crudo: string;
  cantidad_leida?: number; id_producto?: string; texto_corregido?: string;
}

// Lo que muestra la revisión del OCR (P24). nivel_coincidencia se CALCULA, no se guarda.
export interface DetalleConCoincidencia extends DetalleExtraido {
  nivel_coincidencia: NivelCoincidencia;
}
