import type { Meta, StoryObj } from '@storybook/react-vite';
import { Alert } from './Alert';

const meta = {
  title: 'UI/Alert',
  component: Alert,
  args: {
    title: 'Datos precargados desde el documento',
    children: 'Revisar las líneas antes de confirmar.',
  },
} satisfies Meta<typeof Alert>;
export default meta;
type Story = StoryObj<typeof meta>;

export const Info: Story = {};
export const Exito: Story = {
  args: { tone: 'success', title: 'Todas las cantidades están reservadas' },
};
export const Advertencia: Story = {
  args: { tone: 'warning', title: 'Hay cantidades sin reservar' },
};
export const Peligro: Story = { args: { tone: 'danger', title: 'Documento ilegible' } };
