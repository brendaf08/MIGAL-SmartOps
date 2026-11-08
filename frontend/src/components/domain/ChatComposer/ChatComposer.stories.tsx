import type { Meta, StoryObj } from '@storybook/react-vite';
import { ChatComposer } from './ChatComposer';

const meta = {
  title: 'Dominio/ChatComposer',
  component: ChatComposer,
  args: { onSend: () => undefined },
} satisfies Meta<typeof ChatComposer>;
export default meta;
type Story = StoryObj<typeof meta>;

export const Basico: Story = {};
export const ConSugerencias: Story = {
  args: { suggestions: ['¿Cuánto cemento hay?', '¿Qué productos están en el mínimo?'] },
};
export const Deshabilitado: Story = { args: { disabled: true } };
