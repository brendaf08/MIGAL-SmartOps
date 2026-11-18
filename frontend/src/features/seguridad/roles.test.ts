import { describe, expect, it } from 'vitest';
import { rolDeApi } from './adaptadores';
import { PERMISOS, permite, type Accion } from './permisos';
import { ROL_DEMOSTRACION, ROLES, tieneRol } from './roles';

describe('rol de demostración (ADR-013)', () => {
  it('se reconoce desde el valor de la API', () => {
    expect(rolDeApi('demostracion')).toBe(ROL_DEMOSTRACION);
    expect(rolDeApi('vendedor')).toBe('Vendedor');
  });

  it('no figura entre los roles asignables del formulario de usuarios', () => {
    expect(ROLES).not.toContain(ROL_DEMOSTRACION);
  });

  it('tiene los permisos de cualquier rol y los demás solo los suyos', () => {
    expect(tieneRol(ROL_DEMOSTRACION, ['Reponedor'])).toBe(true);
    expect(tieneRol('Administrador', ['Reponedor'])).toBe(false);
    expect(tieneRol('Reponedor', ['Reponedor'])).toBe(true);
    expect(tieneRol(undefined, ['Reponedor'])).toBe(false);
  });

  it('puede ejecutar todas las acciones de la matriz de permisos', () => {
    for (const accion of Object.keys(PERMISOS) as Accion[]) {
      expect(permite(ROL_DEMOSTRACION, accion), accion).toBe(true);
    }
  });
});
