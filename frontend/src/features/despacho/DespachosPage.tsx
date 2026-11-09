import { Button, Text, TextInput } from '@mantine/core';
import { useForm } from '@mantine/form';
import { notifications } from '@mantine/notifications';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { zod4Resolver } from 'mantine-form-zod-resolver';
import { useState } from 'react';
import { useNavigate } from 'react-router';
import { z } from 'zod';
import { StatusBadge } from '@/components/domain/StatusBadge/StatusBadge';
import { PageHeader } from '@/components/layout/PageHeader/PageHeader';
import { Card } from '@/components/ui/Card/Card';
import { type Column } from '@/components/ui/DataTable/DataTable';
import { Icon } from '@/components/ui/Icon/Icon';
import { ConfirmDialog } from '@/components/ui/Modal/ConfirmDialog';
import { Modal } from '@/components/ui/Modal/Modal';
import { QueryTable } from '@/components/ui/QueryTable/QueryTable';
import { SearchInput } from '@/components/ui/SearchInput/SearchInput';
import { Tabs } from '@/components/ui/Tabs/Tabs';
import { formatDate, formatNumber } from '@/lib/format';
import { listarOrdenes, reservaCompleta } from './api';
import { generarOrden, reimprimirOrden, ventasParaOrden, type VentaParaOrden } from './ordenes/api';
import type { EstadoGuia, OrdenDespacho } from './types';

/** Pestañas del listado: cada una agrupa las órdenes de un estado de la guía. */
const PESTANAS: { value: string; label: string; estado: EstadoGuia }[] = [
  { value: 'pendientes', label: 'Pendientes de despacho', estado: 'Emitida' },
  { value: 'preparacion', label: 'En preparación', estado: 'Preparacion' },
  { value: 'confirmada', label: 'Salida confirmada', estado: 'SalidaConfirmada' },
  { value: 'entregadas', label: 'Entregadas', estado: 'Entregada' },
];

const schemaMotivo = z.object({
  motivo: z.string().trim().min(1, 'Indicar el motivo de la reimpresión.'),
});

const FORMULARIO_REIMPRESION = 'formulario-reimpresion';

const columnsOrdenes: Column<OrdenDespacho>[] = [
  { key: 'id', header: 'Orden', mono: true, primary: true },
  { key: 'venta', header: 'Venta', mono: true },
  { key: 'cliente', header: 'Cliente' },
  { key: 'entrega', header: 'Entrega', phone: false },
  {
    key: 'productos',
    header: 'Productos',
    numeric: true,
    render: (o) => (
      <>
        <span>{o.lineas.length === 1 ? '1 línea' : `${o.lineas.length} líneas`}</span>
        {!reservaCompleta(o.lineas) && (
          <Text component="span" display="block" size="sm" fw={600} c="warning">
            <Icon name="alert-triangle" size={16} /> Reserva incompleta
          </Text>
        )}
      </>
    ),
  },
  {
    key: 'estado',
    header: 'Estado',
    render: (o) => <StatusBadge entity="guia" value={o.estado} />,
  },
];

