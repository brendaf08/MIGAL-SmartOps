import { expect, test, type Page, type TestInfo } from '@playwright/test';
import { appendFile } from 'node:fs/promises';
import { join } from 'node:path';

const runId = process.env.E2E_RUN_ID ?? '';
const password = process.env.E2E_ADMIN_PASSWORD ?? '';
const apiStatuses = new WeakMap<Page, string[]>();
const usuario = (rol: 'admin' | 'v' | 'r' | 'a') =>
  rol === 'admin' ? (process.env.E2E_ADMIN_USER ?? '') : `e2e${runId}${rol}`;

test.beforeEach(async ({ page }) => {
  const statuses: string[] = [];
  apiStatuses.set(page, statuses);
  page.on('response', (response) => {
    const url = new URL(response.url());
    if (!url.pathname.startsWith('/api/v1/')) return;
    const ruta = url.pathname
      .replace(/\/\d+(?=\/|$)/g, '/:id')
      .replace(/\/[A-Z]{2,}-\d+(?=\/|$)/g, '/:codigo');
    statuses.push(`${response.request().method()} ${ruta} ${response.status()}`);
  });
});

test.afterEach(async ({ page }, info) => {
  const statuses = apiStatuses.get(page) ?? [];
  const reportDir = process.env.E2E_REPORT_DIR;
  if (reportDir) {
    const safeTitle = info.title.replace(/[\r\n|]/g, ' ');
    const content = `\n[${safeTitle}]\n${statuses.join('\n') || 'No se observaron respuestas API.'}\n`;
    await appendFile(join(reportDir, 'api-estados.txt'), content, 'utf8');
  }
});

async function iniciarSesion(page: Page, user: string) {
  await page.goto('/acceso');
  await page.getByLabel('Usuario').fill(user);
  await page.getByLabel(/^Contraseña/).fill(password);
  await page.getByRole('button', { name: 'Iniciar sesión' }).click();
  await page.waitForURL((url) => url.pathname !== '/acceso');
  await expect(page.getByRole('button', { name: 'Cerrar sesión' })).toBeVisible();
}

async function cerrarSesion(page: Page) {
  await page.getByRole('button', { name: 'Cerrar sesión' }).click();
  await expect(page.getByLabel('Usuario')).toBeVisible();
}

/** Existencias físicas de un SKU (tienda + almacenes) según la pantalla de disponibilidad. */
async function existenciasFisicas(page: Page, sku: string): Promise<number> {
  await page.goto('/disponibilidad');
  await expect(page.getByRole('heading', { name: 'Disponibilidad' })).toBeVisible();
  await page.getByLabel('Buscar producto').fill(sku);
  const tabla = page.getByRole('table', { name: 'Disponibilidad por producto' });
  const fila = tabla.getByRole('row').filter({ hasText: sku });
  await expect(fila).toHaveCount(1);
  const encabezados = (await tabla.getByRole('columnheader').allTextContents()).map((t) =>
    t.trim(),
  );
  const celdas = (await fila.locator('td, th').allTextContents()).map((t) => t.trim());
  const ubicaciones = ['Tienda', 'Almacén 1', 'Almacén 2'];
  return ubicaciones.reduce((total, ubicacion) => {
    const indice = encabezados.indexOf(ubicacion);
    expect(indice, `columna ${ubicacion}`).toBeGreaterThanOrEqual(0);
    return total + Number(celdas[indice].replace(/,/g, ''));
  }, 0);
}

async function evidenciaSegura(page: Page, info: TestInfo, nombre: string) {
  const encabezado = page.locator('h1').first();
  await expect(encabezado).toBeVisible();
  await info.attach(`${nombre}.png`, {
    body: await encabezado.screenshot(),
    contentType: 'image/png',
  });
}

test('login real y consultas principales de catálogo, inventario y abastecimiento', async ({
  page,
}, info) => {
  await iniciarSesion(page, usuario('admin'));
  await page.goto('/catalogo');
  await expect(page.getByRole('heading', { name: 'Catálogo' })).toBeVisible();
  await expect(page.getByRole('table', { name: 'Catálogo de productos' })).toBeVisible();
  await evidenciaSegura(page, info, 'consulta-catalogo');

  await page.goto('/disponibilidad');
  await expect(page.getByRole('heading', { name: 'Disponibilidad' })).toBeVisible();
  await expect(page.getByRole('table', { name: 'Disponibilidad por producto' })).toBeVisible();
  await evidenciaSegura(page, info, 'consulta-inventario');

  await page.goto('/compras');
  await expect(page.getByRole('heading', { name: 'Órdenes de compra' })).toBeVisible();
  await expect(page.getByRole('table', { name: /Órdenes de compra/ })).toBeVisible();
  await evidenciaSegura(page, info, 'consulta-abastecimiento');
});

