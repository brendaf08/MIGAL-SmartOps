import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { AppProviders } from '@/app/AppProviders';
import { DataTable, type Column } from './DataTable';

interface Row {
  sku: string;
  nombre: string;
  stock: number;
}

const columns: Column<Row>[] = [
  { key: 'nombre', header: 'Producto', primary: true, sortable: true },
  { key: 'sku', header: 'SKU', mono: true, phone: false },
  { key: 'stock', header: 'Stock', numeric: true },
];
const rows: Row[] = [
  { sku: 'A-1', nombre: 'Martillo', stock: 4 },
  { sku: 'B-2', nombre: 'Taladro', stock: 0 },
];

const renderTable = (props: Partial<Parameters<typeof DataTable<Row>>[0]> = {}) =>
  render(
    <AppProviders>
      <DataTable columns={columns} rows={rows} rowKey="sku" caption="Inventario" {...props} />
    </AppProviders>,
  );

describe('DataTable', () => {
  it('expone una tabla semántica con encabezados y etiquetas de ficha para el teléfono', () => {
    renderTable();

    expect(screen.getByRole('table', { name: 'Inventario' })).toBeInTheDocument();
    expect(screen.getAllByRole('columnheader')).toHaveLength(3);
    expect(screen.getByText('Martillo')).toHaveAttribute('data-label', 'Producto');
  });

  it('informa el orden con aria-sort y notifica el cambio de dirección', async () => {
    const onSort = vi.fn();
    renderTable({ sort: { key: 'nombre', dir: 'asc' }, onSort });

    expect(screen.getByRole('columnheader', { name: /Producto/ })).toHaveAttribute(
      'aria-sort',
      'ascending',
    );
    await userEvent.click(screen.getByRole('button', { name: /Producto/ }));
    expect(onSort).toHaveBeenCalledWith({ key: 'nombre', dir: 'desc' });
  });

  it('activa la fila con Enter', async () => {
    const onRowClick = vi.fn();
    renderTable({ onRowClick });

    screen.getByText('Taladro').closest('tr')?.focus();
    await userEvent.keyboard('{Enter}');
    expect(onRowClick).toHaveBeenCalledWith(rows[1]);
  });

  it('muestra el estado vacío y el de carga', () => {
    const { rerender } = renderTable({ rows: [] });
    expect(screen.getByText('Sin resultados')).toBeInTheDocument();

    rerender(
      <AppProviders>
        <DataTable columns={columns} rows={[]} rowKey="sku" caption="Inventario" loading />
      </AppProviders>,
    );
    expect(screen.getByRole('status')).toHaveTextContent('Cargando datos');
    expect(screen.queryByText('Sin resultados')).not.toBeInTheDocument();
  });

  it('marca la columna principal, omite las de teléfono y etiqueta las de bloque', () => {
    const cols: Column<Row>[] = [
      { key: 'nombre', header: 'Producto', primary: true },
      { key: 'sku', header: 'SKU', phone: false },
      { key: 'stock', header: 'Ubicaciones', phoneOnly: true, block: true },
    ];
    renderTable({ columns: cols });

    expect(screen.getByText('Martillo').className).toMatch(/primary/);
    expect(screen.getByText('A-1').className).toMatch(/hidePhone/);
    expect(screen.getAllByText('4')[0]!.className).toMatch(/phoneOnly/);
    expect(screen.getAllByText('4')[0]!.className).toMatch(/block/);
  });

  it('retira las columnas phoneOnly cuando las fichas están desactivadas', () => {
    const cols: Column<Row>[] = [
      { key: 'nombre', header: 'Producto' },
      { key: 'stock', header: 'Ubicaciones', phoneOnly: true },
    ];
    renderTable({ columns: cols, stack: false });

    expect(screen.queryByRole('columnheader', { name: 'Ubicaciones' })).not.toBeInTheDocument();
  });

  it('resalta la fila cambiada solo cuando cambia después de la carga', () => {
    const view = (changedKey?: string) => (
      <AppProviders>
        <DataTable
          columns={columns}
          rows={rows}
          rowKey="sku"
          caption="Inventario"
          changedKey={changedKey}
        />
      </AppProviders>
    );
    const { rerender } = render(view('A-1'));
    expect(screen.getByText('Martillo').closest('tr')?.className ?? '').not.toMatch(/changed/);

    rerender(view('B-2'));
    expect(screen.getByText('Taladro').closest('tr')?.className).toMatch(/changed/);
    expect(screen.getByText('Martillo').closest('tr')?.className ?? '').not.toMatch(/changed/);
  });
});
