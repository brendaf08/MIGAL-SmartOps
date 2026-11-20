import { notifications } from '@mantine/notifications';
import { PRODUCTOS } from '@/features/inventario/fixtures';

/** Avisa qué productos aparecían más de una vez y cuyas cantidades se sumaron en una sola línea. */
export function avisarRepetidos(skus: string[]) {
  if (skus.length === 0) return;
  const nombres = skus.map((sku) => PRODUCTOS.find((p) => p.sku === sku)?.nombre ?? sku);
  notifications.show({
    color: 'warning',
    title: 'Se sumaron productos repetidos',
    message: `${nombres.join('; ')}. Revisar las cantidades.`,
    autoClose: 10000,
  });
}