test('SIMULADO PARCIAL: cotización → venta/reserva → despacho (selectores de producto con fixtures del frontend)', async ({
  page,
}, info) => {
  await iniciarSesion(page, usuario('v'));
  await page.goto('/cotizaciones/nueva');
  await page.getByRole('combobox', { name: 'Cliente' }).click();
  await page.getByRole('option', { name: /Constructora Andina/ }).click();
  await page.getByRole('button', { name: 'Agregar producto' }).click();
  await page.getByRole('combobox', { name: 'Producto de la línea 1' }).fill('CEM-001');
  await page.getByRole('option', { name: /CEM-001/ }).click();
  await page.getByRole('textbox', { name: 'Cantidad de la línea 1' }).fill('1');
  await page.getByRole('textbox', { name: 'Precio de la línea 1' }).fill('29.90');
  await page.getByRole('button', { name: 'Emitir cotización' }).click();
  await page.getByRole('dialog').getByRole('button', { name: 'Emitir cotización' }).click();
  await expect(page.getByRole('heading', { name: /^Cotización / })).toBeVisible();
  await page.getByRole('button', { name: 'Registrar aceptación' }).click();
  await page.getByRole('dialog').getByRole('button', { name: 'Registrar aceptación' }).click();
  const cotizacion = page.url().match(/cotizaciones\/(\d+)/)?.[1];
  expect(cotizacion).toBeTruthy();

  await cerrarSesion(page);
  await iniciarSesion(page, usuario('admin'));
  await page.goto(`/cotizaciones/${cotizacion}`);
  await page.getByRole('button', { name: 'Validar viabilidad' }).click();
  await page.getByRole('dialog').getByRole('button', { name: 'Validar' }).click();
  // Validada la viabilidad, la acción desaparece. Convertir en venta corresponde solo al Vendedor.
  await expect(page.getByRole('button', { name: 'Validar viabilidad' })).toBeHidden();

  // CU-09: el Administrador valida la viabilidad y el Vendedor registra la venta (registrarVentaDesdeCotizacion).
  await cerrarSesion(page);
  await iniciarSesion(page, usuario('v'));
  await page.goto(`/cotizaciones/${cotizacion}`);
  await page.getByRole('link', { name: 'Convertir en venta' }).click();
  await expect(page.getByRole('heading', { name: 'Registrar venta' })).toBeVisible();
  // El enlace de conversión abre el formulario con ?cotizacion=<id>; la pantalla debe precargarla desde la API.
  await expect(page.getByRole('combobox', { name: 'Cotización aceptada' })).toHaveValue(
    new RegExp(`^${cotizacion},`),
  );
  await page.getByRole('button', { name: 'Confirmar venta' }).click();
  await page.getByRole('dialog').getByRole('button', { name: 'Confirmar venta' }).click();
  await expect(page.getByRole('heading', { name: /Venta .* registrada/ })).toBeVisible();
  await evidenciaSegura(page, info, 'venta-registrada');
  await page.getByRole('link', { name: 'Ver la venta' }).click();
  await expect(page.getByText(/Pendiente de despacho|Pendiente despacho/i)).toBeVisible();
  const ventaId = page.url().match(/ventas\/(\d+)/)?.[1];
  expect(ventaId).toBeTruthy();

  await cerrarSesion(page);
  await iniciarSesion(page, usuario('a'));
  await page.goto('/despachos');
  await expect(page.getByRole('heading', { name: 'Despachos' })).toBeVisible();
  await page.getByRole('button', { name: `Generar orden de despacho para ${ventaId}` }).click();
  await page.getByRole('dialog').getByRole('button', { name: 'Generar orden de despacho' }).click();
  await expect(page.getByRole('heading', { name: /^Orden de despacho / })).toBeVisible();
  await evidenciaSegura(page, info, 'despacho-generado');
  await page.getByRole('button', { name: 'Confirmar salida' }).click();
  await page.getByRole('dialog').getByRole('button', { name: 'Confirmar salida' }).click();
  await expect(
    page.getByRole('heading', { name: /Salida de la orden .* confirmada/ }),
  ).toBeVisible();
  await evidenciaSegura(page, info, 'despacho-confirmado');
});

