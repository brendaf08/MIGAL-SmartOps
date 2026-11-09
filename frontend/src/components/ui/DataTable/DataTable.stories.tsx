import type { Meta, StoryObj } from '@storybook/react-vite';
import { DataTable, type Column } from './DataTable';

interface Fila {
  sku: string;
  nombre: string;
  libre: number;
}

const columns: Column<Fila>[] = [
  { key: 'sku', header: 'SKU', mono: true },
  { key: 'nombre', header: 'Producto', primary: true },
  { key: 'libre', header: 'Libre', numeric: true },
];
const rows: Fila[] = [
  { sku: 'CEM-001', nombre: 'Cemento Sol tipo I', libre: 1200 },
  { sku: 'FIE-003', nombre: 'Fierro corrugado 1/2 in', libre: 340 },
];

const meta = {
  title: 'UI/DataTable',
  component: DataTable<Fila>,
  args: { caption: 'Disponibilidad por producto', columns, rows, rowKey: 'sku' },
} satisfies Meta<typeof DataTable<Fila>>;
export default meta;
type Story = StoryObj<typeof meta>;

export const Basica: Story = {};
export const Cargando: Story = { args: { loading: true } };
export const Vacia: Story = { args: { rows: [] } };
export const ConTotales: Story = {
  args: { footer: { nombre: 'Total', libre: '1,540' } },
};
export const FilaCambiada: Story = { args: { changedKey: 'FIE-003' } };
