import type { Meta, StoryObj } from '@storybook/react-vite';
import { ChatMessage } from './ChatMessage';

const meta = { title: 'Dominio/ChatMessage', component: ChatMessage } satisfies Meta<
  typeof ChatMessage
>;
export default meta;
type Story = StoryObj<typeof meta>;

export const Usuario: Story = {
  args: { author: 'user', time: '10:42', children: '¿Cuánto cemento hay?' },
};
export const Agente: Story = {
  args: {
    author: 'agent',
    name: 'Agente de IA',
    time: '10:42',
    tools: [{ label: 'Consultar disponibilidad', status: 'done' }],
    children: 'Hay 1,200 bolsas libres de Cemento Sol tipo I, 900 en el Almacén 1.',
  },
};
export const SinInformacion: Story = {
  args: {
    author: 'agent',
    name: 'Agente de IA',
    uncertain: true,
    children: 'No hay información suficiente para responder.',
  },
};
