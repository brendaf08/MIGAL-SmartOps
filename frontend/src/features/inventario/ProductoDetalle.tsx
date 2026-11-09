import { Stack, Text } from '@mantine/core';
import { StockBar } from '@/components/domain/StockBar/StockBar';
import { StockLocations } from '@/components/domain/StockLocations/StockLocations';
import { StockSummary } from '@/components/domain/StockSummary/StockSummary';
import { Modal } from '@/components/ui/Modal/Modal';
import { resumen, ubicacionesDe } from './calculos';
import type { ProductoDisponibilidad } from './types';

interface ProductoDetalleProps {
  producto: ProductoDisponibilidad | null;
  onClose: () => void;
}

/** Detalle de un producto: existencias consolidadas y por ubicación (en teléfono la tabla las omite). */
export function ProductoDetalle({ producto, onClose }: ProductoDetalleProps) {
  const r = producto && resumen(producto);

  return (
    <Modal open={!!producto} title={producto?.nombre ?? ''} onClose={onClose}>
      {producto && r && (
        <Stack gap="md">
          <Text c="dimmed" ff="monospace">
            {producto.sku} · {producto.categoria}
          </Text>
          <StockSummary
            fisico={r.fisico}
            reservado={producto.reservado}
            minimo={producto.minimo}
            unidad={producto.unidad}
          />
          <StockBar
            libre={Math.max(r.libre, 0)}
            reservado={producto.reservado}
            unidad={producto.unidad}
            minimo={producto.minimo}
          />
          <StockLocations ubicaciones={ubicacionesDe(producto)} unidad={producto.unidad} />
        </Stack>
      )}
    </Modal>
  );
}
