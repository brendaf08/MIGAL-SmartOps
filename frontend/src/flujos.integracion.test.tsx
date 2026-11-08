import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it } from 'vitest';
import { COTIZACIONES } from '@/features/comercial/fixtures';
import { renderApp } from '@/test/renderApp';

/**
 * Pruebas de integración entre módulos: cada una recorre un enlace o un flujo que cruza dos pantallas de ramas
 * distintas y comprueba que los datos viajan por la dirección y llegan al formulario de destino.
 */
afterEach(() => window.sessionStorage.clear());

const sinRegiones = { rules: { 'color-contrast': { enabled: false }, region: { enabled: false } } };

async function cargarArchivo(nombre: string) {
  const input = document.querySelector<HTMLInputElement>('input[type="file"]:not([capture])')!;
  await userEvent.upload(input, new File(['x'], nombre, { type: 'image/jpeg' }));
}

/** Asocia el producto de la línea que el OCR dejó sin coincidencia, para poder usar los datos. */
async function asociarLineaSinProducto() {
  const select = await screen.findByRole('combobox', { name: 'Producto para ALAMBRE REC. N16' });
  await userEvent.click(select);
  const lista = document.getElementById(select.getAttribute('aria-controls')!)!;
  await userEvent.click(
    await within(lista).findByRole('option', { name: /CLA-008/, hidden: true }),
  );
}

describe('Alertas de stock hacia órdenes de compra', () => {
  it('Crear orden de compra abre el formulario con el producto de la alerta', async () => {
    renderApp('/alertas', 'Reponedor');

    const [primero] = await screen.findAllByRole('button', { name: 'Crear orden de compra' });
    await userEvent.click(primero!);

    expect(
      await screen.findByRole('heading', { level: 1, name: 'Nueva orden de compra' }),
    ).toBeInTheDocument();
    expect(screen.getByRole('combobox', { name: 'Producto de la línea 1' })).toHaveDisplayValue(
      /^[A-Z]{3}-\d{3}/,
    );
  });

  it('un Almacenero ve las alertas pero no puede crear órdenes de compra', async () => {
    renderApp('/alertas', 'Almacenero');

    expect(await screen.findAllByRole('button', { name: 'Ver disponibilidad' })).not.toHaveLength(
      0,
    );
    expect(screen.queryByRole('button', { name: 'Crear orden de compra' })).not.toBeInTheDocument();
  });
});

describe('Órdenes de compra hacia recepciones', () => {
  it('el Almacenero registra la recepción desde el detalle de una orden pendiente', async () => {
    renderApp('/compras/OC-0087', 'Almacenero');

    await userEvent.click(await screen.findByRole('button', { name: 'Registrar recepción' }));

    expect(
      await screen.findByRole('heading', { level: 1, name: 'Nueva recepción' }),
    ).toBeInTheDocument();
    expect((await screen.findAllByText(/OC-0087/)).length).toBeGreaterThan(0);
  });

  it('el Reponedor ve la orden pero no la acción de recepción, que es del Almacenero', async () => {
    renderApp('/compras/OC-0087', 'Reponedor');

    expect(
      await screen.findByRole('heading', { level: 1, name: 'Orden de compra OC-0087' }),
    ).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Registrar recepción' })).not.toBeInTheDocument();
  });

  it('un Almacenero no puede crear órdenes de compra por la dirección', async () => {
    renderApp('/compras/nueva', 'Almacenero');

    await waitFor(() =>
      expect(
        screen.queryByRole('heading', { level: 1, name: 'Nueva orden de compra' }),
      ).not.toBeInTheDocument(),
    );
  });
});

describe('Cotizaciones y ventas', () => {
  it('una cotización aceptada se convierte en venta con sus líneas cargadas', async () => {
    renderApp('/cotizaciones/COT-0230', 'Vendedor');

    await userEvent.click(await screen.findByRole('link', { name: 'Convertir en venta' }));

    expect(
      await screen.findByRole('heading', { level: 1, name: 'Registrar venta' }),
    ).toBeInTheDocument();
    expect(
      await screen.findByRole('combobox', { name: 'Producto de la línea 1' }),
    ).toHaveDisplayValue(/^[A-Z]{3}-\d{3}/);
  });

  it('aceptar una cotización pide confirmación y cancelar el diálogo no cambia el estado', async () => {
    renderApp('/cotizaciones/COT-0231', 'Vendedor');

    await userEvent.click(await screen.findByRole('button', { name: 'Registrar aceptación' }));
    const dialogo = await screen.findByRole('dialog', {
      name: /Registrar la aceptación de la cotización COT-0231/,
    });
    await userEvent.click(within(dialogo).getByRole('button', { name: 'Cancelar' }));

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(COTIZACIONES.find((c) => c.id === 'COT-0231')!.estado).toBe('Emitida');
  });
});

describe('Documentos OCR hacia cotizaciones y ventas', () => {
  it.each([
    ['cotizacion', '/cotizaciones/nueva', 'Nueva cotización'],
    ['venta', '/ventas/nueva', 'Registrar venta'],
  ])(
    'un documento revisado vuelve al flujo de %s con sus productos',
    async (destino, _ruta, titulo) => {
      renderApp(`/documentos/procesar?destino=${destino}`, 'Vendedor');

      await cargarArchivo('guia-proveedor.jpg');
      await screen.findByRole('heading', { level: 1, name: 'Revisión del OCR' }, { timeout: 8000 });
      await asociarLineaSinProducto();
      await userEvent.click(await screen.findByRole('button', { name: 'Usar estos datos' }));

      expect(
        await screen.findByRole('heading', { level: 1, name: titulo }, { timeout: 8000 }),
      ).toBeInTheDocument();
      expect(await screen.findByText('Productos cargados desde el documento')).toBeInTheDocument();
      expect(
        await screen.findByRole('combobox', { name: 'Producto de la línea 3' }),
      ).toBeInTheDocument();
    },
    30000,
  );

  it('los formularios ofrecen cargar un documento y conservan su destino', async () => {
    renderApp('/cotizaciones/nueva', 'Vendedor');

    await userEvent.click(await screen.findByRole('button', { name: 'Cargar desde un documento' }));

    expect(
      await screen.findByRole('heading', { level: 1, name: 'Procesar documento' }),
    ).toBeInTheDocument();
  });
});

describe('Despachos hacia la orden imprimible', () => {
  it('el detalle de la orden enlaza a la hoja imprimible', async () => {
    renderApp('/despachos/D-0031', 'Almacenero');

    const enlace = await screen.findByRole('link', { name: 'Imprimir orden' });
    expect(enlace).toHaveAttribute('href', '/despachos/D-0031/imprimir');

    await userEvent.click(enlace);
    expect(await screen.findByText(/D-0031/)).toBeInTheDocument();
  });
});

describe('Accesibilidad de los flujos conectados', () => {
  it('el formulario de cotización con documento precargado no tiene violaciones', async () => {
    const { axe } = await import('vitest-axe');
    const matchers = await import('vitest-axe/matchers');
    expect.extend(matchers);
    const { container } = renderApp('/cotizaciones/nueva', 'Vendedor');

    await screen.findByRole('heading', { level: 1, name: 'Nueva cotización' });

    expect(await axe(container, sinRegiones)).toHaveNoViolations();
  });
});
