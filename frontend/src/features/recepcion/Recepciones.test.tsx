import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it } from 'vitest';
import { renderApp } from '@/test/renderApp';
import { RECEPCIONES } from './api';

afterEach(() => window.sessionStorage.clear());

const guia = new File(['x'], 'guia-proveedor.jpg', { type: 'image/jpeg' });

/** Carga la guía y espera las líneas precargadas por el OCR simulado. */
async function cargarDocumento() {
  renderApp('/recepciones/nueva', 'Almacenero');
  const input = document.querySelector<HTMLInputElement>('input[type="file"]:not([capture])')!;
  await userEvent.upload(input, guia);
  await screen.findByRole('table', { name: 'Líneas extraídas' }, { timeout: 4000 });
}

describe('Recepciones', () => {
  it('lista el historial y ofrece iniciar una nueva recepción', async () => {
    renderApp('/recepciones', 'Almacenero');

    expect(await screen.findByText('R-0012')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Nueva recepción' }));
    expect(
      await screen.findByRole('heading', { level: 1, name: 'Nueva recepción' }),
    ).toBeInTheDocument();
  });

  it('precarga las líneas, marca la sin coincidencia y bloquea la confirmación hasta asociarla', async () => {
    await cargarDocumento();

    expect(screen.getByText('Datos precargados desde el documento')).toBeInTheDocument();
    expect(screen.getByText('Sin coincidencia')).toBeInTheDocument();
    expect(screen.getByText('Asociar un producto.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Confirmar recepción' })).toBeDisabled();
  });

  it('confirma la recepción tras asociar el producto y aceptar las consecuencias', async () => {
    await cargarDocumento();
    const antes = RECEPCIONES.length;

    const select = screen.getByRole('combobox', { name: 'Producto para ALAMBRE REC. N16' });
    await userEvent.click(select);
    // Cada Select monta su propia lista; se elige la opción de la lista de este campo.
    const lista = document.getElementById(select.getAttribute('aria-controls')!)!;
    await userEvent.click(
      await within(lista).findByRole('option', { name: /CLA-008/, hidden: true }),
    );
    await userEvent.click(screen.getByRole('button', { name: 'Confirmar recepción' }));

    const dialog = await screen.findByRole('dialog', { name: 'Confirmar recepción OC-0087' });
    expect(dialog).toHaveTextContent('3 movimientos de entrada en el Kardex del Almacén 1');
    expect(dialog).toHaveTextContent('recepción parcial');
    await userEvent.click(within(dialog).getByRole('button', { name: 'Confirmar recepción' }));

    expect(await screen.findByText(/Se registró la recepción/)).toBeInTheDocument();
    expect(RECEPCIONES).toHaveLength(antes + 1);
    RECEPCIONES.shift();
  });
});
