import type { CambioStock } from '@/components/domain/Constancia/Constancia';
import type { Movimiento, ProductoDisponibilidad } from './types';

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

/** Movimiento de Kardex según `MovimientoKardex` del contrato de inventario. */
export interface MovimientoKardexDto {
  id: string;
  fechaHora: string;
  productoId: string;
  sku: string;
  nombreProducto: string;
  tipoOperacion: 'entrada' | 'salida';
  cantidad: number;
  origenMovimiento: 'despacho' | 'recepcion' | 'ajuste' | 'traslado';
  ubicacionId: string;
  usuarioId: string;
}

/** Existencia antes y después de una operación, según `CambioExistencia` del contrato de inventario. */
export interface CambioExistenciaDto {
  productoId: string;
  sku: string;
  ubicacionId: string;
  unidad: string;
  antes: number;
  despues: number;
}

/** Ubicación según `Almacen` del contrato de catálogo. */
export interface AlmacenDto {
  id: string;
  nombre: string;
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

const ORIGEN_DE_API: Record<MovimientoKardexDto['origenMovimiento'], Movimiento['origen']> = {
  despacho: 'Despacho',
  recepcion: 'Recepcion',
  ajuste: 'Ajuste',
  traslado: 'Traslado',
};

/** Movimiento de Kardex a la forma de la interfaz. La ubicación y el responsable se muestran por su nombre o identificador. */
export const aMovimiento = (dto: MovimientoKardexDto, nombresUbicacion: Map<string, string>): Movimiento => ({
  id: dto.id,
  fecha: dto.fechaHora.slice(0, 10),
  sku: dto.sku,
  producto: dto.nombreProducto,
  tipo: dto.tipoOperacion === 'entrada' ? 'Entrada' : 'Salida',
  cantidad: dto.cantidad,
  origen: ORIGEN_DE_API[dto.origenMovimiento],
  ubicacion: nombresUbicacion.get(dto.ubicacionId) ?? dto.ubicacionId,
  usuario: dto.usuarioId,
});

/** Existencia antes y después a la forma de la constancia de la interfaz. */
export const aCambioStock = (
  dto: CambioExistenciaDto,
  nombreProducto: string,
  nombresUbicacion: Map<string, string>,
): CambioStock => ({
  producto: nombreProducto,
  ubicacion: nombresUbicacion.get(dto.ubicacionId) ?? dto.ubicacionId,
  antes: dto.antes,
  despues: dto.despues,
  unidad: dto.unidad,
});
