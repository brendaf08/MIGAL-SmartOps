import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { axe } from 'vitest-axe';
import * as matchers from 'vitest-axe/matchers';
import { afterEach, describe, expect, it } from 'vitest';
import { renderApp } from '@/test/renderApp';

expect.extend(matchers);
afterEach(() => window.sessionStorage.clear());

/** Sube un archivo en el campo de carga que no es de cámara. */
async function cargar(nombre: string) {
  const input = document.querySelector<HTMLInputElement>('input[type="file"]:not([capture])')!;
  await userEvent.upload(input, new File(['x'], nombre, { type: 'image/jpeg' }));
}

describe('Documentos OCR', () => {
  it('la lista filtra los documentos por estado', async () => {
    renderApp('/documentos', 'Almacenero');

    expect(
      await screen.findByRole('heading', { level: 1, name: 'Documentos OCR' }),
    ).toBeInTheDocument();
    expect(await screen.findAllByText('guia-cemento-05.jpg')).not.toHaveLength(0);

    await userEvent.click(screen.getByRole('tab', { name: /Procesado por OCR/ }));
    expect(await screen.findAllByText('guia-proveedor.jpg')).not.toHaveLength(0);
    expect(screen.queryAllByText('guia-cemento-05.jpg')).toHaveLength(0);

    await userEvent.click(screen.getByRole('tab', { name: /Validado/ }));
    expect(await screen.findAllByText('pedido-obra-sur.png')).not.toHaveLength(0);
    expect(screen.queryAllByText('guia-proveedor.jpg')).toHaveLength(0);
  });

  it('una carga válida procesa el documento y lleva a su revisión', async () => {
    renderApp('/documentos/procesar', 'Almacenero');

    await cargar('guia-proveedor.jpg');

    expect(
      await screen.findByRole('heading', { level: 1, name: 'Revisión del OCR' }),
    ).toBeInTheDocument();
    expect(
      await screen.findByRole('table', { name: 'Líneas extraídas del documento' }),
    ).toBeInTheDocument();
  });

  it('una carga ilegible ofrece cargar otra foto o registrar manualmente', async () => {
    renderApp('/documentos/procesar', 'Vendedor');

    await cargar('foto-ilegible.jpg');
    expect(await screen.findByText('Documento ilegible')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Cargar otra foto' })).toBeEnabled();
    expect(screen.getByRole('button', { name: 'Registrar manualmente' })).toBeEnabled();

    await userEvent.click(screen.getByRole('button', { name: 'Cargar otra foto' }));
    await waitFor(() => expect(screen.queryByText('Documento ilegible')).not.toBeInTheDocument());

    await cargar('otra-ilegible.jpg');
    await userEvent.click(await screen.findByRole('button', { name: 'Registrar manualmente' }));
    expect(
      await screen.findByRole('heading', { level: 1, name: 'Documentos OCR' }),
    ).toBeInTheDocument();
  });

  it('la revisión bloquea el uso de los datos hasta asociar las líneas sin producto', async () => {
    renderApp('/documentos/5/revision?destino=recepcion', 'Almacenero');

    const barra = await screen.findByRole('region', { name: 'Resumen y acciones del documento' });
    expect(await within(barra).findByRole('status')).toHaveTextContent(
      'Falta asociar 1 línea a un producto del catálogo.',
    );
    expect(within(barra).getByRole('button', { name: 'Usar estos datos' })).toBeDisabled();

    const select = screen.getByRole('combobox', { name: 'Producto para ALAMBRE REC. N16' });
    await userEvent.click(select);
    const lista = document.getElementById(select.getAttribute('aria-controls')!)!;
    await userEvent.click(
      await within(lista).findByRole('option', { name: /CLA-008/, hidden: true }),
    );

    expect(within(barra).queryByRole('status')).not.toBeInTheDocument();
    await userEvent.click(within(barra).getByRole('button', { name: 'Usar estos datos' }));
    // El destino de la recepción conserva el documento validado.
    expect(
      await screen.findByRole('heading', { level: 1, name: 'Nueva recepción' }),
    ).toBeInTheDocument();
  });

  it('descartar el documento pide confirmación antes de quitarlo', async () => {
    renderApp('/documentos/4/revision', 'Vendedor');

    await userEvent.click(await screen.findByRole('button', { name: 'Descartar documento' }));
    const dialogo = await screen.findByRole('dialog', {
      name: 'Descartar documento guia-proveedor.jpg',
    });
    await userEvent.click(within(dialogo).getByRole('button', { name: 'Cancelar' }));
    await waitFor(() =>
      expect(
        screen.queryByRole('dialog', { name: 'Descartar documento guia-proveedor.jpg' }),
      ).not.toBeInTheDocument(),
    );

    await userEvent.click(screen.getByRole('button', { name: 'Descartar documento' }));
    const confirmar = await screen.findByRole('dialog', {
      name: 'Descartar documento guia-proveedor.jpg',
    });
    await userEvent.click(within(confirmar).getByRole('button', { name: 'Descartar documento' }));
    expect(
      await screen.findByRole('heading', { level: 1, name: 'Documentos OCR' }),
    ).toBeInTheDocument();
  });

  it('un Almacenero y un Vendedor acceden a los documentos', async () => {
    const { unmount } = renderApp('/documentos', 'Almacenero');
    expect(
      await screen.findByRole('heading', { level: 1, name: 'Documentos OCR' }),
    ).toBeInTheDocument();
    unmount();

    renderApp('/documentos/procesar', 'Vendedor');
    expect(
      await screen.findByRole('heading', { level: 1, name: 'Procesar documento' }),
    ).toBeInTheDocument();
  });

  it('un Reponedor no accede a los documentos', async () => {
    renderApp('/documentos/procesar', 'Reponedor');

    // Redirige al primer módulo visible para el rol: Alertas de stock.
    expect(
      await screen.findByRole('heading', { level: 1, name: 'Alertas de stock' }),
    ).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Procesar documento' })).not.toBeInTheDocument();
  });

  it.each([
    ['la lista', '/documentos', 'Almacenero', 'Documentos OCR'],
    ['el procesamiento', '/documentos/procesar', 'Almacenero', 'Procesar documento'],
    ['la revisión', '/documentos/5/revision', 'Almacenero', 'Revisión del OCR'],
  ])('%s no tiene violaciones de accesibilidad', async (_, ruta, rol, titulo) => {
    renderApp(ruta, rol as 'Almacenero');
    await screen.findByRole('heading', { level: 1, name: titulo });

    expect(
      await axe(document.body, { rules: { 'color-contrast': { enabled: false } } }),
    ).toHaveNoViolations();
  });
});
