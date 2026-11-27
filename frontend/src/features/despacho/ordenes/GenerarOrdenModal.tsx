import { Button, NumberInput, Radio, Stack, Table, Text, TextInput } from '@mantine/core';
import { useQuery } from '@tanstack/react-query';
import { useMemo, useState } from 'react';
import { Alert } from '@/components/ui/Alert/Alert';
import { Modal } from '@/components/ui/Modal/Modal';
import { formatNumber } from '@/lib/format';
import {
  opcionesSalida,
  type DatosEmision,
  type OpcionProducto,
  type TipoEntrega,
  type VentaParaOrden,
} from './api';

interface GenerarOrdenModalProps {
  venta: VentaParaOrden | null;
  /** Rol que solo despacha ventas de mostrador (Vendedor): únicamente puede elegir el retiro en tienda. */
  soloRetiro: boolean;
  loading: boolean;
  onClose: () => void;
  onSubmit: (datos: DatosEmision) => void;
}

const clave = (productoId: string, ubicacionId: string) => `${productoId}:${ubicacionId}`;

/** Suma de lo asignado a un producto entre todas sus ubicaciones. */
const sumaDe = (p: OpcionProducto, cantidades: Record<string, number>) =>
  p.ubicaciones.reduce((s, u) => s + (cantidades[clave(p.productoId, u.ubicacionId)] ?? 0), 0);

/**
 * Genera la orden de despacho eligiendo cómo se entrega (envío a una dirección o retiro en tienda) y de qué
 * ubicaciones sale cada producto. La salida de Kardex se registra en las ubicaciones elegidas, de modo que siempre se
 * sabe de dónde se sacó la mercadería.
 */
export function GenerarOrdenModal(props: GenerarOrdenModalProps) {
  // Cada venta abre un formulario nuevo: sin arrastrar lo editado en la anterior.
  return <FormularioOrden key={props.venta?.venta.id ?? 'cerrado'} {...props} />;
}

