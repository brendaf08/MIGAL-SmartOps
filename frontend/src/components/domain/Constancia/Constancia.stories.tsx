import type { Meta, StoryObj } from '@storybook/react-vite';
import { Button } from '@mantine/core';
import { StatusBadge } from '../StatusBadge/StatusBadge';
import { Constancia } from './Constancia';

const meta = {
  title: 'Dominio/Constancia',
  component: Constancia,
  args: {
    sello: <StatusBadge entity="recepcion" value="Validada" variant="sello" />,
    title: 'Recepción R-0013 registrada',
    description: 'Orden OC-0087, Distribuidora Andina S.A.C., Almacén 1.',
    cambios: [
      {
        producto: 'Cemento Sol tipo I',
        ubicacion: 'Almacén 1',
        antes: 1200,
        despues: 1300,
        unidad: 'bolsa',
      },
      {
        producto: 'Fierro corrugado 1/2 in',
        ubicacion: 'Almacén 1',
        antes: 310,
        despues: 350,
        unidad: 'varilla',
      },
    ],
    nota: 'Recepción parcial: quedan por recibir 20 unidades en 1 línea.',
    vibrate: false,
    actions: (
      <>
        <Button variant="default">Volver a recepciones</Button>
        <Button>Ver movimientos en el Kardex</Button>
      </>
    ),
  },
} satisfies Meta<typeof Constancia>;
export default meta;
type Story = StoryObj<typeof meta>;

export const Recepcion: Story = {};
export const SinNota: Story = { args: { nota: undefined } };
