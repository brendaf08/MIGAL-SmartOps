import { describe, expect, it } from 'vitest';
import { siguienteCodigo } from './codigos';

describe('siguienteCodigo', () => {
  it('suma uno al mayor código con el mismo prefijo', () => {
    expect(siguienteCodigo('OC', ['OC-0087', 'OC-0090', 'OC-0084'])).toBe('OC-0091');
  });

  it('empieza en 1 cuando no hay códigos previos', () => {
    expect(siguienteCodigo('V', [])).toBe('V-0001');
  });

  it('ignora los códigos de otro prefijo o mal formados', () => {
    expect(siguienteCodigo('OC', ['COT-0231', 'OC-abc', 'OC-0005'])).toBe('OC-0006');
  });

  it('respeta el ancho pedido', () => {
    expect(siguienteCodigo('CLI', ['CLI-008'], 3)).toBe('CLI-009');
  });
});
