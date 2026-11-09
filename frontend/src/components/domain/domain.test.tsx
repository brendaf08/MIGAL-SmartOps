import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { AppProviders } from '@/app/AppProviders';
import { MovementBadge } from './MovementBadge/MovementBadge';
import { StatusBadge } from './StatusBadge/StatusBadge';
import { stockLevel } from './StatusBadge/status';
import { StockBar } from './StockBar/StockBar';
import { StockSummary } from './StockSummary/StockSummary';

const renderUi = (ui: React.ReactElement) => render(<AppProviders>{ui}</AppProviders>);

describe('stockLevel', () => {
  it('clasifica el stock libre frente al mínimo', () => {
    expect(stockLevel(0, 10)).toBe('Critico');
    expect(stockLevel(10, 10)).toBe('Minimo');
    expect(stockLevel(11, 10)).toBe('Normal');
    expect(stockLevel(5)).toBe('Normal');
  });
});

describe('StatusBadge', () => {
  it('muestra el texto del mapa central y el valor original si es desconocido', () => {
    renderUi(
      <>
        <StatusBadge entity="venta" value="PendienteAbastecimiento" />
        <StatusBadge entity="venta" value="Inventado" />
      </>,
    );

    expect(screen.getByText('Pendiente de abastecimiento')).toBeInTheDocument();
    expect(screen.getByText('Inventado')).toBeInTheDocument();
  });
});

describe('StockSummary', () => {
  it('rotula las tres cantidades, calcula la libre y muestra el nivel', () => {
    renderUi(<StockSummary fisico={100} reservado={90} minimo={10} />);

    expect(screen.getByText('Libre').nextSibling).toHaveTextContent('10');
    expect(screen.getByText('En el mínimo')).toBeInTheDocument();
  });
});

describe('StockBar y MovementBadge', () => {
  it('resume el reparto de stock en el texto accesible', () => {
    renderUi(<StockBar libre={30} reservado={20} />);

    expect(screen.getByRole('img')).toHaveAccessibleName(
      'Distribución de stock: Libre 30, Reservado 20',
    );
  });

  it('antepone el signo según el tipo de movimiento', () => {
    renderUi(
      <>
        <MovementBadge tipo="Entrada" cantidad={5} />
        <MovementBadge tipo="Salida" cantidad={3} />
      </>,
    );

    expect(screen.getByText('+5')).toBeInTheDocument();
    expect(screen.getByText('−3')).toBeInTheDocument();
  });
});

describe('papeles tipográficos', () => {
  const css = (ruta: string) => readFileSync(resolve(__dirname, ruta), 'utf-8');
  const usaFuenteDeAcento = (fuente: string) => /font-family:\s*var\(--font-(display|heading)\)/.test(fuente);

  it('los datos corrientes no usan la fuente de cifras', () => {
    const fuentes = ['MatchConfidence/MatchConfidence.tsx', 'MovementBadge/MovementBadge.tsx'].map(css);
    for (const fuente of fuentes) expect(fuente).not.toMatch(/ff=.*font-(display|heading)/);
  });

  it('StockSummary en modo compacto (filas de tabla) vuelve a la fuente de texto', () => {
    const fuente = css('StockSummary/StockSummary.module.css');
    expect(usaFuenteDeAcento(fuente.slice(fuente.indexOf('.compact')))).toBe(false);
    expect(fuente.slice(fuente.indexOf('.compact'))).toContain('font-family: var(--font-sans)');
  });

  it('el título de página usa la fuente de encabezados y los demás encabezados la de texto', () => {
    const global = readFileSync(resolve(__dirname, '../../theme/global.css'), 'utf-8');
    const resolver = readFileSync(resolve(__dirname, '../../theme/cssVariablesResolver.ts'), 'utf-8');
    expect(resolver).toContain("'--mantine-font-family-headings': 'var(--font-sans)'");
    expect(global).toMatch(/h1,\s*\.mantine-Title-root\[data-order='1'\]\s*\{\s*font-family: var\(--font-heading\)/);
  });
});
