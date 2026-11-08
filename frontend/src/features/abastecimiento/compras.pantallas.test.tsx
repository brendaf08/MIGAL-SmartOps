import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it } from 'vitest';
import { axe } from 'vitest-axe';
import * as matchers from 'vitest-axe/matchers';
import { renderApp } from '@/test/renderApp';

expect.extend(matchers);
afterEach(() => window.sessionStorage.clear());

describe('Órdenes de compra, listado (P25)', () => {
  it('lista las órdenes con su avance de recepción y filtra por estado', async () => {
    const { container } = renderApp('/compras', 'Reponedor');

    await screen.findByText('OC-0086');
    const tabla = screen.getByRole('table', { name: 'Órdenes de compra' });
    expect(within(tabla).getByText('180 de 300 unidades')).toBeInTheDocument();

    await userEvent.click(await screen.findByRole('tab', { name: /Recepción parcial/ }));
    expect(screen.queryByText('OC-0087')).not.toBeInTheDocument();
    expect(screen.getByText('OC-0086')).toBeInTheDocument();

    expect(
      await axe(container, {
        rules: { 'color-contrast': { enabled: false }, region: { enabled: false } },
      }),
    ).toHaveNoViolations();
  });

  it('un Almacenero no accede al módulo y se redirige al inicio de su rol', async () => {
    renderApp('/compras', 'Almacenero');

    await waitFor(() =>
      expect(
        screen.queryByRole('heading', { level: 1, name: 'Órdenes de compra' }),
      ).not.toBeInTheDocument(),
    );
    expect(screen.queryByText('OC-0086')).not.toBeInTheDocument();
  });
});

