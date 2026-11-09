import type { Meta, StoryObj } from '@storybook/react-vite';
import { Button, Text } from '@mantine/core';
import { Card } from './Card';

const meta = {
  title: 'UI/Card',
  component: Card,
  args: { title: 'Documento', description: 'Foto o PDF de la guía del proveedor.' },
} satisfies Meta<typeof Card>;
export default meta;
type Story = StoryObj<typeof meta>;

export const Basica: Story = { args: { children: <Text>Contenido de la tarjeta.</Text> } };
export const ConAccionesYPie: Story = {
  args: {
    children: <Text>Contenido de la tarjeta.</Text>,
    actions: <Button variant="default">Reemplazar</Button>,
    footer: <Button>Confirmar</Button>,
  },
};
