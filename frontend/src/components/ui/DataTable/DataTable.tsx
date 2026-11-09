import { Skeleton, Table, UnstyledButton, VisuallyHidden } from '@mantine/core';
import { useState, type KeyboardEvent, type ReactNode } from 'react';
import { cx } from '@/lib/cx';
import { EmptyState } from '../EmptyState/EmptyState';
import { Icon } from '../Icon/Icon';
import classes from './DataTable.module.css';

export interface Column<T> {
  key: string;
  header: string;
  /** Contenido de la celda; por omisión, `row[key]`. */
  render?: (row: T) => ReactNode;
  /** Alinea a la derecha y usa cifras tabulares. */
  numeric?: boolean;
  /** SKU y números de documento en fuente monoespaciada. */
  mono?: boolean;
  sortable?: boolean;
  /** Teléfono: título de la ficha. */
  primary?: boolean;
  /** Teléfono: `false` omite la columna de la ficha. */
  phone?: boolean;
  /** Teléfono: etiqueta sobre el contenido, a ancho completo. */
  block?: boolean;
  /** Muestra la columna solo en las fichas del teléfono (por ejemplo, las ubicaciones). */
  phoneOnly?: boolean;
}

export interface Sort {
  key: string;
  dir: 'asc' | 'desc';
}

export interface DataTableProps<T> {
  columns: Column<T>[];
  rows: T[];
  rowKey: keyof T | ((row: T) => string | number);
  /** Texto accesible de la tabla. */
  caption: string;
  loading?: boolean;
  /** Reemplaza al estado vacío predeterminado. */
  empty?: ReactNode;
  onRowClick?: (row: T) => void;
  selectedKey?: string | number;
  /** Fila cuyo dato cambió tras una acción: se resalta 1.2 s. No se anima si ya estaba definida en la carga inicial. */
  changedKey?: string | number;
  sort?: Sort;
  onSort?: (sort: Sort) => void;
  /** Totales: una fila (o varias, como subtotal, descuento y total) con el contenido por clave de columna. */
  footer?: Record<string, ReactNode> | Record<string, ReactNode>[];
  /** Teléfono: una ficha por fila. Desactivar solo en tablas de comparación. */
  stack?: boolean;
}

const SKELETON_ROWS = 5;

/** Tabla de datos semántica. En teléfono cada fila se presenta como ficha (ver DataTable.module.css). */
export function DataTable<T>({
  columns,
  rows,
  rowKey,
  caption,
  loading,
  empty,
  onRowClick,
  selectedKey,
  changedKey,
  sort,
  onSort,
  footer,
  stack = true,
}: DataTableProps<T>) {
  const [initialChangedKey] = useState(changedKey);
  const visibleColumns = stack ? columns : columns.filter((col) => !col.phoneOnly);
  const keyOf = (row: T) =>
    typeof rowKey === 'function' ? rowKey(row) : (row[rowKey] as string | number);
  const cellClass = (col: Column<T>) =>
    cx(
      col.numeric && classes.numeric,
      col.mono && classes.mono,
      col.primary && classes.primary,
      col.block && classes.block,
      col.phone === false && classes.hidePhone,
      col.phoneOnly && classes.phoneOnly,
    );
  // La etiqueta se muestra en la ficha del teléfono mediante `attr(data-label)`.
  const cellProps = (col: Column<T>) => ({ className: cellClass(col), 'data-label': col.header });

  const toggleSort = (col: Column<T>) =>
    onSort?.({ key: col.key, dir: sort?.key === col.key && sort.dir === 'asc' ? 'desc' : 'asc' });

  const sortIcon = (col: Column<T>) =>
    sort?.key !== col.key ? 'chevrons-up-down' : sort.dir === 'asc' ? 'arrow-up' : 'arrow-down';

  const activate = (e: KeyboardEvent, row: T) => {
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault();
      onRowClick?.(row);
    }
  };

  return (
    <div className={cx(classes.wrap, stack && classes.stack)}>
      <Table className={classes.table} highlightOnHover={!!onRowClick}>
        <Table.Caption className={classes.caption}>{caption}</Table.Caption>
        <Table.Thead>
          <Table.Tr>
            {visibleColumns.map((col) => (
              <Table.Th
                key={col.key}
                scope="col"
                className={cellClass(col)}
                aria-sort={
                  sort?.key === col.key
                    ? sort.dir === 'asc'
                      ? 'ascending'
                      : 'descending'
                    : undefined
                }
              >
                {col.sortable ? (
                  <UnstyledButton className={classes.sortButton} onClick={() => toggleSort(col)}>
                    {col.header}
                    <Icon name={sortIcon(col)} size={16} />
                  </UnstyledButton>
                ) : (
                  col.header
                )}
              </Table.Th>
            ))}
          </Table.Tr>
        </Table.Thead>
        <Table.Tbody>
          {loading
            ? Array.from({ length: SKELETON_ROWS }, (_, i) => (
                <Table.Tr key={i}>
                  {visibleColumns.map((col) => (
                    <Table.Td key={col.key} {...cellProps(col)}>
                      <Skeleton height={16} />
                    </Table.Td>
                  ))}
                </Table.Tr>
              ))
            : rows.map((row) => {
                const key = keyOf(row);
                return (
                  <Table.Tr
                    key={key}
                    // Fila navegable: se activa con Enter y Espacio, además del clic.
                    tabIndex={onRowClick ? 0 : undefined}
                    className={cx(
                      onRowClick && classes.clickable,
                      key === changedKey && key !== initialChangedKey && classes.changed,
                    )}
                    data-selected={key === selectedKey || undefined}
                    onClick={onRowClick && (() => onRowClick(row))}
                    onKeyDown={onRowClick && ((e) => activate(e, row))}
                  >
                    {visibleColumns.map((col) => (
                      <Table.Td key={col.key} {...cellProps(col)}>
                        {col.render ? col.render(row) : (row[col.key as keyof T] as ReactNode)}
                      </Table.Td>
                    ))}
                  </Table.Tr>
                );
              })}
        </Table.Tbody>
        {footer && (
          <Table.Tfoot>
            {[footer].flat().map((row, i) => (
              <Table.Tr key={i}>
                {visibleColumns.map((col) => (
                  <Table.Td key={col.key} {...cellProps(col)}>
                    {row[col.key]}
                  </Table.Td>
                ))}
              </Table.Tr>
            ))}
          </Table.Tfoot>
        )}
      </Table>
      {!loading &&
        rows.length === 0 &&
        (empty ?? (
          <EmptyState
            title="Sin resultados"
            description="No hay registros que coincidan con la consulta."
          />
        ))}
      {loading && <VisuallyHidden role="status">Cargando datos</VisuallyHidden>}
    </div>
  );
}
