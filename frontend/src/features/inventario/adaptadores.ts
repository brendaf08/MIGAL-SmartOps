import type { ProductoDisponibilidad } from './types';

/** Existencia de un producto en una ubicación, según `ExistenciaUbicacion` del contrato de inventario. */
export interface ExistenciaUbicacionDto {
  ubicacionId: string;
  nombre: string;
  fisica: number;
  reservada: number;
  libre: number;
}

/** Existencias consolidadas y por ubicación, según `ProductoExistencias` del contrato de inventario. */
export interface ProductoExistenciasDto {
  productoId: string;
  sku: string;
  nombre: string;
  categoria: string;
  unidad: string;
  stockMinimo: number;
  fisico: number;
  reservado: number;
  libre: number;
  nivelAlerta: 'normal' | 'minimo' | 'critico';
  ubicaciones: ExistenciaUbicacionDto[];
}

/**
 * Convierte las existencias de la API al formato de disponibilidad de la interfaz. La interfaz muestra tres ubicaciones
 * fijas (tienda principal, almacén 1 y almacén 2); el contrato devuelve las ubicaciones en ese orden, por lo que se
 * asignan por posición. Las ubicaciones ausentes se muestran sin existencias.
 */
export const aDisponibilidad = (dto: ProductoExistenciasDto): ProductoDisponibilidad => {
  const fisicaEn = (posicion: number) => dto.ubicaciones[posicion]?.fisica ?? 0;
  return {
    sku: dto.sku,
    nombre: dto.nombre,
    categoria: dto.categoria,
    unidad: dto.unidad,
    tienda: fisicaEn(0),
    almacen1: fisicaEn(1),
    almacen2: fisicaEn(2),
    reservado: dto.reservado,
    minimo: dto.stockMinimo,
  };
};
