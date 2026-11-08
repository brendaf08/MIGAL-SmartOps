import { Select, type SelectProps } from '@mantine/core';

/** Producto ofrecido en un selector: el SKU es el valor y `SKU, nombre` la etiqueta. */
export interface OpcionProducto {
  sku: string;
  nombre: string;
}

export interface ProductoSelectProps extends Omit<
  SelectProps,
  'data' | 'value' | 'onChange' | 'searchable'
> {
  productos: readonly OpcionProducto[];
  /** SKU elegido, o `null` si no hay selección. */
  value: string | null;
  onChange: (sku: string | null) => void;
  /** Si se indica, estos SKU se ofrecen primero (por ejemplo, los que suministra un proveedor). */
  sugeridos?: readonly string[];
}

/** Selector buscable de productos del catálogo; la lista tiene un ancho propio para que las opciones no se partan. */
export function ProductoSelect({
  productos,
  value,
  onChange,
  sugeridos = [],
  ...props
}: ProductoSelectProps) {
  const ordenados = [
    ...productos.filter((p) => sugeridos.includes(p.sku)),
    ...productos.filter((p) => !sugeridos.includes(p.sku)),
  ];

  return (
    <Select
      placeholder="Seleccionar producto"
      searchable
      comboboxProps={{ width: 360, position: 'bottom-start' }}
      data={ordenados.map((p) => ({ value: p.sku, label: `${p.sku}, ${p.nombre}` }))}
      value={value}
      onChange={onChange}
      {...props}
    />
  );
}
