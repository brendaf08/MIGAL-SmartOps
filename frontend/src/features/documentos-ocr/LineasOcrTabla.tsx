import { Select } from '@mantine/core';
import { MatchConfidence } from '@/components/domain/MatchConfidence/MatchConfidence';
import { DataTable, type Column } from '@/components/ui/DataTable/DataTable';
import { PRODUCTOS } from '@/features/inventario/fixtures';

const OPCIONES = PRODUCTOS.map((p) => ({ value: p.sku, label: `${p.sku}, ${p.nombre}` }));

/** Campos comunes de una línea extraída por el OCR y asociada, o no, a un producto. */
export interface LineaOcrFila {
  id: number | string;
  texto: string;
  sku: string;
  score: number;
}

export interface LineasOcrTablaProps<T extends LineaOcrFila> {
  /** Texto accesible de la tabla. */
  caption: string;
  lineas: T[];
  /** Asocia un producto del catálogo a la línea; vacío si se quita la asociación. */
  onSkuChange: (id: T['id'], sku: string) => void;
  /** Columnas que siguen a la de coincidencia: cantidades, pedido y diferencia según el flujo. */
  columnas: Column<T>[];
}

/**
 * Tabla de revisión de líneas del OCR (P24 y P27): texto extraído, producto del catálogo con su coincidencia y
 * las columnas de cantidad que aporta cada flujo. El OCR sugiere la asociación; el usuario la confirma (RF-FUZ-06).
 */
export function LineasOcrTabla<T extends LineaOcrFila>({
  caption,
  lineas,
  onSkuChange,
  columnas,
}: LineasOcrTablaProps<T>) {
  const columns: Column<T>[] = [
    { key: 'texto', header: 'Texto extraído', primary: true, mono: true },
    {
      key: 'sku',
      header: 'Producto del catálogo',
      block: true,
      render: (l) => (
        <Select
          aria-label={`Producto para ${l.texto}`}
          placeholder="Seleccionar producto"
          searchable
          miw={150}
          comboboxProps={{ width: 360, position: 'bottom-start' }}
          data={OPCIONES}
          value={l.sku || null}
          onChange={(sku) => onSkuChange(l.id, sku ?? '')}
          error={l.sku ? undefined : 'Asociar un producto.'}
        />
      ),
    },
    {
      key: 'score',
      header: 'Coincidencia',
      block: true,
      render: (l) => (
        <div style={{ minWidth: 180 }}>
          <MatchConfidence score={l.score} compact />
        </div>
      ),
    },
    ...columnas,
  ];

  return <DataTable caption={caption} columns={columns} rows={lineas} rowKey="id" />;
}
