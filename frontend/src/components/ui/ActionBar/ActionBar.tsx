import type { ReactNode } from 'react';
import { Icon } from '../Icon/Icon';
import classes from './ActionBar.module.css';

export interface ActionBarProps {
  /** Resumen de lo que se va a confirmar, normalmente insignias con el recuento por estado. */
  summary?: ReactNode;
  /** Motivo por el que la acción principal no está disponible. Se anuncia con `role="status"`. */
  reason?: string | null;
  /** Etiqueta accesible de la región. */
  'aria-label'?: string;
  /** Acciones: cancelar a la izquierda y la principal a la derecha. */
  children: ReactNode;
}

/**
 * Barra fija al pie de un flujo de confirmación (recepción, despacho, venta). Reúne el resumen por estado,
 * el motivo del bloqueo y las acciones; la acción principal solo se deshabilita junto con una explicación visible.
 */
export function ActionBar({
  summary,
  reason,
  'aria-label': ariaLabel = 'Resumen y acciones',
  children,
}: ActionBarProps) {
  return (
    <div className={classes.root} role="region" aria-label={ariaLabel}>
      <div className={classes.info}>
        {summary && <div className={classes.summary}>{summary}</div>}
        {reason && (
          <p className={classes.reason} role="status">
            <Icon name="alert-triangle" size={16} />
            <span>{reason}</span>
          </p>
        )}
      </div>
      <div className={classes.actions}>{children}</div>
    </div>
  );
}
