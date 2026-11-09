import { Button, Select } from '@mantine/core';
import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { Icon } from '@/components/ui/Icon/Icon';
import { ClienteFormModal } from './ClienteFormModal';
import { listarClientes } from './api';
import type { Cliente } from './tipos';
import classes from './Clientes.module.css';

/** Valor del selector que indica una venta sin cliente identificado. */
export const SIN_CLIENTE = 'sin-cliente';

export interface ClientePickerProps {
  /** Código del cliente elegido, `SIN_CLIENTE` o `null` si aún no se elige. */
  value: string | null;
  onChange: (value: string | null) => void;
  error?: string;
}

const etiqueta = (c: Cliente) =>
  c.estado === 'Activo'
    ? `${c.nombre} (${c.documento})`
    : `${c.nombre} (${c.documento}), inactivo: no se puede seleccionar`;

/**
 * Selector de cliente para cotizaciones y ventas. Busca por nombre o documento, ofrece continuar sin cliente
 * y permite registrar un cliente nuevo, que queda seleccionado al guardarse. Los inactivos se muestran
 * deshabilitados con la causa.
 */
export function ClientePicker({ value, onChange, error }: ClientePickerProps) {
  const query = useQuery({ queryKey: ['clientes'], queryFn: listarClientes });
  const [registrando, setRegistrando] = useState(false);

  const data = [
    { value: SIN_CLIENTE, label: 'Continuar sin cliente' },
    ...(query.data ?? []).map((c) => ({
      value: c.id,
      label: etiqueta(c),
      disabled: c.estado === 'Inactivo',
    })),
  ];

  const seleccionar = (id: string) => {
    onChange(id);
    setRegistrando(false);
  };

  return (
    <div className={classes.picker}>
      <div className={classes.pickerCampo}>
        <Select
          label="Cliente"
          placeholder="Buscar por nombre o documento"
          searchable
          clearable
          nothingFoundMessage="Ningún cliente coincide con la búsqueda"
          data={data}
          value={value}
          onChange={onChange}
          disabled={query.isLoading}
          error={query.isError ? 'No se pudieron cargar los clientes.' : error}
        />
      </div>
      <Button
        variant="default"
        leftSection={<Icon name="plus" size={16} />}
        onClick={() => setRegistrando(true)}
      >
        Registrar cliente nuevo
      </Button>
      <ClienteFormModal
        open={registrando}
        onClose={() => setRegistrando(false)}
        onGuardado={(cliente) => seleccionar(cliente.id)}
        onSeleccionarExistente={(cliente) => seleccionar(cliente.id)}
      />
    </div>
  );
}
