import type { Meta, StoryObj } from '@storybook/react-vite';
import { Button } from '@mantine/core';
import { EmptyState } from './EmptyState';

const meta = {
  title: 'UI/EmptyState',
  component: EmptyState,
  args: {
    title: 'Sin resultados',
    description: 'Ningún producto coincide con la búsqueda o el filtro.',
  },
} satisfies Meta<typeof EmptyState>;
export default meta;
type Story = StoryObj<typeof meta>;

export const Basico: Story = {};
export const ConAccion: Story = {
  args: { action: <Button variant="default">Limpiar filtros</Button> },
};