/** CU-06: órdenes de despacho por estado y ventas listas para generar su orden. */
export function DespachosPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [pestana, setPestana] = useState(PESTANAS[0]!.value);
  const [busqueda, setBusqueda] = useState('');
  /** Venta elegida para generar su orden o reimprimir la existente. */
  const [seleccion, setSeleccion] = useState<VentaParaOrden | null>(null);

  const ordenes = useQuery({ queryKey: ['ordenes'], queryFn: listarOrdenes });
  const ventas = useQuery({ queryKey: ['ventas-para-orden'], queryFn: ventasParaOrden });
  const formMotivo = useForm({
    initialValues: { motivo: '' },
    validate: zod4Resolver(schemaMotivo),
  });

  const todas = ordenes.data ?? [];
  const pestanaActiva = PESTANAS.find((p) => p.value === pestana) ?? PESTANAS[0]!;
  const termino = busqueda.trim().toLowerCase();
  const visibles = todas.filter(
    (o) =>
      o.estado === pestanaActiva.estado &&
      (!termino || [o.id, o.venta, o.cliente].some((t) => t.toLowerCase().includes(termino))),
  );

  const cerrarSeleccion = () => {
    setSeleccion(null);
    formMotivo.reset();
  };

  const invalidar = () => {
    void queryClient.invalidateQueries({ queryKey: ['ordenes'] });
    void queryClient.invalidateQueries({ queryKey: ['ventas-para-orden'] });
  };

  const generar = useMutation({
    mutationFn: (ventaId: string) => generarOrden(ventaId),
    onSuccess: (orden) => {
      cerrarSeleccion();
      invalidar();
      void navigate(`/despachos/${orden.id}`);
    },
    onError: (e) => {
      cerrarSeleccion();
      notifications.show({
        color: 'danger',
        title: 'No se pudo generar la orden',
        message: e.message,
      });
    },
  });

  const reimprimir = useMutation({
    mutationFn: (motivo: string) => reimprimirOrden(seleccion!.orden!.id, motivo),
    onSuccess: (orden) => {
      cerrarSeleccion();
      void navigate(`/despachos/${orden.id}/imprimir`);
    },
    onError: (e) => {
      notifications.show({
        color: 'danger',
        title: 'No se pudo reimprimir la orden',
        message: e.message,
      });
    },
  });

  const columnsVentas: Column<VentaParaOrden>[] = [
    { key: 'venta', header: 'Venta', mono: true, primary: true, render: (v) => v.venta.id },
    { key: 'cliente', header: 'Cliente', render: (v) => v.cliente },
    { key: 'fecha', header: 'Fecha', phone: false, render: (v) => formatDate(v.venta.fecha) },
    {
      key: 'orden',
      header: 'Orden',
      render: (v) => (v.orden ? `Con orden ${v.orden.id}` : 'Sin orden'),
    },
    {
      key: 'accion',
      header: 'Acción',
      render: (v) => (
        <Button
          size="sm"
          variant="default"
          leftSection={<Icon name="despacho" size={16} />}
          aria-label={`Generar orden de despacho para ${v.venta.id}`}
          onClick={() => setSeleccion(v)}
        >
          Generar orden de despacho
        </Button>
      ),
    },
  ];

  return (
    <>
      <PageHeader
        title="Despachos"
        description="Órdenes de despacho por estado y ventas listas para despachar."
      />
      <Tabs
        aria-label="Estado de los despachos"
        value={pestana}
        onChange={setPestana}
        items={PESTANAS.map((p) => ({
          value: p.value,
          label: p.label,
          count: todas.filter((o) => o.estado === p.estado).length,
        }))}
      />
      <div role="tabpanel" aria-label={pestanaActiva.label}>
        <SearchInput
          aria-label="Buscar por orden, venta o cliente"
          placeholder="Buscar por orden, venta o cliente"
          value={busqueda}
          onChange={setBusqueda}
          mb="md"
        />
        <QueryTable
          query={{ data: visibles, isLoading: ordenes.isLoading, isError: ordenes.isError }}
          errorTitle="No se pudieron cargar las órdenes"
          caption={`Órdenes de despacho: ${pestanaActiva.label.toLowerCase()}`}
          columns={columnsOrdenes}
          rowKey="id"
          onRowClick={(o) => void navigate(`/despachos/${o.id}`)}
        />
      </div>

      <Card title="Ventas listas para generar orden" padded={false}>
        <QueryTable
          query={{ data: ventas.data, isLoading: ventas.isLoading, isError: ventas.isError }}
          errorTitle="No se pudieron cargar las ventas"
          caption="Ventas listas para generar orden de despacho"
          columns={columnsVentas}
          rowKey={(v) => v.venta.id}
        />
      </Card>

      <ConfirmDialog
        open={!!seleccion && !seleccion.orden}
        title={`Generar la orden de despacho de la venta ${seleccion?.venta.id ?? ''}`}
        description={`Cliente: ${seleccion?.cliente ?? ''}.`}
        consequences={[
          `Dirección de entrega: ${seleccion?.direccion ?? ''}.`,
          ...(seleccion?.venta.lineas ?? []).map(
            (l) => `Productos: ${formatNumber(l.atendida)} x ${l.nombre}`,
          ),
        ]}
        confirmLabel="Generar orden de despacho"
        cancelLabel="Cancelar"
        loading={generar.isPending}
        onConfirm={() => seleccion && generar.mutate(seleccion.venta.id)}
        onCancel={cerrarSeleccion}
      />

      <Modal
        open={!!seleccion?.orden}
        title={`La venta ya tiene la orden ${seleccion?.orden?.id ?? ''}`}
        size="sm"
        onClose={cerrarSeleccion}
        persistent={reimprimir.isPending}
        footer={
          <>
            <Button variant="default" onClick={cerrarSeleccion} disabled={reimprimir.isPending}>
              Cancelar
            </Button>
            <Button type="submit" form={FORMULARIO_REIMPRESION} loading={reimprimir.isPending}>
              Reimprimir orden
            </Button>
          </>
        }
      >
        <form
          id={FORMULARIO_REIMPRESION}
          noValidate
          onSubmit={formMotivo.onSubmit((v) => reimprimir.mutate(v.motivo))}
        >
          <Text mb="md">Reimprimir exige una validación adicional: indicar el motivo.</Text>
          <TextInput
            label="Motivo de la reimpresión"
            required
            autoComplete="off"
            {...formMotivo.getInputProps('motivo')}
          />
        </form>
      </Modal>
    </>
  );
}
