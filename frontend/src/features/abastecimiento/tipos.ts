/** Proveedor del directorio (CU-11). */
export interface Proveedor {
  id: string;
  /** RUC de 11 dígitos. */
  ruc: string;
  razonSocial: string;
  contacto: string;
  telefono: string;
  correo: string;
  condiciones: string;
  /** SKU del catálogo que suministra. */
  skus: string[];
  estado: 'Activo' | 'Inactivo';
}

export interface LineaOrdenCompra {
  sku: string;
  nombre: string;
  cantidad: number;
  recibido: number;
  precio: number;
}

/** Orden de compra (CU-12). El estado `Parcial` indica recepciones incompletas. */
export interface OrdenCompra {
  id: string;
  proveedorId: string;
  fecha: string;
  estado: 'Pendiente' | 'Parcial' | 'Completada';
  lineas: LineaOrdenCompra[];
}
