import { request, type FullConfig } from '@playwright/test';

export default async function globalSetup(_config: FullConfig) {
  const baseURL = process.env.E2E_BACKEND_URL;
  const runId = process.env.E2E_RUN_ID;
  const admin = process.env.E2E_ADMIN_USER;
  const password = process.env.E2E_ADMIN_PASSWORD;
  if (!baseURL || !runId || !admin || !password) {
    throw new Error('Falta la configuración local aislada para Playwright.');
  }

  const api = await request.newContext({ baseURL });
  try {
    const login = await api.post('/api/v1/sesiones', {
      data: { username: admin, clave: password },
    });
    if (!login.ok())
      throw new Error(`No se pudo iniciar la sesión de preparación (${login.status()}).`);
    const { token } = (await login.json()) as { token: string };
    const usuarios = [
      { sufijo: 'v', rol: 'vendedor', nombres: 'E2E Vendedor' },
      { sufijo: 'r', rol: 'reponedor', nombres: 'E2E Reponedor' },
      { sufijo: 'a', rol: 'almacenero', nombres: 'E2E Almacenero' },
    ];
    for (const usuario of usuarios) {
      const creado = await api.post('/api/v1/usuarios', {
        headers: { Authorization: `Bearer ${token}` },
        data: {
          nombres: usuario.nombres,
          apellidos: 'Local',
          username: `e2e${runId}${usuario.sufijo}`,
          claveTemporal: password,
          rol: usuario.rol,
        },
      });
      if (!creado.ok()) {
        throw new Error(`No se pudo preparar un usuario de prueba (${creado.status()}).`);
      }
    }
  } finally {
    await api.dispose();
  }
}
