import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { axe } from 'vitest-axe';
import * as matchers from 'vitest-axe/matchers';
import { afterEach, describe, expect, it } from 'vitest';
import { renderApp } from '@/test/renderApp';
import { RECEPCIONES } from './api';

expect.extend(matchers);
afterEach(() => window.sessionStorage.clear());

const guia = new File(['x'], 'guia-proveedor.jpg', { type: 'image/jpeg' });

/** Continúa desde el paso de la orden de compra cuando la orden ya está cargada. */
async function continuar() {
  const boton = await screen.findByRole('button', { name: 'Continuar' });
  await waitFor(() => expect(boton).toBeEnabled());
  await userEvent.click(boton);
}

/** Elige la orden OC-0087 y carga la guía; espera las líneas precargadas por el OCR simulado. */
async function cargarDocumento() {
  renderApp('/recepciones/nueva?oc=OC-0087', 'Almacenero');
  await continuar();
  const input = document.querySelector<HTMLInputElement>('input[type="file"]:not([capture])')!;
  await userEvent.upload(input, guia);
  await screen.findByRole('table', { name: 'Líneas extraídas' }, { timeout: 4000 });
}

/** Asocia el producto de la línea sin coincidencia (ALAMBRE REC. N16) con el SKU indicado. */
async function asociarAlambre(sku: RegExp) {
  const select = screen.getByRole('combobox', { name: 'Producto para ALAMBRE REC. N16' });
  await userEvent.click(select);
  // Sin escribir solo se ofrecen los productos relacionados con el texto: se busca por el código.
  await userEvent.type(select, sku.source.replace(/\\/g, ''));
  // Cada Select monta su propia lista; se elige la opción de la lista de este campo.
  const lista = document.getElementById(select.getAttribute('aria-controls')!)!;
  await userEvent.click(await within(lista).findByRole('option', { name: sku, hidden: true }));
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

  it('no ofrece escanear una guía desde el listado: el OCR se usa dentro de una orden', async () => {
    renderApp('/recepciones', 'Almacenero');

    expect(await screen.findByText('R-0012')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Escanear guía/ })).not.toBeInTheDocument();
  });

  it('desde el detalle de una orden la deja asociada, sin lista para elegir otra', async () => {
    renderApp('/recepciones/nueva?oc=OC-0087', 'Almacenero');

    expect(await screen.findByText('Orden asociada')).toBeInTheDocument();
    expect(
      screen.queryByRole('table', { name: 'Órdenes de compra para recibir' }),
    ).not.toBeInTheDocument();
  });

  it('desde Nueva recepción muestra todas las órdenes para elegir', async () => {
    renderApp('/recepciones/nueva', 'Almacenero');

    expect(
      await screen.findByRole('table', { name: 'Órdenes de compra para recibir' }),
    ).toBeInTheDocument();
    expect(screen.queryByText('Orden asociada')).not.toBeInTheDocument();
  });

  it('precarga las líneas de la guía y bloquea el uso de los datos hasta asociar la sin coincidencia', async () => {
    await cargarDocumento();

    expect(screen.getByText('Datos precargados desde el documento')).toBeInTheDocument();
    expect(screen.getByText('Sin coincidencia')).toBeInTheDocument();
    expect(screen.getByText('Asociar un producto.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Usar datos de la guía' })).toBeDisabled();
  });

  it('explica el bloqueo en la barra de acciones y resume las líneas', async () => {
    await cargarDocumento();

    const barra = screen.getByRole('region', { name: 'Documento de la recepción' });
    expect(within(barra).getByRole('status')).toHaveTextContent(
      'Falta asociar 1 línea a un producto del catálogo.',
    );
    expect(barra).toHaveTextContent('3 líneas');
    expect(barra).toHaveTextContent('1 sin asociar');
    expect(within(barra).getByRole('button', { name: 'Usar datos de la guía' })).toBeDisabled();
  });

  it('el paso del documento no tiene violaciones de accesibilidad', async () => {
    await cargarDocumento();

    expect(
      await axe(document.body, { rules: { 'color-contrast': { enabled: false } } }),
    ).toHaveNoViolations();
  });

  it('iguala lo recibido al pedido de una línea con su botón', async () => {
    await cargarDocumento();
    await asociarAlambre(/CLA-008/);
    await userEvent.click(screen.getByRole('button', { name: 'Usar datos de la guía' }));

    // Pendiente: PIN-005 (60) sin recibir; FIE-003 no está en la orden y sobra 40.
    expect(screen.getByText('Faltan 60')).toBeInTheDocument();
    expect(screen.getByText('Sobran 40')).toBeInTheDocument();
    expect(screen.getAllByText('Completa')).toHaveLength(2);

    await userEvent.click(
      screen.getByRole('button', { name: 'Igual al pedido, Pintura látex blanco, balde 4 L' }),
    );

    expect(screen.queryByText('Faltan 60')).not.toBeInTheDocument();
    expect(screen.getAllByText('Completa')).toHaveLength(3);
  });

  it('al cambiar la ubicación que recibe, el conteo muestra y suma lo recibido en cada una', async () => {
    await cargarDocumento();
    await asociarAlambre(/CLA-008/);
    await userEvent.click(screen.getByRole('button', { name: 'Usar datos de la guía' }));

    const nombre = 'Pintura látex blanco, balde 4 L';
    await userEvent.click(screen.getByRole('button', { name: `Igual al pedido, ${nombre}` }));
    const enAlmacen1 = screen.getByLabelText(
      `Cantidad recibida en Almacén 1 de ${nombre}`,
    ) as HTMLInputElement;
    expect(Number(enAlmacen1.value)).toBeGreaterThan(0);
    expect(screen.queryByRole('columnheader', { name: 'Total recibido' })).not.toBeInTheDocument();

    // La vista pasa a Almacén 2: arranca en 0 y lo que se registre allí se suma al total de la línea.
    await userEvent.click(screen.getByRole('combobox', { name: /Ubicación que recibe/ }));
    await userEvent.click(await screen.findByRole('option', { name: 'Almacén 2', hidden: true }));
    const enAlmacen2 = screen.getByLabelText(
      `Cantidad recibida en Almacén 2 de ${nombre}`,
    ) as HTMLInputElement;
    expect(enAlmacen2.value).toBe('0');
    await userEvent.type(enAlmacen2, '15');

    expect(await screen.findByRole('columnheader', { name: 'Total recibido' })).toBeInTheDocument();
    expect(screen.getByText(/Recibido por ubicación:.*Almacén 2 15/)).toBeInTheDocument();

    // Al volver a Almacén 1 se conserva lo registrado allí.
    await userEvent.click(screen.getByRole('combobox', { name: /Ubicación que recibe/ }));
    await userEvent.click(await screen.findByRole('option', { name: 'Almacén 1', hidden: true }));
    expect(
      (screen.getByLabelText(`Cantidad recibida en Almacén 1 de ${nombre}`) as HTMLInputElement)
        .value,
    ).toBe(enAlmacen1.value);
  });

  it('confirma la recepción tras asociar el producto, anotar el sobrante y aceptar las consecuencias', async () => {
    await cargarDocumento();
    const antes = RECEPCIONES.length;

    await asociarAlambre(/CLA-008/);
    await userEvent.click(screen.getByRole('button', { name: 'Usar datos de la guía' }));
    await userEvent.type(
      screen.getByRole('textbox', { name: 'Nota del sobrante' }),
      'Material de otra obra',
    );
    await userEvent.click(screen.getByRole('button', { name: 'Confirmar recepción' }));

    const dialog = await screen.findByRole('dialog', { name: 'Confirmar recepción OC-0087' });
    expect(dialog).toHaveTextContent('3 movimientos de entrada en el Kardex de Almacén 1');
    expect(dialog).toHaveTextContent('recepción parcial');
    await userEvent.click(within(dialog).getByRole('button', { name: 'Confirmar recepción' }));

    const constancia = await screen.findByRole('status', { name: /Recepción R-\d+ registrada/ });
    expect(within(constancia).getByText('Validada')).toBeInTheDocument();
    const tabla = within(constancia).getByRole('table', { name: 'Existencia antes y después' });
    expect(tabla).toHaveTextContent('Cemento Sol tipo I');
    // Pintura: se esperaban 60 y no se recibió nada, de modo que queda una línea por recibir.
    expect(constancia).toHaveTextContent('quedan por recibir 60 unidades en 1 línea');
    expect(
      within(constancia).getByRole('button', { name: 'Ver movimientos en el Kardex' }),
    ).toBeEnabled();
    expect(RECEPCIONES).toHaveLength(antes + 1);
    RECEPCIONES.shift();
  }, 15000);
});
