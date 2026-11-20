import { Select } from '@mantine/core';
import { MatchConfidence } from '@/components/domain/MatchConfidence/MatchConfidence';
import { DataTable, type Column } from '@/components/ui/DataTable/DataTable';
import { useCatalogoVigente } from '@/features/catalogo/catalogoVigente';
import { coincideBusqueda, sugerirSkus } from './sugerencias';

/** Campos comunes de una línea extraída por el OCR y asociada, o no, a un producto. */
export interface LineaOcrFila {
  id: number | string;
  texto: string;
  sku: string;
  score: number;
  /** SKU que el servicio de OCR propone para la línea, de mayor a menor coincidencia. */
  candidatos?: string[];
}

export interface LineasOcrTablaProps<T extends LineaOcrFila> {
  /** Texto accesible de la tabla. */
  caption: string;
  lineas: T[];
  /** Asocia un producto del catálogo a la línea; vacío si se quita la asociación. */
  onSkuChange: (id: T['id'], sku: string) => void;
  /** Columnas que siguen a la de coincidencia: cantidades, pedido y diferencia según el flujo. */
  columnas: Column<T>[];
  /**
   * Sin OCR no hay texto leído ni coincidencia que revisar: se muestra solo el nombre del producto, fijo. Con OCR se
   * mantienen el texto extraído, el selector de producto y la coincidencia.
   */
  soloCatalogo?: boolean;
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
  soloCatalogo = false,
}: LineasOcrTablaProps<T>) {
  // Con la API real, el catálogo vigente llega después del primer render y vuelve a pintar la tabla.
  const { productos } = useCatalogoVigente();
  const opciones = productos.map((p) => ({ value: p.sku, label: `${p.sku}, ${p.nombre}` }));
  const etiquetaActual = (sku: string) => opciones.find((o) => o.value === sku)?.label ?? '';
  const columns: Column<T>[] = [
    { key: 'texto', header: 'Texto extraído', primary: true, mono: true },
    {
      key: 'sku',
      header: 'Producto del catálogo',
      block: true,
      render: (l) => (
        <Select
          aria-label={`Producto para ${l.texto}`}
          placeholder="Elegir una sugerencia o escribir para buscar"
          searchable
          miw={150}
          comboboxProps={{ width: 360, position: 'bottom-start' }}
          data={opciones}
          limit={30}
          nothingFoundMessage="Ningún producto coincide con la búsqueda"
          filter={({ options, search }) => {
            if (search.trim() && search !== etiquetaActual(l.sku)) {
              return options
                .filter((o) => 'label' in o && coincideBusqueda(o.label, search))
                .slice(0, 30);
            }
            // Sin escribir nada solo se ofrecen los productos relacionados con el texto leído.
            const sugeridos = new Set([
              ...(l.candidatos ?? []),
              ...sugerirSkus(l.texto, productos),
              ...(l.sku ? [l.sku] : []),
            ]);
            return options.filter((o) => 'value' in o && sugeridos.has(o.value));
          }}
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

  if (soloCatalogo) {
    const fijas: Column<T>[] = [{ key: 'texto', header: 'Producto', primary: true }, ...columnas];
    return <DataTable caption={caption} columns={fijas} rows={lineas} rowKey="id" />;
  }

  return <DataTable caption={caption} columns={columns} rows={lineas} rowKey="id" />;
}
