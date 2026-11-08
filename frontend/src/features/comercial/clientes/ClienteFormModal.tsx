import { Button, Select, Stack, TextInput } from '@mantine/core';
import { useForm } from '@mantine/form';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { zod4Resolver } from 'mantine-form-zod-resolver';
import { useState } from 'react';
import { Alert } from '@/components/ui/Alert/Alert';
import { ConfirmDialog } from '@/components/ui/Modal/ConfirmDialog';
import { Modal } from '@/components/ui/Modal/Modal';
import {
  actualizarCliente,
  desactivarCliente,
  DocumentoDuplicadoError,
  registrarCliente,
  type ClienteDatos,
} from './api';
import { clienteSchema, type ClienteFormValues } from './esquema';
import type { Cliente } from './tipos';
import classes from './Clientes.module.css';

export interface ClienteFormModalProps {
  open: boolean;
  onClose: () => void;
  /** Cliente a modificar; si no se indica, el diálogo registra un cliente nuevo. */
  cliente?: Cliente | null;
  /** Se invoca con el cliente guardado, nuevo o modificado, después de actualizar la lista. */
  onGuardado?: (cliente: Cliente) => void;
  /** Se invoca con el cliente desactivado, tras confirmar la baja. */
  onDesactivado?: (cliente: Cliente) => void;
  /** Si se indica, un documento repetido ofrece seleccionar al cliente existente. */
  onSeleccionarExistente?: (cliente: Cliente) => void;
}

/** Valores iniciales del formulario: vacíos para un cliente nuevo; los del cliente, al modificarlo. */
const valoresIniciales = (cliente?: Cliente | null): ClienteFormValues => ({
  tipoDocumento: cliente?.tipoDocumento ?? 'DNI',
  documento: cliente?.documento ?? '',
  nombre: cliente?.nombre ?? '',
  telefono: cliente?.telefono ?? '',
  correo: cliente?.correo ?? '',
});

/**
 * CU-10. Diálogo para registrar o modificar un cliente. Con cliente existente y activo ofrece desactivarlo
 * tras una confirmación. Al cerrarse, el formulario se desmonta y se reinicia en la siguiente apertura.
 */
export function ClienteFormModal({
  open,
  onClose,
  cliente,
  onGuardado,
  onDesactivado,
  onSeleccionarExistente,
}: ClienteFormModalProps) {
  return (
    <Modal
      open={open}
      onClose={onClose}
      title={cliente ? 'Modificar cliente' : 'Registrar cliente'}
      size="md"
    >
      <ClienteFormulario
        cliente={cliente}
        onClose={onClose}
        onGuardado={onGuardado}
        onDesactivado={onDesactivado}
        onSeleccionarExistente={onSeleccionarExistente}
      />
    </Modal>
  );
}

type FormularioProps = Omit<ClienteFormModalProps, 'open'>;

function ClienteFormulario({
  cliente,
  onClose,
  onGuardado,
  onDesactivado,
  onSeleccionarExistente,
}: FormularioProps) {
  const queryClient = useQueryClient();
  const [duplicado, setDuplicado] = useState<Cliente | null>(null);
  const [confirmarBaja, setConfirmarBaja] = useState(false);
  const form = useForm<ClienteFormValues>({
    initialValues: valoresIniciales(cliente),
    validate: zod4Resolver(clienteSchema),
  });

  const refrescarClientes = () => queryClient.invalidateQueries({ queryKey: ['clientes'] });

  const guardar = useMutation({
    mutationFn: (valores: ClienteFormValues) => {
      const datos: ClienteDatos = {
        tipoDocumento: valores.tipoDocumento,
        documento: valores.documento.trim(),
        nombre: valores.nombre.trim(),
        telefono: valores.telefono.trim(),
        correo: valores.correo.trim(),
      };
      return cliente ? actualizarCliente(cliente.id, datos) : registrarCliente(datos);
    },
    onSuccess: async (guardado) => {
      await refrescarClientes();
      onGuardado?.(guardado);
      onClose();
    },
    onError: (error) => {
      if (error instanceof DocumentoDuplicadoError) {
        setDuplicado(error.cliente);
        form.setFieldError('documento', 'Ya existe un cliente con este documento.');
      }
    },
  });

  const desactivar = useMutation({
    mutationFn: (id: string) => desactivarCliente(id),
    onSuccess: async (baja) => {
      await refrescarClientes();
      setConfirmarBaja(false);
      onDesactivado?.(baja);
      onClose();
    },
  });

  const enviar = form.onSubmit((valores) => {
    setDuplicado(null);
    guardar.mutate(valores);
  });

  return (
    <form
      onSubmit={enviar}
      noValidate
      aria-label={cliente ? 'Modificar cliente' : 'Registrar cliente'}
    >
      <Stack gap="md">
        {duplicado && (
          <Alert tone="warning" title="Documento registrado">
            <Stack gap="xs" align="flex-start">
              <span>
                El documento pertenece a {duplicado.nombre}.
                {onSeleccionarExistente ? ' Seleccionar ese cliente en lugar de registrarlo.' : ''}
              </span>
              {onSeleccionarExistente && (
                <Button
                  type="button"
                  variant="light"
                  size="sm"
                  onClick={() => onSeleccionarExistente(duplicado)}
                >
                  Seleccionar cliente existente
                </Button>
              )}
            </Stack>
          </Alert>
        )}
        <Select
          label="Tipo de documento"
          data={['DNI', 'RUC']}
          allowDeselect={false}
          required
          {...form.getInputProps('tipoDocumento')}
        />
        <TextInput
          label="Documento"
          description="DNI de 8 dígitos o RUC de 11 dígitos"
          inputMode="numeric"
          autoComplete="off"
          maxLength={11}
          classNames={{ input: classes.mono }}
          required
          {...form.getInputProps('documento')}
        />
        <TextInput
          label="Nombre o razón social"
          autoComplete="off"
          required
          {...form.getInputProps('nombre')}
        />
        <TextInput
          label="Teléfono (opcional)"
          inputMode="tel"
          autoComplete="tel"
          maxLength={9}
          {...form.getInputProps('telefono')}
        />
        <TextInput
          label="Correo (opcional)"
          type="email"
          autoComplete="email"
          {...form.getInputProps('correo')}
        />
        <div className={classes.acciones}>
          {cliente?.estado === 'Activo' ? (
            <Button
              type="button"
              variant="subtle"
              color="danger"
              onClick={() => setConfirmarBaja(true)}
            >
              Desactivar cliente
            </Button>
          ) : (
            <span />
          )}
          <div className={classes.accionesPrincipales}>
            <Button type="button" variant="default" onClick={onClose}>
              Cancelar
            </Button>
            <Button type="submit" loading={guardar.isPending}>
              {cliente ? 'Guardar cambios' : 'Registrar cliente'}
            </Button>
          </div>
        </div>
      </Stack>
      {cliente && (
        <ConfirmDialog
          open={confirmarBaja}
          title={`Desactivar al cliente ${cliente.nombre}`}
          consequences={[
            'El cliente deja de ofrecerse al registrar ventas.',
            'Su historial de cotizaciones y ventas se conserva.',
          ]}
          confirmLabel="Desactivar cliente"
          cancelLabel="Volver"
          tone="danger"
          loading={desactivar.isPending}
          onConfirm={() => desactivar.mutate(cliente.id)}
          onCancel={() => setConfirmarBaja(false)}
        />
      )}
    </form>
  );
}
