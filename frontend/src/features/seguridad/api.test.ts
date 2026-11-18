import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import {
  activarApiReal,
  cargarModulo,
  problema,
  restablecerApiReal,
  simularServidor,
} from '@/test/servidorApi';

beforeEach(activarApiReal);
afterEach(restablecerApiReal);

describe('inicio de sesión (rama de la API real)', () => {
  it('informa la cuenta desactivada con el detalle del backend', async () => {
    simularServidor({
      'POST /api/v1/sesiones': () =>
        problema(403, 'CUENTA_INACTIVA', 'La cuenta está desactivada.'),
    });
    const { iniciarSesion } = await cargarModulo(() => import('./api'));

    await expect(iniciarSesion('rquispe', 'Clave1234')).rejects.toThrow(
      'La cuenta está desactivada.',
    );
  });

  it('informa el bloqueo por intentos fallidos', async () => {
    simularServidor({
      'POST /api/v1/sesiones': () => problema(429, 'DEMASIADOS_INTENTOS', 'Esperar 15 minutos.'),
    });
    const { iniciarSesion } = await cargarModulo(() => import('./api'));

    await expect(iniciarSesion('rquispe', 'Clave1234')).rejects.toThrow('Esperar 15 minutos.');
  });

  it('no confunde un servidor caído con una clave incorrecta', async () => {
    simularServidor({
      'POST /api/v1/sesiones': () => problema(503, 'SERVICIO_NO_DISPONIBLE', 'Caído.'),
    });
    const { iniciarSesion } = await cargarModulo(() => import('./api'));

    await expect(iniciarSesion('rquispe', 'Clave1234')).rejects.toThrow(
      /No fue posible comunicarse/,
    );
  });

  it('cambia la clave con PUT /sesiones/actual/clave', async () => {
    const llamadas = simularServidor({
      'PUT /api/v1/sesiones/actual/clave': () => new Response(null, { status: 204 }),
    });
    const { cambiarClave } = await cargarModulo(() => import('./api'));

    await cambiarClave('Clave1234', 'Nueva2026');

    expect(llamadas[0]!.cuerpo).toEqual({ claveActual: 'Clave1234', claveNueva: 'Nueva2026' });
  });
});