test('SIMULADO PARCIAL: compra → recepción → inventario (selector de producto con fixtures del frontend)', async ({
  page,
}, info) => {
  await iniciarSesion(page, usuario('r'));
  await page.goto('/compras/nueva');
  await page.getByRole('combobox', { name: 'Proveedor' }).click();
  await page.getByRole('option', { name: /Distribuidora Cementera del Sur/ }).click();
  await page.getByRole('button', { name: 'Agregar producto' }).click();
  await page.getByRole('combobox', { name: 'Producto de la línea 1' }).fill('CEM-001');
  await page.getByRole('option', { name: /CEM-001/ }).click();
  await page.getByRole('textbox', { name: 'Cantidad de la línea 1' }).fill('2');
  await page.getByRole('textbox', { name: 'Precio de la línea 1' }).fill('26.50');
  await page.getByRole('button', { name: 'Guardar orden de compra' }).click();
  await page.getByRole('dialog').getByRole('button', { name: 'Crear orden de compra' }).click();
  await expect(page.getByRole('heading', { name: /^Orden de compra / })).toBeVisible();
  const ordenId = page.url().match(/compras\/([^/?]+)/)?.[1];
  expect(ordenId).toBeTruthy();
  await evidenciaSegura(page, info, 'orden-compra-pendiente');

  await cerrarSesion(page);
  await iniciarSesion(page, usuario('a'));
  const existenciasAntes = await existenciasFisicas(page, 'CEM-001');
  const recepcionables = page.waitForResponse((response) => {
    const url = new URL(response.url());
    return (
      response.request().method() === 'GET' &&
      url.pathname === '/api/v1/ordenes-compra/recepcionables'
    );
  });
  await page.goto('/recepciones/nueva');
  expect((await recepcionables).status()).toBe(200);
  await page.getByRole('button', { name: `Elegir orden ${ordenId}` }).click();
  await page.getByRole('button', { name: 'Continuar' }).click();
  await page.getByRole('button', { name: 'Registrar sin documento' }).click();
  await expect(page.getByRole('heading', { name: 'Nueva recepción' })).toBeVisible();
  // CU-13: el Almacenero registra el conteo físico; sin cantidades la confirmación queda bloqueada.
  const confirmar = page.getByRole('button', { name: 'Confirmar recepción' });
  await expect(confirmar).toBeDisabled();
  await page.getByRole('textbox', { name: /^Cantidad recibida de / }).fill('2');
  await expect(confirmar).toBeEnabled();
  await confirmar.click();
  await page.getByRole('dialog').getByRole('button', { name: 'Confirmar recepción' }).click();
  await expect(page.getByRole('heading', { name: /Recepción .* registrada/ })).toBeVisible();
  await evidenciaSegura(page, info, 'recepcion-validada');

  expect(await existenciasFisicas(page, 'CEM-001')).toBe(existenciasAntes + 2);
  await evidenciaSegura(page, info, 'inventario-actualizado');
});

test('SIMULADO: OCR y agente consultivo con API/backend real y proveedores simulados', async ({
  page,
}, info) => {
  await iniciarSesion(page, usuario('v'));
  await page.goto('/documentos/procesar?destino=cotizacion');
  await page.getByRole('radio', { name: 'Cotización de cliente' }).locator('..').click();
  await page.locator('input[type="file"]:not([capture])').setInputFiles({
    name: `e2e-${runId}.jpg`,
    mimeType: 'image/jpeg',
    buffer: Buffer.from([0xff, 0xd8, 0xff, 0xd9]),
  });
  await expect(page.getByRole('heading', { name: 'Revisión del OCR' })).toBeVisible({
    timeout: 15_000,
  });
  await expect(page.getByText(/LINEA SIMULADA/)).toBeVisible();
  await evidenciaSegura(page, info, 'simulado-revision-ocr');

  await page.goto('/agente-ia');
  await expect(page.getByRole('heading', { name: 'Agente de IA' })).toBeVisible();
  await page.getByLabel('Consulta al agente').fill('Productos en stock crítico');
  await page.getByRole('button', { name: 'Enviar consulta' }).click();
  await expect(page.getByRole('log', { name: 'Conversación con el agente' })).not.toContainText(
    'Revisando existencias…',
    { timeout: 15_000 },
  );
  await expect(page.getByRole('log', { name: 'Conversación con el agente' })).toContainText(
    /stock|alerta|producto/i,
  );
  await evidenciaSegura(page, info, 'simulado-agente');
});
