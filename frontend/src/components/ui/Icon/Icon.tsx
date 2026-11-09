import type { SVGProps } from 'react';
import { ICONS, type IconName, type Shape } from './icons';

export interface IconProps extends Omit<SVGProps<SVGSVGElement>, 'name' | 'title'> {
  name: IconName;
  /** 16 en insignias, 20 en botones y campos, 24 en avisos. */
  size?: 16 | 20 | 24;
  /** Obligatorio solo si el ícono comunica algo sin texto adyacente; si falta, queda oculto a lectores de pantalla. */
  title?: string;
}

/** Ícono de trazo con color heredado (`currentColor`), dibujado a partir de `icons.ts`. */
export function Icon({ name, size = 20, title, ...props }: IconProps) {
  const a11y = title
    ? { role: 'img', 'aria-label': title }
    : { 'aria-hidden': true, focusable: false };

  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.75}
      strokeLinecap="round"
      strokeLinejoin="round"
      {...a11y}
      {...props}
    >
      {ICONS[name].map((shape: Shape, i) =>
        typeof shape === 'string' ? (
          <path key={i} d={shape} />
        ) : 'c' in shape ? (
          <circle key={i} cx={shape.c[0]} cy={shape.c[1]} r={shape.c[2]} />
        ) : (
          <rect
            key={i}
            x={shape.r[0]}
            y={shape.r[1]}
            width={shape.r[2]}
            height={shape.r[3]}
            rx={shape.r[4]}
          />
        ),
      )}
    </svg>
  );
}
