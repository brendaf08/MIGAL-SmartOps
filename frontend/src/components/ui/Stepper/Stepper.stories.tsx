import type { Meta, StoryObj } from '@storybook/react-vite';
import { Stepper } from './Stepper';

const meta = {
  title: 'UI/Stepper',
  component: Stepper,
  args: {
    'aria-label': 'Pasos de la recepción',
    current: 1,
    steps: [
      { label: 'Cargar documento', description: 'Foto o PDF de la guía' },
      { label: 'Revisar datos', description: 'Corregir y asociar SKU' },
      { label: 'Confirmar recepción' },
    ],
  },
} satisfies Meta<typeof Stepper>;
export default meta;
type Story = StoryObj<typeof meta>;

export const PasoIntermedio: Story = {};
export const Inicio: Story = { args: { current: 0 } };
