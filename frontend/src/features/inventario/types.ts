/** Existencias de un producto por ubicación (CU-04, RF-KAR-02). El reservado es consolidado. */
export interface ProductoDisponibilidad {
  sku: string;
  nombre: string;
  categoria: string;
  unidad: string;
  tienda: number;
  almacen1: number;
  almacen2: number;
  reservado: number;
  minimo: number;
}

export interface ConsultaDisponibilidad {
  q: string;
  soloAlerta: boolean;
  page: number;
  pageSize: number;
}

export interface RespuestaDisponibilidad {
  items: ProductoDisponibilidad[];
  /** Total que cumple la consulta, para paginar. */
  total: number;
  /** Productos en el mínimo o críticos, para el contador de la pestaña "En alerta". */
  enAlerta: number;
  /** Total de productos sin filtros, para el contador de la pestaña "Todos". */
  totalGeneral: number;
}
