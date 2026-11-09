import { Text, Title } from '@mantine/core';
import { useEffect, type ReactNode } from 'react';
import { formatNumber } from '@/lib/format';
import classes from './Constancia.module.css';

export interface CambioStock {
  producto: string;
  ubicacion: string;
  antes: number;
  despues: number;
  unidad: string;
}

export interface ConstanciaProps {
  /** Normalmente un `StatusBadge` con `variant="sello"`. */
  sello: ReactNode;
  title: string;
  description?: string;
  cambios: CambioStock[];
  /** Lo que queda pendiente o el siguiente paso. */
  nota?: string;
  /** Una acción principal (ver el movimiento en el Kardex) y una secundaria para continuar. */
  actions: ReactNode;
  /** Confirmación táctil de 30 ms en dispositivos que la admiten. */
  vibrate?: boolean;
}

const prefiereMenosMovimiento = () =>
  typeof window.matchMedia === 'function' &&
  window.matchMedia('(prefers-reduced-motion: reduce)').matches;

/**
 * Constancia que cierra una operación de inventario: responde "¿qué cambió?" con el sello, la existencia
 * antes y después, lo pendiente y el siguiente paso. Se monta como resultado de confirmar, nunca al cargar.
 */
export function Constancia({
  sello,
  title,
  description,
  cambios,
  nota,
  actions,
  vibrate = true,
}: ConstanciaProps) {
  useEffect(() => {
    if (vibrate && typeof navigator.vibrate === 'function' && !prefiereMenosMovimiento()) {
      navigator.vibrate(30);
    }
  }, [vibrate]);

  return (
    <section className={classes.root} role="status" aria-label={title}>
      <div className={classes.sello}>{sello}</div>
      <Title order={2} className={classes.title}>
        {title}
      </Title>
      {description && (
        <Text c="dimmed" size="sm">
          {description}
        </Text>
      )}
      <table className={classes.table}>
        <caption>Existencia antes y después</caption>
        <thead>
          <tr>
            <th scope="col">Producto</th>
            <th scope="col" className={classes.numeric}>
              Antes
            </th>
            <th scope="col" className={classes.numeric}>
              Después
            </th>
          </tr>
        </thead>
        <tbody>
          {cambios.map((c) => (
            <tr key={`${c.producto}-${c.ubicacion}`}>
              <th scope="row">
                <span>{c.producto}</span>
                <small>{c.ubicacion}</small>
              </th>
              <td className={classes.numeric}>
                {formatNumber(c.antes)} <small>{c.unidad}</small>
              </td>
              <td className={`${classes.numeric} ${classes.after}`}>
                {formatNumber(c.despues)} <small>{c.unidad}</small>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      {nota && <p className={classes.note}>{nota}</p>}
      <div className={classes.actions}>{actions}</div>
    </section>
  );
}
