import { Button, NumberInput } from '@mantine/core';
import { formatCurrency } from '@/lib/format';
import { ProductoSelect, type OpcionProducto } from '../ProductoSelect/ProductoSelect';
import { DataTable, type Column } from '../DataTable/DataTable';
import { Icon } from '../Icon/Icon';
import { MoneyInput } from '../MoneyInput/MoneyInput';
import {
  siguienteIdLinea,
  subtotalLinea,
  subtotalLineas,
  totalLineas,
  type LineaEditable,
} from './calculos';
import classes from './LineasEditor.module.css';

export interface LineasEditorProps {
  lineas: LineaEditable[];
  onChange: (lineas: LineaEditable[]) => void;
  productos: readonly OpcionProducto[];
  /** Muestra el precio unitario, el subtotal por línea y los totales. */
  conPrecio?: boolean;
  /** Muestra el descuento global en soles; requiere `conPrecio`. */
  conDescuento?: boolean;
  descuento?: number;
  onDescuentoChange?: (descuento: number) => void;
  /** Mensaje de error de la lista completa (por ejemplo, "Agregar al menos un producto."). */
  error?: string;
  /** SKU que se ofrecen primero en cada selector. */
  sugeridos?: readonly string[];
}

/**
 * Tabla editable de líneas (producto, cantidad y, opcionalmente, precio) con alta y baja de filas y totales con doble
 * línea contable. En teléfono cada línea se presenta como ficha.
 */
export function LineasEditor({
  lineas,
  onChange,
  productos,
  conPrecio = false,
  conDescuento = false,
  descuento = 0,
  onDescuentoChange,
  error,
  sugeridos,
}: LineasEditorProps) {
  const actualizar = (id: number, cambio: Partial<LineaEditable>) =>
    onChange(lineas.map((l) => (l.id === id ? { ...l, ...cambio } : l)));
  const quitar = (id: number) => onChange(lineas.filter((l) => l.id !== id));
  const agregar = () =>
    onChange([...lineas, { id: siguienteIdLinea(lineas), sku: '', cantidad: 1, precio: 0 }]);

  const columns: Column<LineaEditable>[] = [
    {
      key: 'sku',
      header: 'Producto',
      primary: true,
      block: true,
      render: (l) => (
        <ProductoSelect
          aria-label={`Producto de la línea ${l.id}`}
          productos={productos}
          sugeridos={sugeridos}
          miw={200}
          value={l.sku || null}
          onChange={(sku) => actualizar(l.id, { sku: sku ?? '' })}
        />
      ),
    },
    {
      key: 'cantidad',
      header: 'Cantidad',
      block: true,
      render: (l) => (
        <NumberInput
          aria-label={`Cantidad de la línea ${l.id}`}
          min={1}
          allowDecimal={false}
          hideControls
          miw={80}
          maw={120}
          value={l.cantidad}
          onChange={(v) => actualizar(l.id, { cantidad: Number(v) || 0 })}
        />
      ),
    },
    ...(conPrecio
      ? ([
          {
            key: 'precio',
            header: 'Precio',
            block: true,
            render: (l) => (
              <MoneyInput
                aria-label={`Precio de la línea ${l.id}`}
                miw={110}
                maw={150}
                value={l.precio}
                onChange={(v) => actualizar(l.id, { precio: Number(v) || 0 })}
              />
            ),
          },
          {
            key: 'subtotal',
            header: 'Subtotal',
            numeric: true,
            render: (l) => formatCurrency(subtotalLinea(l)),
          },
        ] satisfies Column<LineaEditable>[])
      : []),
    {
      key: 'acciones',
      header: 'Quitar',
      render: (l) => (
        <Button
          variant="subtle"
          color="danger"
          size="compact-sm"
          leftSection={<Icon name="trash" size={16} />}
          aria-label={`Quitar la línea ${l.id}`}
          onClick={() => quitar(l.id)}
        >
          Quitar
        </Button>
      ),
    },
  ];

  const subtotal = subtotalLineas(lineas);
  const pie = conPrecio
    ? [
        { cantidad: 'Subtotal', subtotal: formatCurrency(subtotal) },
        ...(conDescuento
          ? [
              {
                cantidad: 'Descuento',
                subtotal: (
                  <MoneyInput
                    aria-label="Descuento en soles"
                    maw={150}
                    max={subtotal}
                    value={descuento}
                    onChange={(v) => onDescuentoChange?.(Number(v) || 0)}
                  />
                ),
              },
            ]
          : []),
        { cantidad: 'Total', subtotal: formatCurrency(totalLineas(lineas, descuento)) },
      ]
    : undefined;

  return (
    <div className={classes.root}>
      <DataTable
        caption="Productos del documento"
        columns={columns}
        rows={lineas}
        rowKey="id"
        footer={pie}
        empty={<p className={classes.empty}>Todavía no hay productos. Agregar el primero.</p>}
      />
      {error && (
        <p className={classes.error} role="alert">
          <Icon name="alert-triangle" size={16} />
          {error}
        </p>
      )}
      <div className={classes.actions}>
        <Button variant="default" leftSection={<Icon name="plus" />} onClick={agregar}>
          Agregar producto
        </Button>
      </div>
    </div>
  );
}
