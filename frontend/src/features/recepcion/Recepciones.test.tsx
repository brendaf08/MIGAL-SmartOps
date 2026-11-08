import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { axe } from 'vitest-axe';
import * as matchers from 'vitest-axe/matchers';
import { afterEach, describe, expect, it } from 'vitest';
import { renderApp } from '@/test/renderApp';
import { RECEPCIONES } from './api';

expect.extend(matchers);
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

  it('explica el bloqueo en la barra de acciones y resume las líneas por estado', async () => {
    await cargarDocumento();

    const barra = screen.getByRole('region', { name: 'Resumen y confirmación de la recepción' });
    expect(within(barra).getByRole('status')).toHaveTextContent(
      'Falta asociar 1 línea a un producto del catálogo.',
    );
    expect(barra).toHaveTextContent('3 líneas');
    expect(barra).toHaveTextContent('1 sin asociar');
    expect(within(barra).getByRole('button', { name: 'Confirmar recepción' })).toBeDisabled();
    expect(screen.getByText('Sin asociar')).toBeInTheDocument();
  });

  it('la revisión con la barra de acciones no tiene violaciones de accesibilidad', async () => {
    await cargarDocumento();

    expect(
      await axe(document.body, { rules: { 'color-contrast': { enabled: false } } }),
    ).toHaveNoViolations();
  });

  it('iguala lo recibido al pedido de una línea con su botón', async () => {
    await cargarDocumento();

    expect(screen.getByText('Faltan 20')).toBeInTheDocument();
    expect(screen.getAllByText('Completa')).toHaveLength(1);

    // Solo la línea con diferencia y producto asociado admite igualar al pedido.
    const habilitados = screen
      .getAllByRole('button', { name: /^Igual al pedido/ })
      .filter((b) => !(b as HTMLButtonElement).disabled);
    expect(habilitados).toHaveLength(1);
    await userEvent.click(habilitados[0]!);

    expect(screen.queryByText('Faltan 20')).not.toBeInTheDocument();
    expect(screen.getAllByText('Completa')).toHaveLength(2);
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