describe('Órdenes de compra, nueva orden (P26)', () => {
  it('bloquea la acción con el motivo visible mientras falta el proveedor y las líneas', async () => {
    renderApp('/compras/nueva', 'Reponedor');

    const accion = await screen.findByRole('button', { name: 'Guardar orden de compra' });
    expect(accion).toBeDisabled();
    expect(screen.getByText('Seleccionar un proveedor para crear la orden.')).toBeInTheDocument();

    await userEvent.click(screen.getByRole('combobox', { name: /Proveedor/ }));
    await userEvent.click(
      await screen.findByRole('option', { name: 'Aceros del Sur S.A.', hidden: true }),
    );
    expect(
      screen.getByText('Agregar al menos un producto para crear la orden.'),
    ).toBeInTheDocument();
    expect(accion).toBeDisabled();
  });

  it('no permite elegir un proveedor inactivo y ofrece el registro de proveedor', async () => {
    renderApp('/compras/nueva', 'Reponedor');

    await userEvent.click(await screen.findByRole('combobox', { name: /Proveedor/ }));
    const inactivo = await screen.findByRole('option', {
      name: 'Cementos Arequipa E.I.R.L. (Inactivo)',
      hidden: true,
    });
    expect(
      inactivo.getAttribute('data-combobox-disabled') ?? inactivo.getAttribute('aria-disabled'),
    ).not.toBeNull();
    expect(screen.getByRole('link', { name: 'Registrar proveedor' })).toHaveAttribute(
      'href',
      '/proveedores/nuevo',
    );
  });

  it('valida la fecha, la cantidad y el precio junto a los campos', async () => {
    renderApp('/compras/nueva', 'Reponedor');

    await userEvent.click(await screen.findByRole('combobox', { name: /Proveedor/ }));
    await userEvent.click(
      await screen.findByRole('option', { name: 'Aceros del Sur S.A.', hidden: true }),
    );
    fireEvent.change(screen.getByLabelText(/Fecha de la orden/), {
      target: { value: '2020-01-01' },
    });
    await userEvent.click(screen.getByRole('button', { name: 'Agregar producto' }));
    await userEvent.click(screen.getByRole('button', { name: 'Guardar orden de compra' }));

    expect(await screen.findByText('La fecha no puede ser anterior a hoy.')).toBeInTheDocument();
    expect(screen.getByText(/Revisar cada línea/)).toBeInTheDocument();
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('preagrega la línea del producto recibido en la URL', async () => {
    renderApp('/compras/nueva?sku=CEM-001', 'Reponedor');

    const producto = await screen.findByRole('combobox', { name: 'Producto de la línea 1' });
    expect(producto).toHaveValue('CEM-001, Cemento Sol tipo I, bolsa 42.5 kg');
    expect(screen.getByLabelText('Cantidad de la línea 1')).toBeInTheDocument();
  });

  it('no tiene violaciones de accesibilidad', async () => {
    const { container } = renderApp('/compras/nueva', 'Reponedor');
    await screen.findByRole('heading', { level: 1, name: 'Nueva orden de compra' });

    expect(
      await axe(container, {
        rules: { 'color-contrast': { enabled: false }, region: { enabled: false } },
      }),
    ).toHaveNoViolations();
  });
});

describe('Órdenes de compra, flujo completo y detalle (P25, P26)', () => {
  it('crea una orden válida como Pendiente y muestra su total en el detalle', async () => {
    renderApp('/compras/nueva', 'Reponedor');

    await userEvent.click(await screen.findByRole('combobox', { name: /Proveedor/ }));
    await userEvent.click(
      await screen.findByRole('option', { name: 'Distribuidora Andina S.A.C.', hidden: true }),
    );
    await userEvent.click(screen.getByRole('button', { name: 'Agregar producto' }));
    await userEvent.click(screen.getByRole('combobox', { name: 'Producto de la línea 1' }));
    await userEvent.click(
      await screen.findByRole('option', {
        name: 'CEM-001, Cemento Sol tipo I, bolsa 42.5 kg',
        hidden: true,
      }),
    );
    await userEvent.clear(screen.getByLabelText('Cantidad de la línea 1'));
    await userEvent.type(screen.getByLabelText('Cantidad de la línea 1'), '100');
    await userEvent.clear(screen.getByLabelText('Precio de la línea 1'));
    await userEvent.type(screen.getByLabelText('Precio de la línea 1'), '26.5');

    await userEvent.click(screen.getByRole('button', { name: 'Guardar orden de compra' }));
    await userEvent.click(await screen.findByRole('button', { name: 'Crear orden de compra' }));

    expect(
      await screen.findByRole('heading', { level: 1, name: 'Orden de compra OC-0091' }),
    ).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Registrar recepción' })).toBeInTheDocument();
    // El importe aparece en la línea (subtotal) y en el pie (total).
    expect(screen.getAllByText(/2,650\.00/)).toHaveLength(2);
  }, 20000);

  it('muestra recibida y pendiente por línea en el detalle', async () => {
    renderApp('/compras/OC-0086', 'Reponedor');

    expect(
      await screen.findByRole('heading', { level: 1, name: 'Orden de compra OC-0086' }),
    ).toBeInTheDocument();
    const tabla = screen.getByRole('table', { name: 'Líneas de la orden OC-0086' });
    expect(within(tabla).getByText('180')).toBeInTheDocument();
    expect(within(tabla).getByText('120')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Registrar recepción' })).toBeInTheDocument();
  });

  it('una orden completada no ofrece la recepción', async () => {
    renderApp('/compras/OC-0084', 'Reponedor');

    expect(
      await screen.findByRole('heading', { level: 1, name: 'Orden de compra OC-0084' }),
    ).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Registrar recepción' })).not.toBeInTheDocument();
  });

  it('avisa cuando la orden no existe y ofrece volver al listado', async () => {
    renderApp('/compras/OC-9999', 'Reponedor');

    expect(
      await screen.findByText('La orden de compra no existe', undefined, { timeout: 4000 }),
    ).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Volver a las órdenes de compra' })).toHaveAttribute(
      'href',
      '/compras',
    );
  });

  it('el detalle no tiene violaciones de accesibilidad', async () => {
    const { container } = renderApp('/compras/OC-0086', 'Reponedor');
    await screen.findByRole('heading', { level: 1, name: 'Orden de compra OC-0086' });

    expect(
      await axe(container, {
        rules: { 'color-contrast': { enabled: false }, region: { enabled: false } },
      }),
    ).toHaveNoViolations();
  });
});
