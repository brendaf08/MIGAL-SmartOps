import type { Meta, StoryObj } from '@storybook/react-vite';
import { Button, Text } from '@mantine/core';
import { ConfirmDialog } from './ConfirmDialog';
import { Modal } from './Modal';

const meta = {
  title: 'UI/Modal',
  component: Modal,
  parameters: { layout: 'fullscreen' },
  args: {
    open: true,
    title: 'Detalle del producto',
    onClose: () => undefined,
    children: <Text>Contenido del diálogo.</Text>,
  },
} satisfies Meta<typeof Modal>;
export default meta;
type Story = StoryObj<typeof meta>;

export const Basico: Story = {};
export const ConAcciones: Story = {
  args: {
    footer: (
      <>
        <Button variant="default">Cancelar</Button>
        <Button>Guardar</Button>
      </>
    ),
  },
};

export const Confirmacion: StoryObj<typeof ConfirmDialog> = {
  render: () => (
    <ConfirmDialog
      open
      title="Confirmar recepción OC-0087"
      description="Se ingresará al inventario la mercadería efectivamente recibida."
      consequences={[
        'Se registrarán 3 movimientos de entrada en el Kardex.',
        'La acción no se puede deshacer.',
      ]}
      confirmLabel="Confirmar recepción"
      cancelLabel="Volver"
      onConfirm={() => undefined}
      onCancel={() => undefined}
    />
  ),
};
