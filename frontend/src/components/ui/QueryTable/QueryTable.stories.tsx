import type { Meta, StoryObj } from '@storybook/react-vite';
import { QueryTable } from './QueryTable';

interface Fila {
  sku: string;
  nombre: string;
}

const meta = {
  title: 'UI/QueryTable',
  component: QueryTable<Fila>,
  args: {
    caption: 'Productos',
    errorTitle: 'No se pudo consultar los productos',
    columns: [
      { key: 'sku', header: 'SKU', mono: true },
      { key: 'nombre', header: 'Producto', primary: true },
    ],
    rowKey: 'sku',
    query: {
      isLoading: false,
      isError: false,
      data: [{ sku: 'CEM-001', nombre: 'Cemento Sol tipo I' }],
    },
  },
} satisfies Meta<typeof QueryTable<Fila>>;
export default meta;
type Story = StoryObj<typeof meta>;

export const ConDatos: Story = {};
export const Cargando: Story = { args: { query: { isLoading: true, isError: false } } };
export const ConError: Story = { args: { query: { isLoading: false, isError: true } } };
