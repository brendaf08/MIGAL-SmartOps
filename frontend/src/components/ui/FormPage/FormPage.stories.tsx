import type { Meta, StoryObj } from '@storybook/react-vite';
import { TextInput } from '@mantine/core';
import { Card } from '../Card/Card';
import { FormPage } from './FormPage';

const meta = {
  title: 'UI/FormPage',
  component: FormPage,
  parameters: { layout: 'fullscreen' },
  args: {
    title: 'Registrar usuario',
    description: 'Completar los datos personales y el rol inicial.',
    breadcrumbs: [{ label: 'Usuarios', href: '/usuarios' }, { label: 'Nuevo' }],
    submitLabel: 'Registrar usuario',
    onSubmit: (e) => e.preventDefault(),
    onCancel: () => undefined,
    children: (
      <Card title="Datos personales">
        <TextInput label="Nombre completo" />
      </Card>
    ),
  },
} satisfies Meta<typeof FormPage>;
export default meta;
type Story = StoryObj<typeof meta>;

export const Basico: Story = {};
export const ConBloqueo: Story = {
  args: { canSubmit: false, reason: 'Falta completar el nombre completo.' },
};
export const Enviando: Story = { args: { submitting: true } };