function FormularioOrden({
  venta,
  soloRetiro,
  loading,
  onClose,
  onSubmit,
}: GenerarOrdenModalProps) {
  const abierta = venta !== null;
  // Lo que la persona cambió; mientras no cambie nada, valen el tipo sugerido, la dirección del cliente y las reservas.
  const [tipoElegido, setTipoElegido] = useState<TipoEntrega | null>(null);
  const [direccionEditada, setDireccionEditada] = useState<string | null>(null);
  const [ediciones, setEdiciones] = useState<Record<string, number>>({});

  const opciones = useQuery({
    queryKey: ['opciones-salida', venta?.venta.id],
    queryFn: () => opcionesSalida(venta!.venta.id),
    enabled: abierta,
    // Cada apertura debe ver el stock vigente: otra venta pudo reservar mientras tanto.
    staleTime: 0,
  });

  const tipo: TipoEntrega = soloRetiro
    ? 'retiro_tienda'
    : (tipoElegido ?? venta?.tipoEntregaSugerido ?? 'envio');
  const direccion =
    direccionEditada ?? (venta && venta.direccion !== 'Retiro en tienda' ? venta.direccion : '');
  const cantidades = useMemo(() => {
    const valores: Record<string, number> = {};
    for (const p of opciones.data ?? []) {
      for (const u of p.ubicaciones) {
        const k = clave(p.productoId, u.ubicacionId);
        valores[k] = ediciones[k] ?? u.asignada;
      }
    }
    return valores;
  }, [opciones.data, ediciones]);

  const problemas = useMemo(() => {
    const lista: string[] = [];
    if (tipo === 'envio' && direccion.trim() === '') lista.push('Indicar la dirección de entrega.');
    for (const p of opciones.data ?? []) {
      const suma = sumaDe(p, cantidades);
      if (suma !== p.cantidad) {
        lista.push(`${p.nombre}: asignaste ${formatNumber(suma)} de ${formatNumber(p.cantidad)}.`);
      }
      for (const u of p.ubicaciones) {
        if ((cantidades[clave(p.productoId, u.ubicacionId)] ?? 0) > u.disponible) {
          lista.push(
            `${p.nombre}: ${u.nombre} solo tiene ${formatNumber(u.disponible)} disponibles.`,
          );
        }
      }
    }
    return lista;
  }, [tipo, direccion, opciones.data, cantidades]);

  const confirmar = () => {
    onSubmit({
      tipoEntrega: tipo,
      direccion: tipo === 'envio' ? direccion.trim() : undefined,
      salidas: (opciones.data ?? []).map((p) => ({
        productoId: p.productoId,
        ubicaciones: p.ubicaciones
          .map((u) => ({
            ubicacionId: u.ubicacionId,
            cantidad: cantidades[clave(p.productoId, u.ubicacionId)] ?? 0,
          }))
          .filter((u) => u.cantidad > 0),
      })),
    });
  };

  return (
    <Modal
      open={abierta}
      title={`Generar la orden de despacho de la venta ${venta?.venta.id ?? ''}`}
      size="lg"
      onClose={onClose}
      persistent={loading}
      footer={
        <>
          <Button variant="default" onClick={onClose} disabled={loading}>
            Cancelar
          </Button>
          <Button
            onClick={confirmar}
            loading={loading}
            disabled={problemas.length > 0 || opciones.isPending}
          >
            Generar orden de despacho
          </Button>
        </>
      }
    >
      <Stack gap="md">
        <Text>Cliente: {venta?.cliente}.</Text>

        <Radio.Group
          label="¿Cómo se entrega?"
          value={tipo}
          onChange={(v) => setTipoElegido(v as TipoEntrega)}
          description={
            soloRetiro
              ? 'Como Vendedor puedes despachar solo ventas de mostrador (retiro en tienda).'
              : 'Un envío lleva la mercadería a una dirección; el retiro es una venta de mostrador.'
          }
        >
          <Stack gap={6} mt={6}>
            <Radio value="retiro_tienda" label="Retiro en tienda (venta de mostrador)" />
            <Radio value="envio" label="Envío a una dirección" disabled={soloRetiro} />
          </Stack>
        </Radio.Group>

        {tipo === 'envio' && (
          <TextInput
            label="Dirección de entrega"
            description="Por defecto la del cliente; puedes cambiarla para esta entrega."
            required
            maxLength={255}
            value={direccion}
            onChange={(e) => setDireccionEditada(e.currentTarget.value)}
          />
        )}

        <div>
          <Text fw={600} mb={4}>
            ¿De dónde sale cada producto?
          </Text>
          <Text size="sm" c="dimmed" mb="xs">
            Esto define en qué almacén o tienda se registra la salida en el Kardex. La suma por
            producto debe ser igual a lo vendido.
          </Text>
          {opciones.isError && (
            <Alert tone="danger" title="No se pudieron cargar las ubicaciones">
              Verificar la conexión e intentar de nuevo.
            </Alert>
          )}
          {(opciones.data ?? []).map((p) => {
            const suma = sumaDe(p, cantidades);
            const correcto = suma === p.cantidad;
            return (
              <div key={p.productoId} style={{ marginBottom: 'var(--mantine-spacing-sm)' }}>
                <Text size="sm" fw={600}>
                  {p.nombre}{' '}
                  <Text component="span" c="dimmed">
                    ({p.sku})
                  </Text>
                </Text>
                <Table withTableBorder withColumnBorders>
                  <Table.Thead>
                    <Table.Tr>
                      <Table.Th>Ubicación</Table.Th>
                      <Table.Th ta="right">Disponible</Table.Th>
                      <Table.Th ta="right">Sale de aquí</Table.Th>
                    </Table.Tr>
                  </Table.Thead>
                  <Table.Tbody>
                    {p.ubicaciones.map((u) => (
                      <Table.Tr key={u.ubicacionId}>
                        <Table.Td>{u.nombre}</Table.Td>
                        <Table.Td ta="right">{formatNumber(u.disponible)}</Table.Td>
                        <Table.Td>
                          <NumberInput
                            aria-label={`Cantidad de ${p.nombre} que sale de ${u.nombre}`}
                            min={0}
                            max={u.disponible}
                            allowDecimal={false}
                            allowNegative={false}
                            clampBehavior="strict"
                            size="xs"
                            value={cantidades[clave(p.productoId, u.ubicacionId)] || ''}
                            onChange={(v) =>
                              setEdiciones((c) => ({
                                ...c,
                                [clave(p.productoId, u.ubicacionId)]: Number(v) || 0,
                              }))
                            }
                          />
                        </Table.Td>
                      </Table.Tr>
                    ))}
                  </Table.Tbody>
                </Table>
                <Text size="xs" c={correcto ? 'success' : 'danger'} mt={2}>
                  {correcto
                    ? `Completo: ${formatNumber(p.cantidad)} de ${formatNumber(p.cantidad)}.`
                    : `Asignado ${formatNumber(suma)} de ${formatNumber(p.cantidad)}.`}
                </Text>
              </div>
            );
          })}
        </div>

        {problemas.length > 0 && !opciones.isPending && (
          <Alert tone="warning" title="Falta completar">
            {problemas[0]}
          </Alert>
        )}
      </Stack>
    </Modal>
  );
}
