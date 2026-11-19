import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { axe } from 'vitest-axe';
import * as matchers from 'vitest-axe/matchers';
import { afterEach, describe, expect, it } from 'vitest';
import { renderApp } from '@/test/renderApp';

expect.extend(matchers);
afterEach(() => window.sessionStorage.clear());

/** Pulsa "Continuar" cuando la orden elegida ya está disponible. */
async function continuar() {
  const boton = await screen.findByRole('button', { name: 'Continuar' });
  await waitFor(() => expect(boton).toBeEnabled());
  await userEvent.click(boton);
}

/** Reemplaza la cantidad recibida de una línea del conteo. */
async function recibir(linea: string, cantidad: number) {
  const campo = screen.getByRole('textbox', { name: `Cantidad recibida en Almacén 1 de ${linea}` });
  await userEvent.clear(campo);
  await userEvent.type(campo, String(cantidad));
}

describe('Recepción desde una orden de compra', () => {
  it('elegir una orden pendiente muestra los productos esperados', async () => {
    renderApp('/recepciones/nueva', 'Almacenero');

    await userEvent.click(await screen.findByRole('button', { name: 'Elegir orden OC-0088' }));

    const productos = await screen.findByRole('table', { name: 'Productos esperados de OC-0088' });
    expect(within(productos).getAllByText('LAD-002')).not.toHaveLength(0);
    expect(screen.queryByRole('status')).not.toBeInTheDocument();
    await continuar();
    expect(await screen.findByText('La carga no modifica el inventario')).toBeInTheDocument();
  });

  it('una recepción parcial deja la orden en recepción parcial y una segunda la completa', async () => {
    renderApp('/recepciones/nueva?oc=OC-0088', 'Almacenero');
    await continuar();
    await userEvent.click(await screen.findByRole('button', { name: 'Registrar sin documento' }));

    const confirmacion = screen.getByRole('button', { name: 'Confirmar recepción' });
    expect(confirmacion).toBeDisabled();
    expect(screen.getByText('Registrar una cantidad recibida mayor que cero.')).toBeInTheDocument();
    await recibir('Ladrillo King Kong 18 huecos', 60);
    expect(confirmacion).toBeEnabled();
    await userEvent.click(screen.getByRole('button', { name: 'Confirmar recepción' }));
    await userEvent.click(
      within(await screen.findByRole('dialog', { name: 'Confirmar recepción OC-0088' })).getByRole(
        'button',
        { name: 'Confirmar recepción' },
      ),
    );

    const parcial = await screen.findByRole('status', { name: /Recepción R-\d+ registrada/ });
    expect(parcial).toHaveTextContent('Recepción parcial: quedan por recibir');
    expect(parcial).toHaveTextContent('en 1 línea');

    await userEvent.click(
      within(parcial).getByRole('button', { name: 'Registrar otra recepción' }),
    );
    await continuar();
    await userEvent.click(await screen.findByRole('button', { name: 'Registrar sin documento' }));
    await recibir('Ladrillo King Kong 18 huecos', 3940);
    await userEvent.click(screen.getByRole('button', { name: 'Confirmar recepción' }));
    await userEvent.click(
      within(await screen.findByRole('dialog', { name: 'Confirmar recepción OC-0088' })).getByRole(
        'button',
        { name: 'Confirmar recepción' },
      ),
    );

    const completa = await screen.findByRole('status', { name: /Recepción R-\d+ registrada/ });
    expect(completa).toHaveTextContent('Orden completada');
    expect(
      within(completa).queryByRole('button', { name: 'Registrar otra recepción' }),
    ).not.toBeInTheDocument();
  }, 15000);

  it('no deja recibir más de lo pendiente: el tope es duro y no hay sobrantes', async () => {
    renderApp('/recepciones/nueva?oc=OC-0090', 'Almacenero');
    await continuar();
    await userEvent.click(await screen.findByRole('button', { name: 'Registrar sin documento' }));

    // Se pide 20 y se intenta recibir 25: el sistema deja el máximo permitido.
    await recibir('Cable THW 12 AWG, rollo 100 m', 25);

    const campo = screen.getByRole('textbox', {
      name: 'Cantidad recibida en Almacén 1 de Cable THW 12 AWG, rollo 100 m',
    }) as HTMLInputElement;
    expect(Number(campo.value)).toBeLessThanOrEqual(20);
    expect(screen.queryByText(/Sobran/)).not.toBeInTheDocument();
    expect(screen.queryByRole('textbox', { name: 'Nota del sobrante' })).not.toBeInTheDocument();
  });

  it('sin documento el conteo muestra solo el producto del catálogo, fijo y sin coincidencia', async () => {
    renderApp('/recepciones/nueva?oc=OC-0090', 'Almacenero');
    await continuar();
    await userEvent.click(await screen.findByRole('button', { name: 'Registrar sin documento' }));

    const tabla = await screen.findByRole('table', { name: 'Conteo de la recepción' });
    expect(within(tabla).getByRole('columnheader', { name: 'Producto' })).toBeInTheDocument();
    expect(
      within(tabla).queryByRole('columnheader', { name: 'Texto extraído' }),
    ).not.toBeInTheDocument();
    expect(
      within(tabla).queryByRole('columnheader', { name: 'Coincidencia' }),
    ).not.toBeInTheDocument();
    expect(within(tabla).queryByRole('combobox')).not.toBeInTheDocument();
    // Sin documento nada llega precargado: todo parte en 0 y el botón completa lo pedido.
    const campo = screen.getByRole('textbox', {
      name: 'Cantidad recibida en Almacén 1 de Cable THW 12 AWG, rollo 100 m',
    }) as HTMLInputElement;
    // El vacío equivale a 0: no se muestra un 0 que estorbe al escribir.
    expect(campo.value).toBe('');
    await userEvent.click(screen.getByRole('button', { name: 'Todo lo pedido llegó a Almacén 1' }));
    expect(Number(campo.value)).toBeGreaterThan(0);
  });

  it('cada paso de la recepción no tiene violaciones de accesibilidad', async () => {
    renderApp('/recepciones/nueva?oc=OC-0086', 'Almacenero');
    await screen.findByRole('button', { name: 'Continuar' });
    const sinViolaciones = () =>
      axe(document.body, { rules: { 'color-contrast': { enabled: false } } });

    expect(await sinViolaciones()).toHaveNoViolations();
    await continuar();
    await screen.findByRole('button', { name: 'Registrar sin documento' });
    expect(await sinViolaciones()).toHaveNoViolations();
    await userEvent.click(screen.getByRole('button', { name: 'Registrar sin documento' }));
    await screen.findByRole('table', { name: 'Conteo de la recepción' });
    expect(await sinViolaciones()).toHaveNoViolations();
  }, 15000);

  it('un documento revisado en la pantalla de documentos llega precargado al paso del documento', async () => {
    renderApp('/recepciones/nueva?oc=OC-0087&documento=4', 'Almacenero');
    await continuar();

    expect(await screen.findByRole('table', { name: 'Líneas extraídas' })).toBeInTheDocument();
    expect(screen.getByText('Datos precargados desde el documento')).toBeInTheDocument();
    const barra = screen.getByRole('region', { name: 'Documento de la recepción' });
    expect(within(barra).getByRole('status')).toHaveTextContent('Falta asociar 1 línea');
  });
});
